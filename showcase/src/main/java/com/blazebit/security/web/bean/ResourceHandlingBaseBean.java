package com.blazebit.security.web.bean;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.EntityResourceDefinition;
import com.blazebit.security.Permission;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.util.Constants;
import com.blazebit.security.web.util.FieldUtils;

public class ResourceHandlingBaseBean extends PermissionTreeHandlingBaseBean {

    protected DefaultTreeNode getResourceTree() throws ClassNotFoundException {
        return getResourceTree(new HashSet<Permission>());
    }

    /**
     * builds up a resource tree
     * 
     * @return
     * @throws ClassNotFoundException
     */
    protected DefaultTreeNode getResourceTree(Collection<Permission> selectedPermissions) throws ClassNotFoundException {
        DefaultTreeNode root = new DefaultTreeNode();
        // gets all the entities that are annotated with resource name -> groups by module name
        Map<String, List<EntityResourceDefinition>> modules = resourceNameExtension.getResourcesByModule();
        for (String module : modules.keySet()) {
            if (!modules.get(module).isEmpty()) {
                // creates module as root
                DefaultTreeNode moduleNode = new DefaultTreeNode(new TreeNodeModel(module, TreeNodeModel.ResourceType.MODULE, null), root);
                moduleNode.setExpanded(true);
                for (EntityResourceDefinition entityResourceDefinition : modules.get(module)) {
                    // second level must always be an entity resource with the entity name
                    EntityField entityField = (EntityField) entityFieldFactory.createResource(entityResourceDefinition.getResourceName());
                    // check if logged in user can grant or revoke these resources
                    // TODO to show or not to show not grantable/revokable permissions
                    // if (isGranted(ActionConstants.GRANT, entityField) || isGranted(ActionConstants.REVOKE, entityField)) {
                    // entity
                    TreeNodeModel entityNodeModel = new TreeNodeModel(entityField.getEntity(), TreeNodeModel.ResourceType.ENTITY, entityField);
                    DefaultTreeNode entityNode = new DefaultTreeNode(entityNodeModel, moduleNode);
                    entityNode.setExpanded(true);
                    // first come actions available only for entity
                    createActionNodes(actionUtils.getActionsForEntity(), entityField, selectedPermissions, entityNode);
                    if (userSession.getSelectedCompany().isFieldLevelEnabled()) {
                        // fields for entity
                        List<Field> primitiveFields = FieldUtils.getPrimitiveFields(Class.forName(entityResourceDefinition.getResource().getEntityClassName()));
                        if (!primitiveFields.isEmpty()) {
                            // actions for primitive fields
                            createActionNodes(actionUtils.getActionsForPrimitiveField(), primitiveFields, entityField, selectedPermissions, entityNode);
                        }
                        List<Field> collectionFields = FieldUtils.getCollectionFields(Class.forName(entityResourceDefinition.getResource().getEntityClassName()));
                        if (!collectionFields.isEmpty()) {
                            // actions for collection fields
                            createActionNodes(actionUtils.getActionsForCollectionField(), collectionFields, entityField, selectedPermissions, entityNode);
                        }
                    } else {
                        createActionNodes(actionUtils.getCommonActionsForEntity(), entityField, selectedPermissions, entityNode);
                    }
                    // if (!primitiveFields.isEmpty() || !collectionFields.isEmpty()) {
                    // // actions for all fields
                    // primitiveFields.addAll(collectionFields);
                    // createActionNodes(actionUtils.getActionsForGeneralField(), primitiveFields, entityField,
                    // selectedPermissions, entityNode);
                    // }
                    // }
                    propagateNodePropertiesTo(entityNode);
                }
                propagateNodePropertiesTo(moduleNode);
            }
        }
        return root;
    }

    private void createActionNodes(List<Action> actions, EntityField entityField, Collection<Permission> selectedPermissions, TreeNode entityNode) {
        for (Action action : actions) {
            EntityAction entityAction = (EntityAction) action;
            TreeNodeModel actionNodeModel = new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction);
            actionNodeModel.setEntityInstance(entityField);

            DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel, entityNode);
            actionNode.setExpanded(true);

            // selected
            Permission found = findActionAndResourceMatch(selectedPermissions, permissionFactory.create(entityAction, entityField), true);
            if (found != null) {
                actionNode.setSelected(true);
                actionNode.setSelectable(isGranted(ActionConstants.REVOKE, entityField));
            } else {
                // if resource does not belong to user and cannot be granted HIDE it
                if (!isGranted(ActionConstants.GRANT, entityField)) {
                    actionNode.setParent(null);
                }
            }
        }
    }

    private void createActionNodes(List<Action> actions, List<Field> fields, EntityField entityField, Collection<Permission> selectedPermissions, TreeNode entityNode) {
        for (Action action : actions) {
            EntityAction entityAction = (EntityAction) action;
            TreeNodeModel actionNodeModel = new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction);
            actionNodeModel.setEntityInstance(entityField);

            DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel, entityNode);
            actionNode.setExpanded(true);
            // fields
            createFieldNodes(fields, actionNode, entityAction, entityField, selectedPermissions);
            // lookup if entity is grantable
            Permission found = findActionAndResourceMatch(selectedPermissions, permissionFactory.create(entityAction, entityField), true);
            if (found == null) {
                if (!isGranted(ActionConstants.GRANT, entityField)) {
                    actionNode.setParent(null);
                }
            } else {
                propagateNodePropertiesTo(actionNode);
            }
        }
    }

    private void createFieldNodes(List<Field> fields, DefaultTreeNode actionNode, EntityAction entityAction, EntityField entityField, Collection<Permission> selectedPermissions) {
        for (Field field : fields) {
            EntityField entityFieldWithField = entityField.getChild(field.getName());
            DefaultTreeNode fieldNode = new DefaultTreeNode(null, actionNode);
            TreeNodeModel fieldNodeModel = new TreeNodeModel(field.getName(), TreeNodeModel.ResourceType.FIELD, entityFieldWithField);
            fieldNodeModel.setEntityInstance(entityFieldWithField);
            // decide how field node should be marked (red/blue/green)
            Permission foundWithField = findActionAndResourceMatch(selectedPermissions, permissionFactory.create(entityAction, entityFieldWithField), true);
            Permission foundWithoutField = findActionAndResourceMatch(selectedPermissions, permissionFactory.create(entityAction, entityField), true);

            if (foundWithField != null || foundWithoutField != null) {
                // entity object resources are blue
                if ((foundWithField != null && foundWithField.getResource() instanceof EntityObjectField)
                    || (foundWithoutField != null && foundWithoutField.getResource() instanceof EntityObjectField)) {
                    fieldNodeModel.setMarking(Marking.OBJECT);
                    fieldNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
                } else {
                    // field is selected=belongs to user -> check for revoke permission
                    fieldNode.setSelected(true);
                    fieldNode.setSelectable(isGranted(ActionConstants.REVOKE, entityField));
                }

            } else {
                // permission can be granted if logged in user has permission to do it
                if (!isGranted(ActionConstants.GRANT, entityField)) {
                    // fieldNode.setSelectable(isGranted(ActionConstants.GRANT, entityField));
                    fieldNode.setParent(null);
                }
            }
            fieldNode.setData(fieldNodeModel);
            fieldNode.setExpanded(true);
        }
    }
}
