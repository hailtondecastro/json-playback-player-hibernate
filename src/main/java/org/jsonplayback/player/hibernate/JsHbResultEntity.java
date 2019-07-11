package org.jsonplayback.player.hibernate;

import org.jsonplayback.player.IJsHbManager;

public class JsHbResultEntity<T> {
	private T result;

	private IJsHbManager jsHbManager;
	
	JsHbResultEntity configJsHbManager(IJsHbManager jsHbManager) {
		this.jsHbManager = jsHbManager;
		return this;
	}
	
	JsHbResultEntity(T result) {
		super();
		this.result = result;
	}

	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}
}
/*gerando conflito*/