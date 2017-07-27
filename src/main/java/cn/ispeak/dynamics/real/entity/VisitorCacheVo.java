/**
 * 
 */
package cn.ispeak.dynamics.real.entity;

/**
 * @author Mr Yin
 *
 */
public class VisitorCacheVo {

	private int userid;
	private int dynamic_id;
	private String nickname;
	private int diannum_grade;
	private int total_xf_grade;

	public int getUserid() {
		return userid;
	}

	public void setUserid(int userid) {
		this.userid = userid;
	}

	public int getDynamic_id() {
		return dynamic_id;
	}

	public void setDynamic_id(int dynamic_id) {
		this.dynamic_id = dynamic_id;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public int getDiannum_grade() {
		return diannum_grade;
	}

	public void setDiannum_grade(int diannum_grade) {
		this.diannum_grade = diannum_grade;
	}

	public int getTotal_xf_grade() {
		return total_xf_grade;
	}

	public void setTotal_xf_grade(int total_xf_grade) {
		this.total_xf_grade = total_xf_grade;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == null)
			return false;

		if (this.getClass() != obj.getClass())
			return false;

		VisitorCacheVo vo = (VisitorCacheVo) obj;

		return vo.getUserid() == this.userid 
				&& vo.getDynamic_id() == this.dynamic_id 
				&& vo.getNickname().equals(this.nickname)
				&& vo.getDiannum_grade() == this.diannum_grade 
				&& vo.getTotal_xf_grade() == this.total_xf_grade;
	}

	@Override
	public int hashCode() {
		return this.nickname.hashCode()+29*this.diannum_grade+29*this.total_xf_grade;
	}
}
