/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    protected TreeNode rebuildCurrentTree(List<Permission> allPermissions, Set<Permission> selectedPermissions, Set<Permission> prevRevoked, Set<Permission> prevReplaced, boolean hideFieldLevel) {
        DefaultTreeNode root = new DefaultTreeNode();
        return rebuildCurrentTree(root, allPermissions, selectedPermissions, prevRevoked, prevReplaced, hideFieldLevel);
    }

    protected TreeNode rebuildCurrentTree(TreeNode node, List<Permission> allPermissions, Set<Permission> selectedPermissions, Set<Permission> prevRevoked, Set<Permission> prevReplaced, boolean hideFieldLevel) {
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

        return getImmutablePermissionTree(node, userPermissions, userDataPermissions, removablePermissions, new HashSet<Permission>(), Marking.REMOVED, Marking.NONE,
                                          hideFieldLevel);
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

                        if (!actionUtils.getUpdateActionsForCollectionField().contains(permissionNodeData.getTarget())
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

    protected TreeNode getMutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean hideFieldLevel) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, true, hideFieldLevel);
    }

    protected TreeNode getMutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {

        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, true, false);
    }

    protected TreeNode getMutablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, true, false);
    }

    protected TreeNode getMutablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean hideFieldLevel) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, true, hideFieldLevel);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, false, false);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean hideFieldLevel) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, false, hideFieldLevel);
    }

    protected TreeNode getImmutablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, false, false);
    }

    protected TreeNode getImmutablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean hideFieldLevel) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, false, hideFieldLevel);
    }

    protected TreeNode getImmutablePermissionTree(DefaultTreeNode groupNode, List<Permission> permissions, List<Permission> dataPermissions) {
        return getPermissionTree(groupNode, permissions, dataPermissions, new HashSet<Permission>(), new HashSet<Permission>(), Marking.NONE, Marking.NONE, false, false);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking selectedMarking) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, new HashSet<Permission>(), selectedMarking, Marking.NONE, false, false);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking selectedMarking, boolean hideFieldLevel) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, new HashSet<Permission>(), selectedMarking, Marking.NONE, false, hideFieldLevel);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, boolean hideFieldLevel) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, new HashSet<Permission>(), new HashSet<Permission>(), Marking.NONE, Marking.NONE, false, hideFieldLevel);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, new HashSet<Permission>(), new HashSet<Permission>(), Marking.NONE, Marking.NONE, false, false);
    }

    protected TreeNode getImmutablePermissionTree(DefaultTreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking selectedMarking) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, new HashSet<Permission>(), selectedMarking, Marking.NONE, false, false);
    }

    protected TreeNode getImmutablePermissionTree(DefaultTreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking selectedMarking, boolean hideFieldLevel) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, new HashSet<Permission>(), selectedMarking, Marking.NONE, false, hideFieldLevel);
    }

    protected TreeNode getImmutablePermissionTree(DefaultTreeNode root, List<Permission> permissions, List<Permission> dataPermissions, boolean hideFieldLevel) {
        return getPermissionTree(root, permissions, dataPermissions, new HashSet<Permission>(), new HashSet<Permission>(), Marking.NONE, Marking.NONE, false, hideFieldLevel);
    }

    private TreeNode getPermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean selectable, boolean hideFieldLevel) {
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
                             selectable, hideFieldLevel);
        }
        for (String entity : dataPermissionMapByEntity.keySet()) {
            List<Permission> permissionsByEntity = dataPermissionMapByEntity.get(entity);
            // create entity node
            createEntityNode(root, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, entity, new ArrayList<Permission>(), permissionsByEntity,
                             selectable, hideFieldLevel);
        }
        if (root.getChildCount() == 0) {
            new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), root).setSelectable(false);
        }
        return root;
    }

    private void createEntityNode(TreeNode root, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, String entity, List<Permission> permissionsByEntity, List<Permission> dataPermisssionsByEntity, boolean selectable, boolean hideFieldLevel) {
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
                             dataPermissionsByAction, action, selectable, hideFieldLevel);
        }

        for (Action action : dataPermissionMapByAction.keySet()) {
            List<Permission> dataPermissionsByAction = dataPermissionMapByAction.get(action);
            createActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, entity, entityField, entityNode, new ArrayList<Permission>(),
                             dataPermissionsByAction, action, selectable, hideFieldLevel);
        }
        propagateNodePropertiesTo(entityNode);
    }

    private void createActionNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, String entity, EntityField entityField, TreeNode entityNode, List<Permission> permissionsByAction, List<Permission> dataPermissionsByAction, Action action, boolean selectable, boolean hideFieldLevel) {
        // create action node
        EntityAction entityAction = (EntityAction) action;
        TreeNodeModel actionNodeModel = new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction);
        DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel, entityNode);
        actionNode.setExpanded(true);
        // actionNodeModel.setEntityInstance(entityField);
        createFieldNodes(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, permissionsByAction, actionNode, selectable, hideFieldLevel);
        createFieldNodes(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, dataPermissionsByAction, actionNode, selectable, hideFieldLevel);
        addRevokedFields(notSelectedPermissions, entity, entityField, permissionsByAction, actionNode);
        propagateNodePropertiesTo(actionNode);
    }

    private void createFieldNodes(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, List<Permission> permissionsByAction, DefaultTreeNode actionNode, boolean selectable, boolean hideFieldLevel) {
        Map<String, List<Permission>> fieldPermissions = resourceUtils.groupEntityPermissionsByField(permissionsByAction);
        for (String field : fieldPermissions.keySet()) {
            List<Permission> permissionsByField = fieldPermissions.get(field);
            // take always just one! at least one for sure exists, otherwise it wouldnt be in the map
            Permission permission = permissionsByField.get(0);

            // add entity fields if there are any
            if (!((EntityField) permission.getResource()).isEmptyField()) {
                if (hideFieldLevel) {
                    ((TreeNodeModel) actionNode.getData()).setMarking(Marking.OBJECT);
                    ((TreeNodeModel) actionNode.getData()).setTooltip(Constants.CONTAINS_FIELDS);
                    adjustActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, actionNode, permission, selectable);
                } else {
                    createFieldNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, actionNode, permission, selectable);
                }
            } else {
                // no field nodes -> fix action node
                adjustActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, actionNode, permission, selectable);
            }
        }
    }

    private void adjustActionNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, DefaultTreeNode actionNode, Permission permission, boolean selectable) {

        TreeNodeModel actionNodeModel = ((TreeNodeModel) actionNode.getData());
        actionNodeModel.setEntityInstance((EntityField) permission.getResource());

        if (permission.getResource() instanceof EntityObjectField) {
            actionNodeModel.getObjectInstances().add((EntityObjectField) permission.getResource());
            actionNodeModel.setMarking(Marking.OBJECT);
            if (StringUtils.isEmpty(actionNodeModel.getTooltip())) {
                actionNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
            } else {
                actionNodeModel.setTooltip(new StringBuilder().append(actionNodeModel.getTooltip()).append("<br/>").append(Constants.CONTAINS_OBJECTS).toString());
            }
        }

        if (permissionHandling.contains(selectedPermissions, permission, false)) {
            actionNodeModel.setMarking(selectedMarking);
            actionNode.setSelected(true);
            actionNode.setSelectable(selectable);

        } else {
            if (permissionHandling.contains(notSelectedPermissions, permission)) {
                actionNode.setSelected(false);
                actionNodeModel.setMarking(notSelectedMarking);
                actionNode.setSelectable(selectable);
            } else {
                // already existing entity permission
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
