package org.jsonplayback.player.hibernate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.codec.binary.Base64;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
//import org.hibernate.metadata.ClassMetadata;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.jsonplayback.hbsupport.Hb3Support;
import org.jsonplayback.hbsupport.HbSupport;
import org.jsonplayback.player.IDirectRawWriter;
import org.jsonplayback.player.IDirectRawWriterWrapper;
import org.jsonplayback.player.IPlayerConfig;
import org.jsonplayback.player.IPlayerManager;
import org.jsonplayback.player.IReplayable;
import org.jsonplayback.player.IdentityRefKey;
import org.jsonplayback.player.LazyProperty;
import org.jsonplayback.player.PlayerMetadatas;
import org.jsonplayback.player.PlayerSnapshot;
import org.jsonplayback.player.SignatureBean;
import org.jsonplayback.player.Tape;
import org.jsonplayback.player.util.PathEntry;
import org.jsonplayback.player.util.ReflectionNamesDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

public class PlayerManagerDefault implements IPlayerManagerImplementor {
	private static final String HBVERSION = "jsonplayback.hbversion"; 
	private static Logger logger = LoggerFactory.getLogger(PlayerManagerDefault.class);
	private static Properties envPrps = new Properties(); 
	
	static {
		InputStream is = PlayerManagerDefault.class.getClassLoader().getResourceAsStream("environment.properties");
		try {
			envPrps.load(is);
		} catch (IOException e) {
			throw new RuntimeException("This should not happen", e);
		}
	}
	
	private IPlayerConfig config = new PlayerConfig();
	ThreadLocal<Long> currIdTL = new ThreadLocal<Long>();
	ThreadLocal<Map<Long, Object>> objectByIdMapTL = new ThreadLocal<>();
	ThreadLocal<Map<IdentityRefKey, Long>> idByObjectMapTL = new ThreadLocal<>();
	ThreadLocal<Map<IdentityRefKey, PlayerMetadatas>> metadatasCacheMapTL = new ThreadLocal<>();
	ThreadLocal<IPlayerConfig> temporaryConfigurationTL = new ThreadLocal<IPlayerConfig>();
	ThreadLocal<Map<IdentityRefKey, OwnerAndProperty>> registeredComponentOwnersTL = new ThreadLocal<>();
	ThreadLocal<Stack<PlayerBeanPropertyWriter>> playerBeanPropertyWriterStepStackTL = new ThreadLocal<Stack<PlayerBeanPropertyWriter>>();
	ThreadLocal<Stack<PlayerJsonSerializer>> playerJsonSerializerStepStackTL = new ThreadLocal<Stack<PlayerJsonSerializer>>(); 
	ThreadLocal<Stack<PlayerMetadatas>> playerMetadatasWritingStackTL = new ThreadLocal<Stack<PlayerMetadatas>>();

//	ThreadLocal<Stack<String>> currentCompositePathStackTL = new ThreadLocal<>();
//	ThreadLocal<Object> currentCompositeOwner = new ThreadLocal<>();

	@Override
	public <T> PlayerSnapshot<T> createPlayerSnapshot(T result) {
		if (logger.isTraceEnabled()) {
			logger.trace(
					MessageFormat.format("createResultEntity for {0}", result != null ? result.getClass() : "null"));
		}
		return new PlayerSnapshot<T>(result).configManager(this);
	}

	@Override
	public PlayerManagerDefault configure(IPlayerConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("config can not be null");
		}
		if (logger.isDebugEnabled()) {
			logger.debug(MessageFormat.format("configure.  config:\n{0}", config));
		}
		this.config = config;
		this.hibernateVersion = HibernateVersion.valueOf(envPrps.getProperty(HBVERSION));
		
		if (this.hibernateVersion.equals(HibernateVersion.HB3)) {
			this.hbSupport = new Hb3Support(this);
		} else {
			throw new RuntimeException("hibernate version not supported: " + this.hibernateVersion);
		}
//		else if (envPrps.getProperty(HBVERSION).equals("HB3")) {
//			this.hbSupport = new Hb3Support(this);
//		}
		return this;
	}

	private boolean initialed = false;

	@Override
	public IPlayerManager init() {
		if (logger.isDebugEnabled()) {
			logger.debug("init()");
		}
		this.getHbSupport().init();
		this.initialed = true;
		return this;
	}
