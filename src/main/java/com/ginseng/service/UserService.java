package com.ginseng.service;

import com.ginseng.pojo.Users;
import com.ginseng.pojo.vo.UsersVO;
import org.apache.catalina.User;

public interface UserService {

    //打出 /** 回车即可

    /**
     * 判断用户是否存在
     *
     * @param username
     * @return
     */
    public boolean queryUsernameIsExist(String username);


    /**
     * 查询用户是否存在
     *
     * @param username
     * @param pwd
     * @return
     */
    public Users queryUserForLogin(String username, String pwd);


    //用户注册
    public Users saveUser(Users users);

    //修改用户的记录
    public Users updateUserInfo(Users user);

}
