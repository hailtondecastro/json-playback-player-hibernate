package org.jsplayback.backend;

import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface IJsHbConfig {
	/* (non-Javadoc)
	 * @see org.jsplayback.backend.hibernate.JsHbConfig#getListeners()
	 */
	List<IJsHbGetBySignatureListener> getListeners();

	/* (non-Javadoc)
	 * @see org.jsplayback.backend.hibernate.JsHbConfig#getSessionFactory()
	 */
	SessionFactory getSessionFactory();

	/* (non-Javadoc)
	 * @see org.jsplayback.backend.hibernate.JsHbConfig#getSignatureCrypto()
	 */
	IJsHbSignatureCrypto getSignatureCrypto();

	/* (non-Javadoc)
	 * @see org.jsplayback.backend.hibernate.JsHbConfig#getNotLazyClasses()
	 */
	Set<Class> getNeverSignedClasses();

	IJsHbConfig configNeverSignedClasses(Set<Class> getNotLazyClasses);

	IJsHbConfig configSessionFactory(SessionFactory sessionFactory);

	IJsHbConfig configSerialiseBySignatureAllRelationship(boolean serialiseBySignatureAllRelationship);

	boolean isSerialiseBySignatureAllRelationship();
	
	IJsHbConfig clone();

	ObjectMapper getObjectMapper();

	IJsHbConfig configObjectMapper(ObjectMapper objectMapper);

	Set<Class> getNonLazybleClasses();

	String getJsHbMetadatasName();

	IJsHbConfig configJsHbMetadatasName(String jsHbMetadatasName);

	boolean isIgnoreAllJsHbLazyProperty();

	IJsHbConfig configIgnoreAllJsHbLazyProperty(boolean ignoreAllJsHbLazyProperty);
}
/*gerando conflito*/