package com.hmall.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hmall.api.client.EmbeddingClient;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ImageEmbeddingDTO;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.domain.R;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.search.domain.po.ItemDOC;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.PageVO;
import com.hmall.search.service.ISearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ：蒋青江
 * @date ：2025/8/1 15:18
 * @description ：
 */
@Service
@Slf4j
public class SearchServiceImpl implements ISearchService {
    private final String INDEX_NAME = "items";
    @Autowired
    private ItemClient itemClient;

    @Autowired
    private EmbeddingClient embeddingClient;
    private final ElasticsearchClient client;

    public SearchServiceImpl() {
        String apiKey = "MW1RMnJaZ0J2SWZpRG9DaFJjNUM6N0dOOXI1VFIxbkVYZE1XR3dPZlh6UQ==";
        String serverUrl = "http://1.116.248.49:9200";
        Rest5Client restClient = null;
        try {
            restClient = Rest5Client
                    .builder(HttpHost.create(serverUrl))
                    .setDefaultHeaders(new Header[]{
                            new BasicHeader("Authorization", "ApiKey " + apiKey)
                    })
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        Rest5ClientTransport clientTransport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper(mapper));

        this.client = new ElasticsearchClient(clientTransport);
    }

    // 保存更新的商品
    @Override
    public void saveItemById(Long id) {
        try {
            // 根据id查询商品
            R<ItemDTO> res = itemClient.queryItemById(id);
            ItemDTO itemDTO = res.getData();

            if (itemDTO != null) {
                // 转化为ItemDoc
                ItemDOC itemDOC = BeanUtils.copyBean(itemDTO, ItemDOC.class);

                itemDOC.setNameVector(embeddingClient.getNameEmbedding(Map.of("name", itemDTO.getName())).getNameVector());

                itemDOC.setImageVector(embeddingClient.getImageEmbedding(Map.of("image", itemDTO.getImage())).getImageVector());


                // 转化为 Json
                IndexRequest<ItemDOC> indexRequest = IndexRequest.of(request -> request
                        .index(INDEX_NAME)
                        .id(itemDOC.getId().toString())
                        .document(itemDOC));
                // 发送请求
                client.index(indexRequest);
                log.info("更新es中的商品成功！参数：商品id" + id);

            }
        } catch (IOException e) {
            throw new RuntimeException("更新es中的商品失败！参数：商品id" + id, e);
        }
    }


    // 删除es中商品
    @Override
    public void deleteItemById(Long id) {
        try {
            DeleteRequest deleteRequest = DeleteRequest.of(req -> req
                    .index(INDEX_NAME).id(id.toString()));
            client.delete(deleteRequest);
        } catch (IOException e) {
            throw new RuntimeException("删除es中的商品商品！参数：商品id" + id, e);
        }
    }

