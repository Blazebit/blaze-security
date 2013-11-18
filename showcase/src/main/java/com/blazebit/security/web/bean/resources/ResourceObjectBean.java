package com.blazebit.security.web.bean.resources;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.PermissionTreeHandlingBaseBean;
import com.blazebit.security.web.bean.model.FieldModel;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.service.api.UserGroupService;
import com.blazebit.security.web.util.FieldUtils;

@Named
@ViewScoped
@Stateless
public class ResourceObjectBean extends PermissionTreeHandlingBaseBean {

    @Inject
    private UserGroupService userGroupService;

    private IdHolder selectedSubject;
    private List<RowModel> selectedObjects = new ArrayList<RowModel>();
    private List<FieldModel> selectedFields = new ArrayList<FieldModel>();
    private List<EntityAction> selectedActions = new ArrayList<EntityAction>();
    private String action = "grant";

    private TreeNode resourceRoot = new DefaultTreeNode();
    private TreeNode[] selectedResourceNodes = new TreeNode[] {};
    private TreeNode[] selectedPermissionNodes = new TreeNode[] {};
    private TreeNode[] selectedUserPermissionNodes = new TreeNode[] {};

    private TreeNode currentPermissionRoot = new DefaultTreeNode();
    private TreeNode newPermissionRoot = new DefaultTreeNode();

    private List<Permission> currentPermissions = new ArrayList<Permission>();
    private List<Permission> currentDataPermissions = new ArrayList<Permission>();

    private TreeNode currentUserPermissionRoot;
    private TreeNode newUserPermissionRoot;

    private boolean confirmed = false;
    private boolean usersConfirmed = false;
    private Set<Permission> selectedPermissions;

