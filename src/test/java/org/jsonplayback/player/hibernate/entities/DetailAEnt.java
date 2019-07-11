package org.jsonplayback.player.hibernate.entities;

public class DetailAEnt {
	private DetailACompId compId;
	private String vcharA;
	private String vcharB;
	private Integer hbVersion;
	private DetailAComp detailAComp;
	
	public DetailAComp getDetailAComp() {
		return detailAComp;
	}
	public Integer getHbVersion() {
		return hbVersion;
	}
	public void setHbVersion(Integer hbVersion) {
		this.hbVersion = hbVersion;
	}
	public DetailACompId getCompId() {
		return compId;
	}
	public void setCompId(DetailACompId compId) {
		this.compId = compId;
	}
	public String getVcharA() {
		return vcharA;
	}
	public void setVcharA(String vcharA) {
		this.vcharA = vcharA;
	}
	public String getVcharB() {
		return vcharB;
	}
	public void setVcharB(String vcharB) {
		this.vcharB = vcharB;
	}
	public DetailAComp getDetailAComponent() {
		return detailAComp;
	}
	public void setDetailAComp(DetailAComp detailAComp) {
		this.detailAComp = detailAComp;
	}
}
