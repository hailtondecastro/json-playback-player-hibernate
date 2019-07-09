package org.jsplayback.backend.hibernate.entities;

import java.sql.Blob;
import java.util.Collection;
import java.util.Date;

import org.jsplayback.backend.hibernate.BlobBase64Serializer;
import org.jsplayback.backend.hibernate.ByteArrayBase64Serializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class MasterBEnt {
	private MasterBCompId compId;
	private String vcharA;
	private String vcharB;
	private Date dateA;
	private Date datetimeA;
	@JsonSerialize(using=ByteArrayBase64Serializer.class)
	private byte[] blobA;
	@JsonSerialize(using=BlobBase64Serializer.class)
	private Blob blobB;
	private Integer hbVersion;
	private Collection<DetailAEnt> detailAEntCol;
	private MasterBComp masterBComp;
	
	public MasterBComp getMasterBComp() {
		return masterBComp;
	}
	public void setMasterBComp(MasterBComp masterBComp) {
		this.masterBComp = masterBComp;
	}
	public Integer getHbVersion() {
		return hbVersion;
	}
	public void setHbVersion(Integer hbVersion) {
		this.hbVersion = hbVersion;
	}
	public Collection<DetailAEnt> getDetailAEntCol() {
		return detailAEntCol;
	}
	public void setDetailAEntCol(Collection<DetailAEnt> detailAEntCol) {
		this.detailAEntCol = detailAEntCol;
	}
	public MasterBCompId getCompId() {
		return compId;
	}
	public void setCompId(MasterBCompId compId) {
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
	public Date getDateA() {
		return dateA;
	}
	public void setDateA(Date dateA) {
		this.dateA = dateA;
	}
	public Date getDatetimeA() {
		return datetimeA;
	}
	public void setDatetimeA(Date datetimeA) {
		this.datetimeA = datetimeA;
	}
	public byte[] getBlobA() {
		return blobA;
	}
	public void setBlobA(byte[] blobA) {
		this.blobA = blobA;
	}
	public Blob getBlobB() {
		return blobB;
	}
	public void setBlobB(Blob blobB) {
		this.blobB = blobB;
	}
}
