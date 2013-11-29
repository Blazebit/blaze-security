package com.blazebit.security.web.bean.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.Company;
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
import com.blazebit.security.web.util.WebUtil;

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

    private List<Permission> allPermissions = new ArrayList<Permission>();
    private List<Permission> currentPermissions = new ArrayList<Permission>();
    private List<Permission> currentDataPermissions = new ArrayList<Permission>();

    private TreeNode currentUserPermissionRoot;
    private TreeNode newUserPermissionRoot;

    private boolean confirmed = false;
    private boolean usersConfirmed = false;
    private Set<Permission> selectedPermissions;

    private String prevPath;

    // builds resource tree based on selected objects
    public void init() {
        allPermissions = getAllPermissions();
        currentPermissions = getCurrentPermissions(allPermissions);
        currentDataPermissions = getCurrentDataPermissions(allPermissions);

        resourceRoot = new DefaultTreeNode();
        for (RowModel selectedObject : selectedObjects) {
            EntityObjectField entityObjectResource = (EntityObjectField) entityFieldFactory.createResource(selectedObject.getEntity().getClass(), selectedObject
                .getEntity()
                .getId());
            TreeNodeModel entityNodeModel = new TreeNodeModel(selectedObject.getFieldSummary(), ResourceType.ENTITY, entityObjectResource);

            DefaultTreeNode entityNode = new DefaultTreeNode(entityNodeModel, resourceRoot);
            entityNode.setExpanded(true);

            createActionNodes(selectedActions, selectedObject, entityObjectResource, entityNode);
            createActionNodes(selectedCollectionActions, selectedObject, entityObjectResource, entityNode);

            propagateNodePropertiesTo(entityNode);
        }
        if (resourceRoot.getChildCount() == 0) {
            new DefaultTreeNode(new TreeNodeModel("No resource can be granted or revoked", null, null), resourceRoot).setSelectable(false);
        }
    }

    private void createActionNodes(List<EntityAction> selectedActions, RowModel selectedObject, EntityObjectField entityObjectResource, DefaultTreeNode entityNode) {
        for (EntityAction action : selectedActions) {
            TreeNodeModel actionNodeModel = new TreeNodeModel(action.getActionName(), ResourceType.ACTION, action);
            actionNodeModel.setEntityInstance(entityObjectResource);
            DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel, entityNode);
            actionNode.setExpanded(true);
            // fields
            if (!selectedFields.isEmpty()) {
                // selectedFields
                List<String> fields;
                if (actionUtils.getActionsForCollectionField().contains(action)) {
                    fields = resourceMetamodel.getCollectionFields(selectedObject.getEntity().getClass());
                } else {
                    fields = resourceMetamodel.getPrimitiveFields(selectedObject.getEntity().getClass());
                }
                for (String field : fields) {
                    EntityObjectField entityObjectFieldResource = (EntityObjectField) entityFieldFactory.createResource(selectedObject.getEntity().getClass(), field,
                                                                                                                        selectedObject.getEntity().getId());
                    TreeNodeModel fieldNodeModel = new TreeNodeModel(field, ResourceType.FIELD, entityObjectFieldResource);
                    fieldNodeModel.setEntityInstance(entityObjectFieldResource);
                    DefaultTreeNode fieldNode = new DefaultTreeNode(fieldNodeModel, actionNode);

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
            if (permissionHandling.implies(currentDataPermissions, permission)) {
                // already existing permission -> when granting dont allow user to revoke
                actionNode.setSelectable(false);
            } else {
                // hide if implied by entity field permissions
                if (permissionHandling.implies(currentPermissions, permission)) {
                    actionNode.setParent(null);
                }
                actionNode.setSelected(true);
                ((TreeNodeModel) actionNode.getData()).setMarking(Marking.NEW);
            }
        } else {
            if (this.action.equals("revoke")) {
                Set<Permission> selectedPermissions = new HashSet<Permission>();
                selectedPermissions.add(permission);
                if (permissionHandling.getRevokableFromRevoked(currentDataPermissions, selectedPermissions, true).get(0).isEmpty()) {
                    actionNode.setParent(null);
                } else {
                    // if (permissionHandlingUtils.contains(currentDataPermissions, permission) ||
                    // permissionHandlingUtils.replaces(currentDataPermissions, permission)) {
                    actionNode.setSelected(true);
                    ((TreeNodeModel) actionNode.getData()).setMarking(Marking.REMOVED);
                    // }
                }
            }
        }
    }

    private void setFieldNodeProperties(String field, DefaultTreeNode fieldNode, Permission permission) {
        if (this.action.equals("grant")) {
            if (permissionHandling.implies(currentPermissions, permission)) {
                fieldNode.setParent(null);
            } else {
                if (permissionHandling.implies(currentDataPermissions, permission)) {
                    fieldNode.setSelectable(false);
                } else {
                    if (selectedFields.contains(new FieldModel(field))) {
                        fieldNode.setSelected(true);
                        ((TreeNodeModel) fieldNode.getData()).setMarking(Marking.NEW);
                    }
                }
            }
        } else {
            if (this.action.equals("revoke")) {
                Set<Permission> selectedPermissions = new HashSet<Permission>();
                selectedPermissions.add(permission);
                if (permissionHandling.getRevokableFromRevoked(currentDataPermissions, selectedPermissions, true).get(0).isEmpty()) {
                    fieldNode.setParent(null);
                } else {
                    if (selectedFields.contains(new FieldModel(field))) {
                        fieldNode.setSelected(true);
                        ((TreeNodeModel) fieldNode.getData()).setMarking(Marking.REMOVED);
                    }
                }
            }
        }
    }

    // wizard step 1\
    public void processSelectedPermissions() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
        if ("grant".equals(action)) {

            Set<Permission> granted = permissionHandling.getGrantable(allPermissions, selectedPermissions).get(0);
            super.setNotGranted(permissionHandling.getGrantable(allPermissions, selectedPermissions).get(1));

            Set<Permission> replaced = permissionHandling.getReplacedByGranting(currentDataPermissions, selectedPermissions);

            // build current tree
            currentPermissionRoot = getPermissionTree(currentPermissions, currentDataPermissions, replaced, Marking.REMOVED);
            // build new tree -> show only new object permissions
            newPermissionRoot = getPermissionTree(new ArrayList<Permission>(), new ArrayList<Permission>(granted), granted, Marking.NEW);
        } else {
            if ("revoke".equals(action)) {

                List<Set<Permission>> revoke = permissionHandling.getRevokableFromRevoked(allPermissions, selectedPermissions, true);
                Set<Permission> revoked = revoke.get(0);
                Set<Permission> granted = revoke.get(2);

                Set<Permission> impliedBy = new HashSet<Permission>();
                Set<Permission> toRevoke = new HashSet<Permission>();
                if (!granted.isEmpty()) {
                    for (Permission permission : selectedPermissions) {
                        if (!permissionHandling.contains(revoked, permission) && permissionHandling.implies(revoked, permission)) {
                            impliedBy.addAll(permissionDataAccess.getImpliedBy(new ArrayList<Permission>(revoked), permission.getAction(), permission.getResource()));
                            toRevoke.add(permission);
                        }
                    }
                }
                revoked = new HashSet<Permission>(permissionHandling.removeAll(revoked, impliedBy));
                revoked.addAll(toRevoke);

                // build current tree
                currentPermissionRoot = getPermissionTree(currentPermissions, currentDataPermissions, new HashSet<Permission>(), Marking.REMOVED);
                // build new tree -> show only new object permissions
                newPermissionRoot = getSelectablePermissionTree(new ArrayList<Permission>(), new ArrayList<Permission>(concat(granted, revoked)), granted, revoked, Marking.NEW,
                                                                Marking.REMOVED, false);
            }
        }
        // reset values
        selectedFields.clear();
        selectedActions.clear();
        selectedCollectionActions.clear();
        selectedObjects.clear();
        confirmed = false;
    }

    // confirm
    public void confirmPermissions() {
        selectedPermissions = getSelectedPermissions(selectedResourceNodes);
        if (action.equals("grant")) {

            Set<Permission> granted = permissionHandling.getGrantable(currentDataPermissions, selectedPermissions).get(0);
            super.setNotGranted(permissionHandling.getGrantable(currentDataPermissions, selectedPermissions).get(1));

            for (Permission permission : granted) {
                grant(permission);
            }
        } else {
            if (action.equals("revoke")) {
                List<Set<Permission>> revoke = permissionHandling.getRevokableFromRevoked(allPermissions, selectedPermissions, true);
                Set<Permission> revoked = revoke.get(0);
                Set<Permission> granted = revoke.get(2);

                for (Permission permission : revoked) {
                    revoke(permission);
                }

                for (Permission permission : granted) {
                    grant(permission);
                }
            }
        }
        // reset view
        allPermissions = getAllPermissions();
        // reset permissions
        currentPermissionRoot = getPermissionTree(getCurrentPermissions(allPermissions), getCurrentDataPermissions(allPermissions), new HashSet<Permission>(), Marking.REMOVED);
        // build new tree
        newPermissionRoot = new DefaultTreeNode();
        // reset initial values
        confirmed = true;

        if (isGroup() && !Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.USER_LEVEL))) {
            propagateChangesToUsers();
            confirmUserPermissions();
        }
    }

    private void propagateChangesToUsers() {
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

        createCurrentUserPermissionNode(userNode);

    }

    private void createCurrentUserPermissionNode(DefaultTreeNode userNode) {
        TreeNodeModel nodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) nodeModel.getTarget();
        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
        List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);

        List<Set<Permission>> grant = permissionHandling.getGrantable(userDataPermissions, selectedPermissions);
        super.setNotGranted(grant.get(1));
        Set<Permission> replaced = permissionHandling.getReplacedByGranting(userDataPermissions, selectedPermissions);
        // current permission tree
        getPermissionTree(userNode, userPermissions, userDataPermissions, replaced, Marking.REMOVED);

    }

    private void createNewUserNode(User user, TreeNode root, boolean selectable) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);

        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true);
        userNode.setSelectable(false);

        createNewUserPermissionNode(userNode, selectable);
    }

    private void createNewUserPermissionNode(DefaultTreeNode userNode, boolean selectable) {
        TreeNodeModel nodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) nodeModel.getTarget();

        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);

        if (action.equals("grant")) {
            List<Set<Permission>> grant = permissionHandling.getGrantable(userDataPermissions, selectedPermissions);
            Set<Permission> granted = grant.get(0);
            super.setNotGranted(grant.get(1));

            getPermissionTree(userNode, new ArrayList<Permission>(), new ArrayList<Permission>(granted), granted, Marking.NEW);
        } else {
            if (action.equals("revoke")) {
                List<Set<Permission>> revoke = permissionHandling.getRevokableFromRevoked(userDataPermissions, selectedPermissions, true);
                Set<Permission> revoked = revoke.get(0);
                Set<Permission> granted = revoke.get(2);

                Set<Permission> impliedBy = new HashSet<Permission>();
                Set<Permission> toRevoke = new HashSet<Permission>();
                if (!granted.isEmpty()) {
                    for (Permission permission : selectedPermissions) {
                        if (!permissionHandling.contains(revoked, permission) && permissionHandling.implies(revoked, permission)) {
                            impliedBy.addAll(permissionDataAccess.getImpliedBy(new ArrayList<Permission>(revoked), permission.getAction(), permission.getResource()));
                            toRevoke.add(permission);
                        }
                    }
                }
                revoked = new HashSet<Permission>(permissionHandling.removeAll(revoked, impliedBy));
                revoked.addAll(toRevoke);

                // build new tree -> show only new object permissions
                getSelectablePermissionTree(userNode, new ArrayList<Permission>(), new ArrayList<Permission>(concat(granted, revoked)), revoked, granted, Marking.REMOVED,
                                            Marking.NEW, false);
            }
        }
    }

    public void confirmUserPermissions() {
        for (TreeNode userNode : newUserPermissionRoot.getChildren()) {
            User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();
            List<Permission> permissions = permissionManager.getPermissions(user);
            List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);
            if (action.equals("grant")) {

                Set<Permission> grant = permissionHandling.getGrantable(userDataPermissions, selectedPermissions).get(0);
                for (Permission permission : grant) {
                    permissionService.grant(userSession.getUser(), user, permission.getAction(), permission.getResource());
                }
            } else {
                if ("revoke".equals(action)) {
                    List<Set<Permission>> revoke = permissionHandling.getRevokableFromRevoked(userDataPermissions, selectedPermissions, true);
                    Set<Permission> revoked = revoke.get(0);
                    Set<Permission> granted = revoke.get(2);

                    for (Permission permission : revoked) {
                        permissionService.revoke(userSession.getUser(), user, permission.getAction(), permission.getResource());
                    }

                    for (Permission permission : granted) {
                        permissionService.grant(userSession.getUser(), user, permission.getAction(), permission.getResource());
                    }

                }
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
                propagateChangesToUsers();
            }
        }
        return event.getNewStep();
    }

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

    public void returnToPreviousPage() {
        WebUtil.redirect(FacesContext.getCurrentInstance(), "/blaze-security-showcase" + prevPath, false);
    }

    private List<Permission> getAllPermissions() {
        if (selectedSubject instanceof Subject) {
            return permissionManager.getPermissions((Subject) selectedSubject);
        } else {
            return permissionManager.getPermissions((Role) selectedSubject);
        }
    }

    private List<Permission> getCurrentPermissions(List<Permission> permissions) {
        return resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
    }

    private List<Permission> getCurrentDataPermissions(List<Permission> permissions) {
        return resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);
    }

    public String getPrevPath() {
        return prevPath;
    }

    public void setPrevPath(String prevPath) {
        this.prevPath = prevPath;
    }

}
