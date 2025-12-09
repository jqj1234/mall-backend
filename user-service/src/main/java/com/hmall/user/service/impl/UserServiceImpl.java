package com.hmall.user.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.domain.R;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.UserContext;
import com.hmall.user.config.CommonConstant;
import com.hmall.user.config.JwtProperties;
import com.hmall.user.domain.dto.LoginFormDTO;
import com.hmall.user.domain.po.User;
import com.hmall.user.domain.vo.UserLoginVO;
import com.hmall.user.enums.UserStatus;
import com.hmall.user.mapper.UserMapper;
import com.hmall.user.service.IUserService;
import com.hmall.user.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final PasswordEncoder passwordEncoder;

    private final JwtTool jwtTool;

    private final JwtProperties jwtProperties;
    private final IdentifierGenerator identifierGenerator;
    private final StringRedisTemplate stringRedisTemplate;


    @Override
    public R<UserLoginVO> login(LoginFormDTO loginDTO) {
        //校验验证码和sessionid是否有效
        if (StringUtils.isBlank(loginDTO.getCode()) || StringUtils.isBlank(loginDTO.getSessionId())) {
            return R.error("验证码不能为空");
        }
        // 根据key从redis中获取缓存的校验码
        String rcode = stringRedisTemplate.opsForValue().get(CommonConstant.CHECK_PREFIX + loginDTO.getSessionId());
        // 判断获取的验证码是否存在，以及是否与输入的验证码相同
        if (StringUtils.isBlank(rcode) || !rcode.equalsIgnoreCase(loginDTO.getCode())) {
            return R.error("验证码错误");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = getOne(queryWrapper);
        if (user == null) {
//            throw new ForbiddenException("用户名错误");
            return R.error("用户不存在");
        }

        if (user.getStatus() == UserStatus.FROZEN) {
//            throw new ForbiddenException("用户被冻结");
            return R.error("用户被冻结");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
//            throw new ForbiddenException("用户名或密码错误");
            return R.error("用户名或密码错误");
        }

        String token = jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL());
        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setToken(token);
        userLoginVO.setUserId(user.getId());
        userLoginVO.setUsername(user.getUsername());
        userLoginVO.setBalance(user.getBalance());
        return R.ok(userLoginVO);
    }

    @Override
    @Transactional
    public void deductMoney(String pw, Integer totalFee) {
        log.info("开始扣款");
        // 1.校验密码
        User user = getById(UserContext.getUser());
        if (user == null || !passwordEncoder.matches(pw, user.getPassword())) {
            // 密码错误
            throw new BizIllegalException("用户密码错误");
        }

        // 2.尝试扣款
        int updated = baseMapper.updateMoney(UserContext.getUser(), totalFee);
        if (updated != 1){
            throw new BizIllegalException("扣款失败，余额不足！");
        }
        log.info("扣款成功");
    }

    /**
     * 生成验证码
     *
     * @return
     */
    @Override
    public R<Map> getCaptchaCode() {
        //参数分别是宽、高、验证码长度、干扰线数量
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(250, 40, 4, 5);
        //设置背景颜色清灰
        captcha.setBackground(Color.lightGray);
        //自定义校验码生成方式
//        captcha.setGenerator(new CodeGenerator() {
//            @Override
//         tCode) {
//               public String generate() {
////                return RandomStringUtils.randomNumeric(4);
////            }
////            @Override
////            public boolean verify(String code, String userInpu    return code.equalsIgnoreCase(userInputCode);
//            }
//        });
        //获取图片中的验证码，默认生成的校验码包含文字和数字，长度为4
        String checkCode = captcha.getCode();
        log.info("生成校验码:{}", checkCode);
        //生成sessionId
        String sessionId = String.valueOf(identifierGenerator.nextId(null));
        //将sessionId和校验码保存在redis下，并设置缓存中数据存活时间一分钟
        stringRedisTemplate.opsForValue().set(CommonConstant.CHECK_PREFIX + sessionId, checkCode, 3, TimeUnit.MINUTES);
        //组装响应数据
        HashMap<String, String> info = new HashMap<>();
        info.put("sessionId", sessionId);
        info.put("imageData", captcha.getImageBase64());//获取base64格式的图片数据
        //设置响应数据格式
        return R.ok(info);
    }

    @Override
    public R<User> getUserInfo() {
        User user = getById(UserContext.getUser());
        user.setPassword(null);

        return R.ok(user);
    }

    /**
     * 注册
     *
     * @param loginFormDTO
     * @return
     */
    @Override
    public R register(LoginFormDTO loginFormDTO) {
        if (StringUtils.isBlank(loginFormDTO.getCode()) || StringUtils.isBlank(loginFormDTO.getSessionId())) {
            return R.error("验证码不能为空");
        }
        String rcode = stringRedisTemplate.opsForValue().get(CommonConstant.CHECK_PREFIX + loginFormDTO.getSessionId());
        if (StringUtils.isBlank(rcode) || !rcode.equalsIgnoreCase(loginFormDTO.getCode())) {
            return R.error("验证码错误");
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginFormDTO.getUsername());
        User user = getOne(queryWrapper);
        if (user != null) {
            return R.error("用户已存在");
        }
        if (loginFormDTO.getUsername().length() < 3) {
            return R.error("用户名长度不能小于3");
        }
        if (loginFormDTO.getUsername().length() > 16) {
            return R.error("用户名长度不能大于16");
        }
        // 用户名只能是数字、字母
        if (!loginFormDTO.getUsername().matches("^[a-zA-Z0-9]+$")) {
            return R.error("用户名只能是数字、字母");
        }

        if (loginFormDTO.getPassword().length() < 3) {
            return R.error("密码长度不能小于3");
        }
        if (loginFormDTO.getPassword().length() > 16) {
            return R.error("密码长度不能大于16");
        }
        if (!loginFormDTO.getPassword().matches("^[a-zA-Z0-9]+$")) {
            return R.error("密码只能是数字、字母");
        }
        user = new User();
        user.setUsername(loginFormDTO.getUsername());
        user.setPassword(passwordEncoder.encode(loginFormDTO.getPassword()));
        user.setStatus(UserStatus.NORMAL);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setBalance(9900);
        save(user);
        return R.ok("注册成功");
    }
}
