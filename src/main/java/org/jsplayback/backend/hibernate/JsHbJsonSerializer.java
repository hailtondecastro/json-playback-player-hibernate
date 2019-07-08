package org.jsplayback.backend.hibernate;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

import org.jsplayback.backend.IJsHbManager;
import org.jsplayback.backend.IdentityRefKey;
import org.jsplayback.backend.SignatureBean;

public class JsHbJsonSerializer extends JsonSerializer<Object> {
	private static Logger logger = LoggerFactory.getLogger(JsHbJsonSerializer.class);
	
	IJsHbManager jsHbManager;
	private SerializerProvider serializers;

	public JsHbJsonSerializer configJsHbManager(IJsHbManager jsHbManager) {
		this.jsHbManager = jsHbManager;
		return this;
	}

	@SuppressWarnings("rawtypes")
	JsonSerializer delegate;

	public JsHbJsonSerializer(JsonSerializer delegate) {
		super();
		this.delegate = delegate;
	}

	// private Boolean isPersistentClass = null;

	private JsHbBeanPropertyWriter hbIdPropertyWriter = null;

	public void findHibernateId(Object value, JsonGenerator gen, SerializerProvider serializers, JsHbBackendMetadatas backendMetadatas)
			throws IOException, JsonProcessingException {
		if (this.delegate instanceof BeanSerializer) {
			BeanSerializer beanSerializer = (BeanSerializer) this.delegate;
			Iterator<PropertyWriter> iterator = beanSerializer.properties();
			while (iterator.hasNext()) {
				PropertyWriter propertyWriter = (PropertyWriter) iterator.next();
				if (propertyWriter instanceof JsHbBeanPropertyWriter) {
					JsHbBeanPropertyWriter jsHbBeanPropertyWriter = (JsHbBeanPropertyWriter) propertyWriter;
					if (jsHbBeanPropertyWriter.getIsHibernateId()) {
						try {
							if (logger.isTraceEnabled()) {
								logger.trace(
										"writeHibernateId(Object, JsonGenerator, SerializerProvider):\n"
												+ " jsHbBeanPropertyWriter.serializeAsFieldHibernateIdentifier(value, gen, serializers)");
							}
							jsHbBeanPropertyWriter.findFieldHibernateIdentifierValue(value, serializers, backendMetadatas);
						} catch (Exception e) {
							throw new RuntimeException("Nao deveria acontecer", e);
						}
						break;
					}
				}
			}
		} else {
			throw new RuntimeException("this.delegate is not " + BeanSerializer.class);
		}
	}

//	public void writeHibernateId(Object value, JsonGenerator gen, SerializerProvider serializers)
//			throws IOException, JsonProcessingException {
//		if (this.delegate instanceof BeanSerializer) {
//			BeanSerializer beanSerializer = (BeanSerializer) this.delegate;
//			Iterator<PropertyWriter> iterator = beanSerializer.properties();
//			while (iterator.hasNext()) {
//				PropertyWriter propertyWriter = (PropertyWriter) iterator.next();
//				if (propertyWriter instanceof JsHbBeanPropertyWriter) {
//					JsHbBeanPropertyWriter jsHbBeanPropertyWriter = (JsHbBeanPropertyWriter) propertyWriter;
//					if (jsHbBeanPropertyWriter.getIsHibernateId()) {
//						try {
//							if (logger.isTraceEnabled()) {
//								logger.trace(
//										"writeHibernateId(Object, JsonGenerator, SerializerProvider):\n"
//												+ " jsHbBeanPropertyWriter.serializeAsFieldHibernateIdentifier(value, gen, serializers)");
//							}
//							jsHbBeanPropertyWriter.serializeAsFieldHibernateIdentifier(value, gen, serializers);
//						} catch (Exception e) {
//							throw new RuntimeException("Nao deveria acontecer", e);
//						}
//						break;
//					}
//				}
//			}
//		} else {
//			throw new RuntimeException("this.delegate nao eh " + BeanSerializer.class);
//		}
//	}

