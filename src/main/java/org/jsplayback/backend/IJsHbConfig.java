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

	String getJsHbIdName();

	String getJsHbIdRefName();

	String getJsHbSignatureName();

	String getJsHbIsLazyUninitializedName();

	String getJsHbHibernateIdName();

	IJsHbConfig configJsHbHibernateIdName(String jsHbHibernateIdName);

	IJsHbConfig configNeverSignedClasses(Set<Class> getNotLazyClasses);

	IJsHbConfig configJsHbSignatureName(String jsHbSignatureName);

	IJsHbConfig configSessionFactory(SessionFactory sessionFactory);

	IJsHbConfig configJsHbIsLazyUninitializedName(String jsHbIsLazyUninitializedName);

	IJsHbConfig configJsHbIdRefName(String jsHbIdRefName);

	IJsHbConfig configJsHbIdName(String jsHbIdName);

	IJsHbConfig configSerialiseBySignatureAllRelationship(boolean serialiseBySignatureAllRelationship);

	boolean isSerialiseBySignatureAllRelationship();
	
	IJsHbConfig clone();

	ObjectMapper getObjectMapper();

	IJsHbConfig configObjectMapper(ObjectMapper objectMapper);

	Set<Class> getNonLazybleClasses();

	IJsHbConfig configJsHbIsComponentName(String jsHbIsComponentName);

	String getJsHbIsComponentName();

	String getJsHbIsAssociativeName();

	IJsHbConfig setJsHbIsAssociativeName(String jsHbIsAssociativeName);
}
/*gerando conflito*/