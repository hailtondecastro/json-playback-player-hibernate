package org.jsonplayback.player.hibernate;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.BagType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ListType;
import org.hibernate.type.SetType;
import org.hibernate.type.Type;
import org.jsonplayback.player.IJsHbChangeActionListener;
import org.jsonplayback.player.IJsHbFluentChangeListener;
import org.jsonplayback.player.IJsHbManager;
import org.jsonplayback.player.IJsHbReplayable;
import org.jsonplayback.player.JsHbChangeActionEventArgs;
import org.jsonplayback.player.util.ReflectionNamesDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

public class JsHbReplayable implements IJsHbReplayable {
	private static Logger logger = LoggerFactory.getLogger(JsHbReplayable.class);
	
	private static class ClassPropertyKey {
		private Class entityClass;
		private String propertyName;

		public ClassPropertyKey(Class entityClass, String propertyName) {
			super();
			this.entityClass = entityClass;
			this.propertyName = propertyName;
		}

		public Class getEntityClass() {
			return entityClass;
		}

		public void setEntityClass(Class entityClass) {
			this.entityClass = entityClass;
		}

		public String getPropertyName() {
			return propertyName;
		}

		public void setPropertyName(String propertyName) {
			this.propertyName = propertyName;
		}

		@Override
		public int hashCode() {
			return 37 * this.entityClass.hashCode() + propertyName.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ClassPropertyKey) {
				ClassPropertyKey classPropertyKeyObj = (ClassPropertyKey) obj;
				return classPropertyKeyObj == this || (this.entityClass.equals(classPropertyKeyObj.getEntityClass())
						&& this.propertyName.equals(classPropertyKeyObj.getPropertyName()));
			} else {
				return false;
			}
		}
	}

	private HashMap<ClassPropertyKey, List<IJsHbChangeActionListener>> listenersByPrpsMap = new HashMap<>();
	private HashMap<Class, List<IJsHbChangeActionListener>> listenersByClassMap = new HashMap<>();
	private ArrayList<IJsHbChangeActionListener> listenersList = new ArrayList<>();
	private JsHbPlayback playback = null;
	private boolean replayed = false;

	private IJsHbManager jsHbManager;
	
	public JsHbReplayable configJsHbManager(IJsHbManager jsHbManager) {
		this.jsHbManager = jsHbManager;
		return this;
	}

	public JsHbReplayable loadPlayback(JsHbPlayback playback) {
		this.playback = playback;
		return this;
	}

	@Override
	public IJsHbReplayable addChangeActionListener(IJsHbChangeActionListener changeActionListener) {
		if (changeActionListener == null) {
			throw new IllegalArgumentException("changeActionListener can not be null");
		}
		
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("addChangeActionListener({0})", changeActionListener.getName()));
		}
		this.listenersList.add(changeActionListener);
		return this;
	}

	@Override
	public <E> IJsHbReplayable addChangeActionListenerForClass(Class<E> entClass,
			IJsHbChangeActionListener changeActionListener) {
		if (entClass == null) {
			throw new IllegalArgumentException("entClass can not be null");
		}
		if (changeActionListener == null) {
			throw new IllegalArgumentException("changeActionListener can not be null");
		}
		
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("addChangeActionListener({0}, {1})",
					entClass, changeActionListener.getName()));
		}
		if (!this.listenersByClassMap.containsKey(entClass)) {
			this.listenersByClassMap.put(entClass, new ArrayList<>());
		}
		this.listenersByClassMap.get(entClass).add(changeActionListener);
		return this;
	}

	@Override
	public <E> IJsHbReplayable addChangeActionListenerForProperty(Class<E> entClass, Function<E, ?> propertyFunc,
			IJsHbChangeActionListener changeActionListener) {
		if (entClass == null) {
			throw new IllegalArgumentException("entClass can not be null");
		}
		if (propertyFunc == null) {
			throw new IllegalArgumentException("propertyFunc can not be null");
		}
		if (changeActionListener == null) {
			throw new IllegalArgumentException("changeActionListener can not be null");
		}
		
		ClassPropertyKey classPropertyKey = new ClassPropertyKey(entClass,
				ReflectionNamesDiscovery.fieldByGetMethod(propertyFunc, entClass));
		
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("addChangeActionListener({0}, {1}, {2})", entClass, classPropertyKey.getPropertyName(), changeActionListener.getName()));
		}
		if (!this.listenersByPrpsMap.containsKey(classPropertyKey)) {
			this.listenersByPrpsMap.put(classPropertyKey, new ArrayList<>());
		}
		this.listenersByPrpsMap.get(classPropertyKey).add(changeActionListener);
		return this;
	}
	
	private void preProcessPlayBack(JsHbPlayback playback, HashMap<Long, Object> creationRefMap) {
		Session ss = this.jsHbManager.getJsHbConfig().getSessionFactory().getCurrentSession();
		ObjectMapper objectMapper = this.jsHbManager.getJsHbConfig().getObjectMapper();
		Collection collection = null;
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("preProcessPlayBack(). playback before pre processing:\n{0}", playback));
		}
		for (JsHbPlaybackAction action : this.playback.getActions()) {
			action.jsHbPlaybackOwner = this.playback;
			Collection resolvedCollection = action.resolveColletion(objectMapper, this.jsHbManager, creationRefMap);
			switch (action.getActionType()) {
			case CREATE:
				if (action.getOwnerCreationId() == null) {
					throw new RuntimeException(MessageFormat.format("jsHbCreationId can not be null\naction:\n{0}", action));
				}
				if (action.getOwnerSignatureStr() != null) {
					throw new RuntimeException(MessageFormat.format("originalSignatureStr can not be null\naction:\n{0}", action));
				}
				Object ownerValue = action.resolveOwnerValue(this.jsHbManager, creationRefMap);
				
				ClassMetadata classMetadata = this.jsHbManager.getJsHbConfig().getSessionFactory().getClassMetadata(action.resolveOwnerJavaClass(jsHbManager, creationRefMap));
				if (classMetadata != null) {
					PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(action.resolveOwnerJavaClass(jsHbManager, creationRefMap));
					for (int i = 0; i < propertyDescriptors.length; i++) {
						PropertyDescriptor propertyDescriptorItem = propertyDescriptors[i];
						if (!("class".equals(propertyDescriptorItem.getName()))) {
							Type prpType = classMetadata.getPropertyType(propertyDescriptorItem.getName());
							Collection resultColl = null;
							if (prpType instanceof CollectionType) {
								if (prpType instanceof SetType) {
									resultColl = new LinkedHashSet<>();
								} else if (prpType instanceof ListType) {
									throw new RuntimeException("Not supported. prpType: " + prpType);
								} else if (prpType instanceof BagType) {
									throw new RuntimeException("Not supported. prpType: " + prpType);
								} else {
									throw new RuntimeException("This should not happen. prpType: " + prpType);
								}
								try {
									PropertyUtils.setProperty(ownerValue, propertyDescriptorItem.getName(), collection);
								} catch (Exception e) {
									throw new RuntimeException("This should not happen. prpType: " + prpType, e);
								}
							} else {
								// non one-to-many
							}
						}
					}
				}				
				
				break;
			case SAVE:
				if (action.getOwnerCreationId() == null) {
					throw new RuntimeException(MessageFormat.format("ownerCreationId can not be null\naction:\n{0}", action));
				}
				if (action.getOwnerSignatureStr() != null) {
					throw new RuntimeException(MessageFormat.format("ownerSignatureStr can not be not null\naction:\n{0}", action));
				}
				
				break;
			case DELETE:
				if (action.getOwnerCreationId() != null) {
					throw new RuntimeException(MessageFormat.format("ownerCreationRefId can not be not null\naction:\n{0}", action));
				}
				if (action.getOwnerSignatureStr() == null) {
					throw new RuntimeException(MessageFormat.format("ownerSignatureStr can not be null\naction:\n{0}", action));
				}
				//nada
				break;
			case COLLECTION_ADD:
				if (action.getFieldName() == null) {
					throw new RuntimeException(MessageFormat.format("fieldName can not be not null\naction:\n{0}", action));
				}
				collection = (Collection) resolvedCollection;
				
				break;
			case COLLECTION_REMOVE:
				if (action.getFieldName() == null) {
					throw new RuntimeException(MessageFormat.format("fieldName can not be not null\naction:\n{0}", action));
				}
				collection = (Collection) resolvedCollection;
				
				break;
			case SET_FIELD:
				if (action.getFieldName() == null) {
					throw new RuntimeException(MessageFormat.format("fieldName can not be not null\naction:\n{0}", action));
				}
				
				break;
			default:
				throw new RuntimeException("This should not happen");
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("preProcessPlayBack(). playback after pre processing:\n{0}", playback));
		}
	}
	
	@Override
	public void replay() {
		if (this.replayed) {
			throw new RuntimeException("Already executed");
		}
		this.replayed = true;
		
		List<IJsHbChangeActionListener> actionListenersList = null;
		HashMap<Long, Object> creationRefMap = new HashMap<>();
		Session ss = this.jsHbManager.getJsHbConfig().getSessionFactory().getCurrentSession();
		ObjectMapper objectMapper = this.jsHbManager.getJsHbConfig().getObjectMapper();
		
		this.preProcessPlayBack(this.playback, creationRefMap);
		
		for (JsHbPlaybackAction action : this.playback.getActions()) {
			// before events
			Object resolvedOwnerValue = action.resolveOwnerValue(this.jsHbManager, creationRefMap);
			String resolvedJavaPropertyName = action.resolveJavaPropertyName(objectMapper, this.jsHbManager, creationRefMap);
			@SuppressWarnings("rawtypes")
			Collection resolvedCollection = action.resolveColletion(objectMapper, this.jsHbManager, creationRefMap);
			Object resolvedSettedValue = action.resolveSettedValue(objectMapper, this.jsHbManager, creationRefMap);
			
			JsHbChangeActionEventArgs<Object> actionEventArgs = new JsHbChangeActionEventArgs<Object>(
					resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection,
					action.getActionType());
			
			if (action.getFieldName() != null) {
				ClassPropertyKey classPropertyKey = new ClassPropertyKey(action.resolveOwnerJavaClass(this.jsHbManager, creationRefMap),
						resolvedJavaPropertyName);
				
				actionListenersList = this.listenersByClassMap.get(action.resolveOwnerJavaClass(this.jsHbManager, creationRefMap));
				if (actionListenersList != null) {
					for (IJsHbChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onBeforeChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, action.getActionType()));
						}
						actionListenerItem.onBeforeChange(actionEventArgs);
					}
				}
				
				actionListenersList = this.listenersByPrpsMap.get(classPropertyKey);
				if (actionListenersList != null) {
					for (IJsHbChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onBeforeChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, action.getActionType()));
						}
						actionListenerItem.onBeforeChange(actionEventArgs);
					}
				}
				
				actionListenersList = this.listenersList;
				if (actionListenersList != null) {
					for (IJsHbChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onBeforeChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, action.getActionType()));
						}
						actionListenerItem.onBeforeChange(actionEventArgs);
					}
				}
			} else {
				actionListenersList = this.listenersByClassMap.get(action.resolveOwnerJavaClass(this.jsHbManager, creationRefMap));
				if (actionListenersList != null) {
					for (IJsHbChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onBeforeChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, action.getActionType()));
						}
						actionListenerItem.onBeforeChange(actionEventArgs);
					}
				}
				
				actionListenersList = this.listenersList;
				if (actionListenersList != null) {
					for (IJsHbChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onBeforeChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, action.getActionType()));
						}
						actionListenerItem.onBeforeChange(actionEventArgs);
					}
				}
			}

			@SuppressWarnings("rawtypes")
			Collection collection = null;
			
			switch (action.getActionType()) {
			case CREATE:
				//nada: feito no preprocessamento		
				break;
			case SAVE:
				ss.save(resolvedOwnerValue);
				break;
			case DELETE:
				ss.delete(resolvedOwnerValue);
				break;
			case COLLECTION_ADD:
				collection = (Collection) resolvedCollection;
				collection.add(resolvedSettedValue);
				
				break;
			case COLLECTION_REMOVE:
				collection = (Collection) resolvedCollection;
				collection.remove(resolvedSettedValue);
				
				break;
			case SET_FIELD:
				try {
					SettableBeanProperty settableBeanProperty = action.resolveBeanDeserializer(objectMapper, this.jsHbManager, creationRefMap).findProperty(action.getFieldName()); 
					settableBeanProperty.set(resolvedOwnerValue, resolvedSettedValue);
				} catch (IOException e) {
					throw new RuntimeException(MessageFormat.format("This should not happen.\naction:\n{0}", action));
				}
				break;
			default:
				throw new RuntimeException("This should not happen");
			}

			// after events
			if (action.getFieldName() != null) {
				ClassPropertyKey classPropertyKey = new ClassPropertyKey(action.resolveOwnerJavaClass(this.jsHbManager, creationRefMap),
						resolvedJavaPropertyName);

				actionListenersList = this.listenersByPrpsMap.get(classPropertyKey);
				if (actionListenersList != null) {
					for (IJsHbChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onAfterChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, action.getActionType()));
						}
						actionListenerItem.onAfterChange(actionEventArgs);
					}
				}

				actionListenersList = this.listenersByClassMap.get(action.resolveOwnerJavaClass(this.jsHbManager, creationRefMap));
				if (actionListenersList != null) {
					for (IJsHbChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onAfterChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, action.getActionType()));
						}
						actionListenerItem.onAfterChange(actionEventArgs);
					}
				}
				
				actionListenersList = this.listenersList;
				if (actionListenersList != null) {
					for (IJsHbChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onAfterChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, action.getActionType()));
						}
						actionListenerItem.onAfterChange(actionEventArgs);
					}
				}
			} else {
				actionListenersList = this.listenersByClassMap.get(action.resolveOwnerJavaClass(this.jsHbManager, creationRefMap));
				if (actionListenersList != null) {
					for (IJsHbChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onAfterChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, action.getActionType()));
						}
						actionListenerItem.onAfterChange(actionEventArgs);
					}
				}

				actionListenersList = this.listenersList;
				if (actionListenersList != null) {
					for (IJsHbChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onAfterChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, action.getActionType()));
						}
						actionListenerItem.onAfterChange(actionEventArgs);
					}
				}
			}
		}
	}

	@Override
	public <E> IJsHbFluentChangeListener<E> fluentChangeListener(Class<E> targetClass) {
		// TODO Auto-generated method stub
		return new JsHbFluentChangeListenerDefault<E>(this, targetClass);
	}
	
	public IJsHbFluentChangeListener<?> fluentChangeListener() {
		return new JsHbFluentChangeListenerDefault<>(this, null);
	}
}
/*gerando conflito*/