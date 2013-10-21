package com.blazebit.security.web.bean;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.inject.Inject;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.reflection.ReflectionUtils;
import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityFieldFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.web.bean.model.NodeModel;

public class ResourceHandlingBaseBean extends PermissionHandlingBaseBean {

    @Inject
    private ResourceNameExtension resourceNameExtension;

    @Inject
    protected EntityFieldFactory entityFieldFactory;

    @Inject
    protected ActionFactory actionFactory;

    protected DefaultTreeNode getResourceTree() {
        return getResourceTree(new HashSet<Permission>(), null);
    }

    /**
     * builds up a resource tree
     * 
     * @return
     */
    protected DefaultTreeNode getResourceTree(Collection<Permission> selectedPermissions, TreeNode[] selectedNodes) {
        DefaultTreeNode root = new DefaultTreeNode("root", null);
        Map<String, List<AnnotatedType<?>>> modules = resourceNameExtension.getResourceNamesByModule();
        for (String module : modules.keySet()) {
            if (!modules.get(module).isEmpty()) {
                DefaultTreeNode moduleNode = new DefaultTreeNode(new NodeModel(module, NodeModel.ResourceType.MODULE, null), root);
                moduleNode.setExpanded(true);
                for (AnnotatedType<?> type : modules.get(module)) {
                    Class<?> entityClass = (Class<?>) type.getBaseType();
                    EntityField entityField = (EntityField) entityFieldFactory.createResource(entityClass);
                    // check if logged in user can grant these resources
                    if (isAuthorizedResource(ActionConstants.GRANT, entityField)) {
                        // entity
                        DefaultTreeNode entityNode = new DefaultTreeNode("root", new NodeModel(entityField.getEntity(), NodeModel.ResourceType.ENTITY, entityField), moduleNode);
                        entityNode.setExpanded(true);
                        List<Action> entityActionFields = actionFactory.getActionsForEntity();
                        // if (ReflectionUtils.isSubtype(entityClass, Subject.class) || ReflectionUtils.isSubtype(entityClass,
                        // Role.class)
                        // || ReflectionUtils.isSubtype(entityClass, Permission.class)) {
                        entityActionFields.addAll(actionFactory.getSpecialActions());
                        // }
                        // fields for entity
                        Field[] allFields = ReflectionUtils.getInstanceFields(entityClass);
                        if (allFields.length > 0) {
                            // actions for fields
                            for (Action action : actionFactory.getActionsForField()) {
                                EntityAction entityAction = (EntityAction) action;
                                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction),
                                    entityNode);
                                // actionNode.setExpanded(true);
                                // fields for entity
                                for (Field field : allFields) {
                                    EntityField entityFieldWithField = (EntityField) entityFieldFactory.createResource(entityClass, field.getName());
                                    DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(field.getName(), NodeModel.ResourceType.FIELD, entityFieldWithField), actionNode);
                                    fieldNode.setExpanded(true);

                                    if (contains(selectedPermissions, permissionFactory.create(entityAction, entityFieldWithField))
                                        || contains(selectedPermissions, permissionFactory.create(entityAction, entityField))) {
                                        fieldNode.setSelected(true);
                                        selectedNodes = addNodeToSelectedNodes(fieldNode, selectedNodes);
                                        fieldNode.setSelectable(isAuthorizedResource(ActionConstants.REVOKE, entityField));
                                    } else {
                                        // permission can be granted if logged in user has permission to do it
                                        fieldNode.setSelectable(isAuthorizedResource(ActionConstants.GRANT, entityField));
                                    }
                                }
                                selectedNodes = propagateSelectionAndMarkingUp(actionNode, selectedNodes);
                                entityActionFields.remove(action);
                            }
                        }
                        // remaining action fields for entity
                        for (Action action : entityActionFields) {
                            EntityAction entityAction = (EntityAction) action;
                            DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction), entityNode);
                            if (contains(selectedPermissions, permissionFactory.create(entityAction, entityField))) {
                                actionNode.setSelected(true);
                                addNodeToSelectedNodes(actionNode, selectedNodes);
                                actionNode.setSelectable(isAuthorizedResource(ActionConstants.REVOKE, entityField));
                            }
                        }
                        // fix selections -> propagate "checked" to entity if every child checked
                        selectedNodes = propagateSelectionAndMarkingUp(entityNode, selectedNodes);
                    }
                }
            }
        }
        return root;
    }

}
