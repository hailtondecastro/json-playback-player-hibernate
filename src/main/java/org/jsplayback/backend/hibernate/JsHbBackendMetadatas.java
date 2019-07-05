package org.jsplayback.backend.hibernate;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Usado para Serializar todos os metadatas.
 * @author Hailton de Castro
 *
 */
public class JsHbBackendMetadatas {
	private Boolean iAmJsHbBackendMetadatas = true;
	private Long id;
	private Long idRef;
	private String signature;
	private Boolean isLazyUninitialized = false;
	private Boolean isComponent = false;
	private Boolean isComponentHibernateId = false;
	private Boolean isAssociative = false;
	private Boolean isLazyProperty = false;
	private Object hibernateId;
	
	public Boolean getIsComponentHibernateId() {
		return isComponentHibernateId;
	}
	public void setIsComponentHibernateId(Boolean isComponentHibernateId) {
		this.isComponentHibernateId = isComponentHibernateId;
	}
	public Boolean getiAmJsHbBackendMetadatas() {
		return iAmJsHbBackendMetadatas;
	}
	public void setiAmJsHbBackendMetadatas(Boolean iAmJsHbBackendMetadatas) {
		this.iAmJsHbBackendMetadatas = iAmJsHbBackendMetadatas;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getIdRef() {
		return idRef;
	}
	public void setIdRef(Long idRef) {
		this.idRef = idRef;
	}
	public String getSignature() {
		return signature;
	}
	public void setSignature(String signature) {
		this.signature = signature;
	}
	public Boolean getIsLazyUninitialized() {
		return isLazyUninitialized;
	}
	public void setIsLazyUninitialized(Boolean isLazyUninitialized) {
		this.isLazyUninitialized = isLazyUninitialized;
	}
	public Boolean getIsComponent() {
		return isComponent;
	}
	public void setIsComponent(Boolean isComponent) {
		this.isComponent = isComponent;
	}
	public Boolean getIsAssociative() {
		return isAssociative;
	}
	public void setIsAssociative(Boolean isAssociative) {
		this.isAssociative = isAssociative;
	}
	public Boolean getIsLazyProperty() {
		return isLazyProperty;
	}
	public void setIsLazyProperty(Boolean isLazyProperty) {
		this.isLazyProperty = isLazyProperty;
	}
	public Object getHibernateId() {
		return hibernateId;
	}
	public void setHibernateId(Object hibernateId) {
		this.hibernateId = hibernateId;
	}
}