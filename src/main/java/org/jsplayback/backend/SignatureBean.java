package org.jsplayback.backend;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SignatureBean {
	private String signature;
	private Class<?> clazz;
	private String entityName;
	/**
	 * Se nulo eh a propria entidade, caso contrario eh o lazy de uma entidade.
	 */
	private String propertyName;
	private Object[] rawKeyValues;
	public String getSignature() {
		return signature;
	}
	public void setSignature(String signature) {
		this.signature = signature;
	}
	public Class<?> getClazz() {
		return clazz;
	}
	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}
	public String getEntityName() {
		return entityName;
	}
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
	public String getPropertyName() {
		return propertyName;
	}
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}
	public Object[] getRawKeyValues() {
		return rawKeyValues;
	}
	public void setRawKeyValues(Object[] rawKeyValues) {
		this.rawKeyValues = rawKeyValues;
	}
	
	@Override
	public String toString() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			LinkedHashMap<String, Object> thisAsMap = new LinkedHashMap<>();
			thisAsMap.put("signature",      this.getSignature()   );
			thisAsMap.put("clazz",          this.getClazz()       );
			thisAsMap.put("entityName",     this.getEntityName()  );
			thisAsMap.put("propertyName",   this.getPropertyName());
			thisAsMap.put("rawKeyValues",   this.getRawKeyValues());
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(thisAsMap);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Isso nao deveria acontecer", e);
		}
	}
}
/*gerando conflito*/