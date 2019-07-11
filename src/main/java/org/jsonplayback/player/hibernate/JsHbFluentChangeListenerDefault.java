package org.jsplayback.backend.hibernate;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jsplayback.backend.IJsHbChangeActionListener;
import org.jsplayback.backend.IJsHbFluentChangeListener;
import org.jsplayback.backend.IJsHbReplayable;
import org.jsplayback.backend.JsHbChangeActionEventArgs;

public class JsHbFluentChangeListenerDefault<E> implements IJsHbFluentChangeListener<E> {

	private IJsHbReplayable jsHbReplayable;

	public JsHbFluentChangeListenerDefault(IJsHbReplayable jsHbReplayable, Class<E> classTarget) {
		this.jsHbReplayable = jsHbReplayable;
		this.classTarget = classTarget;
	}

	private Class<E> classTarget;

	@SuppressWarnings("unused")
	static class ChangeActionListener<T> implements IJsHbChangeActionListener {

		private String name;
		private Consumer<JsHbChangeActionEventArgs<Object>> beforeCallback;
		private Consumer<JsHbChangeActionEventArgs<Object>> afterCallback;

		public ChangeActionListener(String name, Class<T> classTarget,
				Consumer<JsHbChangeActionEventArgs<Object>> beforeCallback,
				Consumer<JsHbChangeActionEventArgs<Object>> afterCallback) {
			super();
			this.name = name;
			this.beforeCallback = beforeCallback;
			this.afterCallback = afterCallback;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public void onBeforeChange(JsHbChangeActionEventArgs eventArgs) {
			if (this.beforeCallback != null) {
				this.beforeCallback.accept(eventArgs);
			}
		}

		@Override
		public void onAfterChange(JsHbChangeActionEventArgs eventArgs) {
			if (this.afterCallback != null) {
				this.afterCallback.accept(eventArgs);
			}
		}
	}

	@Override
	public IJsHbFluentChangeListener<E> onBeforeForClass(String name,
			Consumer<JsHbChangeActionEventArgs<E>> forAllPropertiesCallback) {

		if (this.classTarget != null) {
			this.jsHbReplayable.addChangeActionListenerForClass(this.classTarget,
					new ChangeActionListener(name, this.classTarget, forAllPropertiesCallback, null));
		} else {
			this.jsHbReplayable.addChangeActionListener(
					new ChangeActionListener(name, this.classTarget, forAllPropertiesCallback, null));
		}

		return this;
	}

	@Override
	public IJsHbFluentChangeListener<E> onBeforeForProperty(String name, Function<E, ?> prpFunction,
			Consumer<JsHbChangeActionEventArgs<E>> forPropertyCallback) {
		this.jsHbReplayable.addChangeActionListenerForProperty(this.classTarget, prpFunction,
				new ChangeActionListener(name, this.classTarget, forPropertyCallback, null));

		return this;
	}

	// @Override
	// public IJsHbFluentChangeListener<E> onAfterForAll(String name,
	// Consumer<JsHbChangeActionEventArgs<Object>> forAllClassiesCallback) {
	// return this.jsHbReplayable.addChangeActionListener(
	// new ChangeActionListener(name, Object.class, null,
	// forAllClassiesCallback));
	// return this;
	// }

	@Override
	public IJsHbFluentChangeListener<E> onAfterForClass(String name,
			Consumer<JsHbChangeActionEventArgs<E>> forAllPropertiesCallback) {
		if (this.classTarget != null) {
			this.jsHbReplayable.addChangeActionListenerForClass((Class<E>) this.classTarget,
					new ChangeActionListener(name, this.classTarget, null, forAllPropertiesCallback));
		} else {
			this.jsHbReplayable.addChangeActionListener(
					new ChangeActionListener(name, this.classTarget, null, forAllPropertiesCallback));
		}

		return this;
	}

	@Override
	public IJsHbFluentChangeListener<E> onAfterForProperty(String name, Function<E, ?> prpFunction,
			Consumer<JsHbChangeActionEventArgs<E>> forPropertyCallback) {
		this.jsHbReplayable.addChangeActionListenerForProperty(this.classTarget, prpFunction,
				new ChangeActionListener(name, this.classTarget, null, forPropertyCallback));

		return this;
	}

	@Override
	public IJsHbReplayable complete() {
		// TODO Auto-generated method stub
		return this.jsHbReplayable;
	}
}
