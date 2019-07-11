package org.jsonplayback.player;

import java.util.function.Function;

import org.jsonplayback.player.hibernate.JsHbPlayback;

public interface IReplayable {
	IReplayable addChangeActionListener(IChangeActionListener changeActionListener);
	<E> IReplayable addChangeActionListenerForClass(Class<E> entClass, IChangeActionListener changeActionListener);
	<E> IReplayable addChangeActionListenerForProperty(Class<E> entClass, Function<E, ?> propertyFunc, IChangeActionListener changeActionListener);
	<E> IFluentChangeListener<E> fluentChangeListener(Class<E> targetClass);
	IFluentChangeListener<?> fluentChangeListener();
	void replay();
}
/*gerando conflito*/