    // 搜索商品
    @Override
    public PageVO<ItemDOC> search(ItemPageQuery query) {
        PageVO<ItemDOC> pageVO = PageVO.empty(0L, 0L);

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        boolean isHighlight = false;
        // 搜索key
        if (StrUtil.isNotBlank(query.getKey())) {

            // 查询条件过短走倒排索引
            if (query.getKey().length() < 8) {
                MatchQuery matchQuery = MatchQuery.of(m -> m
                        .field("name")
                        .query(query.getKey())
                );
                boolQueryBuilder.must(matchQuery);
                // 高亮
                isHighlight = true;
            } else {
                // 1. 获取搜索关键词的向量
                Map<String, String> params = new HashMap<>();
                params.put("name", query.getKey());
                List<Float> nameVector = embeddingClient.getNameEmbedding(params).getNameVector();

                if (CollUtils.isNotEmpty(nameVector)) {
                    // 2. 构建向量相似度查询
                    KnnQuery knnQuery = KnnQuery.of(k -> k
                            .field("nameVector")
                            .queryVector(nameVector)
                            .k(100)
                            .numCandidates(1000));
                    boolQueryBuilder.must(knnQuery);
                } else {
                    // 降级为倒排索引查询
                    MatchQuery matchQuery = MatchQuery.of(m -> m
                            .field("name")
                            .query(query.getKey())
                    );
                    boolQueryBuilder.must(matchQuery);
                    // 高亮
                    isHighlight = true;
                }
            }


        }
        // 分类
        if (StrUtil.isNotBlank(query.getCategory())) {
//            boolQueryBuilder.filter(QueryBuilders.termQuery("category", query.getCategory()));
            boolQueryBuilder.filter(QueryBuilders.term()
                    .field("category")
                    .value(query.getCategory()).build());
        }
        // 品牌
        if (StrUtil.isNotBlank(query.getBrand())) {
//            boolQueryBuilder.filter(QueryBuilders.termQuery("brand", query.getBrand()));
            boolQueryBuilder.filter(QueryBuilders.term()
                    .field("brand")
                    .value(query.getBrand()).build());
        }
        //  价格
        if (query.getMinPrice() != null) {
            // boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
            TermRangeQuery termRangeQuery = TermRangeQuery.of(q -> q
                    .field("price")
                    .gte(String.valueOf(query.getMinPrice())));

            boolQueryBuilder.filter(termRangeQuery._toRangeQuery());
        }
        if (query.getMaxPrice() != null) {
//            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
            TermRangeQuery termRangeQuery = TermRangeQuery.of(q -> q
                    .field("price")
                    .lte(String.valueOf(query.getMaxPrice())));

            boolQueryBuilder.filter(termRangeQuery._toRangeQuery());
        }


        Highlight.Builder highlightBuilder = new Highlight.Builder();
        // 设置高亮
        if (isHighlight) {
            HighlightField highlightField = HighlightField.of(h -> h
                    .preTags("<em>")
                    .postTags("</em>"));

            highlightBuilder.fields("name", highlightField);
        }

        // 分页
        int pageNo = query.getPageNum();
        int pageSize = query.getPageSize();
        int from = (pageNo - 1) * pageSize;
//        searchRequest.source().from((pageNo - 1) * pageSize).size(pageSize);
        Query queryBuilder = boolQueryBuilder.build()._toQuery();


//        searchRequest.source().query(boolQueryBuilder);
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .from(from)
                .size(pageSize)
                .query(queryBuilder)
                // 排除不需要的向量字段，只返回业务所需字段
                .source(s -> s.filter(f -> f.excludes("nameVector", "imageVector")));

        // 设置排序
        if (StrUtil.isNotBlank(query.getSortBy())) {
            searchRequestBuilder.sort(s -> s
                    .field(f -> f
                            .field(query.getSortBy())
                            .order(query.getIsAsc() ? SortOrder.Asc : SortOrder.Desc)
                    )
            );
        }

        // 设置高亮
        if (isHighlight) {
            searchRequestBuilder.highlight(highlightBuilder.build());
        }

        // 发起请求
        try {
            SearchResponse<ItemDOC> searchResponse = client.search(
                    searchRequestBuilder.build(),
                    ItemDOC.class // 自动反序列化文档类型
            );
            HitsMetadata<ItemDOC> hits = searchResponse.hits();
            long total = hits.total().value();
            // 设置总数和总页数
            pageVO.setTotal(total);
            pageVO.setPages(total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
            ArrayList<ItemDOC> itemDOCS = new ArrayList<>(pageSize);
            for (Hit<ItemDOC> hit : hits.hits()) {
                ItemDOC itemDOC = hit.source();
                // 设置高亮
                if (isHighlight) {
                    List<String> list = hit.highlight().get("name");
                    if (list != null) {
                        itemDOC.setName(list.get(0));
                    }
                }
                itemDOCS.add(itemDOC);
            }
            pageVO.setList(itemDOCS);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return pageVO;
    }


    // 搜索商品分类、品牌列表
    @Override
    public Map<String, List<String>> filters(ItemPageQuery query) {
        // 当没有选择分类、品牌时，才更新
        if (StrUtil.isBlank(query.getCategory()) || StrUtil.isBlank(query.getBrand())) {
            HashMap<String, List<String>> map = new HashMap<>();
            // 创建查询请求
            // 不返回文档
            SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                    .index(INDEX_NAME)
                    .size(0);
            // 是否聚合查询分类
            boolean isNeedCategoryAgg = StrUtil.isBlank(query.getCategory());
            // 是否聚合查询品牌
            boolean isNeedBrandAgg = StrUtil.isBlank(query.getBrand());

            // 设置搜索关键字
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
            if (StrUtil.isNotBlank(query.getKey())) {
                boolQueryBuilder.must(MatchQuery.of(m -> m.field("name").query(query.getKey())));
            }

            // 设置过滤品牌和分类
            if (!isNeedCategoryAgg) {
                boolQueryBuilder.filter(MatchQuery.of(m -> m.field("category").query(query.getCategory())));
            }
            if (!isNeedBrandAgg) {
                boolQueryBuilder.filter(MatchQuery.of(m -> m.field("brand").query(query.getBrand())));
            }

            // 设置过滤价格
            if (query.getMinPrice() != null) {
//                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
                TermRangeQuery termRangeQuery = TermRangeQuery.of(q -> q
                        .field("price")
                        .gte(String.valueOf(query.getMinPrice())));
                boolQueryBuilder.filter(termRangeQuery._toRangeQuery());
            }
            if (query.getMaxPrice() != null) {
//                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
                boolQueryBuilder.filter(TermRangeQuery.of(q -> q
                        .field("price")
                        .lte(String.valueOf(query.getMaxPrice())))._toRangeQuery());
            }


            // 设置分类聚合
            if (isNeedCategoryAgg) {
                // 构建terms聚合，统计category字段的分布
                TermsAggregation categoryAgg = TermsAggregation.of(t -> t
                        .field("category")  // 聚合字段
                        .size(20)           // 最多返回20个分类
                );
                // 添加到请求中，指定聚合名称为"categoryAgg"
                searchRequestBuilder.aggregations("categoryAgg", categoryAgg);

            }
            // 设置品牌聚合
            if (isNeedBrandAgg) {
                // 构建terms聚合，统计brand字段的分布
                TermsAggregation brandAgg = TermsAggregation.of(t -> t
                        .field("brand")    // 聚合字段
                        .size(20)          // 最多返回20个品牌
                );
                // 添加到请求中，指定聚合名称为"brandAgg"
                searchRequestBuilder.aggregations("brandAgg", brandAgg);
            }

            SearchRequest searchRequest = searchRequestBuilder.query(boolQueryBuilder.build()._toQuery()).build();

            // 发起请求
            try {
                SearchResponse searchResponse = client.search(searchRequest);
                // 5. 解析聚合结果
                Map<String, Aggregate> aggregations = searchResponse.aggregations();
                if (aggregations != null) {
                    Aggregate categoryAgg = aggregations.get("categoryAgg");
//
//                    Terms categoryAgg = aggregations.get("categoryAgg");
                    if (categoryAgg != null) {
                        StringTermsAggregate sterms = categoryAgg.sterms();
                        List<String> categoryList = sterms.buckets()
                                .array()         // 拿到 List<StringTermsBucket>
                                .stream()
                                .map(b -> b.key().stringValue()) // 桶的 key
                                .collect(Collectors.toList());

                        map.put("category", categoryList);
                    }
                    Aggregate brandAgg = aggregations.get("brandAgg");
                    if (brandAgg != null) {
                        StringTermsAggregate sterms = brandAgg.sterms();
                        List<String> brandList = sterms.buckets()
                                .array()         // 拿到 List<StringTermsBucket>
                                .stream()
                                .map(b -> b.key().stringValue()) // 桶的 key
                                .collect(Collectors.toList());
                        map.put("brand", brandList);
                    }
                }
                return map;

            } catch (IOException e) {
                System.out.println("查询分类、品牌聚合数据失败！" + e);
            }
        }
        return CollUtils.emptyMap();
    }

    @Override
    public PageVO<ItemDOC> imageSearch(Map<String,String> image) {
        ImageEmbeddingDTO embeddingDTO = embeddingClient.getImageEmbedding(Map.of("image", image.get("url")));
        if(embeddingDTO == null){
            return PageVO.empty(0L, 0L);
        }
//        System.out.println("图片向量：" + embeddingDTO.getImageVector());

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        // 2. 构建向量相似度查询
        KnnQuery knnQuery = KnnQuery.of(k -> k
                .field("imageVector")
                .queryVector(embeddingDTO.getImageVector())
                .k(100)
                .numCandidates(1000));
        boolQueryBuilder.must(knnQuery);
        Query queryBuilder = boolQueryBuilder.build()._toQuery();

        int pageSize = 20;
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .query(queryBuilder)
                .from(0)
                .size(pageSize)
                // 排除不需要的向量字段，只返回业务所需字段
                .source(s -> s.filter(f -> f.excludes("nameVector", "imageVector")));

        PageVO<ItemDOC> pageVO = PageVO.empty(0L, 0L);
        // 发起请求
        try {
            SearchResponse<ItemDOC> searchResponse = client.search(
                    searchRequestBuilder.build(),
                    ItemDOC.class // 自动反序列化文档类型
            );
            HitsMetadata<ItemDOC> hits = searchResponse.hits();
            long total = hits.total().value();
            // 设置总数和总页数
            pageVO.setTotal(total);
            pageVO.setPages(total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
            ArrayList<ItemDOC> itemDOCS = new ArrayList<>(pageSize);
            for (Hit<ItemDOC> hit : hits.hits()) {
                ItemDOC itemDOC = hit.source();
                itemDOCS.add(itemDOC);
            }
            pageVO.setList(itemDOCS);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return pageVO;
    }
}
