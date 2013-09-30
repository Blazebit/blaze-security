/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityFieldFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.impl.model.AbstractPermission;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.model.GroupModel;
import com.blazebit.security.web.bean.model.NodeModel;
import com.blazebit.security.web.bean.model.NodeModel.Marking;
import com.blazebit.security.web.service.impl.UserGroupService;

/**
 * 
 * @author cuszk
 */
public class PermissionHandlingBaseBean {

    @Inject
    protected PermissionFactory permissionFactory;
    @Inject
    protected UserSession userSession;
    @Inject
    protected PermissionDataAccess permissionDataAccess;
    @Inject
    protected PermissionManager permissionManager;
    @Inject
    protected EntityFieldFactory entityFieldFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected PermissionService permissionService;

    protected boolean permissionExists(Permission permisison) {
        return permissionDataAccess.findPermission(userSession.getSelectedUser(), permisison.getAction(), permisison.getResource()) != null;
    }

    protected List<TreeNode> sortTreeNodesByType(TreeNode[] selectedTreeNodes) {
        List<TreeNode> sortedSelectedNodes = Arrays.asList(selectedTreeNodes);
        Collections.sort(sortedSelectedNodes, new Comparator<TreeNode>() {

            @Override
            public int compare(TreeNode o1, TreeNode o2) {
                NodeModel model1 = (NodeModel) o1.getData();
                NodeModel model2 = (NodeModel) o2.getData();
                if (model1.getType().ordinal() == model2.getType().ordinal()) {
                    return 0;
                } else {
                    return model1.getType().ordinal() < model2.getType().ordinal() ? -1 : 1;
                }
            }

        });
        return sortedSelectedNodes;
    }

    protected boolean contains(Collection<Permission> permissions, Permission permission) {
        return find(permissions, permission) != null;
    }

    protected Permission find(Collection<Permission> permissions, Permission permission) {
        for (Permission p : permissions) {
            AbstractPermission givenPermission = (AbstractPermission) p;
            if (givenPermission.getAction().equals(permission.getAction()) && givenPermission.getResource().equals(permission.getResource())) {
                return p;
            }
        }
        return null;
    }

    /**
     * removes given permission from a permission collection (collection contains user and usergroup permissions)
     * 
     * @param permissions
     * @param permission
     * @return
     */
    protected List<Permission> remove(List<Permission> permissions, Permission permission) {
        Permission found = find(permissions, permission);
        permissions.remove(found);
        return permissions;
    }

    /**
     * removes given permissions from a permission collection (collection contains user and usergroup permissions)
     * 
     * @param permissions
     * @param permissionsToRemove
     * @return
     */
    protected List<Permission> removeAll(List<Permission> permissions, Collection<Permission> permissionsToRemove) {
        for (Permission permission : permissionsToRemove) {
            Permission found = find(permissions, permission);
            permissions.remove(found);
        }
        return permissions;
    }

    /**
     * 
     * @param selectedPermissionNodes - selected tree nodes
     * @param allFieldsListed - decides whether the permissions are selected from the resources view or from the group view. in
     *        the resources view all the fields are listed, while at the groups only the ones from the group
     * @return list of permissions
     */
    protected Set<Permission> processSelectedPermissions(TreeNode[] selectedPermissionNodes, boolean allFieldsListed) {
        Set<Permission> ret = new HashSet<Permission>();
        List<TreeNode> sortedSelectedNodes = sortTreeNodesByType(selectedPermissionNodes);
        for (TreeNode permissionNode : sortedSelectedNodes) {
            NodeModel permissionNodeData = (NodeModel) permissionNode.getData();
            switch (permissionNodeData.getType()) {
                case ENTITY:
                    for (TreeNode actionNode : permissionNode.getChildren()) {
                        NodeModel actionNodeData = (NodeModel) actionNode.getData();
                        if (actionNode.getChildCount() == 0 || (allFieldsListed && allChildrenSelected(actionNode))) {
                            ret.add(permissionFactory.create((EntityAction) actionNodeData.getTarget(), (EntityField) permissionNodeData.getTarget()));
                        } else {
                            for (TreeNode fieldNode : actionNode.getChildren()) {
                                NodeModel fieldNodeData = (NodeModel) fieldNode.getData();
                                ret.add(permissionFactory.create((EntityAction) actionNodeData.getTarget(), (EntityField) fieldNodeData.getTarget()));
                            }
                        }
                    }
                    break;
                case ACTION:
                    TreeNode entityNode = permissionNode.getParent();
                    NodeModel entityNodeModel = (NodeModel) entityNode.getData();
                    Permission actionPermission = permissionFactory.create((EntityAction) permissionNodeData.getTarget(), (EntityField) entityNodeModel.getTarget());
                    if (permissionNode.getChildCount() == 0 || (allFieldsListed && allChildrenSelected(permissionNode))) {
                        if (!contains(ret, actionPermission)) {
                            ret.add(permissionFactory.create((EntityAction) permissionNodeData.getTarget(), (EntityField) entityNodeModel.getTarget()));
                        }
                    }
                    break;
                case FIELD:
                    TreeNode actionNode = permissionNode.getParent();
                    NodeModel actionNodeData = (NodeModel) actionNode.getData();

                    TreeNode actionEntityNode = actionNode.getParent();
                    NodeModel actionEntityNodeModel = (NodeModel) actionEntityNode.getData();
                    Permission fieldPermission = permissionFactory.create(userSession.getSelectedUser(), (EntityAction) actionNodeData.getTarget(),
                                                                          (EntityField) actionEntityNodeModel.getTarget());
                    if (!contains(ret, fieldPermission)) {
                        ret.add(permissionFactory.create(userSession.getSelectedUser(), (EntityAction) actionNodeData.getTarget(), ((EntityField) permissionNodeData.getTarget())));
                    }
                    break;
            }
        }
        return ret;
    }

