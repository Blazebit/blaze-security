/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Named;

import org.apache.deltaspike.core.util.StringUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.util.Constants;
import com.blazebit.security.web.util.FieldUtils;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "permissionHandlingBaseBean")
@Named
public class PermissionTreeHandlingBaseBean extends PermissionHandlingBaseBean {

    protected Set<Permission> getSelectedPermissions(TreeNode[] selectedPermissionNodes) {
        return getSelectedPermissions(selectedPermissionNodes, null);
    }

    /**
     * 
     * @param selectedPermissionNodes - selected tree nodes
     * @param allFieldsListed - decides whether the permissions are selected from the resources view or from the group view. in
     *        the resources view all the fields are listed, while at the groups only the ones from the group
     * @return list of permissions
     */
    protected Set<Permission> getSelectedPermissions(TreeNode[] selectedPermissionNodes, TreeNode parentNode) {
        Set<Permission> ret = new HashSet<Permission>();
        List<TreeNode> sortedSelectedNodes = sortTreeNodesByType(selectedPermissionNodes);
        for (TreeNode permissionNode : sortedSelectedNodes) {
            TreeNodeModel permissionNodeData = (TreeNodeModel) permissionNode.getData();
            switch (permissionNodeData.getType()) {
                case ENTITY:
                    break;
                case ACTION:
                    TreeNode entityNode = permissionNode.getParent();
                    TreeNodeModel entityNodeData = (TreeNodeModel) entityNode.getParent().getData();
                    if ((parentNode != null && entityNodeData.equals(parentNode.getData())) || parentNode == null) {
                        // collection field resources are ALWAYS stored as entity field resources; all field childs selected
                        // then action node should be taken, if object resources specified action node should be taken

                        if (!permissionNodeData.getObjectInstances().isEmpty()) {
                            // add object resources if exist
                            for (EntityField instance : permissionNodeData.getObjectInstances()) {
                                Permission actionPermission = permissionFactory.create((EntityAction) permissionNodeData.getTarget(), instance);
                                ret.add(actionPermission);
                            }
                        }

                        if (!actionUtils.getActionsForCollectionField().contains(permissionNodeData.getTarget())
                            && allChildFieldsListed(permissionNode, ((TreeNodeModel) entityNode.getData()).getName())) {

                            if (permissionNodeData.getEntityInstance() != null) {
                                // add entity field resource
                                Permission actionPermission = permissionFactory.create((EntityAction) permissionNodeData.getTarget(), permissionNodeData.getEntityInstance());
                                ret.add(actionPermission);
                            }
                        }
                    }
                    break;
                case FIELD:
                    TreeNode actionNode = permissionNode.getParent();
                    TreeNodeModel actionNodeData = (TreeNodeModel) actionNode.getData();
                    TreeNode actionEntityNode = actionNode.getParent();

                    if ((parentNode != null && actionEntityNode.getParent().getData().equals(parentNode.getData())) || parentNode == null) {
                        for (EntityField instance : permissionNodeData.getObjectInstances()) {
                            Permission fieldPermission = permissionFactory.create((EntityAction) actionNodeData.getTarget(), instance);
                            ret.add(fieldPermission);
                        }
                        if (permissionNodeData.getEntityInstance() != null) {
                            Permission actionPermission = permissionFactory.create((EntityAction) actionNodeData.getTarget(), permissionNodeData.getEntityInstance());
                            ret.add(actionPermission);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return mergePermissions(ret);
    }

    /**
     * builds a simple, not selectable permission tree with a given root and permission list
     * 
     * @param root
     * @param permissions
     * @return
     */
    protected TreeNode getPermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions) {
        return getPermissionTree(root, permissions, dataPermissions, new HashSet<Permission>(), Marking.NONE);
    }

    /**
     * builds a simple, not selectable permission tree from the given permission list
     * 
     * @param permissions
     * @return
     */
    protected TreeNode getPermissionTree(List<Permission> permissions, List<Permission> dataPermissions) {
        return getPermissionTree(permissions, dataPermissions, new HashSet<Permission>(), Marking.NONE);
    }

    /**
     * 
     * builds a simple, not selectable permission tree from the given permission list and marks with the given marking the
     * selected permissions
     * 
     * @param permissions
     * @param selectedPermissions
     * @param marking
     * @return
     */
    protected TreeNode getPermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking marking) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, marking);
    }

