package org.jsonplayback.hbsupport;

import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.jsonplayback.player.hibernate.AssociationAndComponentPath;

public abstract class AssociationAndComponentPathHb6Support extends AssociationAndComponentPath {
	private CompositeType[] compositeTypePath;
	private CollectionType collType;
	private CompositeType compType;	
	
	public CompositeType getCompType() {
		return compType;
	}
	public void setCompType(CompositeType compType) {
		this.compType = compType;
	}
	public CollectionType getCollType() {
		return collType;
	}
	public void setCollType(CollectionType collType) {
		this.collType = collType;
	}
	public CompositeType[] getCompositeTypePath() {
		return compositeTypePath;
	}
	public void setCompositeTypePath(CompositeType[] compositeTypePath) {
		this.compositeTypePath = compositeTypePath;
	}
}
