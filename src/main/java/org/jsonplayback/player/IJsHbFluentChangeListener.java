package org.jsonplayback.player;

import java.util.function.Consumer;
import java.util.function.Function;

public interface IJsHbFluentChangeListener<E> {
//	IJsHbReplayable onBeforeForAll(String name, Consumer<JsHbChangeActionEventArgs<Object>> forAllCallback);
	IJsHbFluentChangeListener<E> onBeforeForClass(String listenerName, Consumer<JsHbChangeActionEventArgs<E>> forClassCallback);
	IJsHbFluentChangeListener<E> onBeforeForProperty(String listenerName, Function<E, ?> prpFunction, Consumer<JsHbChangeActionEventArgs<E>> forPropertyCallback);
	
//	IJsHbReplayable onAfterForAll(String name, Consumer<JsHbChangeActionEventArgs<Object>> forAllCallback);
	IJsHbFluentChangeListener<E> onAfterForClass(String listenerName, Consumer<JsHbChangeActionEventArgs<E>> forClassCallback);
	IJsHbFluentChangeListener<E> onAfterForProperty(String listenerName, Function<E, ?> prpFunction, Consumer<JsHbChangeActionEventArgs<E>> forPropertyCallback);
	
	IJsHbReplayable complete();
}