    /**
     * builds the simplest permission tree from the given permission list with the given root. object permissions have tooltip
     * and marking
     * 
     * @param permissions
     * @param permissionRoot
     */
    protected TreeNode getPermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking marking) {
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions);
        Map<String, List<Permission>> dataPermissionMapByEntity = groupPermissionsByEntity(dataPermissions);

        for (String entity : permissionMapByEntity.keySet()) {
            // ignore entity names that appear as entity resources
            List<Permission> dataPermisssionsByEntity = new ArrayList<Permission>();
            if (dataPermissionMapByEntity.containsKey(entity)) {
                dataPermisssionsByEntity = dataPermissionMapByEntity.get(entity);
                dataPermissionMapByEntity.remove(entity);
            }
            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            createEntityNode(root, dataPermissions, selectedPermissions, marking, permissionsByEntity, dataPermisssionsByEntity, entity);
        }
        // remaining entity object resources will be attached to the end if they dont appear before
        for (String objectEntity : dataPermissionMapByEntity.keySet()) {
            List<Permission> permissionsByEntity = dataPermissionMapByEntity.get(objectEntity);
            createEntityNode(root, dataPermissions, selectedPermissions, marking, new ArrayList<Permission>(), permissionsByEntity, objectEntity);
        }
        return root;
    }

    private void createEntityNode(TreeNode root, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking marking, List<Permission> permissionsByEntity, List<Permission> dataPermissionsByEntity, String entity) {
        // entity node
        EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
        DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), root);
        entityNode.setExpanded(true);
        entityNode.setSelectable(false);

        Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
        Map<Action, List<Permission>> dataPermissionMapByAction = groupPermissionsByAction(dataPermissionsByEntity);

        // actions from 'normal' permissions
        for (Action action : permissionMapByAction.keySet()) {
            List<Permission> dataPermissionsByAction = new ArrayList<Permission>();
            if (dataPermissionMapByAction.containsKey(action)) {
                dataPermissionsByAction = dataPermissionMapByAction.get(action);
                dataPermissionMapByAction.remove(action);
            }
            List<Permission> permissionsByAction = permissionMapByAction.get(action);
            // action node
            createActionNode(dataPermissions, selectedPermissions, marking, entityNode, permissionsByAction, dataPermissionsByAction, action);
        }

        // remaining actions
        for (Action action : dataPermissionMapByAction.keySet()) {
            // action node
            List<Permission> permissionsByAction = dataPermissionMapByAction.get(action);
            createActionNode(dataPermissions, selectedPermissions, marking, entityNode, new ArrayList<Permission>(), permissionsByAction, action);
        }
        // propagate to entity
        propagateNodePropertiesTo(entityNode);
    }

    private DefaultTreeNode createActionNode(List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking marking, DefaultTreeNode entityNode, List<Permission> permissionsByAction, List<Permission> dataPermissionsByAction, Action action) {
        EntityAction entityAction = (EntityAction) action;
        DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction), entityNode);
        actionNode.setExpanded(true);
        actionNode.setSelectable(false);

        createFieldNodes(dataPermissions, selectedPermissions, marking, permissionsByAction, actionNode);
        createFieldNodes(dataPermissions, selectedPermissions, marking, dataPermissionsByAction, actionNode);

        propagateNodePropertiesTo(actionNode);

        return actionNode;
    }

    private void createFieldNodes(List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking marking, List<Permission> permissionsByAction, DefaultTreeNode actionNode) {
        for (Permission permission : permissionsByAction) {

            EntityField resource = (EntityField) permission.getResource();

            if (!StringUtils.isEmpty(resource.getField())) {
                // field node
                createFieldNode(selectedPermissions, dataPermissions, marking, actionNode, permission);
            } else {
                // no field nodes -> fix action node properties
                adjustActionNode(selectedPermissions, dataPermissions, marking, actionNode, permission);
            }
        }
    }

    /**
     * adjusts the action node with marking and tooltip
     * 
     * @param selectedPermissions
     * @param marking
     * @param actionNode
     * @param permission
     * @param actioneNodeModel
     */
    private void adjustActionNode(Set<Permission> selectedPermissions, List<Permission> dataPermissions, Marking marking, DefaultTreeNode actionNode, Permission permission) {
        TreeNodeModel actioneNodeModel = (TreeNodeModel) actionNode.getData();
        actioneNodeModel.setEntityInstance((EntityField) permission.getResource());
        // mark as object
        if (contains(dataPermissions, permission, false)) {
            actioneNodeModel.getObjectInstances().add((EntityObjectField) permission.getResource());
            actioneNodeModel.setMarking(Marking.OBJECT);
            actioneNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
        }
        // action node marked. if its object it will still have a tooltip, but marking object is overridden by selected marking!
        if (contains(selectedPermissions, permission)) {
            actioneNodeModel.setMarking(marking);
            actionNode.setSelected(true);
        }
        
    }

    /**
     * creates fieldnode
     * 
     * @param selectedPermissions
     * @param marking
     * @param actionNode
     * @param permission
     * @param actioneNodeModel
     */
    private void createFieldNode(Set<Permission> selectedPermissions, List<Permission> dataPermissions, Marking marking, DefaultTreeNode actionNode, Permission permission) {
        DefaultTreeNode fieldNode = new DefaultTreeNode(new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD,
            permission.getResource(), Marking.NONE), actionNode);
        fieldNode.setSelectable(false);
        TreeNodeModel fieldNodeModel = (TreeNodeModel) fieldNode.getData();
        fieldNodeModel.setEntityInstance((EntityField) permission.getResource());
        // add warning that it contains object permissions
        if (contains(dataPermissions, permission, false)) {
            fieldNodeModel.getObjectInstances().add((EntityObjectField) permission.getResource());
            fieldNodeModel.setMarking(Marking.OBJECT);
            fieldNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
        }
        if (contains(selectedPermissions, permission)) {
            fieldNodeModel.setMarking(marking);
            fieldNode.setSelected(true);
        }
        
    }

    /**
     * builds a selectable permission tree from the given permission list and marks the selected and not selected permissions
     * respectively
     * 
     * @param permissions
     * @param selectedPermissions
     * @param notSelectedPermissions
     * @param selectedMarking
     * @param notSelectedMarking
     * @return
     */
    protected DefaultTreeNode getSelectablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        DefaultTreeNode root = new DefaultTreeNode();
        return getSelectablePermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking);
    }

    /**
     * builds a selectable permission tree with the give rootm from the given permission list and marks the selected and not
     * selected permissions respectively
     * 
     * @param permissions
     * @param selectedPermissions
     * @param notSelectedPermissions
     */
    protected DefaultTreeNode getSelectablePermissionTree(DefaultTreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        // group permissions by entity
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions);
        Map<String, List<Permission>> dataPermissionMapByEntity = groupPermissionsByEntity(dataPermissions);

        for (String entity : permissionMapByEntity.keySet()) {
            List<Permission> dataPermisssionsByEntity = new ArrayList<Permission>();
            if (dataPermissionMapByEntity.containsKey(entity)) {
                dataPermisssionsByEntity = dataPermissionMapByEntity.get(entity);
                dataPermissionMapByEntity.remove(entity);
            }
            List<Permission> permissionsByEntity = permissionMapByEntity.get(entity);
            // create entity node
            createEntityNode(root, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, entity, permissionsByEntity, dataPermisssionsByEntity);
        }
        for (String entity : dataPermissionMapByEntity.keySet()) {
            List<Permission> permissionsByEntity = dataPermissionMapByEntity.get(entity);
            // create entity node
            createEntityNode(root, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, entity, permissionsByEntity, permissionsByEntity);
        }
        return root;
    }

    private void createEntityNode(DefaultTreeNode root, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, String entity, List<Permission> permissionsByEntity, List<Permission> dataPermisssionsByEntity) {
        EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
        DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), root);
        entityNode.setExpanded(true);
        // group again by action
        Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
        Map<Action, List<Permission>> dataPermissionMapByAction = groupPermissionsByAction(dataPermisssionsByEntity);
        for (Action action : permissionMapByAction.keySet()) {
            List<Permission> dataPermissionsByAction = new ArrayList<Permission>();
            if (dataPermissionMapByAction.containsKey(action)) {
                dataPermissionsByAction = dataPermissionMapByAction.get(action);
                dataPermissionMapByAction.remove(action);
            }
            List<Permission> permissionsByAction = permissionMapByAction.get(action);
            createActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, entity, entityField, entityNode, permissionsByAction,
                             dataPermissionsByAction, action);
        }

        for (Action action : dataPermissionMapByAction.keySet()) {
            List<Permission> dataPermissionsByAction = dataPermissionMapByAction.get(action);
            createActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, entity, entityField, entityNode, new ArrayList<Permission>(),
                             dataPermissionsByAction, action);
        }
        propagateNodePropertiesTo(entityNode);
    }

    private void createActionNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, String entity, EntityField entityField, DefaultTreeNode entityNode, List<Permission> permissionsByAction, List<Permission> dataPermissionsByAction, Action action) {
        // create action node
        EntityAction entityAction = (EntityAction) action;
        TreeNodeModel actionNodeModel = new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction);
        DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel, entityNode);
        actionNode.setExpanded(true);
        //actionNodeModel.setEntityInstance(entityField);

        createFieldNodes(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, permissionsByAction, actionNode);
        createFieldNodes(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, dataPermissionsByAction, actionNode);
        addRevokedFields(notSelectedPermissions, entity, entityField, permissionsByAction, actionNode);

        propagateNodePropertiesTo(actionNode);
    }

    private void createFieldNodes(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, List<Permission> permissionsByAction, DefaultTreeNode actionNode) {
        for (Permission permission : permissionsByAction) {
            // add entity fields if there are any
            if (!((EntityField) permission.getResource()).isEmptyField()) {
                createFieldNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, actionNode, permission);
            } else {
                // no field nodes -> fix action node
                adjustActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, actionNode, permission);
            }
        }
    }

    // in this case we know that entity field has no field
    private void adjustActionNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, DefaultTreeNode actionNode, Permission permission) {

        TreeNodeModel actionNodeModel = ((TreeNodeModel) actionNode.getData());
        actionNodeModel.setEntityInstance((EntityField) permission.getResource());

        if (contains(selectedPermissions, permission, false)) {
            actionNodeModel.setMarking(selectedMarking);
            actionNode.setSelected(true);
            if (permission.getResource() instanceof EntityObjectField) {
                actionNodeModel.getObjectInstances().add((EntityObjectField) permission.getResource());
                actionNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
            } else {
                actionNodeModel.setEntityInstance((EntityField) permission.getResource());
            }
        } else {
            if (contains(notSelectedPermissions, permission)) {
                actionNode.setSelected(false);
                actionNodeModel.setMarking(notSelectedMarking);
            } else {
                if (permission.getResource() instanceof EntityObjectField) {
                    actionNodeModel.getObjectInstances().add((EntityObjectField) permission.getResource());
                    actionNodeModel.setMarking(Marking.OBJECT);
                    actionNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
                } else {
                    actionNodeModel.setEntityInstance((EntityField) permission.getResource());
                }
                actionNode.setSelected(true);
                actionNode.setSelectable(false);
            }
        }
    }

    private void createFieldNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking marking1, Marking marking2, DefaultTreeNode actionNode, Permission permission) {
        TreeNodeModel fieldNodeModel = new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD, permission.getResource(),
            selectedPermissions.contains(permission) ? marking1 : Marking.NONE);
        fieldNodeModel.setEntityInstance((EntityField) permission.getResource());
        DefaultTreeNode fieldNode = new DefaultTreeNode(fieldNodeModel, actionNode);
        // mark and select permission on field level-> will be propagated upwards at the end
        if (contains(selectedPermissions, permission)) {
            fieldNode.setSelected(true);
        } else {
            if (contains(notSelectedPermissions, permission)) {
                fieldNode.setSelected(false);
                fieldNodeModel.setMarking(marking2);
            } else {
                // field resource: entity field or entity object field
                if (permission.getResource() instanceof EntityObjectField) {
                    fieldNodeModel.getObjectInstances().add((EntityObjectField) permission.getResource());
                    fieldNodeModel.setMarking(Marking.OBJECT);
                    fieldNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
                }
                fieldNode.setSelected(true);
                fieldNode.setSelectable(false);
            }
        }
    }

    private void addRevokedFields(Set<Permission> notSelectedPermissions, String entity, EntityField entityField, List<Permission> permissionsByAction, DefaultTreeNode actionNode) {
        List<Permission> entityResource = filterPermissionsByEntityField(permissionsByAction).get(0);
        List<Permission> entityFieldResource = filterPermissionsByEntityField(permissionsByAction).get(1);
        // entity permission is revoked, but separate field permissions are granted
        if ((!entityResource.isEmpty() && contains(notSelectedPermissions, entityResource.get(0))) && !entityFieldResource.isEmpty()) {
            try {
                List<Field> revokedFields = FieldUtils.getPrimitiveFields(Class.forName(resourceNameExtension.getEntityResourceByResourceName(entity).getEntityClassName()));
                for (Field field : revokedFields) {
                    if (!findEntityWithFieldName(entityFieldResource, field.getName())) {
                        TreeNodeModel fieldNodeModel = new TreeNodeModel(field.getName(), TreeNodeModel.ResourceType.FIELD, entityField.getChild(field.getName()), Marking.REMOVED);
                        new DefaultTreeNode(fieldNodeModel, actionNode);
                    }
                }
            } catch (ClassNotFoundException e) {
            }
        }
    }

}
