package org.jsonplayback.player;

import java.util.Map;
import java.util.Stack;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.jsonplayback.player.hibernate.AssociationAndComponentTrackInfo;
import org.jsonplayback.player.hibernate.JsHbBeanPropertyWriter;
import org.jsonplayback.player.hibernate.JsHbJsonSerializer;
import org.jsonplayback.player.hibernate.JsHbPlayerManager;
import org.jsonplayback.player.hibernate.JsHbResultEntity;

public interface IPlayerManager {
	String serializeSignature(SignatureBean signatureBean);

	SignatureBean deserializeSignature(String signatureStr);

	/**
	 * Return not null value if the property is annotatted with
	 * {@link LazyProperty} for a
	 * {@link IDirectRawWriterWrapper#getCallback()}.{@link IDirectRawWriter#write(java.io.OutputStream)}
	 * call after set http header with {@link LazyProperty#contentTypePrefix()}.
	 * {@link LazyProperty#charset()} must be added to content-type if
	 * {@link LazyProperty#contentTypePrefix()} is "content-type: text/plain".
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

	IConfig getJsHbConfig();

	JsHbPlayerManager configure(IConfig jsHbConfig);

	/**
	 * Threadsafe
	 */
	void startJsonWriteIntersept();

	/**
	 * Threadsafe
	 */
	void stopJsonWriteIntersept();

	boolean isStarted();

//	boolean isPersistentClassOrComponent(Class clazz);

	/**
	 * Sobreescreve a configuracao temporariamente. Eh thead safe e nao afeta as
	 * demais theads. Eh descartado em {@link #stopJsonWriteIntersept()}.
	 * 
	 * @param newConfig
	 * @return
	 */
	IPlayerManager overwriteConfigurationTemporarily(IConfig newConfig);

	<T> JsHbResultEntity<T> createResultEntity(T result);

	/**
	 * Initializes the Manager. It does as the initial metadata loads hibernate
	 * and all other necessary information. Home
	 * Eh automatically called in the first {@link #startJsonWriteIntersept ()} if not
	 * has been called yet. It can be recalled at any time. Not
	 * eh thread safe.
	 * 
	 * @return
	 */
	IPlayerManager init();

	IReplayable prepareReplayable(Tape tape);


	IPlayerManager cloneWithNewConfiguration(IConfig newConfig);
}
/*gerando conflito*/