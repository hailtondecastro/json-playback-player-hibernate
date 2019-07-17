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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.codec.binary.Base64;
import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.jsonplayback.player.IDirectRawWriter;
import org.jsonplayback.player.IDirectRawWriterWrapper;
import org.jsonplayback.player.IConfig;
import org.jsonplayback.player.IPlayerManager;
import org.jsonplayback.player.IReplayable;
import org.jsonplayback.player.IdentityRefKey;
import org.jsonplayback.player.PlayerSnapshot;
import org.jsonplayback.player.Tape;
import org.jsonplayback.player.PlayerMetadatas;
import org.jsonplayback.player.LazyProperty;
import org.jsonplayback.player.SignatureBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

public class JsHbPlayerManager implements IPlayerManagerImplementor {
	private static Logger logger = LoggerFactory.getLogger(JsHbPlayerManager.class);
	
	private IConfig jsHbConfig = new JsHbConfig();
	ThreadLocal<Long> currIdTL = new ThreadLocal<Long>();
	ThreadLocal<Map<Long, Object>> objectByIdMapTL = new ThreadLocal<>();
	ThreadLocal<Map<IdentityRefKey, Long>> idByObjectMapTL = new ThreadLocal<>();
	ThreadLocal<Map<IdentityRefKey, PlayerMetadatas>> metadatasCacheMapTL = new ThreadLocal<>();
	ThreadLocal<IConfig> temporaryConfigurationTL = new ThreadLocal<IConfig>();
	ThreadLocal<Stack<JsHbBeanPropertyWriter>> jsHbBeanPropertyWriterStepStackTL = new ThreadLocal<Stack<JsHbBeanPropertyWriter>>();
	ThreadLocal<Stack<JsHbJsonSerializer>> JsHbJsonSerializerStepStackTL = new ThreadLocal<Stack<JsHbJsonSerializer>>(); 
	ThreadLocal<Stack<PlayerMetadatas>> jsHbBackendMetadatasWritingStackTL = new ThreadLocal<Stack<PlayerMetadatas>>();

//	ThreadLocal<Stack<String>> currentCompositePathStackTL = new ThreadLocal<>();
//	ThreadLocal<Object> currentCompositeOwner = new ThreadLocal<>();

	@Override
	public <T> PlayerSnapshot<T> createPlayerSnapshot(T result) {
		if (logger.isTraceEnabled()) {
			logger.trace(
					MessageFormat.format("createResultEntity for {0}", result != null ? result.getClass() : "null"));
		}
		return new PlayerSnapshot<T>(result).configJsHbManager(this);
	}

	@Override
	public JsHbPlayerManager configure(IConfig jsHbConfig) {
		if (jsHbConfig == null) {
			throw new IllegalArgumentException("jsHbConfig can not be null");
		}
		if (logger.isDebugEnabled()) {
			logger.debug(MessageFormat.format("configure.  jsHbConfig:\n{0}", jsHbConfig));
		}
		this.jsHbConfig = jsHbConfig;
		return this;
	}

	private Map<AssociationAndComponentPathKey, AssociationAndComponentPath> associationAndCompositiesMap = new HashMap<>();
	private Set<Class<?>> compositiesSet = new HashSet<>();

	private boolean initialed = false;

	@Override
	public IPlayerManager init() {
		if (logger.isDebugEnabled()) {
			logger.debug("init()");
		}
		this.associationAndCompositiesMap.clear();
		this.collectAssociationAndCompositiesMap();
		this.initialed = true;
		return this;
	}

	private void collectAssociationAndCompositiesMap() {
		if (logger.isDebugEnabled()) {
			logger.debug("collectAssociationAndCompositiesMap()");
		}
		for (String entityName : this.jsHbConfig.getSessionFactory().getAllClassMetadata().keySet()) {
			ClassMetadata classMetadata = this.jsHbConfig.getSessionFactory().getClassMetadata(entityName);
			
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

					AssociationAndComponentPath relEacPath = new AssociationAndComponentPath();
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

					AssociationAndComponentPath relEacPath = new AssociationAndComponentPath();
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
			AssociationAndComponentPath aacPath = this.associationAndCompositiesMap.get(key);
			if (aacPath.getCompType() != null) {
				Class<?> compositeClass = this.associationAndCompositiesMap.get(key).getCompType().getReturnedClass();
			this.compositiesSet.add(compositeClass);
	}
	}
	}

