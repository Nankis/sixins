package com.ginseng.pojo.bo;

/**
 * BO类, 前端传给 controller的   BO?Browser Object?
 * */
public class UsersBO {
    private String userId;
    private String faceData;
    private String nickname;
    
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getFaceData() {
		return faceData;
	}
	public void setFaceData(String faceData) {
		this.faceData = faceData;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}
