package cn.ispeak.dynamics.real.entity;

import org.bson.Document;
import org.bson.types.ObjectId;

public class Kf_anchor_queue {

	private ObjectId _id;
	private Document doc;
	private long createtime;
	private int type;
	
	public ObjectId get_id() {
		return _id;
	}
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	public Document getDoc() {
		return doc;
	}
	public void setDoc(Document doc) {
		this.doc = doc;
	}
	public long getCreatetime() {
		return createtime;
	}
	public void setCreatetime(long createtime) {
		this.createtime = createtime;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
}
