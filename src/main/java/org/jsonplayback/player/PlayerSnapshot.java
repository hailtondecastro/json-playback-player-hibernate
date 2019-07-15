package org.jsonplayback.player;

public class PlayerSnapshot<T> {
	private T wrappedSnapshot;

	private IPlayerManager jsHbManager;
	
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