	@SuppressWarnings("unchecked")
	@Override
	public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		try {
			if (this.jsHbManager.isStarted()) {
				this.jsHbManager.getJsHbJsonSerializerStepStack().push(this);
			}
			
			Class valueResolvedClass = null;
			// if (this.isPersistentClass == null)
			// if (value != null) {
			// if (value instanceof HibernateProxy) {
			// valueResolvedClass = value.getClass().getSuperclass();
			// this.isPersistentClass =
			// this.jsHbManager.isPersistentClass(valueResolvedClass);
			// }
			// }

			if (!this.jsHbManager.isStarted()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Not Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). !this.jsHbManager.isStarted()");
				}
				this.delegate.serialize(value, gen, serializers);
			} else if (value == null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Not Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). value == null");
				}
				this.delegate.serialize(value, gen, serializers);
			} else if (value instanceof JsHbResultEntity) {
				if (logger.isTraceEnabled()) {
					logger.trace("Not Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). value instanceof JsHbResultEntity");
				}
				this.delegate.serialize(value, gen, serializers);
			} else if (value instanceof JsHbBackendMetadatas) {
				if (logger.isTraceEnabled()) {
					logger.trace("Not Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). value instanceof JsHbBackendMetadatas");
				}
				this.delegate.serialize(value, gen, serializers);
			} else {
				boolean wasWritenByRefOrBySigne = this.mayByRefOrBySigneSerialize(value, gen, serializers);
				if (!wasWritenByRefOrBySigne) {
					if (logger.isTraceEnabled()) {
						logger.trace("Not serialize by reference or By Signature or by reference. JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). !wasWritenByRefOrBySigne");
					}
					JsonGenerator newGen = gen;
					if (!(gen instanceof JsHbJsonGeneratorDelegate)) {
//@formatter:off
					newGen = new JsHbJsonGeneratorDelegate(gen)
							.configJsHbManager(this.jsHbManager)
							.configSerializers(serializers);
//@formatter:on
					}
					// nao pode ser serializado por referencia ou lazy
					this.delegate.serialize(value, newGen, serializers);
				}
			}
		} finally {
			if (this.jsHbManager.isStarted()) {
				if (this.jsHbManager.getJsHbJsonSerializerStepStack() != null) {
					JsHbJsonSerializer jsHbJsonSerializer = this.jsHbManager.getJsHbJsonSerializerStepStack().pop();
					if (jsHbJsonSerializer != this) {
						throw new RuntimeException("This should not happen");
					}
				}
			}
		}
	}

	private boolean mayByRefOrBySigneSerialize(Object valueToSerialize, JsonGenerator gen,
			SerializerProvider serializers) throws IOException {
		// Object unwrappedvalue = valueToSerialize;
		if (valueToSerialize == null) {
			throw new IllegalArgumentException("value can not be null");
		}
		// boolean wasWritenByLazyRef = this.mayWriteBySignatureRef(owner,
		// valueToSerialize, gen, serializers, fieldName);
		boolean wasWritenByLazyRef = this.mayWriteBySignatureRef(valueToSerialize, gen, serializers);
		if (wasWritenByLazyRef) {
			return true;
		} else {
			if (this.jsHbManager.getIdByObjectMap().containsKey(new IdentityRefKey(valueToSerialize))) {
				gen.writeStartObject();
				JsHbBackendMetadatas backendMetadatas = new JsHbBackendMetadatas();
				backendMetadatas.setIdRef(this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize)));
//				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIdRefName());
//				gen.writeNumber(this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize)));
//				if (logger.isTraceEnabled()) {
//					logger.trace(MessageFormat.format(
//							"Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). Writing '{'\"{0}\": \"{1}\"'}'",
//							this.jsHbManager.getJsHbConfig().getJsHbMetadatasName(),
//							backendMetadatas));
				// }
				if (logger.isTraceEnabled()) {
					logger.trace(
						MessageFormat.format(
							"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object).\n"
							+ " gen.writeStartObject();\n"
							+ " gen.writeEndObject();\n"
							+ " gen.writeFieldName(\"{0}\");\n"
							+ " gen.writeObject({1});\n",								
						this.jsHbManager.getJsHbConfig().getJsHbMetadatasName(), 
						backendMetadatas));
				}
				
				try {
					this.jsHbManager.getJsHbBackendMetadatasWritingStack().push(backendMetadatas);
					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbMetadatasName());
					gen.writeObject(backendMetadatas);
				} finally {
					if (this.jsHbManager.getJsHbBackendMetadatasWritingStack() != null) {
						JsHbBackendMetadatas backendMetadatasPoped = this.jsHbManager.getJsHbBackendMetadatasWritingStack().pop();
						if (backendMetadatasPoped != backendMetadatas) {
							throw new RuntimeException("This should not happen");
						}					
					}
				}
				
				gen.writeEndObject();
				return true;
			} else if (valueToSerialize instanceof HibernateProxy) {
				Class forValueClass = valueToSerialize.getClass().getSuperclass();
				if (this.jsHbManager.getIdByObjectMap().containsKey(new IdentityRefKey(valueToSerialize))) {
					gen.writeStartObject();
					JsHbBackendMetadatas backendMetadatas = new JsHbBackendMetadatas();
					backendMetadatas.setIdRef(this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize)));
