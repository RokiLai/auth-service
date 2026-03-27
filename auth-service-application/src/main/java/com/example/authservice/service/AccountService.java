package com.example.authservice.service;

import java.util.List;

import com.example.authservice.service.dto.UserLoginDTO;

public interface AccountService {
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果，包含用户信息
     */
    UserLoginDTO login(String username, String password);
    /**
     * 校验用户名和密码是否匹配
     * @param username 用户名
     * @param oldPassword 旧密码
     * @return 是否匹配
     */
    boolean validatePassword(String username, String oldPassword);
    /**
     * 更新用户密码
     * @param username 用户名
     * @param newPassword 新密码
     */
    boolean updatePassword(String username, String newPassword);

    /**
     * 注销当前登录会话
     * @return 注销结果
     */
    boolean logout();

    /**
     * 注册用户
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @param roleIds 角色列表
     * @return 注册结果
     */
    boolean register(String username, String password, String email, List<Long> roleIds);
    
}
