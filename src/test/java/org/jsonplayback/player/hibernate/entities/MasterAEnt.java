package org.jsonplayback.player.hibernate.entities;

import java.sql.Blob;
import java.sql.Clob;
import java.util.Collection;
import java.util.Date;

import org.jsonplayback.player.LazyProperty;
import org.jsonplayback.player.hibernate.BlobBase64Serializer;
import org.jsonplayback.player.hibernate.ByteArrayBase64Serializer;
import org.jsonplayback.player.hibernate.ClobStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class MasterAEnt {
	private Integer id;
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
	@LazyProperty(nonLazyMaxSize=1024)
	@JsonSerialize(using=ByteArrayBase64Serializer.class)
	private byte[] blobLazyA;
	@JsonSerialize(using=BlobBase64Serializer.class)
	@LazyProperty(nonLazyMaxSize=1024)
	private Blob blobLazyB;
	@LazyProperty(nonLazyMaxSize=1024)
	private String clobLazyA;
	@JsonSerialize(using=ClobStringSerializer.class)
	@LazyProperty(nonLazyMaxSize=1024)
	private Clob clobLazyB;
	
	
	public byte[] getBlobLazyA() {
		return blobLazyA;
	}
	public void setBlobLazyA(byte[] blobLazyA) {
		this.blobLazyA = blobLazyA;
	}
	public Blob getBlobLazyB() {
		return blobLazyB;
	}
	public void setBlobLazyB(Blob blobLazyB) {
		this.blobLazyB = blobLazyB;
	}
	public String getClobLazyA() {
		return clobLazyA;
	}
	public void setClobLazyA(String clobLazyA) {
		this.clobLazyA = clobLazyA;
	}
	public Clob getClobLazyB() {
		return clobLazyB;
	}
	public void setClobLazyB(Clob clobLazyB) {
		this.clobLazyB = clobLazyB;
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
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
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
