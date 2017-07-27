/**
 * 
 */
package cn.ispeak.dynamics.real.entity;

/**
 * 最近一小时访客表
 * @author Mr Yin
 *
 */
public class Dynamics_visitor {

	private int uid;
	private String nickname;
	private int total_xf_grade;
	private int diannum_grade;
	private long diannum;
	private long last_review_time;
	private int reply;
	private long reply_time;
	
	public int getUid() {
		return uid;
	}
	public void setUid(int uid) {
		this.uid = uid;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public int getTotal_xf_grade() {
		return total_xf_grade;
	}
	public void setTotal_xf_grade(int total_xf_grade) {
		this.total_xf_grade = total_xf_grade;
	}
	public int getDiannum_grade() {
		return diannum_grade;
	}
	public void setDiannum_grade(int diannum_grade) {
		this.diannum_grade = diannum_grade;
	}
	public long getDiannum() {
		return diannum;
	}
	public void setDiannum(long diannum) {
		this.diannum = diannum;
	}
	public long getLast_review_time() {
		return last_review_time;
	}
	public void setLast_review_time(long last_review_time) {
		this.last_review_time = last_review_time;
	}
	public int getReply() {
		return reply;
	}
	public void setReply(int reply) {
		this.reply = reply;
	}
	public long getReply_time() {
		return reply_time;
	}
	public void setReply_time(long reply_time) {
		this.reply_time = reply_time;
	}
}
