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

	public void writeHibernateId(Object value, JsonGenerator gen, SerializerProvider serializers)
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
							jsHbBeanPropertyWriter.serializeAsFieldHibernateIdentifier(value, gen, serializers);
						} catch (Exception e) {
							throw new RuntimeException("Nao deveria acontecer", e);
						}
						break;
					}
				}
			}
		} else {
			throw new RuntimeException("this.delegate nao eh " + BeanSerializer.class);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		try {
			if (this.jsHbManager.isStarted()) {
				this.jsHbManager.getJsHbJsonSerializerStepStackTL().push(this);
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
			} else {
				boolean wasWritenByRefOrBySigne = this.mayByRefOrBySigneSerialize(value, gen, serializers);
				if (!wasWritenByRefOrBySigne) {
					if (logger.isTraceEnabled()) {
						logger.trace("Not intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). !wasWritenByRefOrBySigne");
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
				if (this.jsHbManager.getJsHbJsonSerializerStepStackTL() != null) {
					JsHbJsonSerializer jsHbJsonSerializer = this.jsHbManager.getJsHbJsonSerializerStepStackTL().pop();
					if (jsHbJsonSerializer != this) {
						throw new RuntimeException("This should not happen");
					}
				}
			}
		}

		// Object unwrappedPojo = value;
		// if (value != null &&
		// generatorDelegate.idByObjectMap.containsKey(value)) {
		// gen.writeStartObject();
		// gen.writeFieldName(generatorDelegate.jsHbManager.getJsHbConfig().getJsHbIdRefName());
		// gen.writeNumber(generatorDelegate.idByObjectMap.get(value));
		// gen.writeEndObject();
		// } else if (value instanceof HibernateProxy) {
		// Class forValueClass = value.getClass().getSuperclass();
		// unwrappedPojo = ((HibernateProxy)
		// value).getHibernateLazyInitializer().gethibernate();
		// if (((HibernateProxy)
		// value).getHibernateLazyInitializer().isUninitialized()) {
		// generatorDelegate.currId++;
		// gen.writeStartObject();
		// generatorDelegate.objectByIdMap.put(generatorDelegate.currId, value);
		// generatorDelegate.idByObjectMap.put(value, generatorDelegate.currId);
		// gen.writeFieldName(generatorDelegate.jsHbManager.getJsHbConfig().getJsHbIdName());
		// gen.writeNumber(generatorDelegate.currId);
		// gen.writeFieldName(generatorDelegate.jsHbManager.getJsHbConfig().getJsHbIsLazyUninitializedName());
		// gen.writeBoolean(true);
		// if (!generatorDelegate.jsHbManager.isNotLazyClass(forValueClass)) {
		// SignatureBean signatureBean = generatorDelegate.jsHbManager
		// .generateLazySignature((HibernateProxy) value);
		// String signatureStr =
		// generatorDelegate.jsHbManager.serializeSignature(signatureBean);
		// gen.writeFieldName(generatorDelegate.jsHbManager.getJsHbConfig().getJsHbSignatureName());
		// gen.writeString(signatureStr);
		// }
		// gen.writeEndObject();
		// } else {
		// delegate.serialize(unwrappedPojo, generatorDelegate, serializers);
		// }
		// } else if (value instanceof PersistentCollection) {
		// PersistentCollection pcPojo = (PersistentCollection) value;
		// if (!pcPojo.wasInitialized()) {
		// gen.writeStartObject();
		// gen.writeFieldName(generatorDelegate.jsHbManager.getJsHbConfig().getJsHbIsLazyUninitializedName());
		// gen.writeBoolean(true);
		// SignatureBean signatureBean =
		// generatorDelegate.jsHbManager.generateLazySignature(pcPojo);
		// String signatureStr =
		// generatorDelegate.jsHbManager.serializeSignature(signatureBean);
		// gen.writeFieldName(generatorDelegate.jsHbManager.getJsHbConfig().getJsHbSignatureName());
		// gen.writeString(signatureStr);
		// } else {
		// ArrayList<Object> unwrappedList = new ArrayList<>();
		// for (Object item : (Collection) pcPojo) {
		// unwrappedList.add(item);
		// }
		// unwrappedPojo = unwrappedList;
		// this.delegate.serialize(unwrappedPojo, gen, serializers);
		// }
		// } else {
		// this.delegate.serialize(unwrappedPojo, gen, serializers);
		// }

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
				// if (fieldName != null) {
				// gen.writeFieldName(fieldName);
				// }
				if (logger.isTraceEnabled()) {
					logger.trace(MessageFormat.format(
							"Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). Writing '{'\"{0}\": \"{1}\"'}'",
							this.jsHbManager.getJsHbConfig().getJsHbIdRefName(),
							this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize))));
				}
				
				gen.writeStartObject();
				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIdRefName());
				gen.writeNumber(this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize)));
				gen.writeEndObject();
				return true;
			} else if (valueToSerialize instanceof HibernateProxy) {
				Class forValueClass = valueToSerialize.getClass().getSuperclass();
				if (this.jsHbManager.getIdByObjectMap().containsKey(new IdentityRefKey(valueToSerialize))) {
					// if (fieldName != null) {
					// gen.writeFieldName(fieldName);
					// }
					if (logger.isTraceEnabled()) {
						logger.trace(MessageFormat.format(
								"Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). Writing '{'\"{0}\": {1}'}'",
								this.jsHbManager.getJsHbConfig().getJsHbIdRefName(),
								this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize))));
					}
					gen.writeStartObject();
					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIdRefName());
					gen.writeNumber(this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize)));
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
				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIdName());
				gen.writeNumber(this.jsHbManager.getCurrId());
				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsLazyUninitializedName());
				gen.writeBoolean(true);
				
				if (logger.isTraceEnabled()) {
					logger.trace(MessageFormat.format(
							"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider):\n"
									+ " gen.writeStartObject();\n" + " gen.writeFieldName(\"{0}\");\n"
									+ " gen.writeNumber({1});\n" + " gen.writeFieldName(\"{2}\");\n"
									+ " gen.writeBoolean({3});",
							this.jsHbManager.getJsHbConfig().getJsHbIdName(), this.jsHbManager.getCurrId(),
							this.jsHbManager.getJsHbConfig().getJsHbIsLazyUninitializedName(), true));
				}
				
				if (this.jsHbManager.isPersistentClass(forValueClass)
						&& !this.jsHbManager.isNeverSigned(forValueClass)) {
					SignatureBean signatureBean = this.jsHbManager
							.generateLazySignature((HibernateProxy) valueToSerialize);
					String signatureStr = this.jsHbManager.serializeSignature(signatureBean);
					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbSignatureName());
					gen.writeString(signatureStr);
					
					if (logger.isTraceEnabled()) {
						logger.trace(MessageFormat.format(
								"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider):\n"
										+ " gen.writeString(\"{0}\");\n" + " gen.writeString(\"{1}\");\n"
												+ " this.writeHibernateId(valueToSerialize, gen, serializers);",
								this.jsHbManager.getJsHbConfig().getJsHbSignatureName(), signatureStr));
					}
					
					this.writeHibernateId(valueToSerialize, gen, serializers);
				}
				
				if (logger.isTraceEnabled()) {
					logger.trace(
							"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider):\n"
									+ " gen.writeEndObject();");
				}
				gen.writeEndObject();
				return true;
			} else {
				// unwrappedvalue =
				// ((HibernateProxy)valueToSerialize).getHibernateLazyInitializer().getImplementation();
				if (this.jsHbManager.getIdByObjectMap().containsKey(new IdentityRefKey(valueToSerialize))) {
					gen.writeStartObject();
					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIdRefName());
					gen.writeNumber(this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize)));
					gen.writeEndObject();
					if (logger.isTraceEnabled()) {
						Map<String, Object> anyLogMap = new LinkedHashMap<>();
						anyLogMap.put(this.jsHbManager.getJsHbConfig().getJsHbIdRefName(), this.jsHbManager.getIdByObjectMap().get(new IdentityRefKey(valueToSerialize)));
						logger.trace(MessageFormat.format(
								"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). wrinting:\n"
										+ "{0}",
								this.generateJsonStringForLog(anyLogMap)));
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
				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsLazyUninitializedName());
				gen.writeBoolean(true);
				SignatureBean signatureBean = this.jsHbManager.generateLazySignature(pcvalue);
				String signatureStr = this.jsHbManager.serializeSignature(signatureBean);
				gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbSignatureName());
				gen.writeString(signatureStr);
				gen.writeEndObject();
				
				if (logger.isTraceEnabled()) {
					Map<String, Object> anyLogMap = new LinkedHashMap<>();
					anyLogMap.put(this.jsHbManager.getJsHbConfig().getJsHbIsLazyUninitializedName(), true);
					anyLogMap.put(this.jsHbManager.getJsHbConfig().getJsHbSignatureName(), signatureStr);
					logger.trace(MessageFormat.format(
							"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). wrinting:\n"
									+ "{0}",
							this.generateJsonStringForLog(anyLogMap)));
				}
				return true;
			} else {
				return false;
			}
		} else {
			if (this.jsHbManager.getJsHbConfig().isSerialiseBySignatureAllRelationship()
					&& this.jsHbManager.getJsHbBeanPropertyWriterStepStack().size() > 0
					&& currPropertyWriter.getRelationshipOwnerClass() != null) {
				if (currPropertyWriter.getIsPersistent()) {
					gen.writeStartObject();
					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsLazyUninitializedName());
					gen.writeBoolean(true);

					SignatureBean signatureBean = this.jsHbManager.generateLazySignatureForRelashionship(
							currPropertyWriter.getCurrOwner().getClass(),
							currPropertyWriter.getBeanPropertyDefinition().getInternalName(),
							currPropertyWriter.getCurrOwner(), valueToSerialize);
					String signatureStr = this.jsHbManager.serializeSignature(signatureBean);
					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbSignatureName());
					gen.writeString(signatureStr);

					if (logger.isTraceEnabled()) {
						logger.trace(MessageFormat.format(
								"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider):\n"
										+ " gen.writeStartObject();\n" + " gen.writeFieldName(\"{0}\");\n"
										+ " gen.writeBoolean({1});\n" + " gen.writeFieldName(\"{2}\");\n"
										+ " gen.writeString(3);",
								this.jsHbManager.getJsHbConfig().getJsHbIsLazyUninitializedName(), true,
								this.jsHbManager.getJsHbConfig().getJsHbSignatureName(), signatureStr));
					}
					
					if (!(valueToSerialize instanceof Collection)) {
						if (logger.isTraceEnabled()) {
							logger.trace(
									"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). !(valueToSerialize instanceof Collection):\n"
											+ " this.writeHibernateId(valueToSerialize, gen, serializers)");
						}
						this.writeHibernateId(valueToSerialize, gen, serializers);
					}
					if (logger.isTraceEnabled()) {
						logger.trace(
								"mayWriteBySignatureRef(). Intercepting JsonSerializer.serialize(T, JsonGenerator, SerializerProvider). !(valueToSerialize instanceof Collection):\n"
										+ " this.writeHibernateId(valueToSerialize, gen, serializers)");
					}
					gen.writeEndObject();
					return true;
				}
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
/*gerando conflito*/
/*gerando conflito*/