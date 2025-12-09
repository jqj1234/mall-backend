package com.hmall.user.controller;

import com.hmall.common.domain.R;
import com.hmall.common.utils.UserContext;
import com.hmall.user.domain.dto.LoginFormDTO;
import com.hmall.user.domain.po.User;
import com.hmall.user.domain.vo.UserLoginVO;
import com.hmall.user.service.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Api(tags = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @ApiOperation("生成验证码")
    @GetMapping("/captcha")
    public R<Map> getCaptchaCode() {
        return userService.getCaptchaCode();
    }

    @ApiOperation("用户登录接口")
    @PostMapping("/login")
    public R<UserLoginVO> login(@RequestBody @Validated LoginFormDTO loginFormDTO) {
        return userService.login(loginFormDTO);
    }

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public R register(@RequestBody LoginFormDTO loginFormDTO) {
        return userService.register(loginFormDTO);
    }

    @ApiOperation("获取用户信息")
    @GetMapping("/info")
    public R<User> getUserInfo() {
        return userService.getUserInfo();
    }

    @ApiOperation("更新用户信息")
    @PutMapping
    public R updateUser(@RequestBody User user) {
        user.setId(UserContext.getUser());
        // 不允许更新用户名
        user.setUsername(null);
        // 不允许更新密码
        user.setPassword(null);
        user.setUpdateTime(LocalDateTime.now());
        return userService.updateById(user) ? R.ok("更新成功") : R.error("更新失败");
    }

    @ApiOperation("扣减余额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pw", value = "支付密码"),
            @ApiImplicitParam(name = "amount", value = "支付金额")
    })
    @PutMapping("/money/deduct")
    public R deductMoney(@RequestParam("pw") String pw, @RequestParam("amount") Integer amount) {
        userService.deductMoney(pw, amount);
        return R.ok("扣减余额成功");
    }
}

