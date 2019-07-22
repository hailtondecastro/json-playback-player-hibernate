package org.jsonplayback.player.hibernate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.jsonplayback.player.IConfig;
import org.jsonplayback.player.IGetBySignatureListener;
import org.jsonplayback.player.SignatureCrypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsHbConfig implements IConfig, Cloneable {

//	private String jsHbIdName = "jsHbId";
//	private String jsHbIdRefName = "jsHbIdRef";
//	private String jsHbSignatureName = "jsHbSignature";
//	private String jsHbIsLazyUninitializedName = "jsHbIsLazyUninitialized";
//	private String jsHbIsComponentName = "jsHbIsComponent";
//	private String jsHbIsAssociativeName = "jsHbIsAssociative";
//	private String jsHbIsLazyPropertyName = "jsHbIsLazyProperty";
//	private String jsHbPlayerObjectIdName = "jsHbPlayerObjectId";
	private String jsHbMetadatasName = "$jsHbMetadatas$";
	private Set<Class> neverSignedClasses = new HashSet<>();
	private Set<Class> nonLazybleClasses = new HashSet<>();
	private List<IGetBySignatureListener> listeners = new ArrayList<>();
	private SessionFactory sessionFactory;
	private ObjectMapper objectMapper;
	private SignatureCrypto signatureCrypto;
	private boolean serialiseBySignatureAllRelationship = false;
	private boolean ignoreAllJsHbLazyProperty = false;
	
	@Override
	public boolean isIgnoreAllJsHbLazyProperty() {
		return ignoreAllJsHbLazyProperty;
	}

	@Override
	public IConfig configIgnoreAllJsHbLazyProperty(boolean ignoreAllJsHbLazyProperty) {
		this.ignoreAllJsHbLazyProperty = ignoreAllJsHbLazyProperty;
		return this;
	}

	@Override
	public String getJsHbMetadatasName() {
		return jsHbMetadatasName;
	}

	@Override
	public IConfig configJsHbMetadatasName(String jsHbMetadatasName) {
		this.jsHbMetadatasName = jsHbMetadatasName;
		return this;
	}

//	public String getJsHbIsLazyPropertyName() {
//		return jsHbIsLazyPropertyName;
//	}
//
//	@Override
//	public IJsHbConfig setJsHbIsLazyPropertyName(String jsHbIsLazyPropertyName) {
//		this.jsHbIsLazyPropertyName = jsHbIsLazyPropertyName;
//		return this;
//	}
//
//	@Override
//	public String getJsHbIsAssociativeName() {
//		return jsHbIsAssociativeName;
//	}
//
//	@Override
//	public IJsHbConfig setJsHbIsAssociativeName(String jsHbIsAssociativeName) {
//		this.jsHbIsAssociativeName = jsHbIsAssociativeName;
//		return this;
//	}

	@Override
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	@Override
	public IConfig configObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		return this;
	}

	@Override
	public boolean isSerialiseBySignatureAllRelationship() {
		return serialiseBySignatureAllRelationship;
	}

	@Override
	public IConfig configSerialiseBySignatureAllRelationship(boolean serialiseBySignatureAllRelationship) {
		this.serialiseBySignatureAllRelationship = serialiseBySignatureAllRelationship;
		return this;
	}

//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see
//	 * org.jsonplayback.implemantation.JsHbConfig
//	 * #configJsHbPlayerObjectIdName(java.lang.String)
//	 */
//	@Override
//	public IJsHbConfig configJsHbPlayerObjectIdName(String jsHbPlayerObjectIdName) {
//		this.jsHbPlayerObjectIdName = jsHbPlayerObjectIdName;
//		return this;
//	}

	@Override
	public IConfig configNeverSignedClasses(Set<Class> neverSignedClasses) {
		this.neverSignedClasses = neverSignedClasses;
		return this;
	}

