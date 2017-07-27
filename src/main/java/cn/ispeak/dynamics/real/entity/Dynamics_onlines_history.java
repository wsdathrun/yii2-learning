/**
 * 
 */
package cn.ispeak.dynamics.real.entity;

/**
 * 访客数
 * @author Mr Yin
 * 
 */
public class Dynamics_onlines_history {

	private int dynamic_id;
	private long userid;
	private int visits;
	private int visits_aud;
	private long last_review_time;
	private int reply;
	private long reply_time;
	private long create_time;
	
	public int getDynamic_id() {
		return dynamic_id;
	}
	public void setDynamic_id(int dynamic_id) {
		this.dynamic_id = dynamic_id;
	}
	public long getUserid() {
		return userid;
	}
	public void setUserid(long userid) {
		this.userid = userid;
	}
	public int getVisits() {
		return visits;
	}
	public void setVisits(int visits) {
		this.visits = visits;
	}
	public int getVisits_aud() {
		return visits_aud;
	}
	public void setVisits_aud(int visits_aud) {
		this.visits_aud = visits_aud;
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
	public long getCreate_time() {
		return create_time;
	}
	public void setCreate_time(long create_time) {
		this.create_time = create_time;
	}
}