//
//	private void collectAssociationAndCompositiesMap() {
//		if (logger.isDebugEnabled()) {
//			logger.debug("collectAssociationAndCompositiesMap()");
//		}
//		for (String entityName : this.config.getSessionFactory().getAllClassMetadata().keySet()) {
//			ClassMetadata classMetadata = this.config.getSessionFactory().getClassMetadata(entityName);
//			
//			Class<?> ownerRootClass;
//			try {
//				ownerRootClass = Class.forName(classMetadata.getEntityName());
//			} catch (ClassNotFoundException e) {
//				throw new RuntimeException(classMetadata.getEntityName() + " not supported.", e);
//			}
//
//			List<Type> allPrpsAndId = new ArrayList<>();
//			List<String> allPrpsAndIdNames = new ArrayList<>();
//			allPrpsAndId.addAll(Arrays.asList(classMetadata.getPropertyTypes()));
//			allPrpsAndId.add(classMetadata.getIdentifierType());
//			allPrpsAndIdNames.addAll(Arrays.asList(classMetadata.getPropertyNames()));
//			allPrpsAndIdNames.add(classMetadata.getIdentifierPropertyName());
//			for (int i = 0; i < allPrpsAndId.size(); i++) {
//				Type prpType = allPrpsAndId.get(i);
//				String prpName = allPrpsAndIdNames.get(i);
//				AssociationAndComponentPathKey aacKeyFromRoot;
//				if (prpType instanceof CompositeType) {
//					Stack<String> pathStack = new Stack<>();
//					Stack<CompositeType> compositeTypePathStack = new Stack<>();
//					pathStack.push(prpName);
//					this.collectAssociationAndCompositiesMapRecursive(classMetadata, null, (CompositeType) prpType,
//							pathStack, compositeTypePathStack);
//				} else if (prpType instanceof EntityType) {
//					EntityType entityType = (EntityType) prpType;
//
//					aacKeyFromRoot = new AssociationAndComponentPathKey(ownerRootClass, prpName);
//
//					AssociationAndComponentPath relEacPath = new AssociationAndComponentPath();
//					relEacPath.setAacKey(aacKeyFromRoot);
//					relEacPath.setCompositeTypePath(new CompositeType[] {});
//					relEacPath.setCompType(null);
//					relEacPath.setRelEntity(entityType);
//					relEacPath.setCollType(null);
//					relEacPath.setCompositePrpPath(new String[] {});
//					this.associationAndCompositiesMap.put(aacKeyFromRoot, relEacPath);
//				} else if (prpType instanceof CollectionType) {
//					CollectionType collType = (CollectionType) prpType;
//
//					aacKeyFromRoot = new AssociationAndComponentPathKey(ownerRootClass, prpName);
//
//					AssociationAndComponentPath relEacPath = new AssociationAndComponentPath();
//					relEacPath.setAacKey(aacKeyFromRoot);
//					relEacPath.setCompositeTypePath(new CompositeType[] {});
//					relEacPath.setCompType(null);
//					relEacPath.setRelEntity(null);
//					relEacPath.setCollType(collType);
//					relEacPath.setCompositePrpPath(new String[] {});
//					this.associationAndCompositiesMap.put(aacKeyFromRoot, relEacPath);
//				}
//			}
//		}
//		for (AssociationAndComponentPathKey key : this.associationAndCompositiesMap.keySet()) {
//			AssociationAndComponentPath aacPath = this.associationAndCompositiesMap.get(key);
//			if (aacPath.getCompType() != null) {
//				Class<?> compositeClass = this.associationAndCompositiesMap.get(key).getCompType().getReturnedClass();
//			this.compositiesSet.add(compositeClass);
//	}
//	}
//	}
//
//	private String mountPathFromStack(Collection<String> pathStack) {
//		String pathResult = "";
//		String dotStr = "";
//		for (String pathItem : pathStack) {
//			pathResult += dotStr + pathItem;
//			dotStr = ".";
//		}
//		return pathResult;
//	}

	@Override
	public String serializeSignature(SignatureBean signatureBean) {
		SignatureBeanJson signatureBeanJson = new SignatureBeanJson();

		ArrayList<String> rawValueList = new ArrayList();
		ArrayList<String> rawTypeList = new ArrayList();
		for (Object item : signatureBean.getRawKeyValues()) {
			if (item != null) {
				rawValueList.add(item.toString());
				rawTypeList.add(item.getClass().getName());
			} else {
				rawValueList.add(null);
				rawTypeList.add(null);
			}
		}

		signatureBeanJson.setClazzName(signatureBean.getClazz().getName());
//		signatureBeanJson.setEntityName(signatureBean.getEntityName());
		signatureBeanJson.setPropertyName(signatureBean.getPropertyName());
		signatureBeanJson.setRawKeyValues(new String[rawValueList.size()]);
		signatureBeanJson.setRawKeyTypeNames(new String[rawValueList.size()]);
		signatureBeanJson.setRawKeyValues(rawValueList.toArray(signatureBeanJson.getRawKeyValues()));
		signatureBeanJson.setRawKeyTypeNames(rawTypeList.toArray(signatureBeanJson.getRawKeyTypeNames()));
//		signatureBeanJson.setIsAssoc(signatureBean.getIsAssoc());
		signatureBeanJson.setIsColl(signatureBean.getIsColl());
		signatureBeanJson.setIsComp(signatureBean.getIsComp());

		String result = "FOO BAA";
		if (this.config.getSignatureCrypto() != null) {
			result = this.config.getSignatureCrypto().encrypt(result);
		}

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_DEFAULT);
		StringWriter writer = new StringWriter();
		try {
			objectMapper.writeValue(writer, signatureBeanJson);
		} catch (JsonGenerationException e) {
			throw new RuntimeException("Isso nao deveria acontencer!", e);
		} catch (JsonMappingException e) {
			throw new RuntimeException("Isso nao deveria acontencer!", e);
		} catch (IOException e) {
			throw new RuntimeException("Isso nao deveria acontencer!", e);
		}
		String resultStr = writer.toString();
		if (this.config.getSignatureCrypto() != null) {
			resultStr = this.config.getSignatureCrypto().encrypt(resultStr);
			if (logger.isTraceEnabled()) {
				logger.trace(MessageFormat.format(
						"serializeSignature(). encrypting. original json signature: ''{0}'', encripted signature: ''{1}''",
						writer.toString(), resultStr));
			}
		} else {
			resultStr = Base64.encodeBase64URLSafeString(resultStr.getBytes());
			if (logger.isTraceEnabled()) {
				logger.trace(MessageFormat.format(
						"serializeSignature(). Using simple ''base64 url safe'' from json signature. original json signature: ''{0}'', base64 signature: ''{1}''",
						writer.toString(), resultStr));
			}
		}
		return resultStr;
	}
	
	@Override
	public SignatureBean deserializeSignature(String signatureStr) {
		String decryptedSignatureStr = signatureStr;

		if (this.config.getSignatureCrypto() != null) {
			decryptedSignatureStr = this.config.getSignatureCrypto().decrypt(signatureStr);
			if (logger.isTraceEnabled()) {
				logger.trace(MessageFormat.format(
						"serializeSignature(). decrypting. original json signature: ''{0}'', dncripted signature: ''{1}''",
						decryptedSignatureStr, signatureStr));
			}
		} else {
			decryptedSignatureStr = new String(Base64.decodeBase64(signatureStr));
			if (logger.isTraceEnabled()) {
				logger.trace(MessageFormat.format(
						"serializeSignature(). Using simple ''base64 url safe'' from json signature. original json signature: ''{0}'', base64 signature: ''{1}''",
						decryptedSignatureStr, signatureStr));
			}
		}
		
		ObjectMapper objectMapper = new ObjectMapper();
		StringReader reader = new StringReader(decryptedSignatureStr);
		SignatureBeanJson signatureBeanJson;
		try {
			signatureBeanJson = objectMapper.readValue(reader, SignatureBeanJson.class);
		} catch (IOException e) {
			throw new RuntimeException("This should not happen", e);
		}
		SignatureBean signatureBean = new SignatureBean();
		
		ArrayList<Object> rawObjValueList = new ArrayList();
		for (int i = 0; i < signatureBeanJson.getRawKeyTypeNames().length; i++) {
			Class<?> itemType;
			try {
				itemType = Class.forName(signatureBeanJson.getRawKeyTypeNames()[i]);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("This should not happen", e);
			}
			String itemValueStr = signatureBeanJson.getRawKeyValues()[i]; 
			Method valueOfMethod;
			try {
				if (itemType == String.class) {
					valueOfMethod = null;
				} else {
					valueOfMethod = itemType.getMethod("valueOf", String.class);					
				}
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("This should not happen", e);
			} catch (SecurityException e) {
				throw new RuntimeException("This should not happen", e);
			}
			Object itemValue;
			try {
				if (valueOfMethod == null) {
					itemValue = itemValueStr;
				} else {
					itemValue = valueOfMethod.invoke(null, itemValueStr);					
				}
			} catch (IllegalAccessException e) {
				throw new RuntimeException("This should not happen", e);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("This should not happen", e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException("This should not happen", e);
			}
			rawObjValueList.add(itemValue);
		}
		signatureBean.setRawKeyValues(rawObjValueList.toArray());
		try {
			signatureBean.setClazz(Class.forName(signatureBeanJson.getClazzName()));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("This should not happen", e);
		}
		signatureBean.setEntityName(signatureBeanJson.getClazzName());
		signatureBean.setPropertyName(signatureBeanJson.getPropertyName());
//		signatureBean.setIsAssoc(signatureBeanJson.getIsAssoc());
		signatureBean.setIsColl(signatureBeanJson.getIsColl());
		signatureBean.setIsComp(signatureBeanJson.getIsComp());
		
		signatureBean.setSignature(signatureStr);
		
		return signatureBean;
	}


	@Override
	public SignatureBean generateLazySignature(Collection<?> persistentCollection) {
		Object ownerValue = this.getHbSupport().getCollectionOwner(persistentCollection);
		Class<?> ownerClass = ownerValue.getClass();
		String fieldName = this.getHbSupport().getCollectionFieldName(persistentCollection);
		Object fieldValue = persistentCollection;

		SignatureBean signatureBean = this.generateLazySignatureForCollRelashionship(ownerClass, fieldName, ownerValue,
				fieldValue);
		signatureBean.setIsColl(true);
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("generateLazySignature(). signatureBean:\nsignatureBean:\n{0}",
					signatureBean));
		}
		
		return signatureBean;
	}

	@Override
	public SignatureBean generateLazySignatureForCollRelashionship(Class<?> ownerClass, String fieldName,
			Object ownerValue, Object fieldValue) {
		if (!this.isRelationship(ownerClass, fieldName)) {
			throw new RuntimeException("This is not a relationship: " + ownerClass + "->" + fieldName);
		}
		//ClassMetadata classMetadata = this.config.getSessionFactory().getClassMetadata(ownerClass);
		
//		//Type prpType = null;
//		//if (classMetadata != null) {
//		if (this.getHbSupport().isPersistentClass(ownerClass)) {
//			//prpType = classMetadata.getPropertyType(fieldName);
//			//nothing
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
		
		if (!this.getHbSupport().isCollectionRelationship(ownerClass, fieldName)) {
			throw new RuntimeException("Unespected type " + ownerClass + "->" + fieldName);
		}
//		if (!(prpType instanceof AssociationType)) {
//			throw new RuntimeException("Unespected type " + ownerClass + "->" + fieldName + ": " + prpType);
//		}

		SignatureBean signatureBean = null;
		// AssociationType assType = (AssociationType)
		// classMetadata.getPropertyType(fieldName);
		//AssociationType assType = (AssociationType) prpType;
		Object idValue = null;
		//if (assType instanceof CollectionType) {
//		if (this.getHbSupport().isCollectionRelationship(ownerClass, fieldName)) {
			if (ownerValue instanceof HibernateProxy) {
				signatureBean = this.generateLazySignature((HibernateProxy) ownerValue);
			} else {
				signatureBean = this.generateSignature(ownerValue);
			}
//			signatureBean.setIsAssoc(true);
			if (ownerValue == null) {
				throw new IllegalArgumentException(
						"ownerValue can not be null em caso de CollectionType: " + ownerClass + "->" + fieldName);
			}
			// if collection is not loaded "collType.getKeyOfOwner" is inconsistent (null)
			// on hb-3
//			CollectionType collType = (CollectionType) assType;
//			idValue = collType.getKeyOfOwner(ownerValue,
//					(SessionImplementor) this.config.getSessionFactory().getCurrentSession());
			signatureBean.setClazz(ownerClass);
			signatureBean.setIsColl(true);
			signatureBean.setPropertyName(fieldName);
//		} else {
//			if (fieldValue instanceof HibernateProxy) {
//				signatureBean = this.generateLazySignature((HibernateProxy) fieldValue);
//			} else {
//				signatureBean = this.generateSignature(fieldValue);
//			}
//		}

			// This will cause two signatures for the same instance!?!?!?!?
			// signatureBean.setIsAssoc(true);

//			classMetadata = this.config.getSessionFactory().getClassMetadata(assType.getReturnedClass());
//			idValue = classMetadata.getIdentifier(fieldValue,
//					(SessionImplementor) this.config.getSessionFactory().getCurrentSession());
//			signatureBean.setClazz(assType.getReturnedClass());
//		}
		// signatureBean.setEntityName(classMetadata.getEntityName());
		
		if (logger.isTraceEnabled()) {
			logger.trace(
					MessageFormat.format("generateLazySignatureForRelashionship().signatureBean:\n{0}", signatureBean));
		}

		return signatureBean;
	}

	@Override
	public SignatureBean generateLazySignatureForLazyProperty(Class<?> ownerClass, String fieldName,
			Object ownerValue, Object fieldValue) {
		if (fieldValue instanceof byte[] || fieldValue instanceof Blob || fieldValue instanceof String
				|| fieldValue instanceof Clob) {
			// nothing
		} else {
			throw new RuntimeException("fieldValue type does not support LazyProperty: " + fieldValue.getClass());
		}
//		ClassMetadata classMetadata = this.config.getSessionFactory().getClassMetadata(ownerClass);
//
//		Type prpType = null;
//		if (classMetadata != null) {
//			prpType = classMetadata.getPropertyType(fieldName);
//		} else {
//			AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(ownerClass, fieldName);
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

		//if (prpType instanceof AssociationType) {
		if (this.getHbSupport().isRelationship(ownerClass, fieldName)) {
			throw new RuntimeException("Unespected type " + ownerClass + "->" + fieldName);
		}

		SignatureBean signatureBean = this.generateSignature(ownerValue);
		signatureBean.setIsLazyProperty(true);
		signatureBean.setPropertyName(fieldName);
		
		return signatureBean;
	}

	@Override
	public SignatureBean generateLazySignature(HibernateProxy hibernateProxy) {
		Class entityClass = hibernateProxy.getClass().getSuperclass();
		SignatureBean signatureBean = new SignatureBean();
		signatureBean.setClazz(entityClass);
		signatureBean.setEntityName(entityClass.getName());
		signatureBean.setPropertyName(null);

		Object[] rawKeyValuesFromHbProxy = this.getHbSupport().getRawKeyValuesFromHbProxy(hibernateProxy);

		signatureBean.setRawKeyValues(rawKeyValuesFromHbProxy);
		
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("generateLazySignature(). signatureBean:\nsignatureBean:\n{0}",
					signatureBean));
		}
		
		return signatureBean;
	}

	@Override
	public SignatureBean generateComponentSignature(AssociationAndComponentTrackInfo entityAndComponentTrackInfo) {
		SignatureBean signatureBean = this.generateSignature(entityAndComponentTrackInfo.getEntityOwner());
		signatureBean.setPropertyName(
				entityAndComponentTrackInfo.getEntityAndComponentPath().getAacKey().getPathFromOwner());
		signatureBean.setIsComp(true);
		return signatureBean;
	}
	
	@Override
	public SignatureBean generateSignature(Object nonHibernateProxy) {
		if (nonHibernateProxy instanceof HibernateProxy) {
			throw new RuntimeException("nonHibernateProxy instanceof HibernateProxy: " + nonHibernateProxy);
		}
		Class entityClass = nonHibernateProxy.getClass();
		SignatureBean signatureBean = new SignatureBean();
		signatureBean.setClazz(entityClass);
		signatureBean.setEntityName(entityClass.getName());
		signatureBean.setPropertyName(null);
		
		Object[] rawKeyValuesFromHbProxy = this.getHbSupport().getRawKeyValuesFromNonHbProxy(nonHibernateProxy);
		
		signatureBean.setRawKeyValues(rawKeyValuesFromHbProxy);
		
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("generateSignature(Object nonHibernateProxy). signatureBean:\n{0}",
					signatureBean));
		}
		
		return signatureBean;
	}

	@Override
	public <T> T getBySignature(SignatureBean signature) {
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("getBySignature(). Begin. \nsignatureBean:\n{0}", signature));
		}
		
