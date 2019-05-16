package com.ginseng.service;

import com.ginseng.netty.ChatMsg;
import com.ginseng.pojo.Users;
import com.ginseng.pojo.vo.FriendRequestVO;
import com.ginseng.pojo.vo.MyFriendsVO;
import com.ginseng.pojo.vo.UsersVO;

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

    //删除好友请求记录
    public void deleteFriendRequest(String sendUserId, String acceptUserId);

    //通过好友请求
    //1.保存好友  2.逆向保存好友   3.删除好友的请求记录
    public void passFriendRequest(String sendUserId, String acceptUserId);

    //查询通讯录好友列表
    public List<MyFriendsVO> queryMyFriends(String userId);


    //此处需要的是netty包下自定义的ChatMsG
    //保存聊天消息到数据库
    public String saveMsg(ChatMsg chatMsg);

    //批量签收消息
    public void updateMsgSigned(List<String> msgIdList);

    //获取未签收消息列表
    public List<com.ginseng.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);


}
