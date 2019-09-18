package org.jsonplayback.hbsupport;

import java.io.Serializable;
import java.sql.Connection;

import org.jsonplayback.player.hibernate.AssociationAndComponentPath;
import org.jsonplayback.player.hibernate.AssociationAndComponentTrackInfo;

public interface HbSupport {
	boolean isPersistentCollection(Object coll);
	boolean isCollectionLazyUnitialized(Object coll);
	boolean isHibernateProxyLazyUnitialized(Object hProxy);
	Connection getConnection();
	Object getCollectionOwner(Object coll);
	String getCollectionFieldName(Object coll);
	String getCollectionGetRole(Object coll);
	Object[] getRawKeyValuesFromHbProxy(Object hibernateProxy);
	Object[] getRawKeyValuesFromNonHbProxy(Object nonHibernateProxy);
	void collectAssociationAndCompositiesMap();
	void init();
	boolean isComponent(Class<?> componentClass);
	boolean isPersistentClass(Class<?> componentClass);
	boolean isRelationship(Class<?> clazz, String fieldName);
	boolean isCollectionRelationship(Class<?> ownerClass, String pathFromOwner);
	boolean isOneToManyRelationship(Class<?> ownerClass, String pathFromOwner);
	boolean isComponentOrRelationship(Class<?> ownerClass, String pathFromOwner);
	boolean isComponentByTrack(AssociationAndComponentTrackInfo aacTrackInfo);
	Serializable getIdValue(Class<?> entityClass, Object[] rawKeyValues);
	Serializable getIdValue(Object entityInstanceOrProxy);
	Object getById(Class<?> entityClass, Serializable idValue);
	AssociationAndComponentPath getAssociationAndComponentOnPath(Class<?> ownerClass, String pathStr);
}
