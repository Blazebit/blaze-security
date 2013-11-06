package com.blazebit.security.web.bean.model;

import com.blazebit.security.Permission;
import com.blazebit.security.RolePermission;
import com.blazebit.security.SubjectPermission;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.UserDataPermission;
import com.blazebit.security.impl.model.UserGroupDataPermission;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserPermission;

public class PermissionModel {

    private Permission entity;

    public PermissionModel(Permission p) {
        this.entity = p;
    }

    
    public Permission getEntity() {
        return entity;
    }

    
    public void setEntity(Permission entity) {
        this.entity = entity;
    }

    public String getSubjectName() {
        if (entity instanceof SubjectPermission) {
            if (entity.getResource() instanceof EntityObjectField) {
                return ((UserDataPermission) entity).getSubject().getUsername();
            } else {
                return ((UserPermission) entity).getSubject().getUsername();
            }
        } else {
            if (entity instanceof RolePermission) {
                if (entity.getResource() instanceof EntityObjectField) {
                    return ((UserGroupDataPermission) entity).getSubject().getName();
                } else {
                    return ((UserGroupPermission) entity).getSubject().getName();
                }
            }
        }
        return "UNKNOWN";
    }

    public String getResourceName() {
        EntityField entityField = (EntityField) entity.getResource();
        StringBuilder ret = new StringBuilder().append("Entity: ").append(entityField.getEntity());
        if (!entityField.isEmptyField()) {
            ret.append(", Field: ").append(entityField.getField());
        }
        if (entity.getResource() instanceof EntityObjectField) {
            ret.append(", Id:").append(((EntityObjectField) entityField).getEntityId());
        }
        return ret.toString();

    }
}
