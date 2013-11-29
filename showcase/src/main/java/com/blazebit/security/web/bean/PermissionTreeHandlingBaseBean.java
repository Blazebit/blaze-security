/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * 
 * @author cuszk
 */
public class PermissionTreeHandlingBaseBean extends PermissionHandlingBaseBean {

    // TODO enough size check?
    protected boolean allChildFieldsListed(TreeNode actionNode, String entityName) {
        if (actionNode.getChildCount() == 0) {
            return true;
        }
        List<String> fields;
        try {
            fields = resourceMetamodel.getPrimitiveFields(entityName);
            return actionNode.getChildCount() == fields.size();
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    protected Set<Permission> getSelectedPermissions(TreeNode[] selectedPermissionNodes) {
        return getSelectedPermissions(selectedPermissionNodes, null);
    }

    protected TreeNode rebuildCurrentTree(List<Permission> allPermissions, Set<Permission> selectedPermissions, Set<Permission> prevRevoked, Set<Permission> prevReplaced) {
        DefaultTreeNode root = new DefaultTreeNode();
        return rebuildCurrentTree(root, allPermissions, selectedPermissions, prevRevoked, prevReplaced);
    }

    protected TreeNode rebuildCurrentTree(TreeNode node, List<Permission> allPermissions, Set<Permission> selectedPermissions, Set<Permission> prevRevoked, Set<Permission> prevReplaced) {
        List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(allPermissions).get(0);
        List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(allPermissions).get(1);

        // add back previously replaced
        for (Permission replacedPermission : prevReplaced) {
            if (!permissionHandling.implies(selectedPermissions, replacedPermission)) {
                selectedPermissions.add(replacedPermission);
            }
        }
        Set<Permission> revoked = new HashSet<Permission>();
        // add back previously revoked
        for (Permission revokedPermission : prevRevoked) {
            if (!permissionHandling.implies(selectedPermissions, revokedPermission)) {
                revoked.add(revokedPermission);
            }
        }
        Set<Permission> replaced = permissionHandling.getReplacedByGranting(allPermissions, selectedPermissions);
        List<Set<Permission>> revoke = permissionHandling.getRevokableFromSelected(userPermissions, concat(userPermissions, selectedPermissions));
        revoked.addAll(revoke.get(0));
        super.setNotRevoked(revoke.get(1));
        super.setNotGranted(new HashSet<Permission>());

        Set<Permission> removablePermissions = new HashSet<Permission>();
        removablePermissions.addAll(revoked);
        removablePermissions.addAll(replaced);
        // current permission tree

        return getPermissionTree(node, userPermissions, userDataPermissions, removablePermissions, Marking.REMOVED);
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
                    TreeNodeModel entityNodeModel = (TreeNodeModel) entityNode.getData();
                    TreeNodeModel entityNodeData = (TreeNodeModel) entityNode.getParent().getData();
                    if ((parentNode != null && entityNodeData.equals(parentNode.getData())) || parentNode == null) {
                        // collection field resources are ALWAYS stored as entity field resources; all field childs selected
                        // then action node should be taken, if object resources specified action node should be taken

                        // if (!permissionNodeData.getObjectInstances().isEmpty()) {
                        // // add object resources if exist
                        // for (EntityField instance : permissionNodeData.getObjectInstances()) {
                        // Permission actionPermission = permissionFactory.create((EntityAction) permissionNodeData.getTarget(),
                        // instance);
                        // ret.add(actionPermission);
                        // }
                        // }

                        if (!actionUtils.getActionsForCollectionField().contains(permissionNodeData.getTarget())
                            && allChildFieldsListed(permissionNode, ((TreeNodeModel) entityNode.getData()).getName())) {

                            // add entity field resource
                            Permission actionPermission = permissionFactory.create((EntityAction) permissionNodeData.getTarget(), (EntityField) entityNodeModel.getTarget());
                            ret.add(actionPermission);
                        }
                    }
                    break;
                case FIELD:
                    TreeNode actionNode = permissionNode.getParent();
                    TreeNodeModel actionNodeData = (TreeNodeModel) actionNode.getData();
                    TreeNode actionEntityNode = actionNode.getParent();

                    if ((parentNode != null && actionEntityNode.getParent().getData().equals(parentNode.getData())) || parentNode == null) {
                        // for (EntityField instance : permissionNodeData.getObjectInstances()) {
                        // Permission fieldPermission = permissionFactory.create((EntityAction) actionNodeData.getTarget(),
                        // instance);
                        // ret.add(fieldPermission);
                        // }
                        Permission actionPermission = permissionFactory.create((EntityAction) actionNodeData.getTarget(), (EntityField) permissionNodeData.getTarget());
                        ret.add(actionPermission);
                    }
                    break;
                default:
                    break;
            }
        }
        return permissionHandling.getNormalizedPermissions(ret);
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
        DefaultTreeNode root = new DefaultTreeNode();
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
        return getSelectablePermissionTree(root, permissions, dataPermissions, selectedPermissions, new HashSet<Permission>(), marking, Marking.NONE, false);
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
    protected TreeNode getSelectablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean selectable) {
        TreeNode root = new DefaultTreeNode();
        return getSelectablePermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, selectable);
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
    protected TreeNode getSelectablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        TreeNode root = new DefaultTreeNode();
        return getSelectablePermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, true);
    }

    /**
     * builds a selectable permission tree with the give rootm from the given permission list and marks the selected and not
     * selected permissions respectively
     * 
     * @param permissions
     * @param selectedPermissions
     * @param notSelectedPermissions
     */
    protected TreeNode getSelectablePermissionTree(DefaultTreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        return getSelectablePermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, true);
    }

    /**
     * builds a selectable permission tree with the give rootm from the given permission list and marks the selected and not
     * selected permissions respectively
     * 
     * @param permissions
     * @param selectedPermissions
     * @param notSelectedPermissions
     */
    protected TreeNode getSelectablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean selectable) {
        // group permissions by entity
        Map<String, List<Permission>> permissionMapByEntity = resourceUtils.groupPermissionsByResourceName(permissions);
        Map<String, List<Permission>> dataPermissionMapByEntity = resourceUtils.groupPermissionsByResourceName(dataPermissions);

        for (String entity : permissionMapByEntity.keySet()) {
            List<Permission> dataPermisssionsByEntity = new ArrayList<Permission>();
            if (dataPermissionMapByEntity.containsKey(entity)) {
                dataPermisssionsByEntity = dataPermissionMapByEntity.get(entity);
                dataPermissionMapByEntity.remove(entity);
            }
            List<Permission> permissionsByEntity = permissionMapByEntity.get(entity);
            // create entity node
            createEntityNode(root, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, entity, permissionsByEntity, dataPermisssionsByEntity,
                             selectable);
        }
        for (String entity : dataPermissionMapByEntity.keySet()) {
            List<Permission> permissionsByEntity = dataPermissionMapByEntity.get(entity);
            // create entity node
            createEntityNode(root, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, entity, new ArrayList<Permission>(), permissionsByEntity,
                             selectable);
        }
        if (root.getChildCount() == 0) {
            new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), root).setSelectable(false);
        }
        return root;
    }

    private void createEntityNode(TreeNode root, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, String entity, List<Permission> permissionsByEntity, List<Permission> dataPermisssionsByEntity, boolean selectable) {
        EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
        TreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), root);
        entityNode.setExpanded(true);
        // group again by action
        Map<Action, List<Permission>> permissionMapByAction = resourceUtils.groupResourcePermissionsByAction(permissionsByEntity);
        Map<Action, List<Permission>> dataPermissionMapByAction = resourceUtils.groupResourcePermissionsByAction(dataPermisssionsByEntity);
        for (Action action : permissionMapByAction.keySet()) {
            List<Permission> dataPermissionsByAction = new ArrayList<Permission>();
            if (dataPermissionMapByAction.containsKey(action)) {
                dataPermissionsByAction = dataPermissionMapByAction.get(action);
                dataPermissionMapByAction.remove(action);
            }
            List<Permission> permissionsByAction = permissionMapByAction.get(action);
            createActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, entity, entityField, entityNode, permissionsByAction,
                             dataPermissionsByAction, action, selectable);
        }

        for (Action action : dataPermissionMapByAction.keySet()) {
            List<Permission> dataPermissionsByAction = dataPermissionMapByAction.get(action);
            createActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, entity, entityField, entityNode, new ArrayList<Permission>(),
                             dataPermissionsByAction, action, selectable);
        }
        propagateNodePropertiesTo(entityNode);
    }

    private void createActionNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, String entity, EntityField entityField, TreeNode entityNode, List<Permission> permissionsByAction, List<Permission> dataPermissionsByAction, Action action, boolean selectable) {
        // create action node
        EntityAction entityAction = (EntityAction) action;
        TreeNodeModel actionNodeModel = new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction);
        DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel, entityNode);
        actionNode.setExpanded(true);
        // actionNodeModel.setEntityInstance(entityField);

        createFieldNodes(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, permissionsByAction, actionNode, selectable);
        createFieldNodes(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, dataPermissionsByAction, actionNode, selectable);
        addRevokedFields(notSelectedPermissions, entity, entityField, permissionsByAction, actionNode);

        propagateNodePropertiesTo(actionNode);
    }

    private void createFieldNodes(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, List<Permission> permissionsByAction, DefaultTreeNode actionNode, boolean selectable) {
        Map<String, List<Permission>> fieldPermissions = resourceUtils.groupEntityPermissionsByField(permissionsByAction);
        for (String field : fieldPermissions.keySet()) {
            List<Permission> permissionsByField = fieldPermissions.get(field);
            // take always just one!
            Permission permission = permissionsByField.get(0);

            // add entity fields if there are any
            if (!((EntityField) permission.getResource()).isEmptyField()) {
                createFieldNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, actionNode, permission, selectable);
            } else {
                // no field nodes -> fix action node
                adjustActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, actionNode, permission, selectable);
            }
        }
    }

    // in this case we know that entity field has no field
    private void adjustActionNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, DefaultTreeNode actionNode, Permission permission, boolean selectable) {

        TreeNodeModel actionNodeModel = ((TreeNodeModel) actionNode.getData());
        actionNodeModel.setEntityInstance((EntityField) permission.getResource());

        if (permissionHandling.contains(selectedPermissions, permission, false)) {
            actionNodeModel.setMarking(selectedMarking);
            actionNode.setSelected(true);
            actionNode.setSelectable(selectable);

            if (permission.getResource() instanceof EntityObjectField) {
                actionNodeModel.getObjectInstances().add((EntityObjectField) permission.getResource());
                actionNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
            } else {
                actionNodeModel.setEntityInstance((EntityField) permission.getResource());
            }
        } else {
            if (permissionHandling.contains(notSelectedPermissions, permission)) {
                actionNode.setSelected(false);
                actionNodeModel.setMarking(notSelectedMarking);
                actionNode.setSelectable(selectable);

            } else {
                if (permission.getResource() instanceof EntityObjectField) {
                    actionNodeModel.getObjectInstances().add((EntityObjectField) permission.getResource());
                    actionNodeModel.setMarking(Marking.OBJECT);
                    actionNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
                } else {
                    actionNodeModel.setEntityInstance((EntityField) permission.getResource());
                }
                // already existis entity permission
                // actionNode.setSelected(true);
                actionNode.setSelectable(false);
            }
        }
    }

    private void createFieldNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking marking1, Marking marking2, DefaultTreeNode actionNode, Permission permission, boolean selectable) {
        TreeNodeModel fieldNodeModel = new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD, permission.getResource(),
            selectedPermissions.contains(permission) ? marking1 : Marking.NONE);
        fieldNodeModel.setEntityInstance((EntityField) permission.getResource());
        DefaultTreeNode fieldNode = new DefaultTreeNode(fieldNodeModel, actionNode);
        // mark and select permission on field level-> will be propagated upwards at the end
        if (permissionHandling.contains(selectedPermissions, permission)) {
            fieldNode.setSelected(true);
            fieldNode.setSelectable(selectable);
        } else {
            if (permissionHandling.contains(notSelectedPermissions, permission)) {
                fieldNode.setSelected(false);
                fieldNodeModel.setMarking(marking2);
                fieldNode.setSelectable(selectable);
            } else {
                // field resource: entity field or entity object field -> paint it blue, add tooltip, color might be overwritten
                if (permission.getResource() instanceof EntityObjectField) {
                    fieldNodeModel.getObjectInstances().add((EntityObjectField) permission.getResource());
                    fieldNodeModel.setMarking(Marking.OBJECT);
                    fieldNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
                }
                // already existing permission for this resource
                // fieldNode.setSelected(true);
                fieldNode.setSelectable(false);
            }
        }
    }

    private void addRevokedFields(Set<Permission> notSelectedPermissions, String entity, EntityField entityField, List<Permission> permissionsByAction, DefaultTreeNode actionNode) {
        List<Permission> entityResource = resourceUtils.getSeparatedPermissionsByResource(permissionsByAction).get(0);
        List<Permission> entityFieldResource = resourceUtils.getSeparatedPermissionsByResource(permissionsByAction).get(1);
        // entity permission is revoked, but separate field permissions are granted
        if ((!entityResource.isEmpty() && permissionHandling.contains(notSelectedPermissions, entityResource.get(0))) && !entityFieldResource.isEmpty()) {
            try {
                List<String> revokedFields = resourceMetamodel.getPrimitiveFields(entity);
                for (String field : revokedFields) {
                    if (!resourceUtils.findResourceWithField(entityFieldResource, field)) {
                        TreeNodeModel fieldNodeModel = new TreeNodeModel(field, TreeNodeModel.ResourceType.FIELD, entityField.getChild(field), Marking.REMOVED);
                        new DefaultTreeNode(fieldNodeModel, actionNode);
                    }
                }
            } catch (ClassNotFoundException e) {
            }
        }
    }

}
