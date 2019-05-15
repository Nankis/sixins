package com.ginseng.controller;


import com.ginseng.enums.OperatorFriendRequestTypeEnum;
import com.ginseng.enums.SearchFriendsStatusEnum;
import com.ginseng.pojo.MyFriends;
import com.ginseng.pojo.Users;
import com.ginseng.pojo.bo.UsersBO;
import com.ginseng.pojo.vo.MyFriendsVO;
import com.ginseng.pojo.vo.UsersVO;
import com.ginseng.service.UserService;
import com.ginseng.utils.FastDFSClient;
import com.ginseng.utils.FileUtils;
import com.ginseng.utils.IMoocJSONResult;
import com.ginseng.utils.MD5Utils;
//import org.apache.commons.beanutils.BeanUtils;
import io.netty.util.internal.StringUtil;
import org.apache.catalina.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("u")  //命名空间
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;

    @PostMapping("/registOrLogin")
    public IMoocJSONResult registOrLogin(@RequestBody Users user) throws Exception {//注意Users 不是User,第二个阿帕奇有自带

        //0.判断用户名和密码不能为空
        if (StringUtils.isBlank(user.getUsername())
                || StringUtils.isBlank(user.getPassword())) {
            return IMoocJSONResult.errorMsg("用户名或密码不能为空!");
        }

        //1.判断用户名是否存在,如果存在就登录,否则注册
        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());

        Users userResult = null;
        if (usernameIsExist) {
            //1.1 登录
            userResult = userService.queryUserForLogin(user.getUsername(),
                    MD5Utils.getMD5Str(user.getPassword()));

            if (userResult == null) {
                return IMoocJSONResult.errorMsg("用户名或密码不正确!");
            }
        } else {
            //1.2 注册
            user.setNickname(user.getUsername());

            //需要添加默认头像,默认头像放在fastdfs服务器,图片路径存在服务器数据库地址
            //如果图片服务器更变,则需要重新设置
            user.setFaceImage("M00/00/00/Cmnl_1zbdxmAMMGNAAJZaZdJjTg378_80x80.png");
            user.setFaceImageBig("M00/00/00/Cmnl_1zbdxmAMMGNAAJZaZdJjTg378.png");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
            userResult = userService.saveUser(user);
        }

        //通过vo去掉多余的字段,再返回给前端
        UsersVO usersVO = new UsersVO();

        //通过深拷贝,拷贝相同字段 ,第一给参数是拷贝源,第二个是生成目标对象,注意,别导阿帕奇的BeanUtils
