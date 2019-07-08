package org.jsplayback.backend.hibernate;

import java.text.MessageFormat;

import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;

import org.jsplayback.backend.IJsHbManager;

public class JsHbBeanPropertyWriter extends BeanPropertyWriter {
	private static Logger logger = LoggerFactory.getLogger(JsHbBeanPropertyWriter.class);

	private IJsHbManager jsHbManager;

//	private Class<?> componentOwnerClass = null;
	private Class<?> relationshipOwnerClass = null;
	private Object currOwner = null;
	private BeanPropertyDefinition beanPropertyDefinition = null;
	private boolean isPersistent;
	private boolean isHibernateId;

	public boolean getIsHibernateId() {
		return isHibernateId;
	}

	public JsHbBeanPropertyWriter loadIsHibernateId(boolean isHibernateId) {
		this.isHibernateId = isHibernateId;
		return this;
	}

	public boolean getIsPersistent() {
		return isPersistent;
	}

	public JsHbBeanPropertyWriter loadIsPersistent(boolean isPersistent) {
		this.isPersistent = isPersistent;
		return this;
	}

	public BeanPropertyDefinition getBeanPropertyDefinition() {
		return beanPropertyDefinition;
	}

	public JsHbBeanPropertyWriter loadBeanPropertyDefinition(BeanPropertyDefinition beanPropertyDefinition) {
		this.beanPropertyDefinition = beanPropertyDefinition;
		return this;
	}

	public Object getCurrOwner() {
		return currOwner;
	}

//	public Class<?> getComponentOwnerClass() {
//		return componentOwnerClass;
//	}

	public Class<?> getRelationshipOwnerClass() {
		return relationshipOwnerClass;
	}

//	public JsHbBeanPropertyWriter loadComponentOwnerClass(Class<?> componentOwnerClass) {
//		this.componentOwnerClass = componentOwnerClass;
//		return this;
//	}

	public JsHbBeanPropertyWriter loadRelationshipOwnerClass(Class<?> relationshipOwnerClass) {
		this.relationshipOwnerClass = relationshipOwnerClass;
		return this;
	}

	public JsHbBeanPropertyWriter configJsHbManager(IJsHbManager jsHbManager) {
		this.jsHbManager = jsHbManager;
		return this;
	}

	public JsHbBeanPropertyWriter(BeanPropertyWriter propertyWriter) {
		super(propertyWriter);
	}

	public void initializeSerializer(JsonGenerator gen, SerializerProvider prov) throws Exception {
		// then find serializer to use
		JsonSerializer<Object> ser = _serializer;
		if (ser == null) {
			Class<?> cls = this.getType().getRawClass();
			PropertySerializerMap m = _dynamicSerializers;
			ser = m.serializerFor(this.getPropertyType());
			if (ser == null) {
				ser = _findAndAddDynamic(m, cls, prov);
			}
		}
	}

	@Override
	public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {

		try {
			if (this.jsHbManager.isStarted()) {
				this.jsHbManager.getJsHbBeanPropertyWriterStepStack().push(this);
			}
			this.currOwner = bean;

			super.serializeAsField(bean, gen, prov);
		} finally {
			this.currOwner = null;
			if (this.jsHbManager.isStarted()) {
				JsHbBeanPropertyWriter propertyWriterPop = this.jsHbManager.getJsHbBeanPropertyWriterStepStack().pop();
				if (propertyWriterPop != this) {
					throw new RuntimeException("This should not happen");
				}
			}
		}
	}

	@Override
	public Class<?> getPropertyType() {
		Class originalClass = super.getPropertyType();
		if (originalClass != null && HibernateProxy.class.isAssignableFrom(originalClass)) {
			return originalClass.getSuperclass();
		} else {
			return super.getPropertyType();
		}
	}

	@Override
	public JavaType getType() {
		JavaType originalClass = super.getType();
		if (originalClass != null && HibernateProxy.class.isAssignableFrom(originalClass.getRawClass())) {
			return originalClass.getSuperClass();
		} else {
			return super.getType().getSuperClass();
		}
	}

