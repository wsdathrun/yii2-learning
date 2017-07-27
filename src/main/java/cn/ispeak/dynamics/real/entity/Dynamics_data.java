/**
 * 
 */
package cn.ispeak.dynamics.real.entity;

/**
 * @author Mr Yin
 *
 */
public class Dynamics_data{
	
	//主播id
	private Long userid;
	//动态id
	private Integer dynamic_id;
	//平台点赞数
	private Integer point_in;
	//用户点赞数
	private Integer point_out;
	//平台评论数
	private Integer comments_in;
	//未处理评论数
	private Integer comments_aud;
	//用户评论数
	private Integer comments_out;
	//最近一小时访客总数
	private Integer visits;
	//最近一小时访客未处理数
	private Integer visits_aud;
	//状态：0-非接待主播，1-接待主播
	private Integer status;
	
	public Long getUserid() {
		return userid;
	}
	public void setUserid(Long userid) {
		this.userid = userid;
	}
	public Integer getDynamic_id() {
		return dynamic_id;
	}
	public void setDynamic_id(Integer dynamic_id) {
		this.dynamic_id = dynamic_id;
	}
	public Integer getPoint_in() {
		return point_in;
	}
	public void setPoint_in(Integer point_in) {
		this.point_in = point_in;
	}
	public Integer getPoint_out() {
		return point_out;
	}
	public void setPoint_out(Integer point_out) {
		this.point_out = point_out;
	}
	public Integer getComments_in() {
		return comments_in;
	}
	public void setComments_in(Integer comments_in) {
		this.comments_in = comments_in;
	}
	public Integer getComments_aud() {
		return comments_aud;
	}
	public void setComments_aud(Integer comments_aud) {
		this.comments_aud = comments_aud;
	}
	public Integer getComments_out() {
		return comments_out;
	}
	public void setComments_out(Integer comments_out) {
		this.comments_out = comments_out;
	}
	public Integer getVisits() {
		return visits;
	}
	public void setVisits(Integer visits) {
		this.visits = visits;
	}
	public Integer getVisits_aud() {
		return visits_aud;
	}
	public void setVisits_aud(Integer visits_aud) {
		this.visits_aud = visits_aud;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
}
