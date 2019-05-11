package com.ginseng.service;

import com.ginseng.pojo.Users;
import com.ginseng.pojo.vo.FriendRequestVO;
import com.ginseng.pojo.vo.UsersVO;
import org.apache.catalina.User;

import java.util.List;

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

    //搜索想添加用户的前置条件
    public Integer preconditionSearchFriends(String myUserId, String friendUsername);


    //根据用户名查询用户对象
    public Users queryUserInfoByUsername(String username);

    //发送添加好友请求,并保存到数据库里
    public void sendFriendRequest(String myUserId, String friendUsername);

    //查询好友添加请求
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);
}
