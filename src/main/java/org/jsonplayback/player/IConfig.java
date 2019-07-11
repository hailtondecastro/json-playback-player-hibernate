package org.jsonplayback.player;

import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface IConfig {
	/* (non-Javadoc)
	 * @see org.jsonplayback.player.hibernate.JsHbConfig#getListeners()
	 */
	List<IGetBySignatureListener> getListeners();

	/* (non-Javadoc)
	 * @see org.jsonplayback.player.hibernate.JsHbConfig#getSessionFactory()
	 */
	SessionFactory getSessionFactory();

	/* (non-Javadoc)
	 * @see org.jsonplayback.player.hibernate.JsHbConfig#getSignatureCrypto()
	 */
	SignatureCrypto getSignatureCrypto();

	/* (non-Javadoc)
	 * @see org.jsonplayback.player.hibernate.JsHbConfig#getNotLazyClasses()
	 */
	Set<Class> getNeverSignedClasses();

	IConfig configNeverSignedClasses(Set<Class> getNotLazyClasses);

	IConfig configSessionFactory(SessionFactory sessionFactory);

	IConfig configSerialiseBySignatureAllRelationship(boolean serialiseBySignatureAllRelationship);

	boolean isSerialiseBySignatureAllRelationship();
	
	IConfig clone();

	ObjectMapper getObjectMapper();

	IConfig configObjectMapper(ObjectMapper objectMapper);

	Set<Class> getNonLazybleClasses();

	String getJsHbMetadatasName();

	IConfig configJsHbMetadatasName(String jsHbMetadatasName);

	boolean isIgnoreAllJsHbLazyProperty();

	IConfig configIgnoreAllJsHbLazyProperty(boolean ignoreAllJsHbLazyProperty);
}
/*gerando conflito*/