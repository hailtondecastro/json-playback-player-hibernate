package org.jsonplayback.player.hibernate;

import java.io.IOException;

import org.jsonplayback.player.IManager;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JsHbResultEntitySerializer extends JsonSerializer<JsHbResultEntity> {

	public JsHbResultEntitySerializer(){
		
	}
	
	private IManager jsHbManager;
	
	public JsHbResultEntitySerializer configJsHbManager(IManager jsHbManager) {
		this.jsHbManager = jsHbManager;
		return this;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void serialize(JsHbResultEntity value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		try {
			this.jsHbManager.startSuperSync();

			final JsonSerializer<Object> defaultJsonSerializer = serializers.findValueSerializer(Object.class);
			
			gen.writeStartObject();
			gen.writeFieldName("result");
			serializers.findValueSerializer(value.getResult().getClass()).serialize(value.getResult(), gen, serializers);
			gen.writeEndObject();
		} finally {
			this.jsHbManager.stopSuperSync();
		}
	}
}
/*gerando conflito*/