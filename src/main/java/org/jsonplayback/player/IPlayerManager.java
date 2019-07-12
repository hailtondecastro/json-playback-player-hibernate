package org.jsonplayback.player;

import java.util.Map;
import java.util.Stack;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.jsonplayback.player.hibernate.AssociationAndComponentTrackInfo;
import org.jsonplayback.player.hibernate.JsHbBeanPropertyWriter;
import org.jsonplayback.player.hibernate.JsHbJsonSerializer;
import org.jsonplayback.player.hibernate.JsHbManager;
import org.jsonplayback.player.hibernate.JsHbResultEntity;

public interface IManager {
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

	JsHbManager configure(IConfig jsHbConfig);

	/**
	 * Threadsafe
	 */
	void startSuperSync();

	/**
	 * Threadsafe
	 */
	void stopSuperSync();

	boolean isStarted();

//	boolean isPersistentClassOrComponent(Class clazz);

	/**
	 * Sobreescreve a configuracao temporariamente. Eh thead safe e nao afeta as
	 * demais theads. Eh descartado em {@link #stopSuperSync()}.
	 * 
	 * @param newConfig
	 * @return
	 */
	IManager overwriteConfigurationTemporarily(IConfig newConfig);

	<T> JsHbResultEntity<T> createResultEntity(T result);

	/**
	 * Inicializa o Manager. Faz as cargas iniciais a partir dos metadatas hibernate
	 * e de quaisquer outras informacoes necessarias. <br>
	 * Eh chamado automaticamente no primeiro {@link #startSuperSync()} caso nao
	 * tenha sido chamado ainda. Pode ser chamado novamente a qualquer momento. Nao
	 * eh thread safe.
	 * 
	 * @return
	 */
	IManager init();

	IReplayable prepareReplayable(Tape tape);


	IManager cloneWithNewConfiguration(IConfig newConfig);
}
/*gerando conflito*/