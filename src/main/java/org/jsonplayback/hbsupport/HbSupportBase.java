package org.jsonplayback.hbsupport;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
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

import javax.persistence.criteria.CriteriaBuilder.In;

import org.hibernate.HibernateException;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.jsonplayback.player.IPlayerManager;
import org.jsonplayback.player.hibernate.AssociationAndComponentPath;
import org.jsonplayback.player.hibernate.AssociationAndComponentPathKey;
import org.jsonplayback.player.hibernate.AssociationAndComponentTrackInfo;
import org.jsonplayback.player.hibernate.PlayerResultSet;
import org.jsonplayback.player.hibernate.PlayerStatment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.util.ClassUtil;

public abstract class HbSupportBase implements HbSupport {
	private static Logger logger = LoggerFactory.getLogger(HbSupportBase.class);

	private IPlayerManager manager;
	private Map<AssociationAndComponentPathKey, AssociationAndComponentPathHbSupport> associationAndCompositiesMap = new HashMap<>();
	private Set<Class<?>> compositiesSet = new HashSet<>();

	public HbSupportBase(IPlayerManager manager) {
		this.manager = manager;
		this.persistentCollecitonClass = this.resolvePersistentCollectionClass();
	}

	protected Class<?> persistentCollecitonClass;

	@Override
	public boolean isPersistentCollection(Object coll) {
		return persistentCollecitonClass.isAssignableFrom(coll.getClass());
	}

	@Override
	public abstract boolean isCollectionLazyUnitialized(Object coll);
	
	public abstract Class<?> resolvePersistentCollectionClass();
	
	public Class<?> getPersistentCollecitonClass() {
		return persistentCollecitonClass;
	}

	@Override
	public abstract Object getCollectionOwner(Object coll);

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
	public abstract  Object[] getRawKeyValuesFromHbProxy(Object hibernateProxy);

	@Override
	public abstract Object[] getRawKeyValuesFromNonHbProxy(Object nonHibernateProxy);

	@Override
	public abstract String getCollectionGetRole(Object coll);

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

	protected void collectAssociationAndCompositiesMapRecursive(ClassMetadata ownerRootClassMetadata,
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
		if (!this.associationAndCompositiesMap.containsKey(aacKeyFromRoot)) {
			AssociationAndComponentPathHbSupport aacPath = new AssociationAndComponentPathHbSupport();
			aacPath.setAacKey(aacKeyFromRoot);
			aacPath.setCompositeTypePath(new CompositeType[] { compositeType });
			aacPath.setCompType(compositeType);
			aacPath.setRelEntity(null);
			aacPath.setCollType(null);
			aacPath.setCompositePrpPath(new String[] { pathStack.peek() });
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
			
					aacKeyFromRoot = new AssociationAndComponentPathKey(ownerRootClass, pathStackRelationStr);

					if (!this.associationAndCompositiesMap.containsKey(aacKeyFromRoot)) {
						AssociationAndComponentPathHbSupport relEacPathFromRoot = new AssociationAndComponentPathHbSupport();
						relEacPathFromRoot.setAacKey(aacKeyFromRoot);
						relEacPathFromRoot.setCompositeTypePath(
								compositeTypePathStack.toArray(new CompositeType[compositeTypePathStack.size()]));
						relEacPathFromRoot.setCompType(null);
						relEacPathFromRoot.setRelEntity(entityType);
						relEacPathFromRoot.setCollType(null);
						relEacPathFromRoot.setCompositePrpPath(pathStack.toArray(new String[pathStack.size()]));
						this.associationAndCompositiesMap.put(aacKeyFromRoot, relEacPathFromRoot);
					}
				} else if (subPrpType instanceof CollectionType) {
					CollectionType collType = (CollectionType) subPrpType;
					Stack<String> pathStackRelation = new Stack<String>();
					pathStackRelation.addAll(pathStack);
					pathStackRelation.push(subPrpName);
					String pathStackRelationStr = this.mountPathFromStack(pathStackRelation);
				
					aacKeyFromRoot = new AssociationAndComponentPathKey(ownerRootClass, pathStackRelationStr);
					if (!this.associationAndCompositiesMap.containsKey(aacKeyFromRoot)) {
						AssociationAndComponentPathHbSupport relEacPathFromRoot = new AssociationAndComponentPathHbSupport();
						relEacPathFromRoot.setAacKey(aacKeyFromRoot);
						relEacPathFromRoot.setCompositeTypePath(
								compositeTypePathStack.toArray(new CompositeType[compositeTypePathStack.size()]));
						relEacPathFromRoot.setCompType(null);
						relEacPathFromRoot.setRelEntity(null);
						relEacPathFromRoot.setCollType(collType);
						relEacPathFromRoot.setCompositePrpPath(pathStack.toArray(new String[pathStack.size()]));
						this.associationAndCompositiesMap.put(aacKeyFromRoot, relEacPathFromRoot);
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
	public abstract Serializable getIdValue(Class<?> entityClass, Object[] rawKeyValues);
	
	@Override
	public abstract Serializable getIdValue(Object entityInstanceOrProxy);

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
	
	protected Object runByReflection(String classStr, String methodName, String[] argsClassStrArr, Object instance, Object[] argsValues) {
		Class<?> clazz;
		try {
			clazz = Class.forName(classStr);
			Class<?>[] argsClassArr = new Class<?>[argsClassStrArr.length];
			for (int i = 0; i < argsClassStrArr.length; i++) {
				argsClassArr[i] = this.correctClass(argsClassStrArr[i]);
			}
			Method method = clazz.getMethod(methodName, argsClassArr);
			return method.invoke(instance, argsValues);
		} catch (Throwable e) {
			throw new RuntimeException(
					"This should not happen. " + classStr + ", " + methodName + ", " + Arrays.toString(argsClassStrArr), e);
		}
	}
	
	protected Class<?> correctClass(String name) {
		if (name == int.class.getName()) {
			return int.class;
		} else {
			try {
				return Class.forName(name);
			} catch (Throwable e) {
				throw new RuntimeException("This should not happen.", e);
			}
		}
	}
}
