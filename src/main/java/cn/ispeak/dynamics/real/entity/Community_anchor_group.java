package cn.ispeak.dynamics.real.entity;

public class Community_anchor_group {

	private long id;
	private int gid;
	private long auid;
	private int partnerid;
	private int sub_partnerid;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public int getGid() {
		return gid;
	}
	public void setGid(int gid) {
		this.gid = gid;
	}
	public long getAuid() {
		return auid;
	}
	public void setAuid(long auid) {
		this.auid = auid;
	}
	public void setAuid(int auid) {
		this.auid = auid;
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
}
