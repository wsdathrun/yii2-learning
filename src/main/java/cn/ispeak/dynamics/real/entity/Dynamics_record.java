package cn.ispeak.dynamics.real.entity;

public class Dynamics_record {

	private int _id;
	private int dynamic_id;
	private long createtime;
	private int status;
	
	public int get_id() {
		return _id;
	}
	public void set_id(int _id) {
		this._id = _id;
	}
	public int getDynamic_id() {
		return dynamic_id;
	}
	public void setDynamic_id(int dynamic_id) {
		this.dynamic_id = dynamic_id;
	}
	public long getCreatetime() {
		return createtime;
	}
	public void setCreatetime(long createtime) {
		this.createtime = createtime;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
}
