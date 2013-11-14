/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.deltaspike.core.util.StringUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.ResourceFactory;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.ResourceNameExtension.EntityResource;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.service.api.ActionUtils;
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
                        TreeNodeModel entityNodeModel = (TreeNodeModel) entityNode.getData();
                        // collection field resources are stored as entity field resources
                        if (!actionUtils.getActionsForCollectionField().contains(permissionNodeData.getTarget())
                            && allChildFieldsListed(permissionNode, ((TreeNodeModel) entityNode.getData()).getName())) {
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
                        Permission fieldPermission = permissionFactory.create((EntityAction) actionNodeData.getTarget(), ((EntityField) permissionNodeData.getTarget()));
                        ret.add(fieldPermission);
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
    protected TreeNode getPermissionTree(TreeNode root, List<Permission> permissions) {
        return getPermissionTree(root, permissions, new HashSet<Permission>(), Marking.NONE);
    }

    /**
     * builds a simple, not selectable permission tree from the given permission list
     * 
     * @param permissions
     * @return
     */
    protected TreeNode getPermissionTree(List<Permission> permissions) {
        return getPermissionTree(permissions, new HashSet<Permission>(), Marking.NONE);
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
    protected TreeNode getPermissionTree(List<Permission> permissions, Set<Permission> selectedPermissions, Marking marking) {
        TreeNode root = new DefaultTreeNode();
        return getPermissionTree(root, permissions, selectedPermissions, marking);
    }

    /**
     * builds the simplest permission tree from the given permission list with the given root. object permissions have tooltip
     * and marking
     * 
     * @param permissions
     * @param permissionRoot
     */
    protected TreeNode getPermissionTree(TreeNode root, List<Permission> permissions, Set<Permission> selectedPermissions, Marking marking) {
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            // entity node
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), root);
            entityNode.setExpanded(true);
            entityNode.setSelectable(false);

            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
            for (Action action : permissionMapByAction.keySet()) {
                // action node
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);
                actionNode.setSelectable(false);
                List<Permission> permissionsByAction = permissionMapByAction.get(action);

                for (Permission permission : permissionsByAction) {

                    if (!StringUtils.isEmpty(((EntityField) permission.getResource()).getField())) {
                        // field node
                        createFieldNode(selectedPermissions, marking, actionNode, permission);
                    } else {
                        // no field nodes -> fix action node
                        adjustActionNode(selectedPermissions, marking, actionNode, permission);
                    }
                }
                propagateNodePropertiesTo(actionNode);
            }
            propagateNodePropertiesTo(entityNode);
        }
        return root;
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
    private void adjustActionNode(Set<Permission> selectedPermissions, Marking marking, DefaultTreeNode actionNode, Permission permission) {
        TreeNodeModel actioneNodeModel = (TreeNodeModel) actionNode.getData();
        if (permission.getResource() instanceof EntityObjectField) {
            actioneNodeModel.setMarking(Marking.OBJECT);
            actioneNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
        }
        // entity without field permission -> dont create node but mark action if permission is marked
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
    private void createFieldNode(Set<Permission> selectedPermissions, Marking marking, DefaultTreeNode actionNode, Permission permission) {
        DefaultTreeNode fieldNode = new DefaultTreeNode(new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD,
            permission.getResource(), Marking.NONE), actionNode);
        fieldNode.setSelectable(false);
        TreeNodeModel fieldNodeModel = (TreeNodeModel) fieldNode.getData();
        if (permission.getResource() instanceof EntityObjectField) {
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
    protected DefaultTreeNode getSelectablePermissionTree(List<Permission> permissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        DefaultTreeNode root = new DefaultTreeNode();
        return getSelectablePermissionTree(root, permissions, selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking);
    }

    /**
     * builds a selectable permission tree with the give rootm from the given permission list and marks the selected and not
     * selected permissions respectively
     * 
     * @param permissions
     * @param selectedPermissions
     * @param notSelectedPermissions
     */
    protected DefaultTreeNode getSelectablePermissionTree(DefaultTreeNode root, List<Permission> permissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking selectedMarking, Marking notSelectedMarking) {
        // group permissions by entity
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions);

        for (String entity : permissionMapByEntity.keySet()) {
            List<Permission> permissionGroup = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            // create entity node
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), root);
            entityNode.setExpanded(true);
            // group again by action
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionGroup);
            for (Action action : permissionMapByAction.keySet()) {
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                // create action node
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);

                for (Permission permission : permissionsByAction) {
                    // add entity fields if there are any
                    if (!((EntityField) permission.getResource()).isEmptyField()) {
                        createFieldNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, actionNode, permission);
                    } else {
                        // no field nodes -> fix action node
                        adjustActionNode(selectedPermissions, notSelectedPermissions, selectedMarking, notSelectedMarking, entityField, entityAction, actionNode);
                    }
                }

                addRevokedFields(notSelectedPermissions, entity, entityField, permissionsByAction, actionNode);

                propagateNodePropertiesTo(actionNode);
            }
            propagateNodePropertiesTo(entityNode);
        }
        return root;
    }

    private void adjustActionNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking marking1, Marking marking2, EntityField entityField, EntityAction entityAction, DefaultTreeNode actionNode) {
        Permission entityPermission = permissionFactory.create(entityAction, entityField);
        TreeNodeModel actionNodeModel = ((TreeNodeModel) actionNode.getData());

        if (contains(selectedPermissions, entityPermission, false)) {
            ((TreeNodeModel) actionNode.getData()).setMarking(marking1);
            actionNode.setSelected(true);
            // actionNode.setSelectable(isGranted(ActionConstants.GRANT, entityField));
            if (entityPermission.getResource() instanceof EntityObjectField) {
                actionNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
            }
        } else {
            if (contains(notSelectedPermissions, entityPermission)) {
                actionNode.setSelected(false);
                actionNodeModel.setMarking(marking2);
            } else {
                actionNode.setSelected(true);
                actionNode.setSelectable(false);
            }
        }
    }

    private void createFieldNode(Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, Marking marking1, Marking marking2, DefaultTreeNode actionNode, Permission permission) {
        TreeNodeModel fieldNodeModel = new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD, permission.getResource(),
            selectedPermissions.contains(permission) ? marking1 : Marking.NONE);
        DefaultTreeNode fieldNode = new DefaultTreeNode(fieldNodeModel, actionNode);
        // TODO add helper which group is it from

        // mark and select permission on field level-> will be propagated upwards at the end
        if (contains(selectedPermissions, permission)) {
            fieldNode.setSelected(true);
        } else {
            if (contains(notSelectedPermissions, permission)) {
                fieldNode.setSelected(false);
                ((TreeNodeModel) fieldNode.getData()).setMarking(marking2);
            } else {
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
