/**
 * 
 */
package cn.ispeak.dynamics.real.entity;

import org.bson.types.ObjectId;

/**
 * @author Mr Yin
 *
 */
public class Dynamics_data_ts {

	private ObjectId _id;
	private String ts_doc;
	private Long ts;
	
	public ObjectId get_id() {
		return _id;
	}
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	public String getTs_doc() {
		return ts_doc;
	}
	public void setTs_doc(String ts_doc) {
		this.ts_doc = ts_doc;
	}
	public Long getTs() {
		return ts;
	}
	public void setTs(Long ts) {
		this.ts = ts;
	}
}
