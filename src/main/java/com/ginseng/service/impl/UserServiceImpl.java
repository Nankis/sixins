package com.ginseng.service.impl;

import com.ginseng.enums.MsgActionEnum;
import com.ginseng.enums.MsgSignFlagEnum;
import com.ginseng.enums.SearchFriendsStatusEnum;
import com.ginseng.mapper.*;
import com.ginseng.netty.ChatMsg;
import com.ginseng.netty.DataContent;
import com.ginseng.netty.UserChannelRel;
import com.ginseng.pojo.FriendsRequest;
import com.ginseng.pojo.MyFriends;
import com.ginseng.pojo.Users;
import com.ginseng.pojo.vo.FriendRequestVO;
import com.ginseng.pojo.vo.MyFriendsVO;
import com.ginseng.service.UserService;
import com.ginseng.utils.FastDFSClient;
import com.ginseng.utils.FileUtils;
import com.ginseng.utils.JsonUtils;
import com.ginseng.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {


    //若有报错,则先检查mapper的逆向映射是否正确,然后自动注入在idea里可能会报红,但实际并不影响结果
    //可以用Resource代替
    @Resource
    @Autowired
    private UsersMapper usersMapper;

    @Resource
    @Autowired
    private Sid sid;

    @Autowired
    private QRCodeUtils qrCodeUtils;

    @Autowired
    private FastDFSClient fastDFSClient;

    @Resource
    @Autowired
    private MyFriendsMapper myFriendsMapper;

    @Resource
    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Resource
    @Autowired
    private UsersMapperCustom usersMapperCustom;

    @Resource
    @Autowired
    private ChatMsgMapper chatMsgMapper;

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUsernameIsExist(String username) {

        Users user = new Users();
        user.setUsername(username);

        //通过对象的唯一属性,返回对象
        Users result = usersMapper.selectOne(user);

        return result != null ? true : false;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserForLogin(String username, String pwd) {

        //通过逆向工具查询对象
        Example userExample = new Example(Users.class);
        //创建条件 用于查询,类似于hibernate
        Example.Criteria criteria = userExample.createCriteria();

        //添加条件,  第一个参数是数据库的属性名,第二个参数是前端传来的值,
        criteria.andEqualTo("username", username);
        criteria.andEqualTo("password", pwd);

        Users result = usersMapper.selectOneByExample(userExample);

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users saveUser(Users user) {
        //生成用户唯一id
        String userId = sid.nextShort();

        //用户注册的时候,为每个用户生成唯一的二维码
        //sixins_qrcode:[username]   可以进行加密

        //Win10测试环境
//        String qrCodePath = "D://users" + userId + "qrcode.png";

        //Linux环境下 /fastdfs/tmp
        String qrCodePath = "/fastdfs/tmp/users" + userId + "qrcode.png";

        //第一个参数是二维码图片路径,第二个是二维码内容
        qrCodeUtils.createQRCode(qrCodePath, "sixins_qrcode:" + user.getUsername());

        //通过路径转换为Multipartfile 类型文件,然后上传到图片服务器
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);

        //TODO 优化删除生成的临时文件,头像上传那块也需要

        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        user.setQrcode(qrCodeUrl);
        user.setId(userId);
        usersMapper.insert(user);

        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUserInfo(Users user) {

        //根据用户的主键,全部做一个更新,selective是只更新有值的字段
        usersMapper.updateByPrimaryKeySelective(user);

        return queryUserById(user.getId());
    }


    // 不加这个的话,更新用户的数据,有一部分会为null
    @Transactional(propagation = Propagation.SUPPORTS)
    public Users queryUserById(String userId) {
        return usersMapper.selectByPrimaryKey(userId);
    }


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchFriends(String myUserId, String friendUsername) {
        //前置条件
        //1. 搜索的用户如果不存在,返回"无此用户"
        Users user = queryUserInfoByUsername(friendUsername);
        if (user == null) {
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }

        //2. 搜索的帐号是你自己的,返回"不能添加自己"
        if (user.getId().equals(myUserId)) {
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }

        //3. 搜索的帐号已经是你的好友,返回"该用户已经是你的好友"
        Example mfe = new Example(MyFriends.class);
        Example.Criteria mfc = mfe.createCriteria();
        mfc.andEqualTo("myUserId", myUserId);
        mfc.andEqualTo("myFriendUserId", user.getId());
        MyFriends myFriendsRel = myFriendsMapper.selectOneByExample(mfe);
        if (myFriendsRel != null) {
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }
        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserInfoByUsername(String username) {
        Example ue = new Example(Users.class);
        Example.Criteria uc = ue.createCriteria();
        uc.andEqualTo("username", username);
        return usersMapper.selectOneByExample(ue);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {
        //根据用户名 查询出朋友信息
        Users friend = queryUserInfoByUsername(friendUsername);

        //1.查询发送好友请求记录表
        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId", myUserId);
        frc.andEqualTo("acceptUserId", friend.getId());
        FriendsRequest friendsRequest = friendsRequestMapper.selectOneByExample(fre);
        if (friendsRequest == null) {
            //2. 如果不是你的好友,并且好友记录没有被添加,则新增好友请求记录
            String requestId = sid.nextShort();

            FriendsRequest request = new FriendsRequest();
            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());
            friendsRequestMapper.insert(request);
        }
        //否则什么也不做
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId", sendUserId);
        frc.andEqualTo("acceptUserId", acceptUserId);
        friendsRequestMapper.deleteByExample(fre);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        saveFriends(sendUserId, acceptUserId);
        //相互保存,所以需要逆向保存
        saveFriends(acceptUserId, sendUserId);
        //删除请求记录表数据
        deleteFriendRequest(sendUserId, acceptUserId);

        Channel sendChannel = UserChannelRel.get(sendUserId);
        if (sendChannel != null) {
            //使用websocket主动推送消息到请求发送者,更新他的通讯录列表为最新
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);

            sendChannel.writeAndFlush(
                    new TextWebSocketFrame(JsonUtils.objectToJson(dataContent)));
        }

    }


    @Transactional(propagation = Propagation.REQUIRED)
    public void saveFriends(String sendUserId, String acceptUserId) {
        MyFriends myFriends = new MyFriends();
        String recordId = sid.nextShort();
        myFriends.setId(recordId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriends.setMyUserId(sendUserId);
        myFriendsMapper.insert(myFriends);

    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        List<MyFriendsVO> myFriends = usersMapperCustom.queryMyFriends(userId);
        return myFriends;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {
        //由于定义的ChatMsg与netty重名了,所以用全路径区分
        com.ginseng.pojo.ChatMsg msgDB = new com.ginseng.pojo.ChatMsg();
        String msgId = sid.nextShort();

        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());

        chatMsgMapper.insert(msgDB);

        return msgId;
    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
        usersMapperCustom.batchUpdateMsgSigned(msgIdList);
    }


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<com.ginseng.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {

        Example chatExample = new Example(com.ginseng.pojo.ChatMsg.class);
        Example.Criteria chatCriteria = chatExample.createCriteria();
        chatCriteria.andEqualTo("signFlag", 0);
        chatCriteria.andEqualTo("acceptUserId", acceptUserId);

        List<com.ginseng.pojo.ChatMsg> result = chatMsgMapper.selectByExample(chatExample);

        return result;
    }



}
