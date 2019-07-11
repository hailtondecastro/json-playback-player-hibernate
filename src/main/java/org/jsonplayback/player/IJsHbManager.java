package org.jsonplayback.player;

import java.util.Map;
import java.util.Stack;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.jsonplayback.player.hibernate.AssociationAndComponentTrackInfo;
import org.jsonplayback.player.hibernate.JsHbBackendMetadatas;
import org.jsonplayback.player.hibernate.JsHbBeanPropertyWriter;
import org.jsonplayback.player.hibernate.JsHbJsonSerializer;
import org.jsonplayback.player.hibernate.JsHbManager;
import org.jsonplayback.player.hibernate.JsHbPlayback;
import org.jsonplayback.player.hibernate.JsHbResultEntity;

public interface IJsHbManager {
	SignatureBean generateLazySignature(PersistentCollection persistentCollection);

	SignatureBean generateLazySignature(HibernateProxy hibernateProxy);

	SignatureBean generateSignature(Object nonHibernateProxy);

	String serializeSignature(SignatureBean signatureBean);

	SignatureBean deserializeSignature(String signatureStr);

	/**
	 * Return not null value if the property is annotatted with
	 * {@link JsHbLazyProperty} for a
	 * {@link IDirectRawWriterWrapper#getCallback()}.{@link IDirectRawWriter#write(java.io.OutputStream)}
	 * call after set http header with {@link JsHbLazyProperty#contentTypePrefix()}.
	 * {@link JsHbLazyProperty#charset()} must be added to content-type if
	 * {@link JsHbLazyProperty#contentTypePrefix()} is "content-type: text/plain".
	 * Example:
	 * <code>directRawWriterWrapper.getJsHbLazyProperty().contentTypePrefix() + "; " + directRawWriterWrapper.getJsHbLazyProperty().charset()</code>
	 * 
	 * @param signature
	 * @return
	 */
	IDirectRawWriterWrapper needDirectWrite(SignatureBean signature);

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

	/**
	 * Internal use.
	 * @return
	 */
	Map<IdentityRefKey, Long> getIdByObjectMap();

	Long getCurrId();

	void currIdPlusPlus();

	boolean isStarted();

//	boolean isPersistentClassOrComponent(Class clazz);

	boolean isRelationship(Class<?> clazz, String fieldName);

	SignatureBean generateLazySignatureForCollRelashionship(Class<?> ownerClass, String fieldName, Object ownerValue,
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

	boolean isComponent(Class<?> componentClass);

	/**
	 * Inicializa o Manager. Faz as cargas iniciais a partir dos metadatas hibernate
	 * e de quaisquer outras informacoes necessarias. <br>
	 * Eh chamado automaticamente no primeiro {@link #startSuperSync()} caso nao
	 * tenha sido chamado ainda. Pode ser chamado novamente a qualquer momento. Nao
	 * eh thread safe.
	 * 
	 * @return
	 */
	IJsHbManager init();

	boolean isPersistentClass(Class clazz);

	Stack<JsHbBeanPropertyWriter> getJsHbBeanPropertyWriterStepStack();

//	Stack<String> getCurrentCompositePathStack();
//	Object getCurrentCompositeOwner();
	
	AssociationAndComponentTrackInfo getCurrentAssociationAndComponentTrackInfo();

	String getHibernateIdName(Class clazz);

	Stack<JsHbJsonSerializer> getJsHbJsonSerializerStepStack();

	IJsHbReplayable prepareReplayable(JsHbPlayback playback);

	SignatureBean generateComponentSignature(AssociationAndComponentTrackInfo entityAndComponentTrackInfo);

	Stack<JsHbBackendMetadatas> getJsHbBackendMetadatasWritingStack();

	SignatureBean generateLazySignatureForJsHbLazyProperty(Class<?> ownerClass, String fieldName, Object ownerValue,
			Object fieldValue);

	IJsHbManager cloneWithNewConfiguration(IJsHbConfig newConfig);

	String getCurrentPathFromLastEntity();

	boolean isCurrentPathFromLastEntityAnEntityRelationship();

	Map<IdentityRefKey, JsHbBackendMetadatas> getMetadatasCacheMap();
}
/*gerando conflito*/