//		ClassMetadata classMetadata = this.config.getSessionFactory().getClassMetadata(signature.getClazz());
//
//		Type hbIdType = classMetadata.getIdentifierType();

//		if (logger.isTraceEnabled()) {
//			logger.trace(MessageFormat.format("getBySignature(). Hibernate id Type: ''{0}''", hbIdType));
//		}
		
//		Serializable idValue = (Serializable) hbIdType.resolve(signature.getRawKeyValues(),
//				(SessionImplementor) this.config.getSessionFactory().getCurrentSession(), null);
//		Serializable idValue = null;
//		
//		PlayerResultSet playerResultSet = new PlayerResultSet(signature.getRawKeyValues());
//		try {
//			idValue = (Serializable) hbIdType.nullSafeGet(playerResultSet, playerResultSet.getColumnNames(),
//					(SessionImplementor) this.config.getSessionFactory().getCurrentSession(), null);
//		} catch (HibernateException e) {
//			throw new RuntimeException("This should not happen. prpType: ");
//		} catch (SQLException e) {
//			throw new RuntimeException("This should not happen. prpType: ");
//		}
//		
//		if (idValue.getClass().isArray()) {
//			if (((Object[])idValue).length == 1) {
//				idValue = (Serializable) ((Object[])idValue)[0];
//			}
//		}
//		Object owner = this.config.getSessionFactory().getCurrentSession().get(signature.getClazz(), idValue);
		Serializable idValue = this.getHbSupport().getIdValue(signature.getClazz(), signature.getRawKeyValues());
		Object owner = this.getHbSupport().getById(signature.getClazz(), idValue);
		Object result = owner;

		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("getBySignature(). Hibernate id Type: ''{0}''", idValue.getClass()));
		}
		
		Type propertyType = null;
		if (signature.getPropertyName() != null) {
//			// significa que eh uma collection, mas no futuro podera ser tambem uma
//			// propriedade lazy, como um blob por exemplo!
//			Type prpType = classMetadata.getPropertyType(signature.getPropertyName());
//			if (logger.isWarnEnabled()) {
//				logger.warn(MessageFormat
//						.format("getBySignature(). propery Type: ''{0}''. We are inferring this is an 'one to many'"
//								+ " relationship because propertyName is not null, on the"
//								+ " future this will not be always true, there will exists"
//								+ " non collection lazy properties like Blob's for instance.", prpType));
//			}
//			Collection resultColl = null;
//			if (prpType instanceof CollectionType) {
//				if (prpType instanceof SetType) {
//					resultColl = new LinkedHashSet<>();
//				} else if (prpType instanceof ListType) {
//					throw new RuntimeException("Not supported. prpType: " + prpType);
//				} else if (prpType instanceof BagType) {
//					throw new RuntimeException("Not supported. prpType: " + prpType);
//				} else {
//					throw new RuntimeException("This should not happen. prpType: " + prpType);
//				}
//			} else {
//				throw new RuntimeException("This should not happen. prpType: " + prpType);
//			}
//			Collection persistentCollection = (Collection) classMetadata.getPropertyValue(owner,
//					signature.getPropertyName(),
//					this.config.getSessionFactory().getCurrentSession().getEntityMode());
//			for (Object item : persistentCollection) {
//				resultColl.add(item);
//			}
			try {
				result = PropertyUtils.getNestedProperty(owner, signature.getPropertyName());
			} catch (Exception e) {
				throw new RuntimeException("This should not happen for property");				
			}
		}
		
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("getBySignature(). \nsignatureBean:\n{0}\nresult.getClass(): ''{1}''",
					signature, (result != null ? result.getClass().toString() : "null")));
		}

		return (T) result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean isNeverSigned(Class clazz) {
		return this.config.getNeverSignedClasses().contains(clazz);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean isPersistentClass(Class clazz) {
		return this.config.getSessionFactory().getClassMetadata(clazz) != null;
	}

	@Override
	public Object getHibernateObjectId(Object object) {
		if (logger.isTraceEnabled()) {
			logger.trace("getHibernateObjectId()");
		}
		Class entityClass = object.getClass();
		if (object instanceof HibernateProxy) {
			entityClass = object.getClass().getSuperclass();
		}

		@SuppressWarnings("deprecation")
		Object idValue = this.config.getSessionFactory().getClassMetadata(entityClass).getIdentifier(object,
				(SessionImplementor) this.config.getSessionFactory().getCurrentSession());
		return idValue;
	}
	
	@Override
	public String getPlayerObjectIdName(Class clazz) {

		return this.config.getSessionFactory().getClassMetadata(clazz).getIdentifierPropertyName();
	}

	@Override
	public IPlayerConfig getConfig() {
		if (this.temporaryConfigurationTL.get() != null) {
			return this.temporaryConfigurationTL.get();
		} else {
			return this.config;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void startJsonWriteIntersept() {
		if (logger.isTraceEnabled()) {
			logger.trace("startSuperSync()");
		}
		if (!this.initialed) { // pode parecer desnecessario mas evita o overead
								// de obtencao de lock no synchronized logo
								// abaixo.
			synchronized (this) {
				if (!this.initialed) {
					this.init();
				}
			}
		}
		if (this.registeredComponentOwnersTL.get() == null) {
			this.registeredComponentOwnersTL.set(new HashMap<>());
		}
		this.currIdTL.set(0L);
		this.objectByIdMapTL.set(new HashMap<Long, Object>());
		this.idByObjectMapTL.set(new HashMap<IdentityRefKey, Long>());
		this.metadatasCacheMapTL.set(new HashMap<>());
		this.playerBeanPropertyWriterStepStackTL.set(new Stack<PlayerBeanPropertyWriter>());
		this.playerJsonSerializerStepStackTL.set(new Stack<PlayerJsonSerializer>());
		this.playerMetadatasWritingStackTL.set(new Stack<PlayerMetadatas>());
		//this.registeredComponentOwnersTL.set(new HashMap<>());
//		this.currentCompositeOwner.set(null);
//		this.currentCompositePathStackTL.set(new Stack<>());
	}

	@Override
	public void stopJsonWriteIntersept() {
		if (logger.isTraceEnabled()) {
			logger.trace("stopSuperSync()");
		}
		this.currIdTL.set(null);
		this.objectByIdMapTL.set(null);
		this.idByObjectMapTL.set(null);
		this.metadatasCacheMapTL.set(null);
		this.playerBeanPropertyWriterStepStackTL.set(null);
		this.playerJsonSerializerStepStackTL.set(null);
		this.playerMetadatasWritingStackTL.set(null);
//		this.currentCompositeOwner.set(null);
//		this.currentCompositePathStackTL.set(null);
		
		this.temporaryConfigurationTL.set(null);
		this.registeredComponentOwnersTL.set(new HashMap<>());
	}

	private void validateStarted() {
		if (this.currIdTL.get() == null) {
			throw new RuntimeException("Not started");
		}
	}

	@Override
	public Map<Long, Object> getObjectByIdMap() {
		this.validateStarted();
		return this.objectByIdMapTL.get();
	}

	@Override
	public Map<IdentityRefKey, Long> getIdByObjectMap() {
		this.validateStarted();
		return this.idByObjectMapTL.get();
	}
	
	@Override
	public Map<IdentityRefKey, PlayerMetadatas> getMetadatasCacheMap() {
		this.validateStarted();
		return this.metadatasCacheMapTL.get();
	}

	@Override
	public Stack<PlayerBeanPropertyWriter> getPlayerBeanPropertyWriterStepStack() {
		return this.playerBeanPropertyWriterStepStackTL.get();
	}
	
	@Override
	public Stack<PlayerJsonSerializer> getPlayerJsonSerializerStepStack() {
		return this.playerJsonSerializerStepStackTL.get();
	}

	@Override
	public Stack<PlayerMetadatas> getPlayerMetadatasWritingStack() {
		return playerMetadatasWritingStackTL.get();
	}

	@Override
	public Long getCurrId() {
		this.validateStarted();
		return this.currIdTL.get();
	}

	@Override
	public void currIdPlusPlus() {
		this.validateStarted();
		this.currIdTL.set(this.currIdTL.get() + 1);
	}

	@Override
	public boolean isStarted() {
		return this.currIdTL.get() != null;
	}

	@Override
	public boolean isRelationship(Class<?> clazz, String fieldName) {
		return this.getHbSupport().isRelationship(clazz, fieldName);
	}

	@Override
	public boolean isComponent(Class<?> componentClass) {
		return this.getHbSupport().isComponent(componentClass);
	}

	@Override
	public IPlayerManager overwriteConfigurationTemporarily(IPlayerConfig newConfig) {
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("overwriteConfigurationTemporarily(). newConfig:\n {0}'", newConfig));
		}
		this.temporaryConfigurationTL.set(newConfig);
		return this;
	}
	
	@Override
	public IPlayerManager cloneWithNewConfiguration(IPlayerConfig newConfig) {
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("cloneWithNewConfiguration(). newConfig:\n {0}'", newConfig));
		}
		IPlayerManager managerCloned = new PlayerManagerDefault();
		managerCloned = managerCloned.configure(newConfig);
		return managerCloned;
	}

	@Override
	public IReplayable prepareReplayable(Tape tape) {
//		throw new RuntimeException("");
//		this.temporaryConfigurationTL.set(newConfig);
//		return null;
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("prepareReplayable(). tape:\n {0}'", tape));
		}
		return new ReplayableDefault().configManager(this).loadPlayback(tape);
	}
	
	@Override
	public AssociationAndComponentTrackInfo getCurrentAssociationAndComponentTrackInfo() {
		List<String> pathList = new ArrayList<>();
		Object lastEntityOwner = null;
		Stack<PlayerJsonSerializer> serStepTackLocal = new Stack<>();
		serStepTackLocal.addAll(this.playerJsonSerializerStepStackTL.get());
		// ignoring last one
		serStepTackLocal.pop();
		for (PlayerJsonSerializer playerJsonSerializer : serStepTackLocal) {
			Object currBean = playerJsonSerializer.getCurrSerializationBean();
			if (currBean instanceof PlayerMetadatas) {
				currBean = ((PlayerMetadatas) currBean).getOriginalPlayerObjectIdOwner();
			}
			if (playerJsonSerializer.getCurrSerializationBean() != null && this.isPersistentClass(currBean.getClass())) {
				lastEntityOwner = currBean;
			}
		}

		for (PlayerBeanPropertyWriter jbHbBeanPropertyWriter : this.getPlayerBeanPropertyWriterStepStack()) {
			if (jbHbBeanPropertyWriter.getCurrOwner() != null 
					&& this.isPersistentClass(jbHbBeanPropertyWriter.getCurrOwner().getClass())) {
				lastEntityOwner = jbHbBeanPropertyWriter.getCurrOwner();
				pathList.clear();
				pathList.add(jbHbBeanPropertyWriter.getBeanPropertyDefinition().getInternalName());
			} else {
				pathList.add(jbHbBeanPropertyWriter.getBeanPropertyDefinition().getInternalName());					
			}
		}
		if (lastEntityOwner != null && pathList.size() > 0) {
			String pathStr = this.mountPathFromStack(pathList);
//			AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(lastEntityOwner.getClass(),
//					pathStr);
			if (this.getHbSupport().isComponentOrRelationship(lastEntityOwner.getClass(), pathStr)) {
				AssociationAndComponentTrackInfo trackInfo = new AssociationAndComponentTrackInfo();
				trackInfo.setEntityOwner(lastEntityOwner);
				trackInfo.setEntityAndComponentPath(this.getHbSupport().getAssociationAndComponentOnPath(lastEntityOwner.getClass(), pathStr));
				return trackInfo;
			} else {
				return null;
			}
		} else {
			return null;			
		}
	}

	@Override
	public String getCurrentPathFromLastEntity() {
		List<String> pathList = new ArrayList<>();
		Object lastEntityOwner = null;
		for (PlayerBeanPropertyWriter jbHbBeanPropertyWriter : this.getPlayerBeanPropertyWriterStepStack()) {
			if (this.isPersistentClass(jbHbBeanPropertyWriter.getCurrOwner().getClass())) {
				lastEntityOwner = jbHbBeanPropertyWriter.getCurrOwner();
				pathList.clear();
				pathList.add(jbHbBeanPropertyWriter.getBeanPropertyDefinition().getInternalName());
			} else {
				pathList.add(jbHbBeanPropertyWriter.getBeanPropertyDefinition().getInternalName());
			}
		}
		String pathStr = null;
		if (lastEntityOwner != null && pathList.size() > 0) {
			pathStr = this.mountPathFromStack(pathList);
		}
		return pathStr;
	}

	@Override
	public boolean isCurrentPathFromLastEntityAnEntityRelationship() {
		List<String> pathList = new ArrayList<>();
		Object lastEntityOwner = null;
		for (PlayerBeanPropertyWriter jbHbBeanPropertyWriter : this.getPlayerBeanPropertyWriterStepStack()) {
			if (this.isPersistentClass(jbHbBeanPropertyWriter.getCurrOwner().getClass())) {
				lastEntityOwner = jbHbBeanPropertyWriter.getCurrOwner();
				pathList.clear();
				pathList.add(jbHbBeanPropertyWriter.getBeanPropertyDefinition().getInternalName());
			} else {
				pathList.add(jbHbBeanPropertyWriter.getBeanPropertyDefinition().getInternalName());
			}
		}
		String pathStr = null;
		if (lastEntityOwner != null && pathList.size() > 0) {
			pathStr = this.mountPathFromStack(pathList);
			if (this.getHbSupport().isOneToManyRelationship(lastEntityOwner.getClass(), pathStr)) {
				return true;
			}
//			AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(lastEntityOwner.getClass(),
//					pathStr);
//			AssociationAndComponentPath entityAndComponentPath = this.associationAndCompositiesMap.get(aacKey);
//			if (entityAndComponentPath != null) {
//				if (entityAndComponentPath.getRelEntity() != null) {
//					return true;
//				}
//			}
		}
		return false;
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
	
	@Override
	public IDirectRawWriterWrapper needDirectWrite(SignatureBean signature) {
		JsonSerializer<Object> jsonSerializer = null;
		try {
			jsonSerializer = this.config.getObjectMapper().getSerializerProvider()
					.findValueSerializer(signature.getClazz());
		} catch (JsonMappingException e) {
			throw new RuntimeException("This should not happen", e);
		}
		if (jsonSerializer instanceof BeanSerializer) {
			BeanSerializer beanSerializer = (BeanSerializer) jsonSerializer;
			Iterator<PropertyWriter> iterator = beanSerializer.properties();
			final Object objectToRawWrite = this.getBySignature(signature);
			while (iterator.hasNext()) {
				PropertyWriter propertyWriter = (PropertyWriter) iterator.next();
				if (propertyWriter instanceof PlayerBeanPropertyWriter) {
					PlayerBeanPropertyWriter playerBeanPropertyWriter = (PlayerBeanPropertyWriter) propertyWriter;
					if (playerBeanPropertyWriter.getBeanPropertyDefinition().getInternalName()
							.equals(signature.getPropertyName())) {
						final LazyProperty lazyPropertyAnn = playerBeanPropertyWriter
								.getAnnotation(LazyProperty.class);
						if (lazyPropertyAnn != null && lazyPropertyAnn.directRawWrite()) {
							return new IDirectRawWriterWrapper() {

								@Override
								public LazyProperty getLazyProperty() {
									return lazyPropertyAnn;
								}

								@Override
								public IDirectRawWriter getCallback() {
									return new IDirectRawWriter() {

										@Override
										public void write(OutputStream outputStream) throws IOException, SQLException {
											if (objectToRawWrite == null) {
												// nothing
											} else if (objectToRawWrite instanceof Blob) {
												Blob blob = (Blob) objectToRawWrite;
												byte[] buffer = new byte[lazyPropertyAnn.bufferSize()];
												int offset = 0;
												int latReadedCount = 0;
												InputStream inputStream = blob.getBinaryStream();
												do {
													latReadedCount = inputStream.read(buffer, offset, buffer.length);
													offset = offset + latReadedCount;
													if (latReadedCount > 0) {
														outputStream.write(buffer, 0, latReadedCount);
													}
												} while (latReadedCount > 0);
												outputStream.flush();

											} else if (objectToRawWrite instanceof Clob) {
												Clob clob = (Clob) objectToRawWrite;
												int latReadedCount = 0;
												Charset charset = Charset.forName(lazyPropertyAnn.charset());
												CharBuffer charBuffer = CharBuffer
														.allocate(lazyPropertyAnn.bufferSize());

												Reader r = clob.getCharacterStream();
												WritableByteChannel channel = Channels.newChannel(outputStream);
												do {
													latReadedCount = r.read(charBuffer);
													if (latReadedCount > 0) {
														channel.write(charset.encode(charBuffer));
													}
												} while (latReadedCount > 0);
												outputStream.flush();
											} else if (objectToRawWrite instanceof byte[]) {
												byte[] byteArr = (byte[]) objectToRawWrite;
												outputStream.write(byteArr, 0, byteArr.length);
												outputStream.flush();
											} else if (objectToRawWrite instanceof String) {
												String str = (String) objectToRawWrite;
												Charset charset = Charset.forName(lazyPropertyAnn.charset());
												WritableByteChannel channel = Channels.newChannel(outputStream);
												channel.write(charset.encode(str));
												outputStream.flush();
											} else {
												throw new RuntimeException(
														"Only Blob, Clob, byte[] or String supported by 'direct OutputStream write. Not supported type: "
																+ objectToRawWrite.getClass().getName());
											}
										}
									};
								}
							};
						}
					}
				}
			}
		} else {
			throw new RuntimeException("This should not happen");
		}
		return null;
	}

	private <O> void registerComponentOwnerPriv(O owner, String propertyName, Object owned) {
		if (owned == null) {
			throw new RuntimeException("propertyFunc can not returm null!");
		}
		IdentityRefKey identityRefKey = new IdentityRefKey(owned);
		Class<O> ownerClass;
		if (owner instanceof HibernateProxy) {
			ownerClass = (Class<O>) owner.getClass().getSuperclass();
		} else {
			ownerClass = (Class<O>) owner.getClass();
		}
		OwnerAndProperty ownerAndProperty = new OwnerAndProperty();
		ownerAndProperty.setOwner(owner);
		ownerAndProperty.setProperty(propertyName);
		this.registeredComponentOwnersTL.get().put(identityRefKey, ownerAndProperty);
	}
	
	/**
	 * Here you can NOT use nested properties.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <O> IPlayerManager registerComponentOwner(O owner, Function<O, ?> propertyFunc) {
		Object owned = propertyFunc.apply(owner);
		Class<O> ownerClass;
		if (owner instanceof HibernateProxy) {
			ownerClass = (Class<O>) owner.getClass().getSuperclass();
		} else {
			ownerClass = (Class<O>) owner.getClass();
		}
		String propertyName = ReflectionNamesDiscovery.fieldByGetMethod(propertyFunc, ownerClass);
		this.registerComponentOwnerPriv(owner, propertyName, owned);
		return this;
	}

	/**
	 * Here you can use nested properties.
	 */
	@Override
	public <O, T> IPlayerManager registerComponentOwner(Class<O> ownerClass, T targetOwned, Function<O, T> propertyFunc) {
		List<PathEntry> pathEntries = ReflectionNamesDiscovery.fieldByGetMethodEntries(propertyFunc, ownerClass);
		Object ownerInst = null;
		for (int i = 0; i < pathEntries.size(); i++) {
			PathEntry pathEntry = pathEntries.get(i);
			try {
				if (ownerInst == null) {
					ownerInst = pathEntry.getDirectOwnerType().newInstance();
				}
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("pathEntry.getDirectOwnerType().newInstance() fail for " + pathEntry.getDirectOwnerType());
			}
			Object ownedInst = null; 
			if (i < pathEntries.size() - 1) {
				try {
					ownedInst = pathEntry.getDirectFieldType().newInstance();
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException("pathEntry.getDirectOwnerType().newInstance() fail for " + pathEntry.getDirectOwnerType());
				}
			} else {
				ownedInst = targetOwned;
			}
			try {
				PropertyUtils.setProperty(ownerInst, pathEntry.getDirectFieldName(), ownedInst);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("could not set field " + pathEntry.getDirectFieldName() + " for " + pathEntry.getDirectOwnerType());
			}
			this.registerComponentOwnerPriv(ownerInst, pathEntry.getDirectFieldName(), ownedInst);
			ownerInst = ownedInst;
		}
		return this;
	}
	
	@Override
	public List<OwnerAndProperty> getRegisteredComponentOwnerList(Object instance) {
		ArrayList<OwnerAndProperty> resultList = new ArrayList<>();
		OwnerAndProperty currOwnerAndProperty = null;
		IdentityRefKey currIdentityRefKey = new IdentityRefKey(instance);
		do {
			currOwnerAndProperty = this.registeredComponentOwnersTL.get().get(currIdentityRefKey);				
			if (currOwnerAndProperty != null) {
				resultList.add(currOwnerAndProperty);
				currIdentityRefKey = new IdentityRefKey(currOwnerAndProperty.getOwner());
			} else {
				currIdentityRefKey = new IdentityRefKey(new Object());
			}
		} while (currOwnerAndProperty != null);
		Collections.reverse(resultList);
		return resultList;
	}	
	private HbSupport hbSupport;
	public HbSupport getHbSupport() {
		return this.hbSupport;
	}
	
	private HibernateVersion hibernateVersion;
	public HibernateVersion getHibernateVersion() {
		return this.hibernateVersion;
	}
	
//	@Override
//	public Stack<String> getCurrentCompositePathStack() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Object getCurrentCompositeOwner() {
//		// TODO Auto-generated method stub
//		return null;
//	}
}
/*gerando conflito*/