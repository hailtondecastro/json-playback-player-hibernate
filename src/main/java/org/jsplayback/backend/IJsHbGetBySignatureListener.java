package org.jsplayback.backend;

public interface IJsHbGetBySignatureListener {
	String getName();
	<T> T onBeforeBySignature(String signature, SignatureBean signatureBean);
	<T> T onAfterBySignature(String signature, SignatureBean signatureBean, T entity);
}
/*gerando conflito*/