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
import org.jsonplayback.player.ChangeActionEventArgs;
import org.jsonplayback.player.IChangeActionListener;
import org.jsonplayback.player.IFluentChangeListener;
import org.jsonplayback.player.IPlayerManager;
import org.jsonplayback.player.IReplayable;
import org.jsonplayback.player.Tape;
import org.jsonplayback.player.TapeAction;
import org.jsonplayback.player.util.ReflectionNamesDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

public class JsHbReplayable implements IReplayable {
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

	private HashMap<ClassPropertyKey, List<IChangeActionListener>> listenersByPrpsMap = new HashMap<>();
	private HashMap<Class, List<IChangeActionListener>> listenersByClassMap = new HashMap<>();
	private ArrayList<IChangeActionListener> listenersList = new ArrayList<>();
	private Tape tape = null;
	private boolean replayed = false;

	private IPlayerManager jsHbManager;
	
	public JsHbReplayable configJsHbManager(IPlayerManager jsHbManager) {
		this.jsHbManager = jsHbManager;
		return this;
	}

	public JsHbReplayable loadPlayback(Tape tape) {
		this.tape = tape;
		return this;
	}

	@Override
	public IReplayable addChangeActionListener(IChangeActionListener changeActionListener) {
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
	public <E> IReplayable addChangeActionListenerForClass(Class<E> entClass,
			IChangeActionListener changeActionListener) {
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
	public <E> IReplayable addChangeActionListenerForProperty(Class<E> entClass, Function<E, ?> propertyFunc,
			IChangeActionListener changeActionListener) {
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
	
	private void preProcessPlayBack(Tape tape, HashMap<Long, Object> creationRefMap) {
		Session ss = this.jsHbManager.getJsHbConfig().getSessionFactory().getCurrentSession();
		ObjectMapper objectMapper = this.jsHbManager.getJsHbConfig().getObjectMapper();
		Collection collection = null;
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("preProcessPlayBack(). tape before pre processing:\n{0}", tape));
		}
		for (TapeAction actionItem : this.tape.getActions()) {
			JsHbTapeAction jsHbAction = (JsHbTapeAction) actionItem;
			jsHbAction.setTapeOwner(this.tape);
			Collection resolvedCollection = jsHbAction.resolveColletion(objectMapper, this.jsHbManager, creationRefMap);
			switch (jsHbAction.getActionType()) {
			case CREATE:
				if (jsHbAction.getOwnerCreationId() == null) {
					throw new RuntimeException(MessageFormat.format("jsHbCreationId can not be null\naction:\n{0}", jsHbAction));
				}
				if (jsHbAction.getOwnerSignatureStr() != null) {
					throw new RuntimeException(MessageFormat.format("originalSignatureStr can not be null\naction:\n{0}", jsHbAction));
				}
				Object ownerValue = jsHbAction.resolveOwnerValue(this.jsHbManager, creationRefMap);
				
				ClassMetadata classMetadata = this.jsHbManager.getJsHbConfig().getSessionFactory().getClassMetadata(jsHbAction.resolveOwnerPlayerType(jsHbManager, creationRefMap));
				if (classMetadata != null) {
					PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(jsHbAction.resolveOwnerPlayerType(jsHbManager, creationRefMap));
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
				if (jsHbAction.getOwnerCreationId() == null) {
					throw new RuntimeException(MessageFormat.format("ownerCreationId can not be null\naction:\n{0}", jsHbAction));
				}
				if (jsHbAction.getOwnerSignatureStr() != null) {
					throw new RuntimeException(MessageFormat.format("ownerSignatureStr can not be not null\naction:\n{0}", jsHbAction));
				}
				
				break;
			case DELETE:
				if (jsHbAction.getOwnerCreationId() != null) {
					throw new RuntimeException(MessageFormat.format("ownerCreationRefId can not be not null\naction:\n{0}", jsHbAction));
				}
				if (jsHbAction.getOwnerSignatureStr() == null) {
					throw new RuntimeException(MessageFormat.format("ownerSignatureStr can not be null\naction:\n{0}", jsHbAction));
				}
				//nada
				break;
			case COLLECTION_ADD:
				if (jsHbAction.getFieldName() == null) {
					throw new RuntimeException(MessageFormat.format("fieldName can not be not null\naction:\n{0}", jsHbAction));
				}
				collection = (Collection) resolvedCollection;
				
				break;
			case COLLECTION_REMOVE:
				if (jsHbAction.getFieldName() == null) {
					throw new RuntimeException(MessageFormat.format("fieldName can not be not null\naction:\n{0}", jsHbAction));
				}
				collection = (Collection) resolvedCollection;
				
				break;
			case SET_FIELD:
				if (jsHbAction.getFieldName() == null) {
					throw new RuntimeException(MessageFormat.format("fieldName can not be not null\naction:\n{0}", jsHbAction));
				}
				
				break;
			default:
				throw new RuntimeException("This should not happen");
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace(MessageFormat.format("preProcessPlayBack(). tape after pre processing:\n{0}", tape));
		}
	}
	
	@Override
	public void play() {
		if (this.replayed) {
			throw new RuntimeException("Already executed");
		}
		this.replayed = true;
		
		List<IChangeActionListener> actionListenersList = null;
		HashMap<Long, Object> creationRefMap = new HashMap<>();
		Session ss = this.jsHbManager.getJsHbConfig().getSessionFactory().getCurrentSession();
		ObjectMapper objectMapper = this.jsHbManager.getJsHbConfig().getObjectMapper();
		
		this.preProcessPlayBack(this.tape, creationRefMap);
		
		for (TapeAction actionItem : this.tape.getActions()) {
			JsHbTapeAction jsHbAction = (JsHbTapeAction) actionItem;
			// before events
			Object resolvedOwnerValue = jsHbAction.resolveOwnerValue(this.jsHbManager, creationRefMap);
			String resolvedJavaPropertyName = jsHbAction.resolveJavaPropertyName(objectMapper, this.jsHbManager, creationRefMap);
			@SuppressWarnings("rawtypes")
			Collection resolvedCollection = jsHbAction.resolveColletion(objectMapper, this.jsHbManager, creationRefMap);
			Object resolvedSettedValue = jsHbAction.resolveSettedValue(objectMapper, this.jsHbManager, creationRefMap);
			
			ChangeActionEventArgs<Object> actionEventArgs = new ChangeActionEventArgs<Object>(
					resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection,
					jsHbAction.getActionType());
			
			if (jsHbAction.getFieldName() != null) {
				ClassPropertyKey classPropertyKey = new ClassPropertyKey(jsHbAction.resolveOwnerPlayerType(this.jsHbManager, creationRefMap),
						resolvedJavaPropertyName);
				
				actionListenersList = this.listenersByClassMap.get(jsHbAction.resolveOwnerPlayerType(this.jsHbManager, creationRefMap));
				if (actionListenersList != null) {
					for (IChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onBeforeChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, jsHbAction.getActionType()));
						}
						actionListenerItem.onBeforeChange(actionEventArgs);
					}
				}
				
				actionListenersList = this.listenersByPrpsMap.get(classPropertyKey);
				if (actionListenersList != null) {
					for (IChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onBeforeChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, jsHbAction.getActionType()));
						}
						actionListenerItem.onBeforeChange(actionEventArgs);
					}
				}
				
