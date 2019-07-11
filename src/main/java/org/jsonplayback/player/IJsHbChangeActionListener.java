package org.jsplayback.backend;

public interface IJsHbChangeActionListener {
	String getName();
	void onBeforeChange(JsHbChangeActionEventArgs eventArgs);
	void onAfterChange(JsHbChangeActionEventArgs eventArgs);
}
/*gerando conflito*/