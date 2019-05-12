package com.ginseng.mapper;

import java.util.List;

import com.ginseng.pojo.Users;
import com.ginseng.pojo.vo.FriendRequestVO;
import com.ginseng.pojo.vo.MyFriendsVO;
import com.ginseng.utils.MyMapper;

public interface UsersMapperCustom extends MyMapper<Users> {
	
	public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);
	
	public List<MyFriendsVO> queryMyFriends(String userId);
	
//	public void batchUpdateMsgSigned(List<String> msgIdList);
	
}