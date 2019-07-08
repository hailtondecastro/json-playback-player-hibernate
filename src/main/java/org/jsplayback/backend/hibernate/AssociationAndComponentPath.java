package org.jsplayback.backend.hibernate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AssociationAndComponentPath implements Cloneable {
	private AssociationAndComponentPathKey aacKey;
	private CompositeType[] compositeTypePath;
	private String[] compositePrpPath;
	private EntityType relEntity;
	private CollectionType collType;
	private CompositeType compType;	

	public CollectionType getCollType() {
		return collType;
	}
	public void setCollType(CollectionType collType) {
		this.collType = collType;
	}
	public AssociationAndComponentPathKey getAacKey() {
		return aacKey;
	}
	public void setAacKey(AssociationAndComponentPathKey aacKey) {
		this.aacKey = aacKey;
	}
	public CompositeType[] getCompositeTypePath() {
		return compositeTypePath;
	}
	public void setCompositeTypePath(CompositeType[] compositeTypePath) {
		this.compositeTypePath = compositeTypePath;
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
	public CompositeType getCompType() {
		return compType;
	}
	public void setCompType(CompositeType compType) {
		this.compType = compType;
	}

	@Override
	public String toString() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			ArrayList<String> compositeTypePathStrList = new ArrayList<>();
			for (CompositeType compositeTypeItem : this.compositeTypePath) {
				compositeTypePathStrList.add(
						"{ owner class: " + this.aacKey.getEntityClassRootOwner().getName() + 
						", component class: " + compositeTypeItem.getReturnedClass().getName() +
						", properties: " + Arrays.toString(compositeTypeItem.getPropertyNames()) + 
						" }"
						);
			}
			
			LinkedHashMap<String, Object> thisAsMap = new LinkedHashMap<>();
			thisAsMap.put("componentTypeKey", this.aacKey);
			thisAsMap.put("compositeTypePath", compositeTypePathStrList);
			thisAsMap.put("compositePrpPath", this.compositePrpPath);
			thisAsMap.put("collType", this.collType != null? this.collType.toString(): null);
			thisAsMap.put("relEntity", this.relEntity != null? this.relEntity.toString(): null);
			thisAsMap.put("compType", this.compType != null? this.compType.toString(): null);
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(thisAsMap);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("This should not happen", e);
		}
	}
}
