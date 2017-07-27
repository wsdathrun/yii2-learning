package cn.ispeak.dynamics.real.entity;

import java.util.Date;

public class Community_kf_login_out {

	private int id;
	private long uid;
	private int status;
	private Date time;
	private int partner_id;
	private int sub_partner_id;
	private int cmgroup;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}
	public int getPartner_id() {
		return partner_id;
	}
	public void setPartner_id(int partner_id) {
		this.partner_id = partner_id;
	}
	public int getSub_partner_id() {
		return sub_partner_id;
	}
	public void setSub_partner_id(int sub_partner_id) {
		this.sub_partner_id = sub_partner_id;
	}
	public int getCmgroup() {
		return cmgroup;
	}
	public void setCmgroup(int cmgroup) {
		this.cmgroup = cmgroup;
	}
}
