package org.jsonplayback.hbsupport;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.jsonplayback.player.IPlayerManager;
import org.jsonplayback.player.SignatureBean;
import org.jsonplayback.player.hibernate.AssociationAndComponentPath;
import org.jsonplayback.player.hibernate.AssociationAndComponentPathKey;
import org.jsonplayback.player.hibernate.AssociationAndComponentTrackInfo;
import org.jsonplayback.player.hibernate.PlayerResultSet;
import org.jsonplayback.player.hibernate.PlayerStatment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hb3Support implements HbSupport {
	private static Logger logger = LoggerFactory.getLogger(Hb3Support.class);

	private IPlayerManager manager;
	private Map<AssociationAndComponentPathKey, AssociationAndComponentPathHbSupport> associationAndCompositiesMap = new HashMap<>();
	private Set<Class<?>> compositiesSet = new HashSet<>();

	public Hb3Support(IPlayerManager manager) {
		this.manager = manager;
		try {
			this.persistentCollecitonClass = Class.forName("org.hibernate.collection.PersistentCollection");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("org.hibernate.collection.PersistentCollection not found", e);
		}
		try {
			this.persistentCollectionWasInitialized = this.persistentCollecitonClass.getMethod("wasInitialized",
					new Class<?>[] {});
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("org.hibernate.collection.PersistentCollection.wasInitialized() not found", e);
		}
		try {
			this.persistentCollectionGetOwner = this.persistentCollecitonClass.getMethod("getOwner", new Class<?>[] {});
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("org.hibernate.collection.PersistentCollection.getOwner() not found", e);
		}
		try {
			this.persistentCollectionGetRole = this.persistentCollecitonClass.getMethod("getRole", new Class<?>[] {});
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("org.hibernate.collection.PersistentCollection.getRole() not found", e);
		}
	}

	private Class<?> persistentCollecitonClass;
	private Method persistentCollectionWasInitialized;
	private Method persistentCollectionGetOwner;
	private Method persistentCollectionGetRole;
	private Method sessionConnectionMethod;

	@Override
	public boolean isPersistentCollection(Object coll) {
		return persistentCollecitonClass.isAssignableFrom(coll.getClass());
	}

	@Override
	public boolean isCollectionLazyUnitialized(Object coll) {
		try {
			return !((boolean) persistentCollectionWasInitialized.invoke(coll));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("This should not happen", e);
		}
	}

	@Override
	public Object getCollectionOwner(Object coll) {
		try {
			return persistentCollectionGetOwner.invoke(coll);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("This should not happen", e);
		}
	}

	@Override
	public String getCollectionFieldName(Object coll) {
		Pattern rxCollectionRole = Pattern
				.compile("^" + Pattern.quote(this.getCollectionOwner(coll).getClass().getName()) + "\\.(.*)");

		String role = this.getCollectionGetRole(coll);
		Matcher matcher = rxCollectionRole.matcher(role);
		if (!matcher.find()) {
			throw new RuntimeException(
					MessageFormat.format("Collection role doesn't matches the expected pattern: ''{0}''", role));
		}

		// Class<?> ownerClass = this.getCollectionOwner(coll).getClass();
		String fieldName = matcher.group(1);
		return fieldName;
	}

	@Override
	public Object[] getRawKeyValuesFromHbProxy(Object hibernateProxy) {
		Class entityClass = hibernateProxy.getClass().getSuperclass();

		ClassMetadata classMetadata = this.manager.getConfig().getSessionFactory().getClassMetadata(entityClass);
		PlayerStatment playerStatment = new PlayerStatment();

		Object idValue = classMetadata.getIdentifier(hibernateProxy,
				(org.hibernate.engine.SessionImplementor) this.manager.getConfig().getSessionFactory()
						.getCurrentSession());
		Type hbIdType = classMetadata.getIdentifierType();
		try {
			hbIdType.nullSafeSet(playerStatment, idValue, 0,
					(SessionImplementor) this.manager.getConfig().getSessionFactory().getCurrentSession());
		} catch (HibernateException e) {
			throw new RuntimeException("This should not happen", e);
		} catch (SQLException e) {
			throw new RuntimeException("This should not happen", e);
		}
		return playerStatment.getInternalValues();
	}

	@Override
	public Object[] getRawKeyValuesFromNonHbProxy(Object nonHibernateProxy) {
		if (nonHibernateProxy instanceof HibernateProxy) {
			throw new RuntimeException("nonHibernateProxy instanceof HibernateProxy: " + nonHibernateProxy);
		}
		Class entityClass = nonHibernateProxy.getClass();
		ClassMetadata classMetadata = this.manager.getConfig().getSessionFactory().getClassMetadata(entityClass);
		PlayerStatment playerStatment = new PlayerStatment();

		Object idValue = classMetadata.getIdentifier(nonHibernateProxy,
				(org.hibernate.engine.SessionImplementor) this.manager.getConfig().getSessionFactory()
						.getCurrentSession());
		Type hbIdType = classMetadata.getIdentifierType();
		try {
			hbIdType.nullSafeSet(playerStatment, idValue, 0,
					(SessionImplementor) this.manager.getConfig().getSessionFactory().getCurrentSession());
		} catch (HibernateException e) {
			throw new RuntimeException("This should not happen", e);
		} catch (SQLException e) {
			throw new RuntimeException("This should not happen", e);
		}
		return playerStatment.getInternalValues();
	}

	@Override
	public String getCollectionGetRole(Object coll) {
		try {
			return (String) this.persistentCollectionGetRole.invoke(coll);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("This should not happen", e);
		}
	}

	@Override
	public boolean isHibernateProxyLazyUnitialized(Object hProxy) {
		return ((HibernateProxy) hProxy).getHibernateLazyInitializer().isUninitialized();
	}

	@Override
	public Connection getConnection() {
		final AtomicReference<Connection> connRef = new AtomicReference<>();
		this.manager.getConfig().getSessionFactory().getCurrentSession().doWork(connection -> {
			connRef.set(connection);
		});
		return connRef.get();
	}

	protected String mountPathFromStack(Collection<String> pathStack) {
		String pathResult = "";
		String dotStr = "";
		for (String pathItem : pathStack) {
			pathResult += dotStr + pathItem;
			dotStr = ".";
		}
		return pathResult;
	}

	private void collectAssociationAndCompositiesMapRecursive(ClassMetadata ownerRootClassMetadata,
			CompositeType ownerCompositeType, CompositeType compositeType, Stack<String> pathStack,
			Stack<CompositeType> compositeTypePathStack) {
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("Collecting CompositeType:{0}", compositeType));
		}
		Class<?> ownerRootClass;
		try {
			ownerRootClass = Class.forName(ownerRootClassMetadata.getEntityName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(ownerRootClassMetadata.getEntityName() + " not supported.", e);
		}
		String pathFromStack = this.mountPathFromStack(pathStack);
		AssociationAndComponentPathKey aacKeyFromRoot = new AssociationAndComponentPathKey(ownerRootClass,
				pathFromStack);
//		AssociationAndComponentPathKey aacKeyDirect = null;
//		if (ownerCompositeType != null) {
//			aacKeyDirect = new AssociationAndComponentPathKey(ownerCompositeType.getReturnedClass(), pathStack.peek());				
//		} else {
//			aacKeyDirect = new AssociationAndComponentPathKey(ownerRootClass, pathFromStack);
//		}
//		if (!this.associationAndCompositiesMap.containsKey(aacKeyDirect)) {
		if (!this.associationAndCompositiesMap.containsKey(aacKeyFromRoot)) {
//			if (!aacKeyDirect.equals(aacKeyFromRoot)) {
//				AssociationAndComponentPath aacPathFromRoot = new AssociationAndComponentPath();
//				aacPathFromRoot.setAacKey(aacKeyDirect);
//				aacPathFromRoot.setCompositePrpPath(pathStack.toArray(new String[pathStack.size()]));
//				aacPathFromRoot.setCompositeTypePath(compositeTypePathStack.toArray(new CompositeType[compositeTypePathStack.size()]));
//				aacPathFromRoot.setCompType(compositeType);
//				aacPathFromRoot.setRelEntity(null);
//				aacPathFromRoot.setCollType(null);
//				
//				this.associationAndCompositiesMap.put(aacKeyFromRoot, aacPathFromRoot);				
//			}
			AssociationAndComponentPathHbSupport aacPath = new AssociationAndComponentPathHbSupport();
//			aacPath.setAacKey(aacKeyDirect);
			aacPath.setAacKey(aacKeyFromRoot);
			aacPath.setCompositeTypePath(new CompositeType[] { compositeType });
			aacPath.setCompType(compositeType);
			aacPath.setRelEntity(null);
			aacPath.setCollType(null);
			aacPath.setCompositePrpPath(new String[] { pathStack.peek() });
//			this.associationAndCompositiesMap.put(aacKeyDirect, aacPath);
			this.associationAndCompositiesMap.put(aacKeyFromRoot, aacPath);

			List<Type> allPrps = new ArrayList<>();
			List<String> allPrpsNames = new ArrayList<>();
			allPrps.addAll(Arrays.asList(compositeType.getSubtypes()));
			allPrpsNames.addAll(Arrays.asList(compositeType.getPropertyNames()));
			for (int i = 0; i < allPrps.size(); i++) {
				Type subPrpType = allPrps.get(i);
				String subPrpName = allPrpsNames.get(i);

				if (subPrpType instanceof CompositeType) {
					if (logger.isTraceEnabled()) {
						logger.trace(MessageFormat.format("Recursion on collect CompositeType: {0} -> {1}",
								compositeType.getReturnedClass().getName(), subPrpName));
					}
					pathStack.push(subPrpName);
					compositeTypePathStack.push((CompositeType) subPrpType);
					this.collectAssociationAndCompositiesMapRecursive(ownerRootClassMetadata, compositeType,
							(CompositeType) subPrpType, pathStack, compositeTypePathStack);
				} else if (subPrpType instanceof EntityType) {
					EntityType entityType = (EntityType) subPrpType;
					Stack<String> pathStackRelation = new Stack<String>();
					pathStackRelation.addAll(pathStack);
					pathStackRelation.push(subPrpName);
					String pathStackRelationStr = this.mountPathFromStack(pathStackRelation);

//					if (ownerCompositeType != null) {		
//						aacKeyDirect = new AssociationAndComponentPathKey(ownerCompositeType.getReturnedClass(), subPrpName);
//					} else {
//						aacKeyDirect = new AssociationAndComponentPathKey(ownerRootClass, pathStackRelationStr);
//					}					
					aacKeyFromRoot = new AssociationAndComponentPathKey(ownerRootClass, pathStackRelationStr);

					if (!this.associationAndCompositiesMap.containsKey(aacKeyFromRoot)) {
//					if (!this.associationAndCompositiesMap.containsKey(aacKeyDirect)) {
//						if (!aacKeyDirect.equals(aacKeyFromRoot)) {
						AssociationAndComponentPathHbSupport relEacPathFromRoot = new AssociationAndComponentPathHbSupport();
						relEacPathFromRoot.setAacKey(aacKeyFromRoot);
						relEacPathFromRoot.setCompositeTypePath(
								compositeTypePathStack.toArray(new CompositeType[compositeTypePathStack.size()]));
						relEacPathFromRoot.setCompType(null);
						relEacPathFromRoot.setRelEntity(entityType);
						relEacPathFromRoot.setCollType(null);
						relEacPathFromRoot.setCompositePrpPath(pathStack.toArray(new String[pathStack.size()]));
						this.associationAndCompositiesMap.put(aacKeyFromRoot, relEacPathFromRoot);
//						}
//						AssociationAndComponentPath relEacPath = new AssociationAndComponentPath();
//						relEacPath.setAacKey(aacKeyDirect);
//						relEacPath.setCompositeTypePath(new CompositeType[]{compositeType});
//						relEacPath.setCompType(null);
//						relEacPath.setRelEntity(entityType);
//						relEacPath.setCollType(null);
//						relEacPath.setCompositePrpPath(new String[]{subPrpName});
//						this.associationAndCompositiesMap.put(aacKeyDirect, relEacPath);
					}
				} else if (subPrpType instanceof CollectionType) {
					CollectionType collType = (CollectionType) subPrpType;
					Stack<String> pathStackRelation = new Stack<String>();
					pathStackRelation.addAll(pathStack);
					pathStackRelation.push(subPrpName);
					String pathStackRelationStr = this.mountPathFromStack(pathStackRelation);

//					if (ownerCompositeType != null) {		
//						aacKeyDirect = new AssociationAndComponentPathKey(ownerCompositeType.getReturnedClass(), subPrpName);
//					} else {
//						aacKeyDirect = new AssociationAndComponentPathKey(ownerRootClass, pathStackRelationStr);
//					}					
					aacKeyFromRoot = new AssociationAndComponentPathKey(ownerRootClass, pathStackRelationStr);
					if (!this.associationAndCompositiesMap.containsKey(aacKeyFromRoot)) {
//						if (!this.associationAndCompositiesMap.containsKey(aacKeyDirect)) {
//						if (!aacKeyDirect.equals(aacKeyFromRoot)) {
						AssociationAndComponentPathHbSupport relEacPathFromRoot = new AssociationAndComponentPathHbSupport();
						relEacPathFromRoot.setAacKey(aacKeyFromRoot);
						relEacPathFromRoot.setCompositeTypePath(
								compositeTypePathStack.toArray(new CompositeType[compositeTypePathStack.size()]));
						relEacPathFromRoot.setCompType(null);
						relEacPathFromRoot.setRelEntity(null);
						relEacPathFromRoot.setCollType(collType);
						relEacPathFromRoot.setCompositePrpPath(pathStack.toArray(new String[pathStack.size()]));
						this.associationAndCompositiesMap.put(aacKeyFromRoot, relEacPathFromRoot);
//						}
//						AssociationAndComponentPath relEacPath = new AssociationAndComponentPath();
//						relEacPath.setAacKey(aacKeyDirect);
//						relEacPath.setCompositeTypePath(new CompositeType[]{compositeType});
//						relEacPath.setCompType(null);
//						relEacPath.setRelEntity(null);
//						relEacPath.setCollType(collType);
//						relEacPath.setCompositePrpPath(new String[]{subPrpName});
//						this.associationAndCompositiesMap.put(aacKeyDirect, relEacPath);
					}
				}
			}
		} else {
			// maybe it is deprecated!?
			CompositeType existingComponent = this.associationAndCompositiesMap.get(aacKeyFromRoot).getCompType();
			boolean isDifferent = false;
			if (logger.isTraceEnabled()) {
				logger.trace(MessageFormat.format(
						"Component already collected, verifying if the definition is the same: {0}", compositeType));
			}
			if (existingComponent.getSubtypes().length == compositeType.getSubtypes().length) {
				for (int i = 0; i < compositeType.getSubtypes().length; i++) {
					if (existingComponent.getSubtypes()[i].getReturnedClass() != compositeType.getSubtypes()[i]
							.getReturnedClass()) {
						isDifferent = true;
						break;
					}
				}
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace(MessageFormat.format(
							"Component already collected, the both has the same definition, ok: {0}, {1}",
							existingComponent, compositeType));
				}
				isDifferent = true;
			}
			if (isDifferent) {
				throw new RuntimeException(
						MessageFormat.format("CompositeType's diferentes: {0}, {1}", existingComponent, compositeType));
			}
		}
		pathStack.pop();
		if (ownerCompositeType != null) {
			compositeTypePathStack.pop();
		}
	}

	@Override
	public void collectAssociationAndCompositiesMap() {
		if (logger.isDebugEnabled()) {
			logger.debug("collectAssociationAndCompositiesMap()");
		}
		for (String entityName : this.manager.getConfig().getSessionFactory().getAllClassMetadata().keySet()) {
			ClassMetadata classMetadata = this.manager.getConfig().getSessionFactory().getClassMetadata(entityName);

			Class<?> ownerRootClass;
			try {
				ownerRootClass = Class.forName(classMetadata.getEntityName());
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(classMetadata.getEntityName() + " not supported.", e);
			}

			List<Type> allPrpsAndId = new ArrayList<>();
			List<String> allPrpsAndIdNames = new ArrayList<>();
			allPrpsAndId.addAll(Arrays.asList(classMetadata.getPropertyTypes()));
			allPrpsAndId.add(classMetadata.getIdentifierType());
			allPrpsAndIdNames.addAll(Arrays.asList(classMetadata.getPropertyNames()));
			allPrpsAndIdNames.add(classMetadata.getIdentifierPropertyName());
			for (int i = 0; i < allPrpsAndId.size(); i++) {
				Type prpType = allPrpsAndId.get(i);
				String prpName = allPrpsAndIdNames.get(i);
				AssociationAndComponentPathKey aacKeyFromRoot;
				if (prpType instanceof CompositeType) {
					Stack<String> pathStack = new Stack<>();
					Stack<CompositeType> compositeTypePathStack = new Stack<>();
					pathStack.push(prpName);
					this.collectAssociationAndCompositiesMapRecursive(classMetadata, null, (CompositeType) prpType,
							pathStack, compositeTypePathStack);
				} else if (prpType instanceof EntityType) {
					EntityType entityType = (EntityType) prpType;

					aacKeyFromRoot = new AssociationAndComponentPathKey(ownerRootClass, prpName);

					AssociationAndComponentPathHbSupport relEacPath = new AssociationAndComponentPathHbSupport();
					relEacPath.setAacKey(aacKeyFromRoot);
					relEacPath.setCompositeTypePath(new CompositeType[] {});
					relEacPath.setCompType(null);
					relEacPath.setRelEntity(entityType);
					relEacPath.setCollType(null);
					relEacPath.setCompositePrpPath(new String[] {});
					this.associationAndCompositiesMap.put(aacKeyFromRoot, relEacPath);
				} else if (prpType instanceof CollectionType) {
					CollectionType collType = (CollectionType) prpType;

					aacKeyFromRoot = new AssociationAndComponentPathKey(ownerRootClass, prpName);

					AssociationAndComponentPathHbSupport relEacPath = new AssociationAndComponentPathHbSupport();
					relEacPath.setAacKey(aacKeyFromRoot);
					relEacPath.setCompositeTypePath(new CompositeType[] {});
					relEacPath.setCompType(null);
					relEacPath.setRelEntity(null);
					relEacPath.setCollType(collType);
					relEacPath.setCompositePrpPath(new String[] {});
					this.associationAndCompositiesMap.put(aacKeyFromRoot, relEacPath);
				}
			}
		}
		for (AssociationAndComponentPathKey key : this.associationAndCompositiesMap.keySet()) {
			AssociationAndComponentPathHbSupport aacPath = this.associationAndCompositiesMap.get(key);
			if (aacPath.getCompType() != null) {
				Class<?> compositeClass = this.associationAndCompositiesMap.get(key).getCompType().getReturnedClass();
				this.compositiesSet.add(compositeClass);
			}
		}
	}

	@Override
	public boolean isComponent(Class<?> componentClass) {
		return this.compositiesSet.contains(componentClass);
	}

	@Override
	public void init() {
		this.associationAndCompositiesMap.clear();
		this.collectAssociationAndCompositiesMap();
	}

	@Override
	public boolean isRelationship(Class<?> clazz, String fieldName) {
		if (clazz == null) {
			throw new IllegalArgumentException("clazz can not be null");
		}
		if (fieldName == null || fieldName.trim().isEmpty()) {
			throw new IllegalArgumentException("fieldName can not be null");
		}

		ClassMetadata classMetadata = this.manager.getConfig().getSessionFactory().getClassMetadata(clazz);
		CompositeType compositeType = null;
		if (classMetadata == null) {
			AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(clazz, fieldName);
			compositeType = this.associationAndCompositiesMap.get(aacKey).getCompType();
			if (compositeType == null) {
				throw new RuntimeException("Class is not mapped and is not a know CompositeType: " + clazz);
			}
		}
		Type prpType = null;
		if (classMetadata != null) {
			boolean hasProperty = false;
			if (fieldName.contains(".")) {
				hasProperty = true;
			} else {
				String[] prpNames = classMetadata.getPropertyNames();
				for (int i = 0; i < prpNames.length; i++) {
					String prpNameItem = prpNames[i];
					if (prpNameItem.equals(fieldName)) {
						hasProperty = true;
						break;
					}
				}
			}

			if (hasProperty) {
				try {
					prpType = classMetadata.getPropertyType(fieldName);
				} catch (HibernateException he) {
					throw new RuntimeException(MessageFormat.format("This should not happen for property: {0}.{1}",
							classMetadata.getEntityName(), fieldName), he);
				}
			}
//			for (int i = 0; i < classMetadata.getPropertyNames().length; i++) {
//				String prpNameItem = classMetadata.getPropertyNames()[i];
//				if (prpNameItem.equals(fieldName)) {
//					prpType = classMetadata.getPropertyTypes()[i];
//					break;
//				}
//			}			
		} else {
			String[] prpNames = compositeType.getPropertyNames();
			for (int i = 0; i < prpNames.length; i++) {
				String prpNameItem = prpNames[i];
				if (prpNameItem.equals(fieldName)) {
					prpType = compositeType.getSubtypes()[i];
					break;
				}
			}
		}

		boolean resultBool = false;
		if (prpType == null) {
			resultBool = false;
		} else {
			if (prpType instanceof AssociationType) {
				resultBool = true;
			} else {
				resultBool = false;
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("isRelationship(). clazz: ''{0}''; fieldName: ''{1}''. return: ", clazz,
					fieldName, resultBool));
		}

		return resultBool;
	}

	@Override
	public boolean isPersistentClass(Class<?> clazz) {
		if (this.manager.getConfig().getSessionFactory().getClassMetadata(clazz) != null) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isCollectionRelationship(Class<?> ownerClass, String pathFromOwner) {
		AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(ownerClass, pathFromOwner);
		if (this.associationAndCompositiesMap.containsKey(aacKey)) {
			AssociationAndComponentPathHbSupport aacPath = this.associationAndCompositiesMap.get(aacKey);
			return aacPath.getCollType() != null;
		} else {
			return false;
		}
		
//		ClassMetadata classMetadata = this.manager.getConfig().getSessionFactory().getClassMetadata(ownerClass);
//		
//		Type prpType = null;
//		//if (classMetadata != null) {
//		if (this.isPersistentClass(ownerClass)) {
//			prpType = classMetadata.getPropertyType(fieldName);
//		} else {
//			AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(ownerClass, fieldName);
//			logger.warn(
//					"########## NAO ESTOU CERTO SOBRE ISSO:\nCompositeType componentType = this.compositiesMap.get(key).getCompType();");
//			CompositeType componentType = this.associationAndCompositiesMap.get(aacKey).getCompType();
//			if (componentType == null) {
//				throw new RuntimeException("Unespected type " + ownerClass + "->" + fieldName + ": " + prpType);
//			}
//			
//			int prpIndex = -1;
//			String[] cpsTpArrPrps = componentType.getPropertyNames();
//			for (int i = 0; i < cpsTpArrPrps.length; i++) {
//				if (fieldName.equals(cpsTpArrPrps[i])) {
//					prpIndex = i;
//					break;
//				}
//			}			
//			if (prpIndex == -1) {
//				throw new RuntimeException("fieldName does not exists: " + ownerClass + "->" + fieldName);
//			}
//			
//			prpType = componentType.getSubtypes()[prpIndex];
//		}
//		
//		if (!(prpType instanceof AssociationType)) {
//			return false;
//		}
//
//		// AssociationType assType = (AssociationType)
//		// classMetadata.getPropertyType(fieldName);
//		AssociationType assType = (AssociationType) prpType;
//		if (assType instanceof CollectionType) {
//			return true;
//		}
//		
//		return false;
	}

	@Override
	public boolean isOneToManyRelationship(Class<?> ownerClass, String pathFromOwner) {
		AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(ownerClass, pathFromOwner);
		AssociationAndComponentPathHbSupport entityAndComponentPath = this.associationAndCompositiesMap.get(aacKey);
		if (entityAndComponentPath != null) {
			return entityAndComponentPath.getRelEntity() != null;
		} else {
			return false;			
		}
	}

	@Override
	public Serializable getIdValue(Class<?> entityClass, Object[] rawKeyValues) {
		ClassMetadata classMetadata = this.manager.getConfig().getSessionFactory().getClassMetadata(entityClass);

		Type hbIdType = classMetadata.getIdentifierType();

		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("getBySignature(). Hibernate id Type: ''{0}''", hbIdType));
		}
		