	public void serializeAsFieldHibernateIdentifier(Object bean, JsonGenerator gen, SerializerProvider prov)
			throws Exception {
		try {
			if (this.jsHbManager.isStarted()) {
				this.jsHbManager.getJsHbBeanPropertyWriterStepStack().push(this);
			}
			this.currOwner = bean;

			// inlined 'get()'
			final Object value = (_accessorMethod == null) ? _field.get(bean) : _accessorMethod.invoke(bean);

			// Null handling is bit different, check that first
			if (value == null) {
				if (_nullSerializer != null) {
					gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbHibernateIdName());
					_nullSerializer.serialize(null, gen, prov);
					
					if (logger.isTraceEnabled()) {
						logger.trace(MessageFormat.format(
								"serializeAsFieldHibernateIdentifier():\n" + " gen.writeFieldName(\"{0}\");\n"
										+ " _nullSerializer.serialize(null, gen, prov); ",
								this.jsHbManager.getJsHbConfig().getJsHbHibernateIdName()));
					}
				}
				return;
			}
			// then find serializer to use
			JsonSerializer<Object> ser = _serializer;
			if (ser == null) {
				Class<?> cls = value.getClass();
				PropertySerializerMap m = _dynamicSerializers;
				ser = m.serializerFor(cls);
				if (ser == null) {
					ser = _findAndAddDynamic(m, cls, prov);
				}
			}
			// and then see if we must suppress certain values (default, empty)
			if (_suppressableValue != null) {
				if (MARKER_FOR_EMPTY == _suppressableValue) {
					if (ser.isEmpty(prov, value)) {
						return;
					}
				} else if (_suppressableValue.equals(value)) {
					return;
				}
			}
			// For non-nulls: simple check for direct cycles
			if (value == bean) {
				// three choices: exception; handled by call; or pass-through
				if (_handleSelfReference(bean, gen, prov, ser)) {
					return;
				}
			}
			gen.writeFieldName(this.jsHbManager.getJsHbConfig().getJsHbHibernateIdName());
			if (logger.isTraceEnabled()) {
				logger.trace(MessageFormat.format(
						"serializeAsFieldHibernateIdentifier():\n" + " gen.writeFieldName(\"{0}\");",
						this.jsHbManager.getJsHbConfig().getJsHbHibernateIdName()));
			}
			if (_typeSerializer == null) {
				if (logger.isTraceEnabled()) {
					logger.trace(MessageFormat.format("serializeAsFieldHibernateIdentifier():\n"
							+ " ser.serialize(value, gen, prov);\n" + " ser: ''{0}''", ser));
				}
				ser.serialize(value, gen, prov);
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace(MessageFormat.format("serializeAsFieldHibernateIdentifier():\n"
							+ " ser.serialize(value, gen, prov, _typeSerializer);\n" + " ser: ''{0}''\n"
									+ " _typeSerializer: " + _typeSerializer, ser, _typeSerializer));
				}
				ser.serializeWithType(value, gen, prov, _typeSerializer);
			}

		} finally {
			this.currOwner = null;
			if (this.jsHbManager.isStarted()) {
				JsHbBeanPropertyWriter propertyWriterPop = this.jsHbManager.getJsHbBeanPropertyWriterStepStack().pop();
				if (propertyWriterPop != this) {
					throw new RuntimeException("This should not happen");
				}
			}
		}
	}
	
	@Override
	public String toString() {
        StringBuilder sb = new StringBuilder(40);
        sb
        	.append("JsHbBeanPropertyWriter with")
//        	.append("componentOwnerClass='").append(componentOwnerClass).append("', ")
        	.append("relationshipOwnerClass='").append(relationshipOwnerClass).append("', ")
        	.append("isPersistent=").append(isPersistent).append(", ")
        	.append("isHibernateId=").append(isHibernateId).append(", ")
        	.append("relationshipOwnerClass='").append(relationshipOwnerClass).append("'")
        	.append(" from original BeanPropertyWriter={ ").append(super.toString()).append(" }");
		return sb.toString();
	}
}
/*gerando conflito*/