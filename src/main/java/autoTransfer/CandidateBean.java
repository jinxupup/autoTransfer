/**
 * 
 */
package com.jjb.cas.quartz;

import java.io.Serializable;

/**
 * @Description: 自动分案候选信息bean
 */
public class CandidateBean implements Serializable{
	private static final long serialVersionUID = 1L;
	int ct;
	String taskKey;
	String userId;
	public int getCt() {
		return ct;
	}
	public void setCt(int ct) {
		this.ct = ct;
	}
	public String getTaskKey() {
		return taskKey;
	}
	public void setTaskKey(String taskKey) {
		this.taskKey = taskKey;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
}
