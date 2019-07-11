package org.jsonplayback.player;

import java.util.function.Function;

public interface IDirectRawWriterWrapper {
	JsHbLazyProperty getJsHbLazyProperty();
	IDirectRawWriter getCallback();
}
