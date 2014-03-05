package com.blazebit.security.integration.entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URLEncoder;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;

import com.blazebit.reflection.ReflectionUtils;
import com.blazebit.security.entity.EntityIdConverter;
import com.blazebit.text.FormatUtils;

@Dependent
public class EntityIdConverterImpl implements EntityIdConverter, Serializable {

    private static final long serialVersionUID = 1L;
    
    @PersistenceContext(unitName = "TestPU")
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public String getIdAsString(Serializable id) {
        if (id == null) {
            return null;
        }

        Class<?> idClass = id.getClass();

        if (FormatUtils.isParseableType(idClass)) {
            return FormatUtils.getFormattedValue((Class<Serializable>) idClass, (Serializable) id);
        } else {
            Class<?> managedIdClass = getManagedIdClass(idClass);

            if (managedIdClass == null) {
                throw new IllegalArgumentException("Type " + idClass.getName() + " is not managed");
            }

            StringBuilder sb = new StringBuilder();

            try {
                // Order of fields is deterministic so we can rely on this for
                // the encoding/decoding
                Field[] fields = ReflectionUtils.getInstanceFields(managedIdClass);

                for (int i = 0; i < fields.length; i++) {
                    if (i != 0) {
                        sb.append('/');
                    }

                    String fieldName = fields[i].getName();
                    Object fieldValue = ReflectionUtils.getGetter(managedIdClass, fieldName).invoke(id);
                    sb.append(fieldValue == null ? "null" : URLEncoder.encode(FormatUtils
                        .getFormattedValue((Class<Serializable>) fields[i].getType(), (Serializable) fieldValue), "UTF-8"));
                }

                return URLEncoder.encode(sb.toString(), "UTF-8");
            } catch (Exception ex) {
                throw new IllegalArgumentException("Could not create id string from object", ex);
            }
        }
    }

    private Class<?> getManagedIdClass(Class<?> idClass) {
        for (EntityType<?> entityType : entityManager.getMetamodel().getEntities()) {
            for (SingularAttribute<?, ?> attribute : entityType.getSingularAttributes()) {
                if (attribute.isId()) {
                    Class<?> attributeType = ReflectionUtils
                        .getResolvedFieldType(entityType.getJavaType(), attribute.getName());

                    if (ReflectionUtils.isSubtype(idClass, attributeType)) {
                        return attributeType;
                    } else {
                        break;
                    }
                }
            }
        }

        return null;
    }
}
