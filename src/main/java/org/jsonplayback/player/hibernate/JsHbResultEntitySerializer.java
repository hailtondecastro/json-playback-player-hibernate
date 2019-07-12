package org.jsonplayback.player.hibernate;

import java.io.IOException;

import org.jsonplayback.player.IPlayerManager;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JsHbResultEntitySerializer extends JsonSerializer<JsHbResultEntity> {

	public JsHbResultEntitySerializer(){
		
	}
	
	private IPlayerManager jsHbManager;
	
	public JsHbResultEntitySerializer configJsHbManager(IPlayerManager jsHbManager) {
		this.jsHbManager = jsHbManager;
		return this;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void serialize(JsHbResultEntity value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		try {
			this.jsHbManager.startJsonWriteIntersept();

			final JsonSerializer<Object> defaultJsonSerializer = serializers.findValueSerializer(Object.class);
			
			gen.writeStartObject();
			gen.writeFieldName("result");
			serializers.findValueSerializer(value.getResult().getClass()).serialize(value.getResult(), gen, serializers);
			gen.writeEndObject();
		} finally {
			this.jsHbManager.stopJsonWriteIntersept();
		}
	}
}
/*gerando conflito*/