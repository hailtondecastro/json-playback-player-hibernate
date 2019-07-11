package org.jsonplayback.player.hibernate;

import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.proxy.HibernateProxy;
import org.jsonplayback.player.IJsHbManager;
import org.jsonplayback.player.SignatureBean;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

public class JsHbPlaybackAction {
	
	private String ownerSignatureStr;
	private Long ownerCreationId;
	private Long ownerCreationRefId;
	private String settedSignatureStr;
	private Long settedCreationId;
	private Long settedCreationRefId;
	private String ownerJavaClass;
	private JsHbPlaybackActionType actionType;
	private String fieldName;

	private Object resolvedOwnerValue = null;
	private boolean isResolvedSettedValue = false;
	private Object resolvedSettedValue = null;
	private Object simpleSettedValue = null;
	@SuppressWarnings("rawtypes")
	private Collection resolvedColletion = null;
	private BeanDeserializerBase resolvedBeanDeserializer = null;
	@SuppressWarnings("rawtypes")
	private Class resolvedOwnerJavaClass = null;
	private String resolvedJavaPropertyName = null;
	private boolean isResolvedJavaPropertyName = false;
	
	protected JsHbPlayback jsHbPlaybackOwner;

	protected String resolveJavaPropertyName(ObjectMapper objectMapper, IJsHbManager jsHbManager,
			HashMap<Long, Object> creationRefMap) {
		if (!this.isResolvedJavaPropertyName) {
			this.isResolvedJavaPropertyName = true;
			if (this.fieldName != null) {
				SettableBeanProperty settableBeanProperty = this
						.resolveBeanDeserializer(objectMapper, jsHbManager, creationRefMap)
						.findProperty(this.fieldName);
				if (settableBeanProperty == null) {
					throw new RuntimeException(MessageFormat.format("O field nao existe na entidade:\n{0}", this));
				}
				this.resolvedJavaPropertyName = settableBeanProperty.getName();
			}
		}
		return resolvedJavaPropertyName;
	}

	/**
	 * Somente deve ser chamado a partir de JsHbReplayable.preProcessPlayBack.
	 * 
	 * @param jsHbManager
	 * @param creationRefMap
	 * @return
	 */
	protected Object resolveOwnerValue(IJsHbManager jsHbManager, HashMap<Long, Object> creationRefMap) {
		if (this.resolvedOwnerValue == null) {
			if (this.ownerSignatureStr != null) {
				SignatureBean signatureBean = jsHbManager.deserializeSignature(this.ownerSignatureStr);
				this.resolvedOwnerValue = jsHbManager.getBySignature(signatureBean);
			} else if (this.ownerCreationId != null) {
				try {
					this.resolvedOwnerValue = this.resolveOwnerJavaClass(jsHbManager, creationRefMap).newInstance();
					
					creationRefMap.put(this.getOwnerCreationId(), this.resolvedOwnerValue);
				} catch (Exception e) {
					throw new RuntimeException(MessageFormat.format("This should not happen\naction:\n{0}", this),
							e);
				}
			} else if (this.ownerCreationRefId != null) {
				this.resolvedOwnerValue = creationRefMap.get(this.ownerCreationRefId);
			} else {
				throw new RuntimeException(MessageFormat.format("This should not happen\naction:\n{0}", this));
			}
		}
		return this.resolvedOwnerValue;
	}

