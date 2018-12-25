
package com.jjb.cas.quartz;

import java.io.Serializable;

/**
 * @Description: 自动分案配置bean
 */
public class DicBean implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// 是否自动分案
	private boolean isAssign;
	// 最大数量，0表示不设置
	private int max;

	public boolean isAssign() {
		return isAssign;
	}

	public void setAssign(boolean isAssign) {
		this.isAssign = isAssign;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}
}
