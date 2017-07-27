/**
 * 
 */
package cn.ispeak.dynamics.real.entity;

/**
 * @author Mr Yin
 *
 */
public class DynamicPoint {

	private int dynamic_id;
	private int to_uid;
	private int point_in;
	private int point_out;
	
	public int getDynamic_id() {
		return dynamic_id;
	}
	public void setDynamic_id(int dynamic_id) {
		this.dynamic_id = dynamic_id;
	}
	public int getTo_uid() {
		return to_uid;
	}
	public void setTo_uid(int to_uid) {
		this.to_uid = to_uid;
	}
	public int getPoint_in() {
		return point_in;
	}
	public void setPoint_in(int point_in) {
		this.point_in = point_in;
	}
	public int getPoint_out() {
		return point_out;
	}
	public void setPoint_out(int point_out) {
		this.point_out = point_out;
	}
}
