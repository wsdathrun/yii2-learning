/**
 * 
 */
package cn.ispeak.dynamics.real.entity;

import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * @author Mr Yin
 *
 */
public class Square_dynamics {
	private ObjectId _id;
	private int dynamic_id;
	private long pub_id;
	private long create_time;
	private long opt_time;
	private int userid;
	private Document contents;
	private int partnerid;
	private int sub_partnerid;
	private String ip;
	private boolean pub;
	private int status;
	private int type;
	private int pub_type;
	
	public ObjectId get_id() {
		return _id;
	}
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	public int getDynamic_id() {
		return dynamic_id;
	}
	public void setDynamic_id(int dynamic_id) {
		this.dynamic_id = dynamic_id;
	}
	public long getPub_id() {
		return pub_id;
	}
	public void setPub_id(long pub_id) {
		this.pub_id = pub_id;
	}
	public long getCreate_time() {
		return create_time;
	}
	public void setCreate_time(long create_time) {
		this.create_time = create_time;
	}
	public long getOpt_time() {
		return opt_time;
	}
	public void setOpt_time(long opt_time) {
		this.opt_time = opt_time;
	}
	public int getUserid() {
		return userid;
	}
	public void setUserid(int userid) {
		this.userid = userid;
	}
	public Document getContents() {
		return contents;
	}
	public void setContents(Document contents) {
		this.contents = contents;
	}
	public int getPartnerid() {
		return partnerid;
	}
	public void setPartnerid(int partnerid) {
		this.partnerid = partnerid;
	}
	public int getSub_partnerid() {
		return sub_partnerid;
	}
	public void setSub_partnerid(int sub_partnerid) {
		this.sub_partnerid = sub_partnerid;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public boolean isPub() {
		return pub;
	}
	public void setPub(boolean pub) {
		this.pub = pub;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getPub_type() {
		return pub_type;
	}
	public void setPub_type(int pub_type) {
		this.pub_type = pub_type;
	}
}
