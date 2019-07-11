package org.jsonplayback.player.hibernate;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jsonplayback.player.IChangeActionListener;
import org.jsonplayback.player.IFluentChangeListener;
import org.jsonplayback.player.IReplayable;
import org.jsonplayback.player.ChangeActionEventArgs;

public class JsHbFluentChangeListenerDefault<E> implements IFluentChangeListener<E> {

	private IReplayable jsHbReplayable;

	public JsHbFluentChangeListenerDefault(IReplayable jsHbReplayable, Class<E> classTarget) {
		this.jsHbReplayable = jsHbReplayable;
		this.classTarget = classTarget;
	}

	private Class<E> classTarget;

	@SuppressWarnings("unused")
	static class ChangeActionListener<T> implements IChangeActionListener {

		private String name;
		private Consumer<ChangeActionEventArgs<Object>> beforeCallback;
		private Consumer<ChangeActionEventArgs<Object>> afterCallback;

		public ChangeActionListener(String name, Class<T> classTarget,
				Consumer<ChangeActionEventArgs<Object>> beforeCallback,
				Consumer<ChangeActionEventArgs<Object>> afterCallback) {
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
		public void onBeforeChange(ChangeActionEventArgs eventArgs) {
			if (this.beforeCallback != null) {
				this.beforeCallback.accept(eventArgs);
			}
		}

		@Override
		public void onAfterChange(ChangeActionEventArgs eventArgs) {
			if (this.afterCallback != null) {
				this.afterCallback.accept(eventArgs);
			}
		}
	}

	@Override
	public IFluentChangeListener<E> onBeforeForClass(String name,
			Consumer<ChangeActionEventArgs<E>> forAllPropertiesCallback) {

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
	public IFluentChangeListener<E> onBeforeForProperty(String name, Function<E, ?> prpFunction,
			Consumer<ChangeActionEventArgs<E>> forPropertyCallback) {
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
	public IFluentChangeListener<E> onAfterForClass(String name,
			Consumer<ChangeActionEventArgs<E>> forAllPropertiesCallback) {
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
	public IFluentChangeListener<E> onAfterForProperty(String name, Function<E, ?> prpFunction,
			Consumer<ChangeActionEventArgs<E>> forPropertyCallback) {
		this.jsHbReplayable.addChangeActionListenerForProperty(this.classTarget, prpFunction,
				new ChangeActionListener(name, this.classTarget, null, forPropertyCallback));

		return this;
	}

	@Override
	public IReplayable complete() {
		// TODO Auto-generated method stub
		return this.jsHbReplayable;
	}
}
