/**
 * 
 */
package cn.ispeak.dynamics.real.entity;

/**
 * 评论数
 * @author Mr Yin
 *
 */
public class Comments {

	//动态id
	private int dynamic_id;
	//内部评论数
	private int comments_in;
	//外部评论未评数
	private int comments_aud;
	//外部评论总数
	private int comments_out;
	
	private int did;
	
	public int getDynamic_id() {
		return dynamic_id;
	}
	public void setDynamic_id(int dynamic_id) {
		this.dynamic_id = dynamic_id;
	}
	public int getComments_in() {
		return comments_in;
	}
	public void setComments_in(int comments_in) {
		this.comments_in = comments_in;
	}
	public int getComments_aud() {
		return comments_aud;
	}
	public void setComments_aud(int comments_aud) {
		this.comments_aud = comments_aud;
	}
	public int getComments_out() {
		return comments_out;
	}
	public void setComments_out(int comments_out) {
		this.comments_out = comments_out;
	}
	public int getDid() {
		return did;
	}
	public void setDid(int did) {
		this.did = did;
	}
}
