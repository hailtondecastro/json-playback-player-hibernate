package org.jsplayback.backend;

import java.util.function.Function;

import org.jsplayback.backend.hibernate.JsHbPlayback;

public interface IJsHbReplayable {
	IJsHbReplayable addChangeActionListener(IJsHbChangeActionListener changeActionListener);
	<E> IJsHbReplayable addChangeActionListenerForClass(Class<E> entClass, IJsHbChangeActionListener changeActionListener);
	<E> IJsHbReplayable addChangeActionListenerForProperty(Class<E> entClass, Function<E, ?> propertyFunc, IJsHbChangeActionListener changeActionListener);
	<E> IJsHbFluentChangeListener<E> fluentChangeListener(Class<E> targetClass);
	IJsHbFluentChangeListener<?> fluentChangeListener();
	void replay();
}
/*gerando conflito*/