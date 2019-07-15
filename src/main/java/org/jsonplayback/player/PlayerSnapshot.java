package org.jsonplayback.player.hibernate;

import org.jsonplayback.player.IPlayerManager;

public class JsHbResultEntity<T> {
	private T result;

	private IPlayerManager jsHbManager;
	
	JsHbResultEntity configJsHbManager(IPlayerManager jsHbManager) {
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