    private boolean allChildrenSelected(TreeNode actionNode) {
        for (TreeNode fieldNode : actionNode.getChildren()) {
            if (!fieldNode.isSelected()) {
                return false;
            }
        }
        return true;
    }

    protected void buildPermissionViewTree(List<Permission> permissions, TreeNode permissionRoot) {
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = new EntityField(entity, "");
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entity, NodeModel.ResourceType.ENTITY, entityField), permissionRoot);
            entityNode.setExpanded(true);
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                for (Permission _permission : permissionsByAction) {
                    AbstractPermission permission = (AbstractPermission) _permission;
                    if (!permission.getResource().isEmptyField()) {
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(permission.getResource().getField(), NodeModel.ResourceType.FIELD, permission.getResource()),
                            actionNode);
                    }
                }
            }
        }
    }

    protected SortedMap<String, List<Permission>> groupPermissionsByEntity(Collection<Permission> permissions) {
        SortedMap<String, List<Permission>> ret = new TreeMap<String, List<Permission>>(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        List<Permission> group;
        for (Permission p : permissions) {
            String entityName = ((EntityField) p.getResource()).getEntity();
            if (ret.containsKey(entityName)) {
                group = ret.get(entityName);
            } else {
                group = new ArrayList<Permission>();

            }
            group.add(p);
            ret.put(entityName, group);
        }
        for (String entityName : ret.keySet()) {
            group = ret.get(entityName);
            Collections.sort(group, new Comparator<Permission>() {

                @Override
                public int compare(Permission o1, Permission o2) {
                    return ((AbstractPermission) o1).getResource().getField().compareTo(((AbstractPermission) o2).getResource().getField());
                }
            });
            ret.put(entityName, group);

        }
        return ret;
    }

    protected SortedMap<Action, List<Permission>> groupPermissionsByAction(Collection<Permission> permissions) {
        SortedMap<Action, List<Permission>> ret = new TreeMap<Action, List<Permission>>(new Comparator<Action>() {

            @Override
            public int compare(Action o1, Action o2) {
                return ((EntityAction) o1).getActionName().compareToIgnoreCase(((EntityAction) o2).getActionName());
            }
        });
        List<Permission> group;
        for (Permission p : permissions) {
            EntityAction entityAction = (EntityAction) p.getAction();
            if (ret.containsKey(entityAction)) {
                group = ret.get(entityAction);
            } else {
                group = new ArrayList<Permission>();

            }
            group.add(p);
            ret.put(entityAction, group);
        }
        for (Action entityAction : ret.keySet()) {
            group = ret.get(entityAction);
            Collections.sort(group, new Comparator<Permission>() {

                @Override
                public int compare(Permission o1, Permission o2) {
                    return ((AbstractPermission) o1).getResource().getField().compareTo(((AbstractPermission) o2).getResource().getField());
                }
            });
            ret.put(entityAction, group);

        }
        return ret;
    }

    /**
     * TODO created permissions dont have a subject/role now. take care when granting!
     * 
     * @param currentPermissions
     * @param selectedPermissions
     * @return
     */
    protected Map<String, List<Permission>> groupPermissionsByEntity(Collection<Permission> currentPermissions, Collection<Permission> selectedPermissions) {
        Map<String, List<Permission>> currentPermissionMap = groupPermissionsByEntity(currentPermissions);
        // selected permissions (existing + new - revoked)
        Map<String, List<Permission>> mergedPermissionMap = groupPermissionsByEntity(new ArrayList<Permission>(selectedPermissions));
        // merge into resource actions map
        for (String entity : currentPermissionMap.keySet()) {
            List<Permission> permissionGroup = new ArrayList<Permission>(currentPermissionMap.get(entity));
            for (Permission _permission : permissionGroup) {
                AbstractPermission permission = (AbstractPermission) _permission;
                List<Permission> temp = new ArrayList<Permission>();
                if (mergedPermissionMap.containsKey(permission.getResource().getEntity())) {
                    temp = mergedPermissionMap.get(permission.getResource().getEntity());
                } else {
                    temp = new ArrayList<Permission>();
                }
                if (permission.getResource().isEmptyField()) {
                    if (!contains(temp, permissionFactory.create(permission.getAction(), permission.getResource()))) {
                        temp.add(permissionFactory.create(permission.getAction(), permission.getResource()));
                        mergedPermissionMap.put(permission.getResource().getEntity(), temp);
                    }
                } else {
                    // check if entity is there
                    if (!contains(temp, permission)
                        && !contains(temp, permissionFactory.create(permission.getAction(), (EntityField) entityFieldFactory.createResource(permission.getResource().getEntity())))) {
                        temp.add(permissionFactory.create(permission.getAction(), permission.getResource()));
                        mergedPermissionMap.put(permission.getResource().getEntity(), temp);
                    }
                }
            }
        }
        return mergedPermissionMap;
    }

    /**
     * helper to mark parent nodes when child nodes are marked
     * 
     * @param node
     */
    protected void markAndSelectParents(DefaultTreeNode node, TreeNode[] selectedNodes) {
        if (node.getChildCount() > 0) {
            NodeModel firstChild = ((NodeModel) node.getChildren().get(0).getData());
            boolean foundDifferentMarking = false;
            boolean foundUnSelected = false;
            boolean foundSelectable = false;
            Marking firstMarking = firstChild.getMarking();
            UserGroup firstRelation = firstChild.getRelation();
            boolean foundDifferentRelation = false;
            for (TreeNode child : node.getChildren()) {
                NodeModel childNodeModel = ((NodeModel) child.getData());
                if (child.isSelectable()) {
                    foundSelectable = true;
                }
                if (!child.isSelected()) {
                    foundUnSelected = true;
                }
                if (!childNodeModel.getMarking().equals(firstMarking)) {
                    foundDifferentMarking = true;
                }
                if ((childNodeModel.getRelation() == null && firstRelation != null)
                    || (childNodeModel.getRelation() != null && !childNodeModel.getRelation().equals(firstRelation))) {
                    foundDifferentRelation = true;
                }
            }
            node.setSelectable(foundSelectable);
            NodeModel nodeModel = ((NodeModel) node.getData());
            if (!foundDifferentMarking) {
                nodeModel.setMarking(firstMarking);
            } else {
                nodeModel.setMarking(Marking.NONE);
            }
            if (!foundUnSelected) {
                if (selectedNodes != null) {
                    addToSelectedPermissionNodes(node, selectedNodes);
                }
                node.setSelected(true);
            }
            if (!foundDifferentRelation && firstRelation != null) {
                nodeModel.setRelation(firstRelation);
                nodeModel.setName(nodeModel.getName() + " (" + firstRelation.getName() + ")");
            } else {
                if (firstRelation != null) {
                    // iterate through children again and add related group name
                    for (TreeNode child : node.getChildren()) {
                        NodeModel childNodeModel = ((NodeModel) child.getData());
                        if (childNodeModel.getRelation() != null) {
                            childNodeModel.setName(childNodeModel.getName() + " (" + childNodeModel.getRelation().getName() + ")");
                        }
                    }
                }
            }
        }
    }

    /**
     * helper to mark node as selected
     * 
     * @param node
     */
    protected void addToSelectedPermissionNodes(DefaultTreeNode node, TreeNode[] selectedNodes) {
        if (selectedNodes.length == 0) {
            selectedNodes = new TreeNode[1];
            selectedNodes[0] = node;
        }
        selectedNodes[selectedNodes.length - 1] = node;
    }

    @Inject
    private UserGroupService userGroupService;

    protected void buildGroupTree(List<UserGroup> userGroups, TreeNode groupRoot) {
        buildGroupTree(userGroups, groupRoot, true);
    }

    /**
     * 
     * @param permissions
     * @param permissionRoot
     */
    protected void buildGroupTree(List<UserGroup> userGroups, TreeNode groupRoot, boolean expand) {
        List<UserGroup> parentGroups = userGroupService.getAllParentGroups();
        for (UserGroup parent : parentGroups) {
            createGroupNode(parent, userGroups, groupRoot, expand);
        }
    }

    private void createGroupNode(UserGroup group, List<UserGroup> allowedGroups, TreeNode node, boolean expand) {
        DefaultTreeNode childNode = new DefaultTreeNode(new GroupModel(group, allowedGroups.contains(group), false), node);
        childNode.setExpanded(expand);
        for (UserGroup ug : userGroupService.getGroupsForGroup(group)) {
            createGroupNode(ug, allowedGroups, childNode, expand);
        }
    }
}
