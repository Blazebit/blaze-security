package com.blazebit.security.web.bean.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.util.Constants;

public class ResourceHandlingBaseBean extends PermissionHandlingBaseBean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected DefaultTreeNode getResourceTree() throws ClassNotFoundException {
        return getResourceTree(new HashSet<Permission>());
    }

    protected DefaultTreeNode getResourceTree(String filter) throws ClassNotFoundException {
        return getResourceTree(new HashSet<Permission>(), filter);
    }
    

    
    protected DefaultTreeNode getResourceTree(Collection<Permission> selectedPermissions) throws ClassNotFoundException {
        return getResourceTree(selectedPermissions, "");
    }

    /**
     * builds up a resource tree
     * 
     * @return
     * @throws ClassNotFoundException
     */
    protected DefaultTreeNode getResourceTree(Collection<Permission> selectedPermissions, String filter) throws ClassNotFoundException {
        DefaultTreeNode root = new DefaultTreeNode();
        // gets all the entities that are annotated with resource name -> groups by module name
        Map<String, List<String>> modules = resourceMetamodel.getResourcesByModule();

        for (String module : modules.keySet()) {
            if (!modules.get(module).isEmpty()) {
                // creates module as root
                DefaultTreeNode moduleNode = new DefaultTreeNode("module", new TreeNodeModel(module, TreeNodeModel.ResourceType.MODULE, null), root);
                moduleNode.setExpanded(true);
                for (String resourceName : modules.get(module)) {
                    if (StringUtils.containsIgnoreCase(resourceName, filter)) {
                        // second level must always be an entity resource with the entity name
                        EntityField entityField = (EntityField) entityFieldFactory.createResource(resourceName);
                        // check if logged in user can grant or revoke these resources
                        // entity
                        TreeNodeModel entityNodeModel = new TreeNodeModel(entityField.getEntity(), TreeNodeModel.ResourceType.ENTITY, entityField);
                        DefaultTreeNode entityNode = new DefaultTreeNode("entity", entityNodeModel, moduleNode);
                        entityNode.setExpanded(true);

                        List<String> primitiveFields = new ArrayList<String>();
                        List<String> collectionFields = new ArrayList<String>();
                        if (isEnabled(Company.FIELD_LEVEL)) {
                            primitiveFields = resourceMetamodel.getPrimitiveFields(resourceName);
                            collectionFields = resourceMetamodel.getCollectionFields(resourceName);
                        }
                        LinkedHashMap<Action, List<String>> actionFields = actionUtils.getActionFieldsCombinations(primitiveFields, collectionFields);
                        for (Action action : actionFields.keySet()) {
                            if (actionFields.get(action) != null) {
                                createActionNode(action, actionFields.get(action), entityNode, entityField, selectedPermissions);
                            }
                        }
                        //add "special" act as user action to User and UserGroup
                        boolean userClazz=resourceMetamodel.getEntityClassNameByResourceName(resourceName).equals(User.class.getName());
                        boolean userGroupClazz=resourceMetamodel.getEntityClassNameByResourceName(resourceName).equals(UserGroup.class.getName());
                        if (userClazz || userGroupClazz){
                            createActionNode(actionFactory.createAction(ActionConstants.ACT_AS), new ArrayList<String>(), entityNode, entityField, selectedPermissions);
                        }
                        propagateNodePropertiesTo(entityNode);
                    }
                }
                propagateNodePropertiesTo(moduleNode);
            }

        }
        return root;
    }

    /**
     * create action node with fields
     * 
     * @param fields
     * @param entityNode
     * @param entityField
     * @param selectedPermissions
     * @param actions
     */
    private void createActionNode(Action action, List<String> fields, TreeNode entityNode, EntityField entityField, Collection<Permission> selectedPermissions) {
        EntityAction entityAction = (EntityAction) action;
        TreeNodeModel actionNodeModel = new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction);
        if (isGranted(ActionConstants.GRANT, entityField) || isGranted(ActionConstants.REVOKE, entityField)) {
            DefaultTreeNode actionNode = new DefaultTreeNode("action", actionNodeModel, entityNode);
            actionNode.setExpanded(true);
            if (!fields.isEmpty()) {
                // fields
                createFieldNodes(fields, actionNode, entityAction, entityField, selectedPermissions);
                // if no field could be attached => grant is not permitted, detach actionNode
                if (actionNode.getChildCount() == 0) {
                    actionNode.getParent().getChildren().remove(actionNode);// setParent(null);
                }
            } else {
                Permission found = permissionDataAccess.findPermission(new ArrayList<Permission>(selectedPermissions), entityAction, entityField);
                if (found == null) {
                    // remove actionNode if entity cannot be granted
                    if (!isGranted(ActionConstants.GRANT, entityField)) {
                        // actionNode.setParent(null);
                        // actionNode = null;
                        entityNode.getChildren().remove(actionNode);
                    }
                } else {
                    actionNode.setSelected(true);
                    actionNode.setSelectable(isGranted(ActionConstants.REVOKE, entityField));
                }
            }
            propagateNodePropertiesTo(actionNode);
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

            if (isGranted(ActionConstants.GRANT, entityFieldWithField) || isGranted(ActionConstants.REVOKE, entityFieldWithField)) {
                DefaultTreeNode fieldNode = new DefaultTreeNode("field", null, actionNode);
                TreeNodeModel fieldNodeModel = new TreeNodeModel(field, TreeNodeModel.ResourceType.FIELD, entityFieldWithField);
                // decide how field node should be marked (red/blue/green)
                Permission foundWithField = permissionDataAccess.findPermission(new ArrayList<Permission>(selectedPermissions), entityAction, entityFieldWithField);
                Permission foundWithoutField = permissionDataAccess.findPermission(new ArrayList<Permission>(selectedPermissions), entityAction, entityField);

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
                    // if fieldNode is not granted and cannot be granted -> remove
                    if (!isGranted(ActionConstants.GRANT, entityField)) {
                        fieldNode.getParent().getChildren().remove(fieldNode);
                    }
                }
                fieldNode.setData(fieldNodeModel);
                fieldNode.setExpanded(true);
            }
        }
    }
}
