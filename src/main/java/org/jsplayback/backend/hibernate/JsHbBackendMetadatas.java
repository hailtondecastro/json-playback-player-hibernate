package org.jsplayback.backend.hibernate;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Usado para Serializar todos os metadatas.
 * @author Hailton de Castro
 *
 */
public class JsHbBackendMetadatas implements Cloneable {
	@JsonProperty("$iAmJsHbBackendMetadatas$")
	@JsonInclude(Include.NON_DEFAULT)
	private Boolean iAmJsHbBackendMetadatas = true;
	
	@JsonProperty("$id$")
	@JsonInclude(Include.NON_NULL)
	private Long id;
	
	@JsonInclude(Include.NON_NULL)
	@JsonProperty("$idRef$")
	private Long idRef;
	
	@JsonProperty("$signature$")
	private String signature;
	
	@JsonProperty("$isLazyUninitialized$")
	@JsonInclude(Include.NON_DEFAULT)
	private Boolean isLazyUninitialized = false;
	
	@JsonProperty("$isComponent$")
	@JsonInclude(Include.NON_DEFAULT)
	private Boolean isComponent = false;
	
	@JsonProperty("$isComponentHibernateId$")
	@JsonInclude(Include.NON_DEFAULT)
	private Boolean isComponentHibernateId = false;
	
	@JsonProperty("$isAssociative$")
	@JsonInclude(Include.NON_DEFAULT)
	private Boolean isAssociative = false;
	
	@JsonProperty("$isLazyProperty$")
	@JsonInclude(Include.NON_DEFAULT)
	private Boolean isLazyProperty = false;
	
	@JsonProperty("$hibernateId$")
	@JsonInclude(Include.NON_NULL)
	private Object hibernateId;

	@JsonIgnore
	private JsHbBeanPropertyWriter originalHibernateIdPropertyWriter;
	@JsonIgnore
	private Object originalHibernateIdOwner;
	
	public Object getOriginalHibernateIdOwner() {
		return originalHibernateIdOwner;
	}
	public void setOriginalHibernateIdOwner(Object originalHibernateIdOwner) {
		this.originalHibernateIdOwner = originalHibernateIdOwner;
	}
	public JsHbBeanPropertyWriter getOriginalHibernateIdPropertyWriter() {
		return originalHibernateIdPropertyWriter;
	}
	public void setOriginalHibernateIdPropertyWriter(JsHbBeanPropertyWriter originalHibernateIdPropertyWriter) {
		this.originalHibernateIdPropertyWriter = originalHibernateIdPropertyWriter;
	}
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
	
	@Override
	public String toString() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			LinkedHashMap<String, Object> thisAsMap = new LinkedHashMap<>();
			thisAsMap.put("iAmJsHbBackendMetadatas", this.iAmJsHbBackendMetadatas);
			thisAsMap.put("id", this.id);
			thisAsMap.put("idRef", this.idRef);
			thisAsMap.put("signature", this.signature);
			thisAsMap.put("isLazyUninitialized", this.isLazyUninitialized);
			thisAsMap.put("isComponent", this.isComponent);
			thisAsMap.put("isComponentHibernateId", this.isComponentHibernateId);
			thisAsMap.put("isAssociative", this.isAssociative);
			thisAsMap.put("isLazyProperty", this.isLazyProperty);
			thisAsMap.put("hibernateId", this.hibernateId != null? this.hibernateId.toString() : null);
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(thisAsMap);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("This should not happen", e);
		}
	}
	
	protected JsHbBackendMetadatas clone() {
		JsHbBackendMetadatas metadatasClone = new JsHbBackendMetadatas();
		metadatasClone.iAmJsHbBackendMetadatas           = this.iAmJsHbBackendMetadatas          ;       
		metadatasClone.id                                = this.id                               ;      
		metadatasClone.idRef                             = this.idRef                            ;      
		metadatasClone.signature                         = this.signature                        ;      
		metadatasClone.isLazyUninitialized               = this.isLazyUninitialized              ;      
		metadatasClone.isComponent                       = this.isComponent                      ;      
		metadatasClone.isComponentHibernateId            = this.isComponentHibernateId           ;      
		metadatasClone.isAssociative                     = this.isAssociative                    ;      
		metadatasClone.isLazyProperty                    = this.isLazyProperty                   ;      
		metadatasClone.hibernateId                       = this.hibernateId                      ;      
		metadatasClone.originalHibernateIdPropertyWriter = this.originalHibernateIdPropertyWriter;      
		metadatasClone.originalHibernateIdOwner          = this.originalHibernateIdOwner         ;      
		return metadatasClone;
	}
}