	private String mountPathFromStack(Collection<String> pathStack) {
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
			AssociationAndComponentPath aacPath = new AssociationAndComponentPath();
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
						AssociationAndComponentPath relEacPathFromRoot = new AssociationAndComponentPath();
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
						AssociationAndComponentPath relEacPathFromRoot = new AssociationAndComponentPath();
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
			//maybe it is deprecated!?
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
		if (this.jsHbConfig.getSignatureCrypto() != null) {
			result = this.jsHbConfig.getSignatureCrypto().encrypt(result);
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
		if (this.jsHbConfig.getSignatureCrypto() != null) {
			resultStr = this.jsHbConfig.getSignatureCrypto().encrypt(resultStr);
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

		if (this.jsHbConfig.getSignatureCrypto() != null) {
			decryptedSignatureStr = this.jsHbConfig.getSignatureCrypto().decrypt(signatureStr);
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
	public SignatureBean generateLazySignature(PersistentCollection persistentCollection) {
		Pattern rxCollectionRole = Pattern
				.compile("^" + Pattern.quote(persistentCollection.getOwner().getClass().getName()) + "\\.(.*)");
		// SessionImplementor ssImplementor = null;
		// Class ownerClass = persistentCollection.getOwner().getClass();
		//
		// if (HibernateProxy.class.isAssignableFrom(ownerClass)) {
		// ownerClass = ownerClass.getSuperclass();
		// }

		Matcher matcher = rxCollectionRole.matcher(persistentCollection.getRole());
		if (!matcher.find()) {
			throw new RuntimeException(MessageFormat.format(
					"Collection role nao se encaixa no padrao esperado: ''{0}''", persistentCollection.getRole()));
		}

		Class<?> ownerClass = persistentCollection.getOwner().getClass();
		String fieldName = matcher.group(1);
		Object ownerValue = persistentCollection.getOwner();
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
		ClassMetadata classMetadata = this.jsHbConfig.getSessionFactory().getClassMetadata(ownerClass);
		
		Type prpType = null;
		if (classMetadata != null) {
			prpType = classMetadata.getPropertyType(fieldName);
		} else {
			AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(ownerClass, fieldName);
			logger.warn(
					"########## NAO ESTOU CERTO SOBRE ISSO:\nCompositeType componentType = this.compositiesMap.get(key).getCompType();");
			CompositeType componentType = this.associationAndCompositiesMap.get(aacKey).getCompType();
			if (componentType == null) {
				throw new RuntimeException("Unespected type " + ownerClass + "->" + fieldName + ": " + prpType);
			}
			
			int prpIndex = -1;
			String[] cpsTpArrPrps = componentType.getPropertyNames();
			for (int i = 0; i < cpsTpArrPrps.length; i++) {
				if (fieldName.equals(cpsTpArrPrps[i])) {
					prpIndex = i;
					break;
				}
			}			
			if (prpIndex == -1) {
				throw new RuntimeException("fieldName does not exists: " + ownerClass + "->" + fieldName);
			}
			
			prpType = componentType.getSubtypes()[prpIndex];
		}
		
		if (!(prpType instanceof AssociationType)) {
			throw new RuntimeException("Unespected type " + ownerClass + "->" + fieldName + ": " + prpType);
		}

		SignatureBean signatureBean = null;
		// AssociationType assType = (AssociationType)
		// classMetadata.getPropertyType(fieldName);
		AssociationType assType = (AssociationType) prpType;
		Object idValue = null;
		if (assType instanceof CollectionType) {
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
//					(SessionImplementor) this.jsHbConfig.getSessionFactory().getCurrentSession());
			signatureBean.setClazz(ownerClass);
			signatureBean.setIsColl(true);
			signatureBean.setPropertyName(fieldName);
		} else {
			if (fieldValue instanceof HibernateProxy) {
				signatureBean = this.generateLazySignature((HibernateProxy) fieldValue);
			} else {
				signatureBean = this.generateSignature(fieldValue);
		}

			// This will cause two signatures for the same instance!?!?!?!?
			// signatureBean.setIsAssoc(true);

//			classMetadata = this.jsHbConfig.getSessionFactory().getClassMetadata(assType.getReturnedClass());
//			idValue = classMetadata.getIdentifier(fieldValue,
//					(SessionImplementor) this.jsHbConfig.getSessionFactory().getCurrentSession());
//			signatureBean.setClazz(assType.getReturnedClass());
		}
		// signatureBean.setEntityName(classMetadata.getEntityName());

//		JsHbStatment jsHbStatment = new JsHbStatment();

//		Type hbIdType = classMetadata.getIdentifierType();
//		try {
//			hbIdType.nullSafeSet(jsHbStatment, idValue, 0,
//					(SessionImplementor) this.jsHbConfig.getSessionFactory().getCurrentSession());
//		} catch (HibernateException e) {
//			throw new RuntimeException("This should not happen", e);
//		} catch (SQLException e) {
//			throw new RuntimeException("This should not happen", e);
//		}
//		signatureBean.setRawKeyValues(jsHbStatment.getInternalValues());
		
		if (logger.isTraceEnabled()) {
			logger.trace(
					MessageFormat.format("generateLazySignatureForRelashionship().signatureBean:\n{0}", signatureBean));
		}

		return signatureBean;
	}

	@Override
	public SignatureBean generateLazySignatureForJsHbLazyProperty(Class<?> ownerClass, String fieldName,
			Object ownerValue, Object fieldValue) {
		if (fieldValue instanceof byte[] || fieldValue instanceof Blob || fieldValue instanceof String
				|| fieldValue instanceof Clob) {
			// nothing
		} else {
			throw new RuntimeException("fieldValue type does not support JsHbLazyProperty: " + fieldValue.getClass());
		}
		ClassMetadata classMetadata = this.jsHbConfig.getSessionFactory().getClassMetadata(ownerClass);

		Type prpType = null;
		if (classMetadata != null) {
			prpType = classMetadata.getPropertyType(fieldName);
		} else {
			AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(ownerClass, fieldName);
			CompositeType componentType = this.associationAndCompositiesMap.get(aacKey).getCompType();
			if (componentType == null) {
				throw new RuntimeException("Unespected type " + ownerClass + "->" + fieldName + ": " + prpType);
			}

			int prpIndex = -1;
			String[] cpsTpArrPrps = componentType.getPropertyNames();
			for (int i = 0; i < cpsTpArrPrps.length; i++) {
				if (fieldName.equals(cpsTpArrPrps[i])) {
					prpIndex = i;
					break;
				}
			}
			if (prpIndex == -1) {
				throw new RuntimeException("fieldName does not exists: " + ownerClass + "->" + fieldName);
			}

			prpType = componentType.getSubtypes()[prpIndex];
		}

		if (prpType instanceof AssociationType) {
			throw new RuntimeException("Unespected type " + ownerClass + "->" + fieldName + ": " + prpType);
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
		JsHbStatment jsHbStatment = new JsHbStatment();

		ClassMetadata classMetadata = this.jsHbConfig.getSessionFactory().getClassMetadata(entityClass);

		@SuppressWarnings("deprecation")
		Object idValue = classMetadata.getIdentifier(hibernateProxy,
				(SessionImplementor) this.jsHbConfig.getSessionFactory().getCurrentSession());
		Type hbIdType = classMetadata.getIdentifierType();
		try {
			hbIdType.nullSafeSet(jsHbStatment, idValue, 0,
					(SessionImplementor) this.jsHbConfig.getSessionFactory().getCurrentSession());
		} catch (HibernateException e) {
			throw new RuntimeException("This should not happen", e);
		} catch (SQLException e) {
			throw new RuntimeException("This should not happen", e);
		}
		signatureBean.setRawKeyValues(jsHbStatment.getInternalValues());
		
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
		JsHbStatment jsHbStatment = new JsHbStatment();

		ClassMetadata classMetadata = this.jsHbConfig.getSessionFactory().getClassMetadata(entityClass);

		@SuppressWarnings("deprecation")
		Object idValue = classMetadata.getIdentifier(nonHibernateProxy,
				(SessionImplementor) this.jsHbConfig.getSessionFactory().getCurrentSession());
		Type hbIdType = classMetadata.getIdentifierType();
		try {
			hbIdType.nullSafeSet(jsHbStatment, idValue, 0,
					(SessionImplementor) this.jsHbConfig.getSessionFactory().getCurrentSession());
		} catch (HibernateException e) {
			throw new RuntimeException("This should not happen", e);
		} catch (SQLException e) {
			throw new RuntimeException("This should not happen", e);
		}
		signatureBean.setRawKeyValues(jsHbStatment.getInternalValues());
		
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
		
		ClassMetadata classMetadata = this.jsHbConfig.getSessionFactory().getClassMetadata(signature.getClazz());

		Type hbIdType = classMetadata.getIdentifierType();

		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("getBySignature(). Hibernate id Type: ''{0}''", hbIdType));
		}
		
//		Serializable idValue = (Serializable) hbIdType.resolve(signature.getRawKeyValues(),
//				(SessionImplementor) this.jsHbConfig.getSessionFactory().getCurrentSession(), null);
		Serializable idValue = null;
		
		JsHbResultSet jsHbResultSet = new JsHbResultSet(signature.getRawKeyValues());
		try {
			idValue = (Serializable) hbIdType.nullSafeGet(jsHbResultSet, jsHbResultSet.getColumnNames(),
					(SessionImplementor) this.jsHbConfig.getSessionFactory().getCurrentSession(), null);
		} catch (HibernateException e) {
			throw new RuntimeException("This should not happen. prpType: ");
		} catch (SQLException e) {
			throw new RuntimeException("This should not happen. prpType: ");
		}
		
		if (idValue.getClass().isArray()) {
			if (((Object[])idValue).length == 1) {
				idValue = (Serializable) ((Object[])idValue)[0];
			}
		}
		Object owner = this.jsHbConfig.getSessionFactory().getCurrentSession().get(signature.getClazz(), idValue);

		Object result = owner;

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
//					this.jsHbConfig.getSessionFactory().getCurrentSession().getEntityMode());
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
		return this.jsHbConfig.getNeverSignedClasses().contains(clazz);
	}

//	@SuppressWarnings("rawtypes")
//	@Override
//	public boolean isPersistentClassOrComponent(Class clazz) {
//		return this.isComponent(clazz)
//				|| this.jsHbConfig.getSessionFactory().getClassMetadata(clazz) != null;
//	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean isPersistentClass(Class clazz) {
		return this.jsHbConfig.getSessionFactory().getClassMetadata(clazz) != null;
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
		Object idValue = this.jsHbConfig.getSessionFactory().getClassMetadata(entityClass).getIdentifier(object,
				(SessionImplementor) this.jsHbConfig.getSessionFactory().getCurrentSession());
		return idValue;
	}
	
	@Override
	public String getPlayerObjectIdName(Class clazz) {

		return this.jsHbConfig.getSessionFactory().getClassMetadata(clazz).getIdentifierPropertyName();
	}

	@Override
	public IConfig getJsHbConfig() {
		if (this.temporaryConfigurationTL.get() != null) {
			return this.temporaryConfigurationTL.get();
		} else {
			return this.jsHbConfig;
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
		this.currIdTL.set(0L);
		this.objectByIdMapTL.set(new HashMap<Long, Object>());
		this.idByObjectMapTL.set(new HashMap<IdentityRefKey, Long>());
		this.metadatasCacheMapTL.set(new HashMap<>());
		this.jsHbBeanPropertyWriterStepStackTL.set(new Stack<JsHbBeanPropertyWriter>());
		this.JsHbJsonSerializerStepStackTL.set(new Stack<JsHbJsonSerializer>());
		this.jsHbBackendMetadatasWritingStackTL.set(new Stack<PlayerMetadatas>());
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
		this.jsHbBeanPropertyWriterStepStackTL.set(null);
		this.JsHbJsonSerializerStepStackTL.set(null);
		this.jsHbBackendMetadatasWritingStackTL.set(null);
//		this.currentCompositeOwner.set(null);
//		this.currentCompositePathStackTL.set(null);
		
		this.temporaryConfigurationTL.set(null);
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
	public Stack<JsHbBeanPropertyWriter> getJsHbBeanPropertyWriterStepStack() {
		return this.jsHbBeanPropertyWriterStepStackTL.get();
	}
	
	@Override
	public Stack<JsHbJsonSerializer> getJsHbJsonSerializerStepStack() {
		return this.JsHbJsonSerializerStepStackTL.get();
	}

	@Override
	public Stack<PlayerMetadatas> getJsHbBackendMetadatasWritingStack() {
		return jsHbBackendMetadatasWritingStackTL.get();
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
		if (clazz == null) {
			throw new IllegalArgumentException("clazz can not be null");
		}
		if (fieldName == null || fieldName.trim().isEmpty()) {
			throw new IllegalArgumentException("fieldName can not be null");
		}
		
		ClassMetadata classMetadata = this.jsHbConfig.getSessionFactory().getClassMetadata(clazz);
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
			resultBool =  false;
		} else {
			if (prpType instanceof AssociationType) {
				resultBool =  true;
			} else {
				resultBool =  false;
			}
		}
		
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("isRelationship(). clazz: ''{0}''; fieldName: ''{1}''. return: ", clazz,
					fieldName, resultBool));
		}
		
		return resultBool;
	}

	@Override
	public boolean isComponent(Class<?> componentClass) {
		return this.compositiesSet.contains(componentClass);
//		if (clazz == null) {
//			throw new IllegalArgumentException("clazz can not be null");
//		}
//		if (fieldName == null || fieldName.trim().isEmpty()) {
//			throw new IllegalArgumentException("fieldName can not be null");
//		}
//		ClassMetadata classMetadata = this.jsHbConfig.getSessionFactory().getClassMetadata(clazz);
//		CompositeType compositeType = null;
//		if (classMetadata == null) {
//			HbComponentTypeEntry componentTypeEntry = new HbComponentTypeEntry(clazz, fieldName);
//			compositeType = this.compositiesMap.get(componentTypeEntry);
//			if (compositeType == null) {
//				throw new RuntimeException("Class is not mapped and is not a know CompositeType: " + clazz + ". Does this exception makes any sense?!");
//			}
//		}
//		
//		Type prpType = null;
//		if (classMetadata != null) {
//			for (int i = 0; i < classMetadata.getPropertyNames().length; i++) {
//				String prpNameItem = classMetadata.getPropertyNames()[i];
//				if (prpNameItem.equals(fieldName)) {
//					prpType = classMetadata.getPropertyTypes()[i];
//					break;
//				}
//			}			
//		} else {
//			for (int i = 0; i < compositeType.getPropertyNames().length; i++) {
//				String prpNameItem = compositeType.getPropertyNames()[i];
//				if (prpNameItem.equals(fieldName)) {
//					prpType = compositeType.getSubtypes()[i];
//					break;
//				}
//			}
//		}
//		
//		boolean resultBool = false;
//		if (prpType == null) {
//			resultBool =  false;
//		} else {
//			if (prpType instanceof ComponentType) {
//				resultBool =  true;
//			} else {
//				resultBool =  false;
//			}
//		}
//		
//		if (logger.isTraceEnabled()) {
//			logger.trace(
//					MessageFormat.format("isComponent(). clazz: ''{0}''; fieldName: ''{1}''. return: {2}", clazz, fieldName, resultBool));
//		}
//		
//		return resultBool;
	}

	@Override
	public IPlayerManager overwriteConfigurationTemporarily(IConfig newConfig) {
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("overwriteConfigurationTemporarily(). newConfig:\n {0}'", newConfig));
		}
		this.temporaryConfigurationTL.set(newConfig);
		return this;
	}
	
	@Override
	public IPlayerManager cloneWithNewConfiguration(IConfig newConfig) {
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("cloneWithNewConfiguration(). newConfig:\n {0}'", newConfig));
		}
		IPlayerManager jsHbManagerCloned = new JsHbPlayerManager();
		jsHbManagerCloned = jsHbManagerCloned.configure(newConfig);
		return jsHbManagerCloned;
	}

	@Override
	public IReplayable prepareReplayable(Tape tape) {
//		throw new RuntimeException("");
//		this.temporaryConfigurationTL.set(newConfig);
//		return null;
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("prepareReplayable(). tape:\n {0}'", tape));
		}
		return new JsHbReplayable().configJsHbManager(this).loadPlayback(tape);
	}

	@Override
	public AssociationAndComponentTrackInfo getCurrentAssociationAndComponentTrackInfo() {
		List<String> pathList = new ArrayList<>();
		Object lastEntityOwner = null;
		Stack<JsHbJsonSerializer> serStepTackLocal = new Stack<>();
		serStepTackLocal.addAll(this.JsHbJsonSerializerStepStackTL.get());
		// ignoring last one
		serStepTackLocal.pop();
		for (JsHbJsonSerializer jsHbJsonSerializer : serStepTackLocal) {
			Object currBean = jsHbJsonSerializer.getCurrSerializationBean();
			if (currBean instanceof PlayerMetadatas) {
				currBean = ((PlayerMetadatas) currBean).getOriginalPlayerObjectIdOwner();
			}
			if (jsHbJsonSerializer.getCurrSerializationBean() != null && this.isPersistentClass(currBean.getClass())) {
				lastEntityOwner = currBean;
			}
		}

		for (JsHbBeanPropertyWriter jbHbBeanPropertyWriter : this.getJsHbBeanPropertyWriterStepStack()) {
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
			AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(lastEntityOwner.getClass(),
					pathStr);
			if (this.associationAndCompositiesMap.containsKey(aacKey)) {
				AssociationAndComponentTrackInfo trackInfo = new AssociationAndComponentTrackInfo();
				trackInfo.setEntityOwner(lastEntityOwner);
				trackInfo.setEntityAndComponentPath(this.associationAndCompositiesMap.get(aacKey));
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
		for (JsHbBeanPropertyWriter jbHbBeanPropertyWriter : this.getJsHbBeanPropertyWriterStepStack()) {
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
		for (JsHbBeanPropertyWriter jbHbBeanPropertyWriter : this.getJsHbBeanPropertyWriterStepStack()) {
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
			AssociationAndComponentPathKey aacKey = new AssociationAndComponentPathKey(lastEntityOwner.getClass(),
					pathStr);
			AssociationAndComponentPath entityAndComponentPath = this.associationAndCompositiesMap.get(aacKey);
			if (entityAndComponentPath != null) {
				if (entityAndComponentPath.getRelEntity() != null) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public IDirectRawWriterWrapper needDirectWrite(SignatureBean signature) {
		JsonSerializer<Object> jsonSerializer = null;
		try {
			jsonSerializer = this.jsHbConfig.getObjectMapper().getSerializerProvider()
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
				if (propertyWriter instanceof JsHbBeanPropertyWriter) {
					JsHbBeanPropertyWriter jsHbBeanPropertyWriter = (JsHbBeanPropertyWriter) propertyWriter;
					if (jsHbBeanPropertyWriter.getBeanPropertyDefinition().getInternalName()
							.equals(signature.getPropertyName())) {
						final LazyProperty jsHbLazyPropertyAnn = jsHbBeanPropertyWriter
								.getAnnotation(LazyProperty.class);
						if (jsHbLazyPropertyAnn != null && jsHbLazyPropertyAnn.directRawWrite()) {
							return new IDirectRawWriterWrapper() {

								@Override
								public LazyProperty getJsHbLazyProperty() {
									return jsHbLazyPropertyAnn;
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
												byte[] buffer = new byte[jsHbLazyPropertyAnn.bufferSize()];
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
												Charset charset = Charset.forName(jsHbLazyPropertyAnn.charset());
												CharBuffer charBuffer = CharBuffer
														.allocate(jsHbLazyPropertyAnn.bufferSize());

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
												Charset charset = Charset.forName(jsHbLazyPropertyAnn.charset());
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