package org.jsonplayback.player.hibernate;

import org.hibernate.type.EntityType;

public abstract class AssociationAndComponentPath implements Cloneable {
	private AssociationAndComponentPathKey aacKey;
	private String[] compositePrpPath;
	private EntityType relEntity;

	public AssociationAndComponentPathKey getAacKey() {
		return aacKey;
	}
	public void setAacKey(AssociationAndComponentPathKey aacKey) {
		this.aacKey = aacKey;
	}
	
	public String[] getCompositePrpPath() {
		return compositePrpPath;
	}
	public void setCompositePrpPath(String[] compositePrpPath) {
		this.compositePrpPath = compositePrpPath;
	}
	public EntityType getRelEntity() {
		return relEntity;
	}
	public void setRelEntity(EntityType relEntity) {
		this.relEntity = relEntity;
	}
}
