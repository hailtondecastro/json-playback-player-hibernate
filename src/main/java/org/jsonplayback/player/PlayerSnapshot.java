package org.jsonplayback.player;

public class PlayerSnapshot<T> {
	private T wrappedSnapshot;

	@SuppressWarnings("unused")
	private IPlayerManager jsHbManager;
	
	@SuppressWarnings("rawtypes")
	public PlayerSnapshot configJsHbManager(IPlayerManager jsHbManager) {
		this.jsHbManager = jsHbManager;
		return this;
	}
	
	public PlayerSnapshot(T wrappedSnapshot) {
		super();
		this.wrappedSnapshot = wrappedSnapshot;
	}

	public T getWrappedSnapshot() {
		return wrappedSnapshot;
	}

	public void setWrappedSnapshot(T wrappedSnapshot) {
		this.wrappedSnapshot = wrappedSnapshot;
	}
}
/*gerando conflito*/