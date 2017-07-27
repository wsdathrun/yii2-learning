/**
 * 
 */
package cn.ispeak.dynamics.real.entity;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * @author Mr Yin
 *
 */
public class BatchUpdateOptions {
	private Query query;
    private Update update;
    private boolean upsert = false;
    private boolean multi = false;
    private int uid;
    
	public Query getQuery() {
		return query;
	}
	public void setQuery(Query query) {
		this.query = query;
	}
	public Update getUpdate() {
		return update;
	}
	public void setUpdate(Update update) {
		this.update = update;
	}
	public boolean isUpsert() {
		return upsert;
	}
	public void setUpsert(boolean upsert) {
		this.upsert = upsert;
	}
	public boolean isMulti() {
		return multi;
	}
	public void setMulti(boolean multi) {
		this.multi = multi;
	}
	public int getUid() {
		return uid;
	}
	public void setUid(int uid) {
		this.uid = uid;
	}
	@Override
	public int hashCode() {
		return (this.uid+"").hashCode()+29*this.uid;
	}
	@Override
	public boolean equals(Object obj) {
		
		if(obj == null){
			return false;
		}
		
		return ((BatchUpdateOptions)obj).getUid() == this.uid;
	}
}
