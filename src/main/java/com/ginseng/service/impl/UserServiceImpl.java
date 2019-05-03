package com.ginseng.service.impl;

import com.ginseng.mapper.UsersMapper;
import com.ginseng.pojo.Users;
import com.ginseng.service.UserService;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;

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

    @Override
    public Users saveUser(Users users) {

        //生成用户唯一id
        String userId = sid.nextShort();

        //TODO 为每个用户生成唯一的二维码
        users.setQrcode("");
        users.setId(userId);
        usersMapper.insert(users);

        return users;
    }
}
