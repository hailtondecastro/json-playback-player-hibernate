package org.jsplayback.backend.hibernate.entities;

import java.sql.Blob;

import org.jsplayback.backend.hibernate.BlobBase64Serializer;
import org.jsplayback.backend.hibernate.ByteArrayBase64Serializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class DetailAComp {
	private MasterBEnt masterB;
	private DetailACompComp detailACompComp;
	private Integer subIdB;
	private String vcharA;
	private String vcharB;
	@JsonSerialize(using=ByteArrayBase64Serializer.class)
	private byte[] blobA;
	@JsonSerialize(using=BlobBase64Serializer.class)
	private Blob blobB;
	
	public DetailACompComp getDetailACompComp() {
		return detailACompComp;
	}
	public void setDetailACompComp(DetailACompComp detailACompComp) {
		this.detailACompComp = detailACompComp;
	}
	public Blob getBlobB() {
		return blobB;
	}
	public void setBlobB(Blob blobB) {
		this.blobB = blobB;
	}
	public MasterBEnt getMasterB() {
		return masterB;
	}
	public void setMasterB(MasterBEnt masterB) {
		this.masterB = masterB;
	}
	public Integer getSubIdB() {
		return subIdB;
	}
	public void setSubIdB(Integer subIdB) {
		this.subIdB = subIdB;
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
	public byte[] getBlobA() {
		return blobA;
	}
	public void setBlobA(byte[] blobA) {
		this.blobA = blobA;
	}
	
	
}
