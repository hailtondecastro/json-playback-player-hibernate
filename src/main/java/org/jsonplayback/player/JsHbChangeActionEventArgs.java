package org.jsonplayback.player;

import java.util.Collection;

import org.jsonplayback.player.hibernate.JsHbPlaybackActionType;

public class JsHbChangeActionEventArgs<E> {
	private E ownerValue;
	private Object settedValue;
	private String propertyName;
	private Collection changedCollection;
	private JsHbPlaybackActionType actionType;
	
	@SuppressWarnings("rawtypes")
	public JsHbChangeActionEventArgs(E ownerValue, Object settedValue, String property, Collection changedCollection,
			JsHbPlaybackActionType actionType) {
		super();
		this.ownerValue = ownerValue;
		this.settedValue = settedValue;
		this.propertyName = property;
		this.changedCollection = changedCollection;
		this.actionType = actionType;
	}
	
	public E getOwnerValue() {
		return ownerValue;
	}
	public void setOwnerValue(E ownerValue) {
		this.ownerValue = ownerValue;
	}
	public Object getSettedValue() {
		return settedValue;
	}
	public void setSettedValue(Object settedValue) {
		this.settedValue = settedValue;
	}
	public String getPropertyName() {
		return propertyName;
	}
	public void setPropertyName(String property) {
		this.propertyName = property;
	}
	public Collection getChangedCollection() {
		return changedCollection;
	}
	public void setChangedCollection(Collection changedCollection) {
		this.changedCollection = changedCollection;
	}
	public JsHbPlaybackActionType getActionType() {
		return actionType;
	}
	public void setActionType(JsHbPlaybackActionType actionType) {
		this.actionType = actionType;
	}
	
	
}
