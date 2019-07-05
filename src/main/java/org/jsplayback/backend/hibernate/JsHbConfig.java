package org.jsplayback.backend.hibernate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsplayback.backend.IJsHbConfig;
import org.jsplayback.backend.IJsHbGetBySignatureListener;
import org.jsplayback.backend.IJsHbSignatureCrypto;

public class JsHbConfig implements IJsHbConfig, Cloneable {

	private String jsHbIdName = "jsHbId";
	private String jsHbIdRefName = "jsHbIdRef";
	private String jsHbSignatureName = "jsHbSignature";
	private String jsHbIsLazyUninitializedName = "jsHbIsLazyUninitialized";
	private String jsHbHibernateIdName = "jsHbHibernateId";
	private Set<Class> neverSignedClasses = new HashSet<>();
	private Set<Class> nonLazybleClasses = new HashSet<>();
	private List<IJsHbGetBySignatureListener> listeners = new ArrayList<>();
	private SessionFactory sessionFactory;
	private ObjectMapper objectMapper;
	private IJsHbSignatureCrypto signatureCrypto;
	private boolean serialiseBySignatureAllRelationship = false;

	@Override
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	@Override
	public IJsHbConfig configObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		return this;
	}

	@Override
	public boolean isSerialiseBySignatureAllRelationship() {
		return serialiseBySignatureAllRelationship;
	}

	@Override
	public IJsHbConfig configSerialiseBySignatureAllRelationship(boolean serialiseBySignatureAllRelationship) {
		this.serialiseBySignatureAllRelationship = serialiseBySignatureAllRelationship;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jsplayback.backend.hibernate.JsHbConfig
	 * #configJsHbHibernateIdName(java.lang.String)
	 */
	@Override
	public IJsHbConfig configJsHbHibernateIdName(String jsHbHibernateIdName) {
		this.jsHbHibernateIdName = jsHbHibernateIdName;
		return this;
	}

	@Override
	public IJsHbConfig configNeverSignedClasses(Set<Class> neverSignedClasses) {
		this.neverSignedClasses = neverSignedClasses;
		return this;
	}

	@Override
	public IJsHbConfig configJsHbSignatureName(String jsHbSignatureName) {
		this.jsHbSignatureName = jsHbSignatureName;
		return this;
	}

	@Override
	public IJsHbConfig configSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		return this;
	}

	@Override
	public IJsHbConfig configJsHbIsLazyUninitializedName(String jsHbIsLazyUninitializedName) {
		this.jsHbIsLazyUninitializedName = jsHbIsLazyUninitializedName;
		return this;
	}

	@Override
	public IJsHbConfig configJsHbIdRefName(String jsHbIdRefName) {
		this.jsHbIdRefName = jsHbIdRefName;
		return this;
	}

	@Override
	public IJsHbConfig configJsHbIdName(String jsHbIdName) {
		this.jsHbIdName = jsHbIdName;
		return this;
	}

	@Override
	public List<IJsHbGetBySignatureListener> getListeners() {
		return Collections.unmodifiableList(this.listeners);
	}

	@Override
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	@Override
	public IJsHbSignatureCrypto getSignatureCrypto() {
		return this.signatureCrypto;
	}

	@Override
	public Set<Class> getNeverSignedClasses() {
		// TODO Auto-generated method stub
		return this.neverSignedClasses;
	}

	@Override
	public String getJsHbIdName() {
		return jsHbIdName;
	}

	@Override
	public String getJsHbIdRefName() {
		return jsHbIdRefName;
	}

	@Override
	public String getJsHbSignatureName() {
		return jsHbSignatureName;
	}

	@Override
	public String getJsHbIsLazyUninitializedName() {
		return jsHbIsLazyUninitializedName;
	}

	@Override
	public String getJsHbHibernateIdName() {
		return jsHbHibernateIdName;
	}

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
			for (IJsHbGetBySignatureListener listenerItem : this.getListeners()) {
				listenersList.add(listenerItem.getName());
			}
			thisAsMap.put("serialiseBySignatureAllRelationship", this.isSerialiseBySignatureAllRelationship());
			thisAsMap.put("listeners", listenersList);
			thisAsMap.put("sessionFactory", this.getSessionFactory() != null? this.getSessionFactory().getClass(): "null");
			thisAsMap.put("signatureCrypto", this.getSignatureCrypto()!= null? this.getSignatureCrypto().getClass(): "null");
			thisAsMap.put("neverSignedClasses", this.getNeverSignedClasses());
			thisAsMap.put("jsHbIdName", this.getJsHbIdName());
			thisAsMap.put("jsHbIdRefName", this.getJsHbIdRefName());
			thisAsMap.put("jsHbSignatureName", this.getJsHbSignatureName());
			thisAsMap.put("jsHbIsLazyUninitializedName", this.getJsHbIsLazyUninitializedName());
			thisAsMap.put("jsHbHibernateIdName", this.getJsHbHibernateIdName());
			thisAsMap.put("nonLazybleClasses", this.getNonLazybleClasses());
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(thisAsMap);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("This should not happen", e);
		}
	}
}
/* gerando conflito */