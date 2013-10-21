/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.AbstractPermission;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.PermissionHandlingBaseBean;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.SecurityBaseBean;
import com.blazebit.security.web.bean.UserSession;
import com.blazebit.security.web.bean.model.NodeModel;
import com.blazebit.security.web.bean.model.NodeModel.Marking;
import com.blazebit.security.web.bean.model.NodeModel.ResourceType;
import com.blazebit.security.web.bean.model.UserModel;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.impl.UserGroupService;
import com.blazebit.security.web.service.impl.UserService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "groupUsersBean")
@ViewScoped
public class GroupUsersBean extends PermissionHandlingBaseBean implements PermissionView, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Inject
    private UserService userService;
    @Inject
    private UserGroupService userGroupService;
    @Inject
    private RoleService roleService;

    private List<User> users = new ArrayList<User>();
    private List<UserModel> userList = new ArrayList<UserModel>();

    @Inject
    private UserSession userSesssion;

    private DefaultTreeNode permissionRoot;
    private DefaultTreeNode permissionTreeViewRoot;
    private List<Permission> selectedGroupPermissions = new ArrayList<Permission>();
    private List<Permission> selectedGroupParentPermissions = new ArrayList<Permission>();
    private String wizardStep;

    public void init() {
        if (getSelectedGroup() != null) {
            initUsers();
            initPermissions();
        }
        wizardStep = "users";
    }

    private void initPermissions() {
        selectedGroupPermissions = permissionManager.getAllPermissions(getSelectedGroup());
        selectedGroupParentPermissions.clear();

        List<UserGroup> parents = new ArrayList<UserGroup>();
        UserGroup parent = getSelectedGroup().getParent();
        parents.add(getSelectedGroup());
        while (parent != null) {
            parents.add(0, parent);
            parent = parent.getParent();
        }
        this.permissionTreeViewRoot = new DefaultTreeNode("root", null);
        DefaultTreeNode groupNode = permissionTreeViewRoot;
        for (UserGroup group : parents) {
            groupNode = new DefaultTreeNode(new NodeModel(group.getName(), ResourceType.USERGROUP, group), groupNode);
            groupNode.setExpanded(true);
            List<Permission> permissions = permissionManager.getAllPermissions(group);
            selectedGroupParentPermissions.addAll(permissions);
            getPermissionTree(permissions, groupNode);
        }
        ((NodeModel) groupNode.getData()).setMarking(Marking.GREEN);

    }

    private void initUsers() {
        List<User> allUsers = userService.findUsers(userSession.getSelectedCompany());
        users = userGroupService.getUsersFor(getSelectedGroup());
        userList.clear();
        for (User user : allUsers) {
            userList.add(new UserModel(user, users.contains(user)));
        }
    }

    /**
     * wizard listener
     * 
     * @param event
     * @return
     */
    public String userWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("users")) {
            processSelectedUsers();
        }
        return event.getNewStep();
    }

    /**
     * wizard step 1
     */
    private void processSelectedUsers() {
        permissionRoot = new DefaultTreeNode();

        for (UserModel userModel : userList) {
            if (userModel.isSelected()) {
                // user is selected
                TreeNode userNode = createUserNode(userModel.getUser(), true);
                // mark user as new -> green
                if (!users.contains(userModel.getUser())) {
                    ((NodeModel) userNode.getData()).setMarking(Marking.GREEN);
                }

            } else {
                if (users.contains(userModel.getUser())) {
                    // user will be removed from group-> mark it red
                    TreeNode userNode = createUserNode(userModel.getUser(), false);
                    ((NodeModel) userNode.getData()).setMarking(Marking.RED);
                }
            }
        }
    }

    private TreeNode createUserNode(User user, boolean addedUser) {
        NodeModel userNodeModel = new NodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, permissionRoot);
        userNode.setExpanded(addedUser);
        userNode.setSelectable(false);
        createPermissionNode(userNode, addedUser);
        return userNode;
    }

    private void createPermissionNode(DefaultTreeNode userNode, boolean addedUser) {
        NodeModel userNodeModel = (NodeModel) userNode.getData();
        List<Permission> userPermissions = permissionManager.getPermissions((User) userNodeModel.getTarget());
        Map<String, List<Permission>> permissionMapByEntity = addedUser ? groupPermissionsByEntity(userPermissions,
                                                                                                   filterOutGrantablePermissions((User) userNodeModel.getTarget(),
                                                                                                                                 selectedGroupParentPermissions)) : groupPermissionsByEntity(userPermissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = new EntityField(entity, "");
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entity, NodeModel.ResourceType.ENTITY, entityField), userNode);
            entityNode.setExpanded(true);
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                for (Permission permission : permissionsByAction) {
                    if (!((EntityField) permission.getResource()).isEmptyField()) {
                        // decide marking
                        Marking marking = Marking.NONE;
                        if (!addedUser && contains(selectedGroupParentPermissions, permission)) {
                            marking = Marking.RED;
                        } else {
                            if (!contains(userPermissions, permission) && contains(selectedGroupParentPermissions, permission)) {
                                marking = Marking.GREEN;
                            }
                        }
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(((EntityField) permission.getResource()).getField(), NodeModel.ResourceType.FIELD, permission.getResource(),
                            marking), actionNode);
                        fieldNode.setSelected(addedUser || (!addedUser && !contains(selectedGroupParentPermissions, permission)));
                        fieldNode.setSelectable(!contains(userPermissions, permission) || !addedUser);
                    } else {
                        // mark actionNode if needed
                        Permission actionPermission = permissionFactory.create(entityAction, entityField);
                        Marking marking = Marking.NONE;
                        if (!addedUser && contains(selectedGroupParentPermissions, actionPermission)) {
                            marking = Marking.RED;
                        } else {
                            if (addedUser && !contains(userPermissions, actionPermission) && contains(selectedGroupParentPermissions, actionPermission)) {
                                marking = Marking.GREEN;
                            }
                        }
                        ((NodeModel) actionNode.getData()).setMarking(marking);
                        actionNode.setSelected(addedUser || (!addedUser && !contains(selectedGroupParentPermissions, actionPermission)));
                        actionNode.setSelectable(!contains(userPermissions, actionPermission) || !addedUser);
                    }
                }
                propagateSelectionAndMarkingUp(actionNode, null);
            }
            propagateSelectionAndMarkingUp(entityNode, null);
        }
    }

    private Collection<Permission> filterOutGrantablePermissions(User target, List<Permission> selectedPermissions) {
        Set<Permission> ret = new HashSet<Permission>();
        for (Permission permission : selectedPermissions) {
            if (permissionDataAccess.isGrantable(target, permission.getAction(), permission.getResource())) {
                ret.add(permission);
            }
        }
        return ret;
    }

    /**
     * confirm button
     */
    public void confirm() {
        for (TreeNode userNode : permissionRoot.getChildren()) {
            NodeModel userNodeModel = (NodeModel) userNode.getData();
            if (!ResourceType.USER.equals(userNodeModel.getType())) {
                throw new InternalError("Root node must be a user node!!!");
            }
            if (Marking.RED.equals(userNodeModel.getMarking())) {
                roleService.removeSubjectFromRole((User) userNodeModel.getTarget(), getSelectedGroup());
                revokePermissions(userNode);

            } else {
                if (Marking.GREEN.equals(userNodeModel.getMarking())) {
                    roleService.addSubjectToRole((User) userNodeModel.getTarget(), getSelectedGroup());
                }
                grantPermissions(userNode);
            }

        }
    }

    private void grantPermissions(TreeNode userNode) throws InternalError {
        NodeModel userNodeModel = (NodeModel) userNode.getData();
        for (TreeNode userNodeChild : userNode.getChildren()) {
            // find unselected permissions=> they have to be revoked
            if (userNodeChild.isSelected() && userNodeChild.isSelectable()) {
                NodeModel userNodeChildModel = (NodeModel) userNodeChild.getData();
                if (!ResourceType.ENTITY.equals(userNodeChildModel.getType())) {
                    throw new InternalError("User node must be followed by entity node!!!");
                }
                for (TreeNode actionNode : userNodeChild.getChildren()) {
                    NodeModel actionNodeModel = (NodeModel) actionNode.getData();
                    if (actionNode.getChildCount() == 0) {
                        permissionService.grant(userSession.getUser(), (User) userNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget(),
                                                (EntityField) userNodeChildModel.getTarget());
                    } else {
                        for (TreeNode fieldNode : actionNode.getChildren()) {
                            NodeModel fieldNodeModel = (NodeModel) fieldNode.getData();
                            if (fieldNode.isSelected()) {
                                // possible that user already has permission for the entity, check if
                                // field permission is grantable
                                if (permissionDataAccess.isGrantable((User) userNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget(),
                                                                     (EntityField) fieldNodeModel.getTarget())) {
                                    permissionService.grant(userSession.getUser(), (User) userNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget(),
                                                            (EntityField) fieldNodeModel.getTarget());
                                }
                            }

                        }
                    }

                }

            }

        }
    }

    private void revokePermissions(TreeNode userNode) throws InternalError {
        NodeModel userNodeModel = (NodeModel) userNode.getData();
        for (TreeNode userNodeChild : userNode.getChildren()) {
            // find unselected permissions=> they have to be revoked
            if (!userNodeChild.isSelected() && userNodeChild.isSelectable()) {
                NodeModel userNodeChildModel = (NodeModel) userNodeChild.getData();
                if (!ResourceType.ENTITY.equals(userNodeChildModel.getType())) {
                    throw new InternalError("User node must be followed by entity node!!!");
                }
                TreeNode entityNode = userNodeChild.getParent();
                NodeModel entityNodeModel = (NodeModel) entityNode.getData();
                for (TreeNode actionNode : userNodeChild.getChildren()) {
                    NodeModel actionNodeModel = (NodeModel) actionNode.getData();
                    if (!actionNode.isSelected() && actionNode.isSelectable()) {
                        // if no children-> revoke permission(action, resource)
                        if (actionNode.getChildCount() == 0) {
                            if (permissionDataAccess.isRevokable((User) userNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget(),
                                                                 (EntityField) entityNodeModel.getTarget())) {
                                permissionService.revoke(userSession.getUser(), (User) userNodeModel.getTarget(), (Action) actionNodeModel.getTarget(),
                                                         (EntityField) entityNodeModel.getTarget());
                            }
                        } else {
                            for (TreeNode fieldNode : userNodeChild.getChildren()) {
                                NodeModel fieldNodeModel = (NodeModel) fieldNode.getData();
                                // fields -> revoke unselected fields
                                if (!fieldNode.isSelected() && fieldNode.isSelectable()) {
                                    if (permissionDataAccess.isRevokable((User) userNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget(),
                                                                         (EntityField) fieldNodeModel.getTarget()))
                                        permissionService.revoke(userSession.getUser(), (User) userNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget(),
                                                                 (EntityField) fieldNodeModel.getTarget());
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    public UserGroup getSelectedGroup() {
        return userSession.getSelectedUserGroup();
    }

    public DefaultTreeNode getPermissionRoot() {
        return permissionRoot;
    }

    @Override
    public TreeNode getPermissionViewRoot() {
        return permissionTreeViewRoot;
    }

    public List<UserModel> getUserList() {
        return userList;
    }

    public String getWizardStep() {
        return wizardStep;
    }

    public void setWizardStep(String wizardStep) {
        this.wizardStep = wizardStep;
    }

}
