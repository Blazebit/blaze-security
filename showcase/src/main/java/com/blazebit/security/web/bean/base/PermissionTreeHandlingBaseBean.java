/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.factory.EntityResourceFactory;
import com.blazebit.security.factory.PermissionFactory;
import com.blazebit.security.impl.data.EntityFieldResourceHandling;
import com.blazebit.security.impl.data.PermissionHandlingImpl;
import com.blazebit.security.impl.data.EntityFieldResourceHandling.PermissionFamily;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.util.Constants;

/**
 * 
 * @author cuszk
 */
public class PermissionTreeHandlingBaseBean extends TreeHandlingBaseBean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Inject
    protected EntityFieldResourceHandling resourceUtils;

    @Inject
    protected PermissionHandlingImpl permissionHandling;

    @Inject
    protected PermissionFactory permissionFactory;

    @Inject
    protected EntityResourceFactory entityFieldFactory;

    protected void selectChildrenInstances(TreeNode selectedNode, boolean select) {
        TreeNodeModel model = (TreeNodeModel) selectedNode.getData();
        for (TreeNodeModel instance : model.getInstances()) {
            instance.setSelected(select);
        }
        for (TreeNode child : selectedNode.getChildren()) {
            selectChildrenInstances(child, select);
        }

    }

    protected TreeNode buildCurrentPermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> grantable, Set<Permission> revokable, Set<Permission> replaced, boolean hideFieldLevel) {
        return getImmutablePermissionTree(permissions, dataPermissions, concat(replaced, revokable), Marking.REMOVED, hideFieldLevel);
    }

    protected TreeNode buildCurrentPermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> revokable, Set<Permission> replaced, boolean hideFieldLevel) {
        return getImmutablePermissionTree(root, permissions, dataPermissions, concat(replaced, revokable), Marking.REMOVED, hideFieldLevel);
    }

    protected TreeNode buildNewPermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> grantable, Set<Permission> revokable, Set<Permission> replaced, boolean hideFieldLevel, boolean mutable) {
        TreeNode root = new DefaultTreeNode();
        return buildNewPermissionTree(root, permissions, dataPermissions, grantable, revokable, replaced, hideFieldLevel, mutable, true);
    }

    protected TreeNode buildNewPermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> grantable, Set<Permission> revokable, Set<Permission> replaced, boolean hideFieldLevel, boolean mutable, boolean addEmptyMessage) {
        TreeNode root = new DefaultTreeNode();
        return buildNewPermissionTree(root, permissions, dataPermissions, grantable, revokable, replaced, hideFieldLevel, mutable, addEmptyMessage);
    }

    protected TreeNode buildNewDataPermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> grantable, Set<Permission> revokable, Set<Permission> replaced, boolean hideFieldLevel, boolean mutable) {
        TreeNode root = new DefaultTreeNode();
        return buildNewDataPermissionTree(root, permissions, dataPermissions, grantable, revokable, replaced, hideFieldLevel, mutable);
    }

    protected TreeNode buildNewDataPermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> grantable, Set<Permission> revokable, Set<Permission> replaced, boolean hideFieldLevel, boolean mutable) {
        // new permission tree
        List<Permission> currentDataPermissions = new ArrayList<Permission>(dataPermissions);
        currentDataPermissions = new ArrayList<Permission>(permissionHandling.removeAll(currentDataPermissions, concat(replaced, revokable)));

        // existing
        SortedMap<String, List<Permission>> entityPermissions = resourceUtils.groupPermissionsByResourceName(currentDataPermissions);
        for (String entity : entityPermissions.keySet()) {
            List<Permission> permissionsByEntity = entityPermissions.get(entity);
            createEntityNode(root, Collections.<Permission>emptySet(), Collections.<Permission>emptySet(), Marking.NONE, Marking.NONE, entity, permissionsByEntity, Collections.<Permission>emptyList(), mutable,
                             hideFieldLevel);
        }
        // granted
        entityPermissions = resourceUtils.groupPermissionsByResourceName(grantable);
        for (String entity : entityPermissions.keySet()) {
            List<Permission> permissionsByEntity = entityPermissions.get(entity);
            createEntityNode(root, new HashSet<Permission>(permissionsByEntity), Collections.<Permission>emptySet(), Marking.NEW, Marking.NONE, entity, permissionsByEntity,
                             Collections.<Permission>emptyList(), mutable, hideFieldLevel);

        }
        // revoked
        entityPermissions = resourceUtils.groupPermissionsByResourceName(revokable);
        for (String entity : entityPermissions.keySet()) {
            List<Permission> permissionsByEntity = entityPermissions.get(entity);
            createEntityNode(root, Collections.<Permission>emptySet(), new HashSet<Permission>(permissionsByEntity), Marking.NONE, Marking.REMOVED, entity, permissionsByEntity,
                             Collections.<Permission>emptyList(), mutable, hideFieldLevel);
        }
        return root;
    }

    protected TreeNode buildNewPermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> grantable, Set<Permission> revokable, Set<Permission> replaced, boolean hideFieldLevel, boolean userLevelEnabled, boolean addEmptyMessage) {
        // new permission tree
        List<Permission> currentPermissions = new ArrayList<Permission>(permissions);
        // the mutable permission tree shows the removed permissions too, therefore remove only the replaced ones
        currentPermissions = new ArrayList<Permission>(permissionHandling.removeAll(currentPermissions, replaced));
        // always separate permissions and data permissions
        currentPermissions.addAll(permissionHandling.getSeparatedPermissions(grantable).get(0));

        List<Permission> currentDataPermissions = new ArrayList<Permission>(dataPermissions);
        currentDataPermissions = new ArrayList<Permission>(permissionHandling.removeAll(currentDataPermissions, replaced));
        currentDataPermissions.addAll(permissionHandling.getSeparatedPermissions(grantable).get(1));

        if (userLevelEnabled) {
            return getMutablePermissionTree(root, currentPermissions, currentDataPermissions, grantable, revokable, Marking.NEW, Marking.REMOVED, hideFieldLevel, addEmptyMessage);
        } else {
            currentPermissions = new ArrayList<Permission>(permissionHandling.removeAll(currentPermissions, concat(replaced, revokable)));
            return getImmutablePermissionTree(root, currentPermissions, currentDataPermissions, grantable, Marking.NEW, hideFieldLevel, addEmptyMessage);
        }
    }

    protected Set<Permission> concat(Collection<Permission> current, Collection<Permission> added) {
        Set<Permission> ret = new HashSet<Permission>();
        ret.addAll(current);
        ret.addAll(added);
        return ret;
    }

    /**
     * 
     * @param actionNode
     * @param entityName
     * @return
     */
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

    /**
     * 
     * @param selectedPermissionNodes
     * @return
     */
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
                    TreeNodeModel entityNodeModel = (TreeNodeModel) entityNode.getData();
                    TreeNodeModel entityNodeData = (TreeNodeModel) entityNode.getParent().getData();
                    if ((parentNode != null && entityNodeData.equals(parentNode.getData())) || parentNode == null) {
                        // collection field resources are ALWAYS stored as entity field resources; all field childs selected
                        // then action node should be taken, if object resources specified action node should be taken

                        if (!permissionNodeData.getInstances().isEmpty()) {
                            // add object resources if exist
                            for (TreeNodeModel instance : permissionNodeData.getInstances()) {
                                if (instance.isSelected()) {
                                    Permission actionPermission = permissionFactory.create((EntityAction) permissionNodeData.getTarget(), (EntityField) instance.getTarget());
                                    ret.add(actionPermission);
                                }
                            }
                        } else {
                            if (!actionUtils.getUpdateActionsForCollectionField().contains(permissionNodeData.getTarget())
                                && allChildFieldsListed(permissionNode, ((TreeNodeModel) entityNode.getData()).getName())) {

                                // add entity field resource
                                Permission actionPermission = permissionFactory.create((EntityAction) permissionNodeData.getTarget(), (EntityField) entityNodeModel.getTarget());
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

                        if (!permissionNodeData.getInstances().isEmpty()) {
                            // add object resources if exist
                            for (TreeNodeModel instance : permissionNodeData.getInstances()) {
                                if (instance.isSelected()) {
                                    Permission fieldPermission = permissionFactory.create((EntityAction) actionNodeData.getTarget(), (EntityField) instance.getTarget());
                                    ret.add(fieldPermission);
                                }
                            }
                        } else {
                            Permission actionPermission = permissionFactory.create((EntityAction) actionNodeData.getTarget(), (EntityField) permissionNodeData.getTarget());
                            ret.add(actionPermission);
                        }
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
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, true, hideFieldLevel, true);
    }

    protected TreeNode getMutablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, true, false, true);
    }

    protected TreeNode getMutablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean hideFieldLevel) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, true, hideFieldLevel, true);
    }

    protected TreeNode getMutablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean hideFieldLevel, boolean addEmptyMessage) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, true, hideFieldLevel,
                                 addEmptyMessage);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, false, false, true);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean hideFieldLevel) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, false, hideFieldLevel, true);
    }

    protected TreeNode getImmutablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, false, false, true);
    }

    protected TreeNode getImmutablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean hideFieldLevel) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, false, hideFieldLevel, true);
    }

    protected TreeNode getImmutablePermissionTree(DefaultTreeNode groupNode, List<Permission> permissions, List<Permission> dataPermissions) {
        return getPermissionTree(groupNode, permissions, dataPermissions, new HashSet<Permission>(), new HashSet<Permission>(), Marking.NONE, Marking.NONE, false, false, true);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking selectedMarking) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, new HashSet<Permission>(), selectedMarking, Marking.NONE, false, false, true);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking selectedMarking, boolean hideFieldLevel) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, new HashSet<Permission>(), selectedMarking, Marking.NONE, false, hideFieldLevel, true);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions, boolean hideFieldLevel) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, new HashSet<Permission>(), new HashSet<Permission>(), Marking.NONE, Marking.NONE, false, hideFieldLevel, true);
    }

    protected TreeNode getImmutablePermissionTree(List<Permission> permissions, List<Permission> dataPermissions) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, dataPermissions, new HashSet<Permission>(), new HashSet<Permission>(), Marking.NONE, Marking.NONE, false, false, true);
    }

    protected TreeNode getImmutablePermissionTree(DefaultTreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking selectedMarking) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, new HashSet<Permission>(), selectedMarking, Marking.NONE, false, false, true);
    }

    protected TreeNode getImmutablePermissionTree(DefaultTreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Marking selectedMarking, boolean hideFieldLevel) {
        return getPermissionTree(root, permissions, dataPermissions, selectedPermissions, new HashSet<Permission>(), selectedMarking, Marking.NONE, false, hideFieldLevel, true);
    }

    protected TreeNode getImmutablePermissionTree(DefaultTreeNode root, List<Permission> permissions, List<Permission> dataPermissions, boolean hideFieldLevel) {
        return getPermissionTree(root, permissions, dataPermissions, new HashSet<Permission>(), new HashSet<Permission>(), Marking.NONE, Marking.NONE, false, hideFieldLevel, true);
    }

    protected TreeNode getImmutablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selected, Marking marking, boolean hideFieldLevel) {
        return getPermissionTree(root, permissions, dataPermissions, selected, new HashSet<Permission>(), marking, Marking.NONE, false, hideFieldLevel, true);
    }

    protected TreeNode getImmutablePermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selected, Marking marking, boolean hideFieldLevel, boolean addEmptyMessage) {
        return getPermissionTree(root, permissions, dataPermissions, selected, new HashSet<Permission>(), marking, Marking.NONE, false, hideFieldLevel, addEmptyMessage);
    }

    private TreeNode getPermissionTree(TreeNode root, List<Permission> permissions, List<Permission> dataPermissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, boolean selectable, boolean hideFieldLevel, boolean addEmptyMessage) {
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
        if (addEmptyMessage && root.getChildCount() == 0) {
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
        EntityAction entityAction = (EntityAction) action;
        TreeNodeModel actionNodeModel = new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction);
        DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel, entityNode);
        actionNode.setExpanded(true);
        // create field nodes
        createFieldNodes(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, permissionsByAction, actionNode, selectable, hideFieldLevel);
        createFieldNodes(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, dataPermissionsByAction, actionNode, selectable, hideFieldLevel);

        // add revoked field nodes
        addRevokedFields(notSelectedPermissions, entity, entityField, permissionsByAction, actionNode);
        addRevokedFields(notSelectedPermissions, entity, entityField, dataPermissionsByAction, actionNode);

        propagateNodePropertiesTo(actionNode);
    }

    private void createFieldNodes(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, List<Permission> permissionsByAction, DefaultTreeNode actionNode, boolean selectable, boolean hideFieldLevel) {
        Map<String, List<Permission>> fieldPermissions = resourceUtils.groupEntityPermissionsByField(permissionsByAction);
        for (String field : fieldPermissions.keySet()) {
            List<Permission> permissionsByField = fieldPermissions.get(field); // list contains at least 1 element, otherwise
                                                                               // wouldnt be in the map

            // add entity fields if there are any
            if (!field.equals(EntityField.EMPTY_FIELD)) {
                if (hideFieldLevel) {
                    TreeNodeModel actionNodeModel = (TreeNodeModel) actionNode.getData();
                    actionNodeModel.setMarking(Marking.OBJECT);
                    if (!StringUtils.isEmpty(actionNodeModel.getTooltip()) && !StringUtils.equals(actionNodeModel.getTooltip(), Constants.CONTAINS_FIELDS)) {
                        actionNodeModel.setTooltip(actionNodeModel.getTooltip() + ", " + Constants.CONTAINS_FIELDS);
                    }else{
                        actionNodeModel.setTooltip(Constants.CONTAINS_FIELDS);
                    }
                    actionNode.setType("field");
                    adjustActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, actionNode, permissionsByField, selectable);
                } else {
                    createFieldNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, actionNode, permissionsByField, selectable);
                }
            } else {
                // no field nodes -> fix action node
                adjustActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, actionNode, permissionsByField, selectable);
            }
        }
    }

    private void adjustActionNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking, DefaultTreeNode actionNode, List<Permission> permissions, boolean selectable) {
        // Permission permission = permissions.get(0);
        TreeNodeModel actionNodeModel = ((TreeNodeModel) actionNode.getData());

        for (Permission permission : permissions) {

            if (permission.getResource() instanceof EntityObjectField) {
                TreeNodeModel instanceModel = new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD, permission.getResource(),
                    permissionHandling.contains(selectedPermissions, permission));
                // instanceModel
                // .setTooltip(permissionHandling.contains(selectedPermissions, permission) ? "New!" :
                // (permissionHandling.contains(notSelectedPermissions, permission) ? "Removed!" : "Existing!"));
                actionNodeModel.getInstances().add(instanceModel);
                actionNodeModel.setMarking(Marking.OBJECT);
                if (!StringUtils.isEmpty(actionNodeModel.getTooltip()) && !StringUtils.equals(actionNodeModel.getTooltip(), Constants.CONTAINS_OBJECTS)) {
                    actionNodeModel.setTooltip(actionNodeModel.getTooltip() + ", " + Constants.CONTAINS_OBJECTS);
                }else{
                    actionNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
                }
                actionNode.setType("object");
                // object level permission cannot be optionally granted or revoked.they are just displayed for info and will be
                // granted or revoked implicitly. TODO find a better solution! at this point there could be 3 object level
                // permissions on the same treenode: 1 in added state, 1 in to be removed state and 1 in existing state. this
                // cannot be displayed in one single node.
                actionNode.setSelectable(false);
                // mark it selected so that it will be processed
                actionNode.setSelected(true);
            }
            if (permissionHandling.contains(selectedPermissions, permission)) {
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

    }

    private void createFieldNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking marking1, Marking marking2, DefaultTreeNode actionNode, List<Permission> permissions, boolean selectable) {
        // there is always only one field node added at first without any marking, so just take first permissions resource
        Permission permission = permissions.get(0);
        TreeNodeModel fieldNodeModel = new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD, permission.getResource());
        DefaultTreeNode fieldNode = new DefaultTreeNode(fieldNodeModel, actionNode);
        // field resource: entity field or entity object field -> paint it blue, add tooltip, color might be overwritten
        for (Permission fieldPermission : permissions) {

            boolean isNewPermission = permissionHandling.contains(selectedPermissions, fieldPermission);
            boolean isRemovedPermission = permissionHandling.contains(notSelectedPermissions, fieldPermission);
            if (permission.getResource() instanceof EntityObjectField) {
                TreeNodeModel instanceModel = new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD,
                    fieldPermission.getResource(), isNewPermission);

                instanceModel.setTooltip(isNewPermission ? "New!" : (isRemovedPermission ? "Removed!" : ""));

                fieldNodeModel.getInstances().add(instanceModel);
                fieldNodeModel.setMarking(Marking.OBJECT);
                fieldNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
                fieldNode.setType("object");
                // object level permission cannot be optionally granted or revoked.they are just displayed for info and will be
                // granted or revoked implicitly. TODO find a better solution! at this point there could be 3 object level
                // permissions on the same treenode: 1 in added state, 1 in to be removed state and 1 in existing state. this
                // cannot be displayed in one single node.
                fieldNode.setSelectable(false);
                // mark it selected so that it will be processed
                fieldNode.setSelected(true);
            }
            // mark and select permission on field level-> will be propagated upwards at the end
            if (isNewPermission) {
                fieldNodeModel.setMarking(marking1);
                fieldNode.setSelected(true);
                fieldNode.setSelectable(selectable);
            } else {
                if (isRemovedPermission) {
                    fieldNode.setSelected(false);
                    fieldNodeModel.setMarking(marking2);
                    fieldNode.setSelectable(selectable);
                } else {
                    // already existing permission for this resource
                    // fieldNode.setSelected(true);
                    fieldNode.setSelectable(false);
                }
            }
        }
    }

    private void addRevokedFields(Set<Permission> notSelectedPermissions, String entity, EntityField entityField, List<Permission> permissionsByAction, DefaultTreeNode actionNode) {
        PermissionFamily family = resourceUtils.getSeparatedParentAndChildEntityPermissions(permissionsByAction);
        if (family.parent != null && permissionHandling.contains(notSelectedPermissions, family.parent) && !family.children.isEmpty()) {
            // entity permission is revoked, but separate field permissions are granted
            Set<Permission> children = resourceUtils.getChildPermissions(family.parent);
            for (Permission child : children) {
                if (!permissionHandling.contains(family.children, child)) {
                    EntityField resource = (EntityField) child.getResource();
                    TreeNodeModel fieldNodeModel = new TreeNodeModel(resource.getField(), TreeNodeModel.ResourceType.FIELD, resource, Marking.REMOVED);
                    new DefaultTreeNode(fieldNodeModel, actionNode);
                }
            }
        }
    }
}
