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
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.web.bean.model.NodeModel;
import com.blazebit.security.web.bean.model.NodeModel.Marking;

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

    protected DefaultTreeNode getResourceTree(Collection<Permission> selectedPermissions, TreeNode[] selectedNodes) {
        return getResourceTree(selectedPermissions, selectedNodes, false);
    }

    /**
     * builds up a resource tree
     * 
     * @return
     */
    protected DefaultTreeNode getResourceTree(Collection<Permission> selectedPermissions, TreeNode[] selectedNodes, boolean dontSelectObjects) {
        DefaultTreeNode root = new DefaultTreeNode();
        // gets all the entities that are annotated with resource name -> groups by module name
        Map<String, List<AnnotatedType<?>>> modules = resourceNameExtension.getResourceNamesByModule();
        for (String module : modules.keySet()) {
            if (!modules.get(module).isEmpty()) {
                // creates module as first root
                DefaultTreeNode moduleNode = new DefaultTreeNode(new NodeModel(module, NodeModel.ResourceType.MODULE, null), root);
                moduleNode.setExpanded(true);
                for (AnnotatedType<?> type : modules.get(module)) {
                    Class<?> entityClass = (Class<?>) type.getBaseType();
                    // second root must always be an entity resource with the entity name
                    EntityField entityField = (EntityField) entityFieldFactory.createResource(entityClass);
                    // check if logged in user can grant these resources
                    if (isAuthorized(ActionConstants.GRANT, entityField)) {
                        // entity
                        DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entityField.getEntity(), NodeModel.ResourceType.ENTITY, entityField), moduleNode);
                        entityNode.setExpanded(true);
                        // action node comes under entity node
                        List<Action> entityActionFields = actionFactory.getActionsForEntity();
                        // if (ReflectionUtils.isSubtype(entityClass, Subject.class) || ReflectionUtils.isSubtype(entityClass,
                        // Role.class)
                        // || ReflectionUtils.isSubtype(entityClass, Permission.class)) {
                        // if (ReflectionUtils.isSubtype(entityClass, Permission.class)) {
                        // every entity also permissions must have create, read, update, delete, grant and revoke actions
                        entityActionFields.addAll(actionFactory.getSpecialActions());
                        // }
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

                                    // decide how field node should be marked (red, blue, green)
                                    Permission foundWithField = findWithoutIdDifferentiation(selectedPermissions, permissionFactory.create(entityAction, entityFieldWithField));
                                    Permission foundWithoutField = findWithoutIdDifferentiation(selectedPermissions, permissionFactory.create(entityAction, entityField));

                                    if (foundWithField != null || foundWithoutField != null) {
                                        // entity object resources are blue
                                        if ((foundWithField!=null && foundWithField.getResource() instanceof EntityObjectField) || (foundWithoutField!=null && foundWithoutField.getResource() instanceof EntityObjectField)) {
                                            ((NodeModel) fieldNode.getData()).setMarking(Marking.BLUE);
                                            ((NodeModel) fieldNode.getData()).setTooltip("Contains permissions for specific entity objects");
                                            if (dontSelectObjects) {
                                                fieldNode.setSelected(false);
                                            }
                                        } else {
                                            fieldNode.setSelected(true);
                                            selectedNodes = addNodeToSelectedNodes(fieldNode, selectedNodes);
                                        }
                                        // fieldNode.setSelectable(isAuthorized(ActionConstants.REVOKE, entityField));
                                    } else {
                                        // permission can be granted if logged in user has permission to do it
                                        // fieldNode.setSelectable(isAuthorized(ActionConstants.GRANT, entityField));
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

                            Permission foundWithoutField = findWithoutIdDifferentiation(selectedPermissions, permissionFactory.create(entityAction, entityField));

                            if (foundWithoutField != null) {
                                if (foundWithoutField.getResource() instanceof EntityObjectField) {
                                    if (dontSelectObjects) {
                                        actionNode.setSelected(false);
                                    }
                                    ((NodeModel) actionNode.getData()).setMarking(Marking.BLUE);
                                    ((NodeModel) actionNode.getData()).setTooltip("Contains permissions for specific entity objects");
                                } else {
                                    actionNode.setSelected(true);
                                    addNodeToSelectedNodes(actionNode, selectedNodes);
                                }
                                // actionNode.setSelectable(isAuthorizedResource(ActionConstants.REVOKE, entityField));
                            }
                        }
                        // fix selections -> propagate "checked" and "color" to entity if every child checked
                        selectedNodes = propagateSelectionAndMarkingUp(entityNode, selectedNodes);
                    }
                }
            }
        }
        return root;
    }
}