	protected Object resolveSettedValue(ObjectMapper objectMapper, IJsHbManager jsHbManager,
			HashMap<Long, Object> creationRefMap) {
		if (!this.isResolvedSettedValue) {
			this.isResolvedSettedValue = true;
			if (this.settedCreationRefId != null) {
				if (creationRefMap == null) {
					//nada: estah sendo chamado a partir da deserializacao Jaxrs/Jacson
				} else {
					//obtendo do creationRefMap: estah sendo chamado a partir do JsHbReplayable
					this.resolvedSettedValue = creationRefMap.get(this.settedCreationRefId);					
				}
			} else if (this.settedSignatureStr != null) {
				SignatureBean signatureBean = jsHbManager.deserializeSignature(this.settedSignatureStr);
				this.resolvedSettedValue = jsHbManager.getBySignature(signatureBean);
			} else if (this.fieldName != null) {
				// Tipo nao bean
				try {
					Map<String, Object> dummyMap = new LinkedHashMap<>();
					dummyMap.put("dummyPrp", this.simpleSettedValue);
					StringWriter writer = new StringWriter();
					objectMapper.writeValue(writer, dummyMap);
					JsonParser jsonParser = objectMapper.getFactory().createParser(writer.toString());
					//begin object
					if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
						throw new RuntimeException("This should not happen!!!!");
					}
					//dummyPrp 
					if (jsonParser.nextToken() != JsonToken.FIELD_NAME) {
						throw new RuntimeException("This should not happen!!!!");
					}
					String dummyPrpName = jsonParser.getCurrentName();
					if (!"dummyPrp".equals(dummyPrpName)) {
						throw new RuntimeException("This should not happen!!!!");
					}
					SettableBeanProperty settableBeanProperty = this.resolveBeanDeserializer(objectMapper, jsHbManager, creationRefMap).findProperty(this.fieldName);
					JsonDeserializer<Object> jsonDeserializer = settableBeanProperty.getValueDeserializer();
					//valor simple
					jsonParser.nextToken();
					this.resolvedSettedValue = jsonDeserializer.deserialize(jsonParser, this.resolveDefaultDeserializationContext(objectMapper, jsHbManager));
					//end object
					if (jsonParser.nextToken() != JsonToken.END_OBJECT) {
						throw new RuntimeException("This should not happen!!!!");
					}
					if (jsonParser.nextToken() != null) {
						throw new RuntimeException("This should not happen!!!!");
					}
				} catch (IOException e) {
					throw new RuntimeException(MessageFormat.format("This should not happen\naction:\n{0}", this),
							e);
				}
			}
		}
		return this.resolvedSettedValue;
	}

	@SuppressWarnings("rawtypes")
	protected Collection resolveColletion(ObjectMapper objectMapper, IJsHbManager jsHbManager,
			HashMap<Long, Object> creationRefMap) {
		if (this.resolvedColletion == null && (this.actionType == JsHbPlaybackActionType.COLLECTION_ADD || this.actionType == JsHbPlaybackActionType.COLLECTION_REMOVE)) {
			try {
				this.resolvedColletion = (Collection) PropertyUtils.getProperty(this.resolveOwnerValue(jsHbManager, creationRefMap),
						this.resolveJavaPropertyName(objectMapper, jsHbManager, creationRefMap));
			} catch (Exception e) {
				throw new RuntimeException(MessageFormat.format("This should not happen. action:\n{0}", this), e);
			}
		}
		return this.resolvedColletion;
	}

	protected DefaultDeserializationContext resolveDefaultDeserializationContext(ObjectMapper objectMapper, IJsHbManager jsHbManager) {
		if (this.jsHbPlaybackOwner.defaultDeserializationContext == null) {
			this.jsHbPlaybackOwner.defaultDeserializationContext = ((DefaultDeserializationContext) objectMapper
					.getDeserializationContext()).createInstance(objectMapper.getDeserializationConfig(), null,
							objectMapper.getInjectableValues());
		}
		return this.jsHbPlaybackOwner.defaultDeserializationContext;
	}
	
	protected BeanDeserializerBase resolveBeanDeserializer(ObjectMapper objectMapper, IJsHbManager jsHbManager,
			HashMap<Long, Object> creationRefMap) {
		if (this.resolvedBeanDeserializer == null) {
			JavaType entJavaType = objectMapper.getTypeFactory().constructType(this.resolveOwnerJavaClass(jsHbManager, creationRefMap));
			try {
				
				this.resolvedBeanDeserializer = (BeanDeserializerBase) this.resolveDefaultDeserializationContext(objectMapper, jsHbManager)
						.findRootValueDeserializer(entJavaType);
//				this.resolvedBeanDeserializer = (BeanDeserializerBase) objectMapper.getDeserializationContext()
//						.findRootValueDeserializer(entJavaType);
			} catch (JsonMappingException e) {
				throw new RuntimeException(MessageFormat.format("This should not happen\naction:\n{0}", this), e);
			}
		}
		return resolvedBeanDeserializer;
	}

	@SuppressWarnings("rawtypes")
	protected Class resolveOwnerJavaClass(IJsHbManager jsHbManager,
			HashMap<Long, Object> creationRefMap) {
		if (this.resolvedOwnerJavaClass == null) {
			try {
				if (this.ownerJavaClass != null) {
					this.resolvedOwnerJavaClass = Class.forName(this.ownerJavaClass);
				} else if (this.ownerSignatureStr != null) {
					this.resolveOwnerValue(jsHbManager, creationRefMap);
					if (this.resolvedOwnerValue instanceof HibernateProxy) {
						this.resolvedOwnerJavaClass = this.resolvedOwnerValue.getClass().getSuperclass();						
					} else {
						this.resolvedOwnerJavaClass = this.resolvedOwnerValue.getClass();
					}
				} else if (this.ownerCreationRefId != null) {
					if (!creationRefMap.containsKey(this.ownerCreationRefId)) {
						throw new RuntimeException(MessageFormat.format("There is no ''{0}'' action with this creation id\naction:\n{1}", JsHbPlaybackActionType.CREATE , this));
					}
					this.resolvedOwnerJavaClass = creationRefMap.get(this.ownerCreationRefId).getClass();
				} else {
					throw new RuntimeException(MessageFormat.format("this.ownerCreationRefId != null. Not supported.\\n{0}", this));
				}
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(MessageFormat.format("This should not happen\naction:\n{0}", this), e);
			}
		}
		return this.resolvedOwnerJavaClass;
	}

	public String getOwnerSignatureStr() {
		return ownerSignatureStr;
	}

	public void setOwnerSignatureStr(String ownerSignatureStr) {
		this.ownerSignatureStr = ownerSignatureStr;
	}

	public Long getOwnerCreationId() {
		return ownerCreationId;
	}

	public void setOwnerCreationId(Long ownerCreationId) {
		this.ownerCreationId = ownerCreationId;
	}

	public Long getOwnerCreationRefId() {
		return ownerCreationRefId;
	}

	public void setOwnerCreationRefId(Long ownerCreationRefId) {
		this.ownerCreationRefId = ownerCreationRefId;
	}

	public String getSettedSignatureStr() {
		return settedSignatureStr;
	}

	public void setSettedSignatureStr(String settedSignatureStr) {
		this.settedSignatureStr = settedSignatureStr;
	}

	public Long getSettedCreationId() {
		return settedCreationId;
	}

	public void setSettedCreationId(Long settedCreationId) {
		this.settedCreationId = settedCreationId;
	}

	public Long getSettedCreationRefId() {
		return settedCreationRefId;
	}

	public void setSettedCreationRefId(Long settedCreationRefId) {
		this.settedCreationRefId = settedCreationRefId;
	}

	public String getOwnerJavaClass() {
		return ownerJavaClass;
	}

	public void setOwnerJavaClass(String ownerJavaClass) {
		this.ownerJavaClass = ownerJavaClass;
	}

	public JsHbPlaybackActionType getActionType() {
		return actionType;
	}

	public void setActionType(JsHbPlaybackActionType actionType) {
		this.actionType = actionType;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public Object getSimpleSettedValue() {
		return simpleSettedValue;
	}

	public void setSimpleSettedValue(Object simpleSettedValue) {
		this.simpleSettedValue = simpleSettedValue;
	}

	@Override
	public String toString() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			LinkedHashMap<String, Object> thisAsMap = new LinkedHashMap<>();
			thisAsMap.put("ownerSignatureStr", this.ownerSignatureStr);
			thisAsMap.put("ownerCreationId", this.ownerCreationId);
			thisAsMap.put("ownerCreationRefId", this.ownerCreationRefId);
			thisAsMap.put("settedSignatureStr", this.settedSignatureStr);
			thisAsMap.put("settedCreationId", this.settedCreationId);
			thisAsMap.put("settedCreationRefId", this.settedCreationRefId);
			thisAsMap.put("ownerJavaClass", this.ownerJavaClass);
			thisAsMap.put("actionType", this.actionType);
			thisAsMap.put("fieldName", this.fieldName);
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(thisAsMap);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("This should not happen", e);
		}
	}
}
/*gerando conflito*/