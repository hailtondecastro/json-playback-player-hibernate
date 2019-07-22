package org.jsonplayback.player.hibernate;

import java.sql.Blob;
import java.sql.Clob;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsonplayback.player.IPlayerManager;
import org.jsonplayback.player.PlayerMetadatas;
import org.jsonplayback.player.LazyProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.type.CollectionType;

public class JsHbBeanSerializerModifier extends BeanSerializerModifier {

	private static Logger logger = LoggerFactory.getLogger(JsHbBeanSerializerModifier.class);
	private IPlayerManagerImplementor jsHbManager;

	public JsHbBeanSerializerModifier configJsHbManager(IPlayerManagerImplementor jsHbManager) {
		this.jsHbManager = jsHbManager;
		return this;
	}

	@Override
	public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
			List<BeanPropertyWriter> beanProperties) {

		Map<String, BeanPropertyDefinition> prpDefsMap = new HashMap<>();
		for (BeanPropertyDefinition beanPropertyDefinition : beanDesc.findProperties()) {
			prpDefsMap.put(beanPropertyDefinition.getName(), beanPropertyDefinition);
		}

		Class beanClass = beanDesc.getType().getRawClass();
		boolean beanClassIsPersistent = this.jsHbManager.isPersistentClass(beanClass);
		String playerObjectIdName = null;
		if (beanClassIsPersistent) {
			playerObjectIdName = this.jsHbManager.getPlayerObjectIdName(beanClass);			
		}
		
		for (int i = 0; i < beanProperties.size(); i++) {
			BeanPropertyWriter beanPropertyWriter = beanProperties.get(i);
			BeanPropertyDefinition prpDef = prpDefsMap.get(beanPropertyWriter.getName());
			Class prpClass = beanPropertyWriter.getType().getRawClass();
			boolean isPersistent = this.jsHbManager.isPersistentClass(prpClass);
			boolean isPlayerObjectId = false;
			boolean isMetadatasPlayerObjectId = false;

			if (beanClassIsPersistent) {
				if (playerObjectIdName.equals(prpDef.getInternalName())) {
					isPlayerObjectId = true;
				}
			}

			if (prpDef.getAccessor().getDeclaringClass().equals(PlayerMetadatas.class)
					&& prpDef.getInternalName().equals("playerObjectId")) {
				isMetadatasPlayerObjectId = true;
			}

			BeanPropertyWriter newBeanPropertyWriter = null;
			if (this.jsHbManager.isPersistentClass(beanClass) || this.jsHbManager.isComponent(beanClass)) {
				LazyProperty jsHbLazyProperty = beanPropertyWriter.getAnnotation(LazyProperty.class);
				if (jsHbLazyProperty != null) {
					if ((beanPropertyWriter.getType().getRawClass().isArray()
							&& beanPropertyWriter.getType().getRawClass().getComponentType() == byte.class)
							|| Blob.class.isAssignableFrom(beanPropertyWriter.getType().getRawClass())
							|| String.class.isAssignableFrom(beanPropertyWriter.getType().getRawClass())
							|| Clob.class.isAssignableFrom(beanPropertyWriter.getType().getRawClass())) {								
					newBeanPropertyWriter = new JsHbBeanPropertyWriter(beanPropertyWriter)
							.configJsHbManager(jsHbManager)
								.loadBeanPropertyDefinition(prpDef)
								.loadJsHbLazyProperty(jsHbLazyProperty);
					}
				} else if (this.jsHbManager.isComponent(beanClass)) {
					newBeanPropertyWriter = new JsHbBeanPropertyWriter(beanPropertyWriter)
							.configJsHbManager(jsHbManager)
//							.loadComponentOwnerClass(beanClass)
							.loadBeanPropertyDefinition(prpDef).loadIsPlayerObjectId(isPlayerObjectId)
							.loadIsPersistent(isPersistent)
							.loadIsMetadatasPlayerObjectId(isMetadatasPlayerObjectId);
				} else if (this.jsHbManager.isRelationship(beanClass, prpDef.getInternalName())) {
					newBeanPropertyWriter = new JsHbBeanPropertyWriter(beanPropertyWriter)
							.configJsHbManager(jsHbManager).loadRelationshipOwnerClass(beanClass)
							.loadBeanPropertyDefinition(prpDef).loadIsPersistent(isPersistent)
							.loadIsPlayerObjectId(isPlayerObjectId)
							.loadIsMetadatasPlayerObjectId(isMetadatasPlayerObjectId);
//					throw new RuntimeException("ISSO TA ERRADO. beanPropertyWriter.getName()?!?!?!");
				} else {
					newBeanPropertyWriter = new JsHbBeanPropertyWriter(beanPropertyWriter).configJsHbManager(jsHbManager)
							.loadBeanPropertyDefinition(prpDef).loadIsPersistent(isPersistent)
							.loadIsPlayerObjectId(isPlayerObjectId)
							.loadIsMetadatasPlayerObjectId(isMetadatasPlayerObjectId);
				}
			} else {
				newBeanPropertyWriter = new JsHbBeanPropertyWriter(beanPropertyWriter).configJsHbManager(jsHbManager)
						.loadBeanPropertyDefinition(prpDef).loadIsPersistent(isPersistent)
						.loadIsPlayerObjectId(isPlayerObjectId)
						.loadIsMetadatasPlayerObjectId(isMetadatasPlayerObjectId);
			}
			
			if (newBeanPropertyWriter != null) {
				beanProperties.set(i, newBeanPropertyWriter);
				if (logger.isTraceEnabled()) {
					logger.trace(MessageFormat.format("changeProperties():\n" + " old BeanPropertyWriter=''{0}''\n"
							+ " new BeanPropertyWriter=''{1}''\n", beanPropertyWriter, newBeanPropertyWriter));
				}
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace(MessageFormat.format("changeProperties(): BeanPropertyWriter not modified=''{0}''",
							beanPropertyWriter));
				}
			}
		}

		return super.changeProperties(config, beanDesc, beanProperties);
	}

	@Override
	public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
			JsonSerializer<?> serializer) {
		if (serializer instanceof BeanSerializer) {
			JsonSerializer<?> newJsonSerializer = super.modifySerializer(config, beanDesc,
					new JsHbJsonSerializer(serializer).configJsHbManager(this.jsHbManager));
			if (logger.isTraceEnabled()) {
				logger.trace(MessageFormat.format("modifySerializer():\n" + " old JsonSerializer=''{0}''\n"
						+ " new JsonSerializer=''{1}''\n", serializer, newJsonSerializer));
			}
			return newJsonSerializer;
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace(MessageFormat.format("modifySerializer(): JsonSerializer not modified=''{0}''",
						serializer));
			}
			return serializer;
		}
	}

	@Override
	public JsonSerializer<?> modifyCollectionSerializer(SerializationConfig config, CollectionType valueType,
			BeanDescription beanDesc, JsonSerializer<?> serializer) {
		JsonSerializer<?> newJsonSerializer = super.modifySerializer(config, beanDesc,
				new JsHbJsonSerializer(serializer).configJsHbManager(this.jsHbManager));
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("modifyCollectionSerializer():\n" + " old JsonSerializer=''{0}''\n"
					+ " new JsonSerializer=''{1}''\n", serializer, newJsonSerializer));
		}
		return newJsonSerializer;
	}
}
/*gerando conflito*/