//		Serializable idValue = (Serializable) hbIdType.resolve(signature.getRawKeyValues(),
//				(SessionImplementor) this.config.getSessionFactory().getCurrentSession(), null);
		Serializable idValue = null;
		
		PlayerResultSet playerResultSet = new PlayerResultSet(rawKeyValues);
		try {
			idValue = (Serializable) hbIdType.nullSafeGet(playerResultSet, playerResultSet.getColumnNames(),
					(SessionImplementor) this.manager.getConfig().getSessionFactory().getCurrentSession(), null);
		} catch (HibernateException e) {
			throw new RuntimeException("This should not happen. prpType: ");
		} catch (SQLException e) {
			throw new RuntimeException("This should not happen. prpType: ");
		}
		
		return idValue;
	}

	@Override
	public Object getById(Class<?> entityClass, Serializable idValue) {
		return this.manager.getConfig().getSessionFactory().getCurrentSession().get(entityClass, idValue);
	}

	@Override
	public AssociationAndComponentPath getAssociationAndComponentOnPath(Class<?> ownerClass, String pathStr) {
		AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(ownerClass, pathStr);
		return this.associationAndCompositiesMap.get(aacKey);
	}

	@Override
	public boolean isComponentByTrack(AssociationAndComponentTrackInfo aacTrackInfo) {
		AssociationAndComponentPathHbSupport aacOnPath = this.associationAndCompositiesMap.get(aacTrackInfo);
		if (aacTrackInfo.getEntityAndComponentPath() instanceof AssociationAndComponentPathHbSupport) {
			return ((AssociationAndComponentPathHbSupport)aacTrackInfo.getEntityAndComponentPath()).getCollType() != null;
		} else {
			throw new RuntimeException("This should not happen. prpType: ");
		}
	}

	@Override
	public boolean isComponentOrRelationship(Class<?> ownerClass, String pathFromOwner) {
		AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(ownerClass, pathFromOwner);
		return this.associationAndCompositiesMap.containsKey(aacKey);
	}
}
