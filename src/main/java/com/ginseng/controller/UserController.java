package com.ginseng.controller;


import com.ginseng.pojo.Users;
import com.ginseng.pojo.bo.UsersBO;
import com.ginseng.pojo.vo.UsersVO;
import com.ginseng.service.UserService;
import com.ginseng.utils.FastDFSClient;
import com.ginseng.utils.FileUtils;
import com.ginseng.utils.IMoocJSONResult;
import com.ginseng.utils.MD5Utils;
//import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            user.setFaceImage("");
            user.setFaceImageBig("");
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
        System.err.println(userFacePath);
        FileUtils.base64ToFile(userFacePath, base64Data);

        //MultipartFIle是Spring提供的一种数据类型
        //上传文件到fastdfs
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println(url);

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
        userService.updateUserInfo(user);

        return IMoocJSONResult.ok(user);
    }

}