//        BeanUtils.copyProperties(userResult, usersVO);
        BeanUtils.copyProperties(userResult, usersVO);

        return IMoocJSONResult.ok(usersVO);
    }

    @PostMapping("/uploadFaceBase64")
    public IMoocJSONResult uploadFaceBase64(@RequestBody UsersBO userBO) throws Exception {
        //获取前端传来的图片base64字符串,然后转换为文件对象再上传
        String base64Data = userBO.getFaceData();
        //创建临时文件路径
        String userFacePath = "D:\\" + userBO.getUserId() + "userface64.png";
//        System.err.println(userFacePath);
        FileUtils.base64ToFile(userFacePath, base64Data);

        //MultipartFIle是Spring提供的一种数据类型
        //上传文件到fastdfs
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
        String url = fastDFSClient.uploadBase64(faceFile);
//        System.out.println(url);

        //图片上传后,会生成一张大图和一张缩略图
        //获取缩略图的url
        String thump = "_80x80.";
        String arr[] = url.split("\\.");
        String thumpImgUrl = arr[0] + thump + arr[1];

        //更新用户头像  //可以返回VO出去,不过serverimp要自己实现
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setFaceImage(thumpImgUrl);
        user.setFaceImageBig(url);

        //对象更新后,需要用最新返回的对象
        Users result = userService.updateUserInfo(user);

        return IMoocJSONResult.ok(result);
    }

    //更新用户昵称
    @PostMapping("/setNickname")
    public IMoocJSONResult setNickname(@RequestBody UsersBO userBO) throws Exception {
        Users user = new Users();
        user.setId(userBO.getUserId());

        //需要添加限制前端传来的昵称不能为空
        if (userBO.getNickname() == null || userBO.getNickname().equals(""))
            user.setNickname("昵称非法!");
        else
            user.setNickname(userBO.getNickname());

        Users result = userService.updateUserInfo(user);
        return IMoocJSONResult.ok(result);
    }

    //搜索用户接口,根据帐号做匹配查询,而不是模糊查询
    @PostMapping("/search")
    public IMoocJSONResult searchUser(String myUserId, String friendUsername) throws Exception {
        // 0. 判断myUserId friendUsername 不能为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("");
        }

        //前置条件
        //1. 搜索的用户如果不存在,返回"无此用户"
        //2. 搜索的帐号是你自己的,返回"不能添加自己"
        //3. 搜索的帐号已经是你的好友,返回"该用户已经是你的好友"
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            Users user = userService.queryUserInfoByUsername(friendUsername);
            UsersVO userVO = new UsersVO();
            BeanUtils.copyProperties(user, userVO);
            return IMoocJSONResult.ok(userVO);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }
    }


    //发送添加好友请求
    @PostMapping("/addFriendRequest")
    public IMoocJSONResult addFriendRequest(String myUserId, String friendUsername)
            throws Exception {
        // 0. 判断myUserId friendUsername 不能为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("");
        }

        //前置条件
        //1. 搜索的用户如果不存在,返回"无此用户"
        //2. 搜索的帐号是你自己的,返回"不能添加自己"
        //3. 搜索的帐号已经是你的好友,返回"该用户已经是你的好友"
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            userService.sendFriendRequest(myUserId, friendUsername);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }

        return IMoocJSONResult.ok();
    }

    //查询添加好友请求
    @PostMapping("/queryFriendRequests")
    public IMoocJSONResult queryFriendRequests(String userId) {
        // 0. 判断userId不能为空
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }

        //1.查询用户接收到的好友申请
        return IMoocJSONResult.ok(userService.queryFriendRequestList(userId));
    }


    //接收方通过或忽略好友请求
    @PostMapping("/operFriendRequest")
    public IMoocJSONResult operFriendRequest(String acceptUserId, String sendUserId,
                                             Integer operType) {
        // 0. acceptUserId sendUserId operType不能为空
        if (StringUtils.isBlank(acceptUserId)
                || StringUtils.isBlank(sendUserId)
                || operType == null) {
            return IMoocJSONResult.errorMsg("");
        }

        // 1.如果operType 没有对应的枚举值,则直接抛出空错误
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))) {
            return IMoocJSONResult.errorMsg("");
        }

        if (operType == OperatorFriendRequestTypeEnum.IGNORE.type) {
            //2. 判断如果忽略好友请求,则直接删除好友请求的数据库表记录
            //注意 这里的两个参数位置,不能相反
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        } else if (operType == OperatorFriendRequestTypeEnum.PASS.type) {
            //3. 判断如果是通过好友请求,则互相增加好友记录到数据库对应的表
            //然后删除好友请求的数据库表记录
            userService.passFriendRequest(sendUserId, acceptUserId);
        }
        //4.数据库查询好友列表  用于更新
        List<MyFriendsVO> myFriends = userService.queryMyFriends(acceptUserId);
        return IMoocJSONResult.ok(myFriends);
    }


    //查询通讯录好友列表
    @PostMapping("/myFriends")
    public IMoocJSONResult myFriends(String userId) {
        // 0. 判断userId不能为空
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }
        //1.数据库查询好友列表
        List<MyFriendsVO> myFriends = userService.queryMyFriends(userId);
        return IMoocJSONResult.ok(myFriends);
    }


    // 用户手机端获取未签收的消息列表
    @PostMapping("/getUnReadMsgList")
    public IMoocJSONResult getUnReadMsgList(String acceptUserId) {
        //0.userId 判断不能为空
        if (StringUtils.isBlank(acceptUserId)) {
            return IMoocJSONResult.errorMsg("");
        }

        //查询列表
        List<com.ginseng.pojo.ChatMsg> unreadMsgList = userService.getUnReadMsgList(acceptUserId);
        return IMoocJSONResult.ok(unreadMsgList);
    }

}
