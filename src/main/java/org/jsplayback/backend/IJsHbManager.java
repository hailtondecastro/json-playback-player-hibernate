package org.jsplayback.backend;

import java.util.Map;
import java.util.Stack;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;

import org.jsplayback.backend.hibernate.JsHbBeanPropertyWriter;
import org.jsplayback.backend.hibernate.JsHbJsonSerializer;
import org.jsplayback.backend.hibernate.JsHbManager;
import org.jsplayback.backend.hibernate.JsHbPlayback;
import org.jsplayback.backend.hibernate.JsHbResultEntity;

public interface IJsHbManager {
	SignatureBean generateLazySignature(PersistentCollection persistentCollection);

	SignatureBean generateLazySignature(HibernateProxy hibernateProxy);

	SignatureBean generateSignature(Object nonHibernateProxy);

	String serializeSignature(SignatureBean signatureBean);

	SignatureBean deserializeSignature(String signatureStr);

	<T> T getBySignature(SignatureBean signature);

	@SuppressWarnings("rawtypes")
	boolean isNeverSigned(Class clazz);

	Object getHibernateObjectId(Object object);

	IJsHbConfig getJsHbConfig();

	JsHbManager configure(IJsHbConfig jsHbConfig);

	/**
	 * Threadsafe
	 */
	void startSuperSync();

	/**
	 * Threadsafe
	 */
	void stopSuperSync();

	Map<Long, Object> getObjectByIdMap();

	Map<IdentityRefKey, Long> getIdByObjectMap();

	Long getCurrId();

	void currIdPlusPlus();

	boolean isStarted();

	boolean isPersistentClassOrComponent(Class clazz);

	boolean isRelationship(Class<?> clazz, String fieldName);

	SignatureBean generateLazySignatureForRelashionship(Class<?> ownerClass, String fieldName, Object ownerValue,
			Object fieldValue);

	/**
	 * Sobreescreve a configuracao temporariamente. Eh thead safe e nao afeta as
	 * demais theads. Eh descartado em {@link #stopSuperSync()}.
	 * 
	 * @param newConfig
	 * @return
	 */
	IJsHbManager overwriteConfigurationTemporarily(IJsHbConfig newConfig);

	<T> JsHbResultEntity<T> createResultEntity(T result);

	boolean isComponent(Class<?> clazz, String fieldName);

	/**
	 * Inicializa o Manager. Faz as cargas iniciais a partir a partir dos
	 * metadatas hibernate e de quaisquer outras informacoes necessarias. <br>
	 * Eh chamado automaticamente no primeiro {@link #startSuperSync()} caso nao
	 * tenha sido chamado ainda. Pode ser chamado novamente a qualquer momento.
	 * Nao eh thread safe.
	 * 
	 * @return
	 */
	IJsHbManager init();

	boolean isPersistentClass(Class clazz);

	Stack<JsHbBeanPropertyWriter> getJsHbBeanPropertyWriterStepStack();

	String getHibernateIdName(Class clazz);

	Stack<JsHbJsonSerializer> getJsHbJsonSerializerStepStackTL();

	IJsHbReplayable prepareReplayable(JsHbPlayback playback);
}
/*gerando conflito*/