//	@Override
//	public IJsHbConfig configJsHbSignatureName(String jsHbSignatureName) {
//		this.jsHbSignatureName = jsHbSignatureName;
//		return this;
//	}

	@Override
	public IConfig configSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		return this;
	}

//	@Override
//	public IJsHbConfig configJsHbIsLazyUninitializedName(String jsHbIsLazyUninitializedName) {
//		this.jsHbIsLazyUninitializedName = jsHbIsLazyUninitializedName;
//		return this;
//	}
//
//	@Override
//	public IJsHbConfig configJsHbIdRefName(String jsHbIdRefName) {
//		this.jsHbIdRefName = jsHbIdRefName;
//		return this;
//	}
	
//	@Override
//	public IJsHbConfig configJsHbIdName(String jsHbIdName) {
//		this.jsHbIdName = jsHbIdName;
//		return this;
//	}
//
//	@Override
//	public IJsHbConfig configJsHbIsComponentName(String jsHbIsComponentName) {
//		this.jsHbIsComponentName = jsHbIsComponentName;
//		return this;
//	}
//	
//	@Override
//	public String getJsHbIsComponentName() {
//		return this.jsHbIsComponentName = jsHbIsComponentName;
//	}

	@Override
	public List<IGetBySignatureListener> getListeners() {
		return Collections.unmodifiableList(this.listeners);
	}

	@Override
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	@Override
	public SignatureCrypto getSignatureCrypto() {
		return this.signatureCrypto;
	}

	@Override
	public Set<Class> getNeverSignedClasses() {
		// TODO Auto-generated method stub
		return this.neverSignedClasses;
	}

//	@Override
//	public String getJsHbIdName() {
//		return jsHbIdName;
//	}
//
//	@Override
//	public String getJsHbIdRefName() {
//		return jsHbIdRefName;
//	}
//
//	@Override
//	public String getJsHbSignatureName() {
//		return jsHbSignatureName;
//	}
//
//	@Override
//	public String getJsHbIsLazyUninitializedName() {
//		return jsHbIsLazyUninitializedName;
//	}

//	@Override
//	public String getJsHbPlayerObjectIdName() {
//		return jsHbPlayerObjectIdName;
//	}

	@Override
	public Set<Class> getNonLazybleClasses() {
		return nonLazybleClasses;
	}

	@Override
	public JsHbConfig clone() {
		try {
			return (JsHbConfig) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("This should not happen", e);
		}
	}

	@Override
	public String toString() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			LinkedHashMap<String, Object> thisAsMap = new LinkedHashMap<>();
			ArrayList<String> listenersList = new ArrayList<>();
			for (IGetBySignatureListener listenerItem : this.getListeners()) {
				listenersList.add(listenerItem.getName());
			}
			thisAsMap.put("serialiseBySignatureAllRelationship", this.isSerialiseBySignatureAllRelationship());
			thisAsMap.put("listeners", listenersList);
			thisAsMap.put("sessionFactory", this.getSessionFactory() != null? this.getSessionFactory().getClass(): "null");
			thisAsMap.put("signatureCrypto", this.getSignatureCrypto()!= null? this.getSignatureCrypto().getClass(): "null");
			thisAsMap.put("neverSignedClasses", this.getNeverSignedClasses());
			thisAsMap.put("jsHbMetadatasName", this.getJsHbMetadatasName());
//			thisAsMap.put("jsHbIdName", this.getJsHbIdName());
//			thisAsMap.put("jsHbIdRefName", this.getJsHbIdRefName());
//			thisAsMap.put("jsHbSignatureName", this.getJsHbSignatureName());
//			thisAsMap.put("jsHbIsLazyUninitializedName", this.getJsHbIsLazyUninitializedName());
//			thisAsMap.put("jsHbPlayerObjectIdName", this.getJsHbPlayerObjectIdName());
			thisAsMap.put("nonLazybleClasses", this.getNonLazybleClasses());
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(thisAsMap);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("This should not happen", e);
		}
	}
}
/* gerando conflito */