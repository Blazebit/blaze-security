package com.blazebit.security.web.bean;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.util.Constants;

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
        Map<String, List<String>> modules = resourceMetamodel.getResourcesByModule();

        for (String module : modules.keySet()) {
            if (!modules.get(module).isEmpty()) {
                // creates module as root
                DefaultTreeNode moduleNode = new DefaultTreeNode(new TreeNodeModel(module, TreeNodeModel.ResourceType.MODULE, null), root);
                moduleNode.setExpanded(true);
                for (String resourceName : modules.get(module)) {
                    // second level must always be an entity resource with the entity name
                    EntityField entityField = (EntityField) entityFieldFactory.createResource(resourceName);
                    // check if logged in user can grant or revoke these resources
                    // entity
                    TreeNodeModel entityNodeModel = new TreeNodeModel(entityField.getEntity(), TreeNodeModel.ResourceType.ENTITY, entityField);
                    DefaultTreeNode entityNode = new DefaultTreeNode(entityNodeModel, moduleNode);
                    entityNode.setExpanded(true);
                    // first come actions available only for entity
                    createActionNodes(actionUtils.getActionsForEntity(), selectedPermissions, entityNode, entityField);
                    // then come actions common for fields and entities
                    if (Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.FIELD_LEVEL))) {
                        // fields for entity
                        List<String> primitiveFields = resourceMetamodel.getPrimitiveFields(resourceName);
                        List<String> collectionFields = resourceMetamodel.getCollectionFields(resourceName);
                        if (!primitiveFields.isEmpty()) {
                            // actions for primitive fields
                            createActionNodes(actionUtils.getActionsForPrimitiveField(), primitiveFields, entityField, selectedPermissions, entityNode);
                        }
                        if (!collectionFields.isEmpty()) {
                            // actions for collection fields
                            createActionNodes(actionUtils.getActionsForCollectionField(), collectionFields, entityField, selectedPermissions, entityNode);
                        }
                    } else {
                        // if field level is not enabled add these common action to the tree as well
                        createActionNodes(actionUtils.getCommonActionsForEntity(), selectedPermissions, entityNode, entityField);
                    }
                    propagateNodePropertiesTo(entityNode);
                }
                propagateNodePropertiesTo(moduleNode);
            }

        }
        return root;
    }

    /**
     * creates nodes for actions without fields
     * 
     * @param actions
     * @param entityField
     * @param selectedPermissions
     * @param entityNode - attach action node to entity node
     */
    private void createActionNodes(List<Action> actions, Collection<Permission> selectedPermissions, TreeNode entityNode, EntityField entityField) {
        for (Action action : actions) {
            EntityAction entityAction = (EntityAction) action;
            TreeNodeModel actionNodeModel = new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction);
            actionNodeModel.setEntityInstance(entityField);

            DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel, entityNode);
            actionNode.setExpanded(true);

            // check if action node should be selected or not based on selectedPermissions
            Permission found = permissionHandlingUtils.findActionAndResourceMatch(selectedPermissions, permissionFactory.create(entityAction, entityField), true);
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

    /**
     * create action node with fields
     * 
     * @param actions
     * @param fields
     * @param entityField
     * @param selectedPermissions
     * @param entityNode
     */
    private void createActionNodes(List<Action> actions, List<String> fields, EntityField entityField, Collection<Permission> selectedPermissions, TreeNode entityNode) {
        for (Action action : actions) {
            EntityAction entityAction = (EntityAction) action;
            TreeNodeModel actionNodeModel = new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction);
            actionNodeModel.setEntityInstance(entityField);

            DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel, entityNode);
            actionNode.setExpanded(true);
            // fields
            createFieldNodes(fields, actionNode, entityAction, entityField, selectedPermissions);
            // lookup if entity is grantable
            Permission found = permissionHandlingUtils.findActionAndResourceMatch(selectedPermissions, permissionFactory.create(entityAction, entityField), true);
            if (found == null) {
                // remove actionNode is entity cannot be granted
                if (!isGranted(ActionConstants.GRANT, entityField)) {
                    actionNode.setParent(null);
                }
            } else {
                propagateNodePropertiesTo(actionNode);
            }
        }
    }

    /**
     * create field nodes
     * 
     * @param fields
     * @param actionNode
     * @param entityAction
     * @param entityField
     * @param selectedPermissions
     */
    private void createFieldNodes(List<String> fields, DefaultTreeNode actionNode, EntityAction entityAction, EntityField entityField, Collection<Permission> selectedPermissions) {
        for (String field : fields) {
            EntityField entityFieldWithField = entityField.getChild(field);
            DefaultTreeNode fieldNode = new DefaultTreeNode(null, actionNode);
            TreeNodeModel fieldNodeModel = new TreeNodeModel(field, TreeNodeModel.ResourceType.FIELD, entityFieldWithField);
            fieldNodeModel.setEntityInstance(entityFieldWithField);
            // decide how field node should be marked (red/blue/green)
            Permission foundWithField = permissionHandlingUtils.findActionAndResourceMatch(selectedPermissions, permissionFactory.create(entityAction, entityFieldWithField), true);
            Permission foundWithoutField = permissionHandlingUtils.findActionAndResourceMatch(selectedPermissions, permissionFactory.create(entityAction, entityField), true);

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
