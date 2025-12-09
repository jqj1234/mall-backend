package com.hmall.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.user.domain.dto.AddressDTO;
import com.hmall.user.domain.po.Address;
import com.hmall.user.mapper.AddressMapper;
import com.hmall.user.service.IAddressService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements IAddressService {
    @Override
    public void addOrUpdateAddress(AddressDTO addressDTO) {
        // 1.转po
        Address address = BeanUtils.copyBean(addressDTO, Address.class);
        // 2.设置是否默认
        if (address.getIsDefault() == 1) {
            // 3.取消其他默认
            this.lambdaUpdate()
                    .eq(Address::getUserId, UserContext.getUser())
                    .set(Address::getIsDefault, 0)
                    .update();
        }
        if(address.getId() != null){
            // 3.修改
            this.updateById(address);
            return;
        }
        // 2.设置用户id
        address.setUserId(UserContext.getUser());
        // 3.保存
        this.save(address);

    }
}