				actionListenersList = this.listenersList;
				if (actionListenersList != null) {
					for (IChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onBeforeChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, jsHbAction.getActionType()));
						}
						actionListenerItem.onBeforeChange(actionEventArgs);
					}
				}
			} else {
				actionListenersList = this.listenersByClassMap.get(jsHbAction.resolveOwnerPlayerType(this.jsHbManager, creationRefMap));
				if (actionListenersList != null) {
					for (IChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onBeforeChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, jsHbAction.getActionType()));
						}
						actionListenerItem.onBeforeChange(actionEventArgs);
					}
				}
				
				actionListenersList = this.listenersList;
				if (actionListenersList != null) {
					for (IChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onBeforeChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, jsHbAction.getActionType()));
						}
						actionListenerItem.onBeforeChange(actionEventArgs);
					}
				}
			}

			@SuppressWarnings("rawtypes")
			Collection collection = null;
			
			switch (jsHbAction.getActionType()) {
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
					SettableBeanProperty settableBeanProperty = jsHbAction.resolveBeanDeserializer(objectMapper, this.jsHbManager, creationRefMap).findProperty(jsHbAction.getFieldName()); 
					settableBeanProperty.set(resolvedOwnerValue, resolvedSettedValue);
				} catch (IOException e) {
					throw new RuntimeException(MessageFormat.format("This should not happen.\naction:\n{0}", jsHbAction));
				}
				break;
			default:
				throw new RuntimeException("This should not happen");
			}

			// after events
			if (jsHbAction.getFieldName() != null) {
				ClassPropertyKey classPropertyKey = new ClassPropertyKey(jsHbAction.resolveOwnerPlayerType(this.jsHbManager, creationRefMap),
						resolvedJavaPropertyName);

				actionListenersList = this.listenersByPrpsMap.get(classPropertyKey);
				if (actionListenersList != null) {
					for (IChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onAfterChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, jsHbAction.getActionType()));
						}
						actionListenerItem.onAfterChange(actionEventArgs);
					}
				}

				actionListenersList = this.listenersByClassMap.get(jsHbAction.resolveOwnerPlayerType(this.jsHbManager, creationRefMap));
				if (actionListenersList != null) {
					for (IChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onAfterChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, jsHbAction.getActionType()));
						}
						actionListenerItem.onAfterChange(actionEventArgs);
					}
				}
				
				actionListenersList = this.listenersList;
				if (actionListenersList != null) {
					for (IChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onAfterChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, jsHbAction.getActionType()));
						}
						actionListenerItem.onAfterChange(actionEventArgs);
					}
				}
			} else {
				actionListenersList = this.listenersByClassMap.get(jsHbAction.resolveOwnerPlayerType(this.jsHbManager, creationRefMap));
				if (actionListenersList != null) {
					for (IChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onAfterChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, jsHbAction.getActionType()));
						}
						actionListenerItem.onAfterChange(actionEventArgs);
					}
				}

				actionListenersList = this.listenersList;
				if (actionListenersList != null) {
					for (IChangeActionListener actionListenerItem : actionListenersList) {
						if (logger.isTraceEnabled()) {
							logger.trace(MessageFormat.format("replay(). running {0}.onAfterChange({1}, {2}, {3}, {4}, {5}) for", actionListenerItem.getName(), resolvedOwnerValue, resolvedSettedValue, resolvedJavaPropertyName, resolvedCollection, jsHbAction.getActionType()));
						}
						actionListenerItem.onAfterChange(actionEventArgs);
					}
				}
			}
		}
	}

	@Override
	public <E> IFluentChangeListener<E> fluentChangeListener(Class<E> targetClass) {
		// TODO Auto-generated method stub
		return new JsHbFluentChangeListenerDefault<E>(this, targetClass);
	}
	
	public IFluentChangeListener<?> fluentChangeListener() {
		return new JsHbFluentChangeListenerDefault<>(this, null);
	}
}
/*gerando conflito*/