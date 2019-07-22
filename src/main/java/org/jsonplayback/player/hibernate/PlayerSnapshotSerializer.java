package org.jsonplayback.player.hibernate;

import java.io.IOException;

import org.jsonplayback.player.IPlayerManager;
import org.jsonplayback.player.PlayerSnapshot;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JsHbPlayerSnapshotSerializer extends JsonSerializer<PlayerSnapshot> {

	public JsHbPlayerSnapshotSerializer(){
		
	}
	
	private IPlayerManager jsHbManager;
	
	public JsHbPlayerSnapshotSerializer configJsHbManager(IPlayerManager jsHbManager) {
		this.jsHbManager = jsHbManager;
		return this;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void serialize(PlayerSnapshot value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		try {
			this.jsHbManager.startJsonWriteIntersept();

			final JsonSerializer<Object> defaultJsonSerializer = serializers.findValueSerializer(Object.class);
			
			gen.writeStartObject();
			gen.writeFieldName("wrappedSnapshot");
			serializers.findValueSerializer(value.getWrappedSnapshot().getClass()).serialize(value.getWrappedSnapshot(), gen, serializers);
			gen.writeEndObject();
		} finally {
			this.jsHbManager.stopJsonWriteIntersept();
		}
	}
}
/*gerando conflito*/