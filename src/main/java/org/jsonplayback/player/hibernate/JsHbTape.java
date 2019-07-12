package org.jsonplayback.player.hibernate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.jsonplayback.player.Tape;
import org.jsonplayback.player.TapeAction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;

public class JsHbTape extends Tape {
	/**
	 * Cache!
	 */
	protected DefaultDeserializationContext defaultDeserializationContext;
	
	public DefaultDeserializationContext getDefaultDeserializationContext() {
		return defaultDeserializationContext;
	}

	public void setDefaultDeserializationContext(DefaultDeserializationContext defaultDeserializationContext) {
		this.defaultDeserializationContext = defaultDeserializationContext;
	}
	@Override
	public String toString() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			LinkedHashMap<String, Object> thisAsMap = new LinkedHashMap<>();
			ArrayList<Object> actionList = new ArrayList<>();
			for (TapeAction actionItem : this.getActions()) {
				JsHbTapeAction jsHbActionItem = (JsHbTapeAction) actionItem;
				
				LinkedHashMap<String, Object> actionAsMap = new LinkedHashMap<>();
				actionAsMap.put("ownerSignatureStr", jsHbActionItem.getOwnerSignatureStr());
				actionAsMap.put("ownerCreationId", jsHbActionItem.getOwnerCreationId());
				actionAsMap.put("ownerCreationRefId", jsHbActionItem.getOwnerCreationRefId());
				actionAsMap.put("settedSignatureStr", jsHbActionItem.getSettedSignatureStr());
				actionAsMap.put("settedCreationId", jsHbActionItem.getSettedCreationId());
				actionAsMap.put("settedCreationRefId", jsHbActionItem.getSettedCreationRefId());
				actionAsMap.put("ownerJavaClass", jsHbActionItem.getOwnerJavaClass());
				actionAsMap.put("actionType", jsHbActionItem.getActionType());
				actionAsMap.put("fieldName", jsHbActionItem.getFieldName());
				actionAsMap.put("simpleSettedValue", jsHbActionItem.getSimpleSettedValue());
				actionList.add(actionAsMap);
			}
			thisAsMap.put("actions", actionList);
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(thisAsMap);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("This should not happen", e);
		}
	}
}
/* gerando conflito */