//					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIdRefName());
//					gen.writeNumber(this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize)));
//					if (logger.isTraceEnabled()) {
//						logger.trace(MessageFormat.format(
//								"Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). Writing '{'\"{0}\": {1}'}'",
//								this.jsHbManager.getJsHbConfig().getJsHbMetadatasName(),
//								backendMetadatas));
					// }
					if (logger.isTraceEnabled()) {
						logger.trace(
							MessageFormat.format(
								"Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). Writing\n"
								+ " gen.writeStartObject();\n"
								+ " gen.writeFieldName(\"{0}\");\n"
								+ " gen.writeObject({1});\n"
								+ " gen.writeEndObject();",								
							this.jsHbManager.getJsHbConfig().getJsHbMetadatasName(), 
							backendMetadatas));
					}
					try {
						this.jsHbManager.getJsHbBackendMetadatasWritingStack().push(backendMetadatas);				
						gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbMetadatasName());
						gen.writeObject(backendMetadatas);
					} finally {
						if (this.jsHbManager.getJsHbBackendMetadatasWritingStack() != null) {
							JsHbBackendMetadatas backendMetadatasPoped = this.jsHbManager.getJsHbBackendMetadatasWritingStack().pop();
							if (backendMetadatasPoped != backendMetadatas) {
								throw new RuntimeException("This should not happen");
							}					
						}
					}
					gen.writeEndObject();
					return true;
				} else {
					return false;
				}
			} else if (valueToSerialize instanceof PersistentCollection) {
				return false;
			} else {
				return false;
			}
		}
	}

	private boolean mayWriteBySignatureRef(Object valueToSerialize, JsonGenerator gen, SerializerProvider serializers)
			throws IOException {
		Object unwrappedvalue = valueToSerialize;
		// SerializableString fieldName = null;
		// Object owner = null;

		JsHbBeanPropertyWriter currPropertyWriter = null;

		if (this.jsHbManager.getJsHbBeanPropertyWriterStepStack().size() > 0) {
			currPropertyWriter = this.jsHbManager.getJsHbBeanPropertyWriterStepStack().peek();
		}

		if (valueToSerialize instanceof HibernateProxy) {
			Class forValueClass = valueToSerialize.getClass().getSuperclass();
			if (this.jsHbManager.getJsHbConfig().isSerialiseBySignatureAllRelationship()
					|| ((HibernateProxy) valueToSerialize).getHibernateLazyInitializer().isUninitialized()) {
				gen.writeStartObject();
				this.jsHbManager.currIdPlusPlus();
				this.jsHbManager.getObjectByIdMap().put(this.jsHbManager.getCurrId(), valueToSerialize);
				this.jsHbManager.getIdByObjectMap().put(new IdentityRefKey(valueToSerialize),
						this.jsHbManager.getCurrId());
				JsHbBackendMetadatas backendMetadatas = new JsHbBackendMetadatas();
				backendMetadatas.setId(this.jsHbManager.getCurrId());
				backendMetadatas.setIsLazyUninitialized(true);
				backendMetadatas.setIsAssociative(true);
//				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIdName());
//				gen.writeNumber(this.jsHbManager.getCurrId());
//				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsLazyUninitializedName());
//				gen.writeBoolean(true);
//				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsAssociativeName());
//				gen.writeBoolean(true);
				
				if (logger.isTraceEnabled()) {
					logger.trace(MessageFormat.format(
							"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider):\n"
									+ " gen.writeStartObject();\n"
									+ " {0}: {1};\n"
									+ " {2}: {3}\n"
									+ " {4}: {5}",
							"backendMetadatas.id", this.jsHbManager.getCurrId(),
							"backendMetadatas.isLazyUninitialized", true,
							"backendMetadatas.isAssociative", true));
				}
				
				if (this.jsHbManager.isPersistentClass(forValueClass)
						&& !this.jsHbManager.isNeverSigned(forValueClass)) {
					SignatureBean signatureBean = this.jsHbManager
							.generateLazySignature((HibernateProxy) valueToSerialize);
					String signatureStr = this.jsHbManager.serializeSignature(signatureBean);
					backendMetadatas.setSignature(signatureStr);
//					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbSignatureName());
//					gen.writeString(signatureStr);
					
					if (logger.isTraceEnabled()) {
						logger.trace(MessageFormat.format(
								"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider):\n"
												+ " this.findHibernateId(valueToSerialize, gen, serializers, {0});",
												backendMetadatas));
					}
					
					this.findHibernateId(valueToSerialize, gen, serializers, backendMetadatas);
				}
				
				if (logger.isTraceEnabled()) {
					logger.trace(
						MessageFormat.format(
							"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider):\n"
							+ " gen.writeFieldName(\"{0}\");\n"
							+ " gen.writeObject({1});\n"
							+ " gen.writeEndObject();",								
						this.jsHbManager.getJsHbConfig().getJsHbMetadatasName(), 
						backendMetadatas));
				}
				try {
					this.jsHbManager.getJsHbBackendMetadatasWritingStack().push(backendMetadatas);				
					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbMetadatasName());
					gen.writeObject(backendMetadatas);
				} finally {
					if (this.jsHbManager.getJsHbBackendMetadatasWritingStack() != null) {
						JsHbBackendMetadatas backendMetadatasPoped = this.jsHbManager.getJsHbBackendMetadatasWritingStack().pop();
						if (backendMetadatasPoped != backendMetadatas) {
							throw new RuntimeException("This should not happen");
						}					
					}
				}
				gen.writeEndObject();
				return true;
			} else {
				// unwrappedvalue =
				// ((HibernateProxy)valueToSerialize).getHibernateLazyInitializer().getImplementation();
				if (this.jsHbManager.getIdByObjectMap().containsKey(new IdentityRefKey(valueToSerialize))) {
					gen.writeStartObject();
					JsHbBackendMetadatas backendMetadatas = new JsHbBackendMetadatas();
					backendMetadatas.setIdRef(this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize)));
					try {
						this.jsHbManager.getJsHbBackendMetadatasWritingStack().push(backendMetadatas);				
						gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbMetadatasName());
						gen.writeObject(backendMetadatas);
					} finally {
						if (this.jsHbManager.getJsHbBackendMetadatasWritingStack() != null) {
							JsHbBackendMetadatas backendMetadatasPoped = this.jsHbManager.getJsHbBackendMetadatasWritingStack().pop();
							if (backendMetadatasPoped != backendMetadatas) {
								throw new RuntimeException("This should not happen");
							}					
						}
					}					
