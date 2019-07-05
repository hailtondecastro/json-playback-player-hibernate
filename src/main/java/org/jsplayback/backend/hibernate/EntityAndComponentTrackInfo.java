package org.jsplayback.backend.hibernate;

public class EntityAndComponentTrackInfo {
	private Object entityOwner;
	private HbComponentTypeEntry componentTypeEntry;
	
	public EntityAndComponentTrackInfo(Object entityOwner, HbComponentTypeEntry componentTypeEntry) {
		super();
		this.entityOwner = entityOwner;
		this.componentTypeEntry = componentTypeEntry;
	}
	public Object getEntityOwner() {
		return entityOwner;
	}
	public void setEntityOwner(Object entityOwner) {
		this.entityOwner = entityOwner;
	}
	public HbComponentTypeEntry getComponentTypeEntry() {
		return componentTypeEntry;
	}
	public void setComponentTypeEntry(HbComponentTypeEntry componentTypeEntry) {
		this.componentTypeEntry = componentTypeEntry;
	}	
}
