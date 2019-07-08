package org.jsplayback.backend.hibernate;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.jsplayback.backend.IJsHbManager;
import org.jsplayback.backend.IdentityRefKey;
import org.jsplayback.backend.SignatureBean;

public class JsHbJsonGeneratorDelegate extends JsonGeneratorDelegate {
	private static Logger logger = LoggerFactory.getLogger(JsHbJsonGeneratorDelegate.class);
	
	IJsHbManager jsHbManager;
	private SerializerProvider serializers;

	public JsHbJsonGeneratorDelegate configSerializers(SerializerProvider serializers) {
		this.serializers = serializers;
		return this;
	}

	public JsHbJsonGeneratorDelegate configJsHbManager(IJsHbManager jsHbManager) {
		this.jsHbManager = jsHbManager;
		return this;
	}
	
	public JsHbJsonGeneratorDelegate(JsonGenerator d) {
		super(d);
	}
	
	@Override
	public void writeStartObject(Object forValue) throws IOException {
		this.delegate.writeStartObject(forValue);
		if (!this.jsHbManager.isStarted()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Not Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). !this.jsHbManager.isStarted()");
			}
			this.delegate.writeStartObject();
		} else if (forValue instanceof JsHbResultEntity) {
			if (logger.isTraceEnabled()) {
				logger.trace("Not Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). forValue instanceof JsHbResultEntity");
			}
			this.delegate.writeStartObject();
		} else if (this.jsHbManager.getIdByObjectMap().containsKey(new IdentityRefKey(forValue))) {
			throw new RuntimeException(MessageFormat.format("Serializing an object that has been serialized and referenced. {0}: {1}", this.jsHbManager.getIdByObjectMap().get(forValue), forValue));
		} else {
			this.jsHbManager.currIdPlusPlus();
			this.jsHbManager.getObjectByIdMap().put(this.jsHbManager.getCurrId(), forValue);
			this.jsHbManager.getIdByObjectMap().put(new IdentityRefKey(forValue), this.jsHbManager.getCurrId());
			if (logger.isTraceEnabled()) {
				logger.trace(MessageFormat.format(
						"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). Injecting field \"{0}\": {1}",
						this.jsHbManager.getJsHbConfig().getJsHbIdName(), this.jsHbManager.getCurrId()));
			}
			this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIdName());
			this.writeNumber(this.jsHbManager.getCurrId());
			if (this.jsHbManager.isPersistentClass(forValue.getClass()) && !this.jsHbManager.isNeverSigned(forValue.getClass())) {
				SignatureBean signatureBean = this.jsHbManager.generateSignature(forValue);
				String signatureStr = this.jsHbManager.serializeSignature(signatureBean);
				if (logger.isTraceEnabled()) {
					logger.trace(MessageFormat.format(
							"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). It is a persistent class. Injecting field \"{0}\": \"{1}\"",
							this.jsHbManager.getJsHbConfig().getJsHbSignatureName(), signatureStr));
				}
				this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbSignatureName());
				this.writeString(signatureStr);
				if (this.jsHbManager.getJsHbBeanPropertyWriterStepStack().size() > 0) {
					if (logger.isTraceEnabled()) {
						logger.trace(MessageFormat.format(
								"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). It is an associative class property. Injecting field \"{0}\": \"{1}\"",
								this.jsHbManager.getJsHbConfig().getJsHbIsAssociativeName(), true));
					}
					this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsAssociativeName());
					this.writeBoolean(true);
				}
				this.jsHbManager.getJsHbJsonSerializerStepStackTL().peek().writeHibernateId(forValue, this, serializers);
			} else {
				EntityAndComponentTrackInfo entityAndComponentTrackInfo = this.jsHbManager.getCurrentComponentTypeEntry();
				if (entityAndComponentTrackInfo != null && !this.jsHbManager.isNeverSigned(forValue.getClass())) {
					SignatureBean signatureBean = this.jsHbManager.generateComponentSignature(entityAndComponentTrackInfo);
					String signatureStr = this.jsHbManager.serializeSignature(signatureBean);
					if (logger.isTraceEnabled()) {
						if (logger.isTraceEnabled()) {
							Map<String, Object> anyLogMap = new LinkedHashMap<>();
							anyLogMap.put(this.jsHbManager.getJsHbConfig().getJsHbSignatureName(), signatureStr);
							anyLogMap.put(this.jsHbManager.getJsHbConfig().getJsHbIsComponentName(), true);
							String jsonLogMsg = this.generateJsonStringForLog(anyLogMap);
							jsonLogMsg = jsonLogMsg.substring(1, jsonLogMsg.length() - 1);
							String logMsg =
								MessageFormat.format(
									"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). It is a componnent class property. Injecting fields:\n"
											+ "{0}",
											jsonLogMsg
									); 
							logger.trace(logMsg);
						}
						
					}
					this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbSignatureName());
					this.writeString(signatureStr);
					this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsComponentName());
					this.writeBoolean(true);
					this.jsHbManager.getJsHbJsonSerializerStepStackTL().peek().writeHibernateId(forValue, this, serializers);
				} else {
//					this.jsHbManager.currIdPlusPlus();
//					this.jsHbManager.getObjectByIdMap().put(this.jsHbManager.getCurrId(), forValue);
//					this.jsHbManager.getIdByObjectMap().put(new IdentityRefKey(forValue), this.jsHbManager.getCurrId());
//					if (logger.isTraceEnabled()) {
//						logger.trace(MessageFormat.format(
//								"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). Injecting field \"{0}\": {1}",
//								this.jsHbManager.getJsHbConfig().getJsHbIdName(), this.jsHbManager.getCurrId()));
//					}
//					this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIdName());
//					this.writeNumber(this.jsHbManager.getCurrId());
				}
			}
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
}
/*gerando conflito*/