//					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIdRefName());
//					gen.writeNumber(this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize)));
					gen.writeEndObject();
//					if (logger.isTraceEnabled()) {
//						Map<String, Object> anyLogMap = new LinkedHashMap<>();
//						anyLogMap.put(this.jsHbManager.getJsHbConfig().getJsHbMetadatasName(), backendMetadatas);
//						logger.trace(MessageFormat.format(
//								"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). wrinting:\n"
//										+ "{0}",
//								this.generateJsonStringForLog(anyLogMap)));
//					}
					if (logger.isTraceEnabled()) {
						logger.trace(
							MessageFormat.format(
								"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). wrinting:\n"
								+ " gen.writeStartObject();\n"
								+ " gen.writeFieldName(\"{0}\");\n"
								+ " gen.writeObject({1});\n"
								+ " gen.writeEndObject();",								
							this.jsHbManager.getJsHbConfig().getJsHbMetadatasName(), 
							backendMetadatas));
					}
					return true;
				} else {
					return false;
				}
			}
		} else if (valueToSerialize instanceof PersistentCollection) {
			PersistentCollection pcvalue = (PersistentCollection) valueToSerialize;
			if (this.jsHbManager.getJsHbConfig().isSerialiseBySignatureAllRelationship() || !pcvalue.wasInitialized()) {
				gen.writeStartObject();
				JsHbBackendMetadatas backendMetadatas = new JsHbBackendMetadatas();
				backendMetadatas.setIsLazyUninitialized(true);
				backendMetadatas.setIsAssociative(true);
//				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsLazyUninitializedName());
//				gen.writeBoolean(true);
//				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsAssociativeName());
//				gen.writeBoolean(true);
				SignatureBean signatureBean = this.jsHbManager.generateLazySignature(pcvalue);
				String signatureStr = this.jsHbManager.serializeSignature(signatureBean);
				backendMetadatas.setSignature(signatureStr);
//				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbSignatureName());
//				gen.writeString(signatureStr);
				try {
					this.jsHbManager.getJsHbBackendMetadatasWritingStack().push(backendMetadatas);				
					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbMetadatasName());
					gen.writeObject(backendMetadatas);				
				} finally {
					if (this.jsHbManager.getJsHbBackendMetadatasWritingStack() != null) {
						JsHbBackendMetadatas backendMetadatasPoped = this.jsHbManager.getJsHbBackendMetadatasWritingStack().pop();
						if (backendMetadatasPoped != backendMetadatas) {
							throw new RuntimeException("This should not happen");
						}					
					}
				}
				gen.writeEndObject();
				
				if (logger.isTraceEnabled()) {
					logger.trace(
						MessageFormat.format(
							"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider).\n"
							+ " gen.writeStartObject();\n"
							+ " gen.writeFieldName(\"{0}\");\n"
							+ " gen.writeObject({1});\n"
							+ " gen.writeEndObject();",								
						this.jsHbManager.getJsHbConfig().getJsHbMetadatasName(), 
						backendMetadatas));
				}
				return true;
			} else {
				return false;
			}
		} else {
			if (this.jsHbManager.getJsHbConfig().isSerialiseBySignatureAllRelationship()
					&& this.jsHbManager.getJsHbBeanPropertyWriterStepStack().size() > 0
					&& currPropertyWriter.getRelationshipOwnerClass() != null) {
				//if (entityAndComponentTrackInfo != null) {
				if (currPropertyWriter.getIsPersistent()) {
					gen.writeStartObject();
					JsHbBackendMetadatas backendMetadatas = new JsHbBackendMetadatas();
					backendMetadatas.setIsLazyUninitialized(true);
					backendMetadatas.setIsAssociative(true);
//					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsLazyUninitializedName());
//					gen.writeBoolean(true);
//					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsAssociativeName());
//					gen.writeBoolean(true);

					SignatureBean signatureBean = null;
						
					signatureBean = this.jsHbManager.generateLazySignatureForRelashionship(
							currPropertyWriter.getCurrOwner().getClass(),
							currPropertyWriter.getBeanPropertyDefinition().getInternalName(),
							currPropertyWriter.getCurrOwner(),
							valueToSerialize);					
					
					String signatureStr = this.jsHbManager.serializeSignature(signatureBean);
					backendMetadatas.setSignature(signatureStr);
//					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbSignatureName());
//					gen.writeString(signatureStr);

					if (logger.isTraceEnabled()) {
						logger.trace(MessageFormat.format(
								"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider):\n"
										+ " gen.writeStartObject();\n"
										+ " backendMetadatas.isLazyUninitialized: {0};\n"
										+ " backendMetadatas.isAssociative: {1});\n"
										+ " backendMetadatas.signature: \"{2}\";\n",
								true,
								true,
								signatureStr));
					}
					
					if (!(valueToSerialize instanceof Collection)) {
						if (logger.isTraceEnabled()) {
							logger.trace(
									"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). !(valueToSerialize instanceof Collection):\n"
											+ " this.findHibernateId(valueToSerialize, gen, serializers, backendMetadatas)");
						}
						this.findHibernateId(valueToSerialize, gen, serializers, backendMetadatas);
					}
					if (logger.isTraceEnabled()) {
						logger.trace(
							MessageFormat.format(
								"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object).\n"
								+ " gen.writeFieldName(\"{0}\");\n"
								+ " gen.writeObject({1});\n"
								+ " gen.writeEndObject();",								
							this.jsHbManager.getJsHbConfig().getJsHbMetadatasName(), 
							backendMetadatas));
					}
					try {
						this.jsHbManager.getJsHbBackendMetadatasWritingStack().push(backendMetadatas);				
						gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbMetadatasName());
						gen.writeObject(backendMetadatas);
					} finally {
						if (this.jsHbManager.getJsHbBackendMetadatasWritingStack() != null) {
							JsHbBackendMetadatas backendMetadatasPoped = this.jsHbManager.getJsHbBackendMetadatasWritingStack().pop();
							if (backendMetadatasPoped != backendMetadatas) {
								throw new RuntimeException("This should not happen");
							}					
						}
					}
					gen.writeEndObject();
					return true;
				}
			} else {
			}
			return false;
		}
	}

	@SuppressWarnings("rawtypes")
	private String generateJsonStringForLog(Map anyMap) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(anyMap);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("This should not happen", e);
		}		
	}
	
	@Override
	public String toString() {
		return "JsHbJsonSerializer for " + this.delegate;
	}
}