    public void init() {
        currentPermissions = getCurrentPermissions();
        currentDataPermissions = getCurrentDataPermissions();
        resourceRoot = new DefaultTreeNode();
        for (RowModel selectedObject : selectedObjects) {
            EntityObjectField entityObjectResource = (EntityObjectField) entityFieldFactory.createResource(selectedObject.getEntity().getClass(), selectedObject
                .getEntity()
                .getId());
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(selectedObject.getFieldSummary(), ResourceType.ENTITY, entityObjectResource), resourceRoot);
            entityNode.setExpanded(true);

            createActionNodes(selectedActions, selectedObject, entityObjectResource, entityNode);
            createActionNodes(selectedCollectionActions, selectedObject, entityObjectResource, entityNode);

            propagateNodePropertiesTo(entityNode);
        }
    }

    private void createActionNodes(List<EntityAction> selectedActions, RowModel selectedObject, EntityObjectField entityObjectResource, DefaultTreeNode entityNode) {
        for (EntityAction action : selectedActions) {
            DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(action.getActionName(), ResourceType.ACTION, action), entityNode);
            actionNode.setExpanded(true);
            // fields
            if (!selectedFields.isEmpty()) {
                // selectedFields
                List<Field> fields = FieldUtils.getPrimitiveFields(selectedObject.getEntity().getClass());
                for (Field field : fields) {
                    EntityObjectField entityObjectFieldResource = (EntityObjectField) entityFieldFactory.createResource(selectedObject.getEntity().getClass(), field.getName(),
                                                                                                                        selectedObject.getEntity().getId());
                    DefaultTreeNode fieldNode = new DefaultTreeNode(new TreeNodeModel(field.getName(), ResourceType.FIELD, entityObjectFieldResource), actionNode);

                    Permission permission = permissionFactory.create(action, entityObjectFieldResource);
                    setFieldNodeProperties(field, fieldNode, permission);
                }
            } else {
                // no fields, only action
                Permission permission = permissionFactory.create(action, entityObjectResource);
                setActionNodeProperties(action, actionNode, permission);
            }

            propagateSelectionUpwards(actionNode);
        }
    }

    private void setActionNodeProperties(EntityAction action, DefaultTreeNode actionNode, Permission permission) {
        if (this.action.equals("grant")) {
            if (implies(currentPermissions, permission)) {
                // already existing permission -> when granting dont allow user to revoke
                actionNode.setSelectable(false);
            } else {
                actionNode.setSelected(true);
                ((TreeNodeModel) actionNode.getData()).setMarking(Marking.NEW);
            }
        } else {
            if (this.action.equals("revoke")) {
                if (revokes(currentPermissions, permission)) {
                    actionNode.setSelected(true);
                    ((TreeNodeModel) actionNode.getData()).setMarking(Marking.REMOVED);
                }
            }
        }
    }

    private void setFieldNodeProperties(Field field, DefaultTreeNode fieldNode, Permission permission) {
        if (this.action.equals("grant")) {
            if (implies(currentPermissions, permission)) {
                fieldNode.setSelectable(false);
            } else {
                if (selectedFields.contains(new FieldModel(field.getName()))) {
                    fieldNode.setSelected(true);
                    ((TreeNodeModel) fieldNode.getData()).setMarking(Marking.NEW);
                }
            }
        } else {
            if (this.action.equals("revoke")) {
                if (selectedFields.contains(new FieldModel(field.getName()))) {
                    fieldNode.setSelected(true);
                    ((TreeNodeModel) fieldNode.getData()).setMarking(Marking.REMOVED);
                }
            }
        }
    }

    // wizard step
    public void processSelectedPermissions() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
        if ("grant".equals(action)) {
            Set<Permission> granted = getGrantedPermission(currentPermissions, selectedPermissions).get(0);
            super.setNotGranted(getGrantedPermission(currentPermissions, selectedPermissions).get(1));
            Set<Permission> revoked = getRevokedPermissions(currentPermissions, selectedPermissions).get(0);
            super.setNotRevoked(getRevokedPermissions(currentPermissions, selectedPermissions).get(1));
            Set<Permission> replaced = getReplacedPermissions(currentPermissions, selectedPermissions);
            Set<Permission> removedPermissions = new HashSet<Permission>(revoked);
            removedPermissions.addAll(replaced);
            // build current tree
            // revoked or replaced permissions cannot be displayed because they can only be object permissions and those cannot
            // be marked
            currentPermissionRoot = getPermissionTree(currentPermissions, currentDataPermissions, new HashSet<Permission>(), Marking.REMOVED);
            // build new tree
            List<Permission> modifiedCurrentPermissions = new ArrayList<Permission>(currentPermissions);
            modifiedCurrentPermissions.removeAll(removedPermissions);
            modifiedCurrentPermissions.addAll(granted);
            newPermissionRoot = getPermissionTree(modifiedCurrentPermissions, currentDataPermissions, selectedPermissions, Marking.NEW);
        } else {
            if ("revoke".equals(action)) {
                Set<Permission> revoked = getRevokablePermissions(selectedPermissions);
                Set<Permission> finalRevoked = revokeImpliedPermissions(currentPermissions, revoked);
                currentPermissionRoot = getPermissionTree(currentPermissions, currentDataPermissions, finalRevoked, Marking.REMOVED);
                // build new tree
                List<Permission> modifiedCurrentPermissions = new ArrayList<Permission>(currentPermissions);
                modifiedCurrentPermissions.removeAll(finalRevoked);
                newPermissionRoot = getPermissionTree(modifiedCurrentPermissions, currentDataPermissions, new HashSet<Permission>(), Marking.NONE);
            }
        }
        // reset values
        selectedFields.clear();
        selectedActions.clear();
        selectedCollectionActions.clear();
        selectedObjects.clear();
        confirmed = false;
    }

    private List<Permission> getCurrentPermissions() {
        if (selectedSubject instanceof Subject) {
            return filterPermissions(permissionManager.getPermissions((Subject) selectedSubject)).get(0);
        } else {
            return filterPermissions(permissionManager.getPermissions((Role) selectedSubject)).get(0);
        }
    }

    private List<Permission> getCurrentDataPermissions() {
        if (selectedSubject instanceof Subject) {
            return filterPermissions(permissionManager.getPermissions((Subject) selectedSubject)).get(1);
        } else {
            return filterPermissions(permissionManager.getPermissions((Role) selectedSubject)).get(1);
        }
    }

    // confirm
    public void confirmPermissions() {
        if (action.equals("grant")) {
            selectedPermissions = getSelectedPermissions(selectedResourceNodes);
            Set<Permission> granted = getGrantedPermission(currentPermissions, selectedPermissions).get(0);
            super.setNotGranted(getGrantedPermission(currentPermissions, selectedPermissions).get(1));
            Set<Permission> replaced = getReplacedPermissions(currentPermissions, selectedPermissions);
            // fix implied actions
            Set<Permission> finalGranted = grantImpliedPermissions(getCurrentPermissions(), granted);

            for (Permission permission : replaced) {
                revoke(permission);
            }
            for (Permission permission : finalGranted) {
                grant(permission);
            }
        } else {
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
            Set<Permission> revoked = getRevokablePermissions(selectedPermissions);
            // fix implied actions
            Set<Permission> finalRevoked = revokeImpliedPermissions(currentPermissions, revoked);

            for (Permission permission : finalRevoked) {
                revoke(permission);
            }
        }
        // reset permissions
        currentPermissionRoot = getPermissionTree(getCurrentPermissions(), getCurrentDataPermissions(), new HashSet<Permission>(), Marking.REMOVED);
        // build new tree
        newPermissionRoot = new DefaultTreeNode();
        // reset initial values
        confirmed = true;
        if (isGroup()) {
            propagateChangesToUsers();
        }

    }

    private void propagateChangesToUsers() {
        if (selectedSubject instanceof Role) {
            // propagate changes to users
            Set<User> users = new HashSet<User>();
            collectUsers((UserGroup) selectedSubject, users);
            List<User> sortedUsers = new ArrayList<User>(users);
            Collections.sort(sortedUsers, new Comparator<User>() {

                @Override
                public int compare(User o1, User o2) {
                    return o1.getUsername().compareToIgnoreCase(o2.getUsername());
                }

            });
            createUserPermissionTrees(sortedUsers, false);
            usersConfirmed = false;

            // if user level is not enabled confirm user permissions immediately
            if (!userSession.getSelectedCompany().isUserLevelEnabled()) {
                confirmUserPermissions();
                usersConfirmed = true;
            }
        }
    }

    private void createUserPermissionTrees(List<User> selectedUsers, boolean selectable) {
        setCurrentUserPermissionRoot(new DefaultTreeNode());
        setNewUserPermissionRoot(new DefaultTreeNode());
        for (User user : selectedUsers) {
            createCurrentUserNode(user, currentUserPermissionRoot);
            createNewUserNode(user, newUserPermissionRoot, selectable);
        }
    }

    private void createCurrentUserNode(User user, TreeNode root) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);

        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true);
        userNode.setSelectable(false);

        createCurrentUserPermissionNode(userNode, selectedPermissions);

    }

    private void createCurrentUserPermissionNode(DefaultTreeNode userNode, Set<Permission> selectedPermissions) {
        TreeNodeModel nodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) nodeModel.getTarget();
        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userPermissions = filterPermissions(permissions).get(0);
        List<Permission> userDataPermissions = filterPermissions(permissions).get(1);

        List<Set<Permission>> grant = getGrantablePermissions(userPermissions, selectedPermissions);
        super.setNotGranted(grant.get(1));
        Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedPermissions);
        // current permission tree
        if (!userPermissions.isEmpty() || !userDataPermissions.isEmpty()) {
            getPermissionTree(userNode, userPermissions, userDataPermissions, replaced, Marking.REMOVED);
        } else {
            new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), userNode).setSelectable(false);
        }

    }

    private void createNewUserNode(User user, TreeNode root, boolean selectable) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);

        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true);
        userNode.setSelectable(false);

        createNewUserPermissionNode(userNode, selectedPermissions, selectable);
    }

    private void createNewUserPermissionNode(DefaultTreeNode userNode, Set<Permission> selectedPermissions, boolean selectable) {
        TreeNodeModel nodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) nodeModel.getTarget();
        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userPermissions = filterPermissions(permissions).get(0);
        List<Permission> userDataPermissions = filterPermissions(permissions).get(1);

        List<Set<Permission>> grant = getGrantablePermissions(userPermissions, selectedPermissions);
        Set<Permission> granted = grant.get(0);
        super.setNotGranted(grant.get(1));
        Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedPermissions);
        // modify current user permissions based on resource selection
        List<Permission> currentUserPermissions = new ArrayList<Permission>(userPermissions);
        // new permission tree without the replaced but with the granted + revoked ones, marked properly
        currentUserPermissions.removeAll(replaced);
        currentUserPermissions.addAll(granted);
        if (currentUserPermissions.isEmpty() && userDataPermissions.isEmpty()) {
            new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), userNode).setSelectable(false);
        } else {
            if (selectable) {
                getSelectablePermissionTree(userNode, currentUserPermissions,userDataPermissions,  granted, new HashSet<Permission>(), Marking.NEW, Marking.REMOVED);
            } else {
                getPermissionTree(userNode, currentUserPermissions, userDataPermissions, granted, Marking.NEW);
            }
        }
    }

    public void confirmUserPermissions() {
        for (TreeNode userNode : newUserPermissionRoot.getChildren()) {
            User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();
            // Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, userNode);
            selectedPermissions = getSelectedPermissions(selectedResourceNodes);
            List<Permission> userPermissions = permissionManager.getPermissions(user);
            Set<Permission> granted = getGrantablePermissions(userPermissions, selectedPermissions).get(0);
            Set<Permission> finalGranted = grantImpliedPermissions(userPermissions, granted);
            Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedPermissions);

            for (Permission permission : replaced) {
                permissionService.revoke(userSession.getUser(), user, permission.getAction(), permission.getResource());
            }
            for (Permission permission : finalGranted) {
                permissionService.grant(userSession.getUser(), user, permission.getAction(), permission.getResource());
            }
        }
        usersConfirmed = true;
        // rebuild new tree
        currentUserPermissionRoot = new DefaultTreeNode();

        for (TreeNode userNode : newUserPermissionRoot.getChildren()) {
            User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();
            createCurrentUserNode(user, currentUserPermissionRoot);
        }
        newUserPermissionRoot = new DefaultTreeNode();

    }

    private void collectUsers(UserGroup group, Set<User> users) {
        for (User user : userGroupService.getUsersFor(group)) {
            users.add(user);
        }
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            collectUsers(child, users);
        }
    }

    private Set<Permission> getRevokablePermissions(Set<Permission> selectedPermissions) {
        Set<Permission> revoked = new HashSet<Permission>();
        Set<Permission> granted = new HashSet<Permission>();
        for (Permission permission : selectedPermissions) {
            if (permissionDataAccess.isRevokable(currentPermissions, permission.getAction(), permission.getResource())) {
                revoked.addAll(permissionDataAccess.getRevokablePermissionsWhenRevoking(currentPermissions, permission.getAction(), permission.getResource()));
            } // else {
              // handle the case when revoking entity field from entity
              // TODO difficult because it needs granting
              // }
        }
        return revoked;
    }

    private void grant(Permission p) {
        if (selectedSubject instanceof Subject) {
            permissionService.grant(userSession.getUser(), (Subject) selectedSubject, p.getAction(), p.getResource());
        } else {
            if (selectedSubject instanceof Role) {
                permissionService.grant(userSession.getUser(), (Role) selectedSubject, p.getAction(), p.getResource());
            }
        }

    }

    private void revoke(Permission p) {
        if (selectedSubject instanceof Subject) {
            permissionService.revoke(userSession.getUser(), (Subject) selectedSubject, p.getAction(), p.getResource());
        } else {
            if (selectedSubject instanceof Role) {
                permissionService.revoke(userSession.getUser(), (Role) selectedSubject, p.getAction(), p.getResource());
            }
        }

    }

    // wizard
    public String permissionWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("selectPermissions")) {
            processSelectedPermissions();
        } else {
            if (event.getOldStep().equals("confirmPermissions")) {
                if (userSession.getSelectedCompany().isUserLevelEnabled()) {
                    propagateChangesToUsers();
                }
            }
        }
        return event.getNewStep();
    }

    // initialize bean

    public TreeNode[] getSelectedUserPermissionNodes() {
        return selectedUserPermissionNodes;
    }

    public void setSelectedUserPermissionNodes(TreeNode[] selectedUserPermissionNodes) {
        this.selectedUserPermissionNodes = selectedUserPermissionNodes;
    }

    public void setSelectedObjects(List<RowModel> selectedObjects) {
        this.selectedObjects = selectedObjects;
    }

    public void setSelectedFields(List<FieldModel> selectedFields) {
        this.selectedFields = selectedFields;
    }

    public void setSelectedSubject(IdHolder selectedSubject) {
        this.selectedSubject = selectedSubject;
    }

    public void setSelectedActions(List<EntityAction> selectedActions) {
        this.selectedActions = selectedActions;
    }

    public void setSelectedCollectionActions(List<EntityAction> selectedCollectionActions) {
        this.selectedCollectionActions = selectedCollectionActions;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public TreeNode getCurrentPermissionRoot() {
        return currentPermissionRoot;
    }

    public void setCurrentPermissionRoot(TreeNode currentPermissionRoot) {
        this.currentPermissionRoot = currentPermissionRoot;
    }

    public TreeNode getNewPermissionRoot() {
        return newPermissionRoot;
    }

    public void setNewPermissionRoot(TreeNode newPermissionRoot) {
        this.newPermissionRoot = newPermissionRoot;
    }

    public String getSelectedSubject() {
        return selectedSubject.toString();
    }

    public String getAction() {
        return action;
    }

    public List<FieldModel> getSelectedFields() {
        return selectedFields;
    }

    public List<RowModel> getSelectedObjects() {
        return selectedObjects;
    }

    public TreeNode[] getSelectedResourceNodes() {
        return selectedResourceNodes;
    }

    public void setSelectedResourceNodes(TreeNode[] selectedResourceNodes) {
        this.selectedResourceNodes = selectedResourceNodes;
    }

    public TreeNode getResourceRoot() {
        return resourceRoot;
    }

    public TreeNode[] getSelectedPermissionNodes() {
        return selectedPermissionNodes;
    }

    public void setSelectedPermissionNodes(TreeNode[] selectedPermissionNodes) {
        this.selectedPermissionNodes = selectedPermissionNodes;
    }

    public TreeNode getCurrentUserPermissionRoot() {
        return currentUserPermissionRoot;
    }

    public void setCurrentUserPermissionRoot(TreeNode currentUserPermissionRoot) {
        this.currentUserPermissionRoot = currentUserPermissionRoot;
    }

    public TreeNode getNewUserPermissionRoot() {
        return newUserPermissionRoot;
    }

    public void setNewUserPermissionRoot(TreeNode newUserPermissionRoot) {
        this.newUserPermissionRoot = newUserPermissionRoot;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean groupConfirmed) {
        this.confirmed = groupConfirmed;
    }

    public boolean isUsersConfirmed() {
        return usersConfirmed;
    }

    public void setUsersConfirmed(boolean usersConfirmed) {
        this.usersConfirmed = usersConfirmed;
    }

    public boolean isGroup() {
        return selectedSubject instanceof Role;
    }

}
