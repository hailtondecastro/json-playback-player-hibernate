package org.jsplayback.backend;

import java.util.function.Function;

public interface IDirectRawWriterWrapper {
	JsHbLazyProperty getJsHbLazyProperty();
	IDirectRawWriter getCallback();
}
