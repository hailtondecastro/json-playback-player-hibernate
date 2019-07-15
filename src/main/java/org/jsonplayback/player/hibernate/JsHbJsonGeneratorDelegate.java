package org.jsonplayback.player.hibernate;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jsonplayback.player.IPlayerManager;
import org.jsonplayback.player.IdentityRefKey;
import org.jsonplayback.player.PlayerSnapshot;
import org.jsonplayback.player.PlayerMetadatas;
import org.jsonplayback.player.SignatureBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JsHbJsonGeneratorDelegate extends JsonGeneratorDelegate {
	private static Logger logger = LoggerFactory.getLogger(JsHbJsonGeneratorDelegate.class);
	
	IPlayerManagerImplementor jsHbManager;
	private SerializerProvider serializers;

	public JsHbJsonGeneratorDelegate configSerializers(SerializerProvider serializers) {
		this.serializers = serializers;
		return this;
	}

	public JsHbJsonGeneratorDelegate configJsHbManager(IPlayerManagerImplementor jsHbManager) {
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
		} else if (forValue instanceof PlayerSnapshot) {
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
						"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). Setting \"{0}\": {1}",
						"backendMetadatas.id", this.jsHbManager.getCurrId()));
			}
			PlayerMetadatas backendMetadatas = new PlayerMetadatas();
			backendMetadatas.setId(this.jsHbManager.getCurrId());
//			this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIdName());
//			this.writeNumber(this.jsHbManager.getCurrId());
			
			if (this.jsHbManager.isPersistentClass(forValue.getClass()) && !this.jsHbManager.isNeverSigned(forValue.getClass())) {
				SignatureBean signatureBean = this.jsHbManager.generateSignature(forValue);
				String signatureStr = this.jsHbManager.serializeSignature(signatureBean);
				if (logger.isTraceEnabled()) {
					logger.trace(MessageFormat.format(
							"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). It is a persistent class. Setting \"{0}\": \"{1}\"",
							"backendMetadatas.signature", signatureStr));
				}
				backendMetadatas.setSignature(signatureStr);
//				this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbSignatureName());
//				this.writeString(signatureStr);
				if (this.jsHbManager.getJsHbBeanPropertyWriterStepStack().size() > 0) {
					if (logger.isTraceEnabled()) {
						logger.trace(MessageFormat.format(
								"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). It is an associative class property. Setting \"{0}\": \"{1}\"",
								"backendMetadatas.isAssociative", true));
					}
					backendMetadatas.setIsAssociative(true);
//					this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsAssociativeName());
//					this.writeBoolean(true);
				}
				this.jsHbManager.getJsHbJsonSerializerStepStack().peek().findPlayerObjectId(forValue, this, serializers, backendMetadatas);
			} else {
				AssociationAndComponentTrackInfo aacTrackInfo = this.jsHbManager.getCurrentAssociationAndComponentTrackInfo();
				if (aacTrackInfo != null && !this.jsHbManager.isNeverSigned(forValue.getClass())) {
					SignatureBean signatureBean = this.jsHbManager.generateComponentSignature(aacTrackInfo);
					String signatureStr = this.jsHbManager.serializeSignature(signatureBean);
					if (logger.isTraceEnabled()) {
						if (logger.isTraceEnabled()) {
							Map<String, Object> anyLogMap = new LinkedHashMap<>();
							anyLogMap.put("backendMetadatas.signature", signatureStr);
							anyLogMap.put("backendMetadatas.isComponent", true);
							String jsonLogMsg = this.generateJsonStringForLog(anyLogMap);
							jsonLogMsg = jsonLogMsg.substring(1, jsonLogMsg.length() - 1);
							String logMsg =
								MessageFormat.format(
									"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). It is a componnent class property. Setting:\n"
											+ "{0}",
											jsonLogMsg
									); 
							logger.trace(logMsg);
						}
						
					}
					backendMetadatas.setSignature(signatureStr);
					backendMetadatas.setIsComponent(true);
//					this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbSignatureName());
//					this.writeString(signatureStr);
//					this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbIsComponentName());
//					this.writeBoolean(true);
					this.jsHbManager.getJsHbJsonSerializerStepStack().peek().findPlayerObjectId(forValue, this, serializers, backendMetadatas);
					//this.jsHbManager.getJsHbJsonSerializerStepStackTL().peek().writePlayerObjectId(forValue, this, serializers);
				} else {
				}
			}
			
			
			//I can be writing a JsHbbackendMetadatas.playerObjectId  
			if (forValue != null
					&& this.jsHbManager.isComponent(forValue.getClass())
					&& this.jsHbManager.getJsHbBackendMetadatasWritingStack().size() > 0
					&& forValue == this.jsHbManager.getJsHbBackendMetadatasWritingStack().peek().getPlayerObjectId()) {
				backendMetadatas.setIsComponent(true);
				backendMetadatas.setIsComponentPlayerObjectId(true);
			}
			
			try {
				this.jsHbManager.getJsHbBackendMetadatasWritingStack().push(backendMetadatas);
				this.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbMetadatasName());
				this.writeObject(backendMetadatas);				
			} finally {
				if (this.jsHbManager.getJsHbBackendMetadatasWritingStack() != null) {
					PlayerMetadatas backendMetadatasPoped = this.jsHbManager.getJsHbBackendMetadatasWritingStack().pop();
					if (backendMetadatasPoped != backendMetadatas) {
						throw new RuntimeException("This should not happen");
					}					
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace(MessageFormat.format(
						"Intercepting com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object). Injecting field \"{0}\": {1}",
						this.jsHbManager.getJsHbConfig().getJsHbMetadatasName(), backendMetadatas));
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