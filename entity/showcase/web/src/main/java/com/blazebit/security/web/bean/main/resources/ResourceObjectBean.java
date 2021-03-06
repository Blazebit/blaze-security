package com.blazebit.security.web.bean.main.resources;

import java.io.Serializable;
import java.util.ArrayList;
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

import com.blazebit.security.PermissionUtils;
import com.blazebit.security.entity.EntityPermissionUtils;
import com.blazebit.security.model.EntityAction;
import com.blazebit.security.model.EntityObjectField;
import com.blazebit.security.model.Features;
import com.blazebit.security.model.IdHolder;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.PermissionChangeSet;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.showcase.data.UserGroupDataAccess;
import com.blazebit.security.web.bean.base.PermissionHandlingBaseBean;
import com.blazebit.security.web.bean.model.FieldModel;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.util.WebUtil;

@Named
@ViewScoped
@Stateless
public class ResourceObjectBean extends PermissionHandlingBaseBean {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	@Inject
	private UserGroupDataAccess userGroupDataAccess;

	private IdHolder<?> selectedSubject;
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

	private Set<Permission> groupGrantedPermissions;

	private Set<Permission> groupRevokedPermissions;

	private List<User> groupUsers;

	// builds resource tree based on selected objects
	public void init() {
		allPermissions = getAllPermissions();
		currentPermissions = getCurrentPermissions(allPermissions);
		currentDataPermissions = getCurrentDataPermissions(allPermissions);

		resourceRoot = new DefaultTreeNode();
		for (RowModel selectedObject : selectedObjects) {
			EntityObjectField entityObjectResource = (EntityObjectField) entityFieldFactory
					.createResource(selectedObject.getEntity().getClass(),
							((IdHolder<Serializable> )selectedObject.getEntity()).getId());
			TreeNodeModel entityNodeModel = new TreeNodeModel(
					selectedObject.getFieldSummary(), ResourceType.ENTITY,
					entityObjectResource);

			DefaultTreeNode entityNode = new DefaultTreeNode(entityNodeModel,
					resourceRoot);
			entityNode.setExpanded(true);
			for (EntityAction action : selectedActions) {
				createActionNode(action, selectedObject, entityObjectResource,
						entityNode);
			}
			for (EntityAction action : selectedCollectionActions) {
				createActionNode(action, selectedObject, entityObjectResource,
						entityNode);
			}

			propagateNodePropertiesTo(entityNode);
		}
		if (resourceRoot.getChildCount() == 0) {
			new DefaultTreeNode(new TreeNodeModel(
					"No resource can be granted or revoked", null, null),
					resourceRoot).setSelectable(false);
		}
	}

	private void createActionNode(EntityAction action, RowModel<IdHolder<Serializable>> selectedObject,
			EntityObjectField entityObjectResource, DefaultTreeNode entityNode) {

		TreeNodeModel actionNodeModel = new TreeNodeModel(
				action.getName(), ResourceType.ACTION, action);
		DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel,
				entityNode);
		actionNode.setExpanded(true);
		// fields
		if (!selectedFields.isEmpty()) {
			// selectedFields
			List<String> fields;
			if (actionUtils.getUpdateActionsForCollectionField().contains(
					action)) {
				fields = resourceMetamodel.getCollectionFields(selectedObject
						.getEntity().getClass());
			} else {
				fields = resourceMetamodel.getPrimitiveFields(selectedObject
						.getEntity().getClass());
			}
			for (String field : fields) {
				EntityObjectField entityObjectFieldResource = (EntityObjectField) entityFieldFactory
						.createResource(selectedObject.getEntity().getClass(),
								field,
								((IdHolder<Serializable>) selectedObject.getEntity())
										.getId());
				TreeNodeModel fieldNodeModel = new TreeNodeModel(field,
						ResourceType.FIELD, entityObjectFieldResource);
				DefaultTreeNode fieldNode = new DefaultTreeNode(fieldNodeModel,
						actionNode);

				Permission permission = permissionFactory.create(action,
						entityObjectFieldResource);
				setFieldNodeProperties(field, fieldNode, permission);
			}
		} else {
			// no fields, only action
			Permission permission = permissionFactory.create(action,
					entityObjectResource);
			setActionNodeProperties(action, actionNode, permission);
		}

		propagateSelectionUpwards(actionNode);
	}

	private void setActionNodeProperties(EntityAction action,
			DefaultTreeNode actionNode, Permission permission) {
		if (this.action.equals("grant")) {
			if (PermissionUtils.implies(currentDataPermissions, permission)) {
				// already existing permission -> when granting dont allow user
				// to revoke
				actionNode.setSelectable(false);
			} else {
				// hide if implied by entity field permissions
				if (PermissionUtils.implies(currentPermissions, permission)) {
					actionNode.getParent().getChildren().remove(actionNode);
					// actionNode.setParent(null);
				}
				actionNode.setSelected(true);
				((TreeNodeModel) actionNode.getData()).setMarking(Marking.NEW);
			}
		} else {
			if (this.action.equals("revoke")) {
				Set<Permission> selectedPermissions = new HashSet<Permission>();
				selectedPermissions.add(permission);
				if (permissionHandling
						.getRevokableFromRevoked(currentDataPermissions,
								selectedPermissions, true).getRevokes().isEmpty()) {
					actionNode.getParent().getChildren().remove(actionNode);
					// actionNode.setParent(null);
				} else {
					// if
					// (permissionHandlingUtils.contains(currentDataPermissions,
					// permission) ||
					// permissionHandlingUtils.replaces(currentDataPermissions,
					// permission)) {
					actionNode.setSelected(true);
					((TreeNodeModel) actionNode.getData())
							.setMarking(Marking.REMOVED);
					// }
				}
			}
		}
	}

	private void setFieldNodeProperties(String field,
			DefaultTreeNode fieldNode, Permission permission) {
		if (this.action.equals("grant")) {
			// hide if implied by entity field permissions. why? TODO
			if (PermissionUtils.implies(currentPermissions, permission)) {
				// fieldNode.setParent(null);
				fieldNode.getParent().getChildren().remove(fieldNode);
				// fieldNode.setSelectable(false);
			} else {
				if (PermissionUtils.implies(currentDataPermissions,
						permission)) {
					fieldNode.setSelectable(false);
				} else {
					if (selectedFields.contains(new FieldModel(field))) {
						fieldNode.setSelected(true);
						((TreeNodeModel) fieldNode.getData())
								.setMarking(Marking.NEW);
					}
				}
			}
		} else {
			if (this.action.equals("revoke")) {
				Set<Permission> selectedPermissions = new HashSet<Permission>();
				selectedPermissions.add(permission);
				if (permissionHandling
						.getRevokableFromRevoked(currentDataPermissions,
								selectedPermissions, true).getRevokes().isEmpty()) {
					// fieldNode.setParent(null);
					fieldNode.getParent().getChildren().remove(fieldNode);
				} else {
					if (selectedFields.contains(new FieldModel(field))) {
						fieldNode.setSelected(true);
						((TreeNodeModel) fieldNode.getData())
								.setMarking(Marking.REMOVED);
					}
				}
			}
		}
	}

	// wizard step 1\
	public void processSelectedPermissions() {
		Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
		if ("grant".equals(action)) {

			Set<Permission> granted = permissionHandling.getGrantable(
					allPermissions, selectedPermissions).get(0);
			dialogBean.setNotGranted(permissionHandling.getGrantable(
					allPermissions, selectedPermissions).get(1));

			Set<Permission> replaced = permissionHandling
					.getReplacedByGranting(currentDataPermissions,
							selectedPermissions);

			// build current tree
			currentPermissionRoot = getImmutablePermissionTree(
					currentPermissions, currentDataPermissions, replaced,
					Marking.REMOVED, !isEnabled(Features.FIELD_LEVEL));
			// build new tree -> show only new object permissions
			newPermissionRoot = getImmutablePermissionTree(
					new ArrayList<Permission>(), new ArrayList<Permission>(
							granted), granted, Marking.NEW);
		} else {
			if ("revoke".equals(action)) {

				PermissionChangeSet revokeChangeSet = permissionHandling
						.getRevokableFromRevoked(allPermissions,
								selectedPermissions, true);
				Set<Permission> revoked = revokeChangeSet.getRevokes();
				Set<Permission> granted = revokeChangeSet.getGrants();

				Set<Permission> impliedBy = new HashSet<Permission>();
				Set<Permission> toRevoke = new HashSet<Permission>();
				if (!granted.isEmpty()) {
					for (Permission permission : selectedPermissions) {
						if (!PermissionUtils.contains(revoked, permission)
								&& PermissionUtils.implies(revoked,
										permission)) {
							impliedBy.addAll(PermissionUtils.getImpliedBy(
									new ArrayList<Permission>(revoked),
									permission.getAction(),
									permission.getResource()));
							toRevoke.add(permission);
						}
					}
				}
				revoked = new HashSet<Permission>(PermissionUtils.removeAll(
						revoked, impliedBy));
				revoked.addAll(toRevoke);

				// build current tree
				currentPermissionRoot = getImmutablePermissionTree(
						currentPermissions, currentDataPermissions,
						new HashSet<Permission>(), Marking.REMOVED,
						!isEnabled(Features.FIELD_LEVEL));
				// build new tree -> show only new object permissions
				newPermissionRoot = getImmutablePermissionTree(
						new ArrayList<Permission>(), new ArrayList<Permission>(
								concat(granted, revoked)), granted, revoked,
						Marking.NEW, Marking.REMOVED, false);
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

			Set<Permission> granted = permissionHandling.getGrantable(
					currentDataPermissions, selectedPermissions).get(0);
			dialogBean.setNotGranted(permissionHandling.getGrantable(
					currentDataPermissions, selectedPermissions).get(1));

			if (selectedSubject instanceof Subject) {
				Subject subject = (Subject) selectedSubject;
				performOperations(subject, currentPermissions,
						new HashSet<Permission>(), granted);
			} else {
				Role role = (Role) selectedSubject;
				groupGrantedPermissions = performOperations(role,
						currentPermissions, new HashSet<Permission>(), granted,
						!groupUsers.isEmpty()).get(1);
			}
		} else {
			if (action.equals("revoke")) {
				PermissionChangeSet revokeChangeSet = permissionHandling
						.getRevokableFromRevoked(allPermissions,
								selectedPermissions, true);
				Set<Permission> revoked = revokeChangeSet.getRevokes();
				Set<Permission> granted = revokeChangeSet.getGrants();

				if (selectedSubject instanceof Subject) {
					Subject subject = (Subject) selectedSubject;
					performOperations(subject, currentPermissions, revoked,
							granted);
				} else {
					Role role = (Role) selectedSubject;
					List<Set<Permission>> result = performOperations(role,
							currentPermissions, revoked, granted,
							!groupUsers.isEmpty());
					groupGrantedPermissions = result.get(1);
					groupRevokedPermissions = result.get(0);
				}
			}
		}
		// reset view
		allPermissions = getAllPermissions();
		// reset permissions
		currentPermissionRoot = getImmutablePermissionTree(
				getCurrentPermissions(allPermissions),
				getCurrentDataPermissions(allPermissions),
				new HashSet<Permission>(), Marking.REMOVED,
				!isEnabled(Features.FIELD_LEVEL));
		// build new tree
		newPermissionRoot = new DefaultTreeNode();
		// reset initial values
		confirmed = true;

		if (isGroup() && !isEnabled(Features.USER_LEVEL)) {
			propagateChangesToUsers();
			confirmUserPermissions();
		}
	}

	private void propagateChangesToUsers() {
		// propagate changes to users
		createUserPermissionTrees(groupUsers, false);
	}

	private void createUserPermissionTrees(List<User> selectedUsers,
			boolean selectable) {
		setCurrentUserPermissionRoot(new DefaultTreeNode());
		setNewUserPermissionRoot(new DefaultTreeNode());

		for (User user : selectedUsers) {
			createCurrentUserNode(user, currentUserPermissionRoot);
			createNewUserNode(user, newUserPermissionRoot, selectable);
		}
	}

	private void createCurrentUserNode(User user, TreeNode root) {
		TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(),
				ResourceType.USER, user);

		DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
		userNode.setExpanded(true);
		userNode.setSelectable(false);

		createCurrentUserPermissionNode(userNode);

	}

	private void createCurrentUserPermissionNode(DefaultTreeNode userNode) {
		TreeNodeModel nodeModel = (TreeNodeModel) userNode.getData();
		User user = (User) nodeModel.getTarget();
		List<Permission> permissions = permissionManager.getPermissions(user);
		List<Permission> userPermissions = EntityPermissionUtils
				.getSeparatedPermissionsByResource(permissions).get(0);
		List<Permission> userDataPermissions = EntityPermissionUtils
				.getSeparatedPermissionsByResource(permissions).get(1);

		List<Set<Permission>> grant = permissionHandling.getGrantable(
				userDataPermissions, selectedPermissions);
		dialogBean.setNotGranted(grant.get(1));
		Set<Permission> replaced = permissionHandling.getReplacedByGranting(
				userDataPermissions, selectedPermissions);
		// current permission tree
		getImmutablePermissionTree(userNode, userPermissions,
				userDataPermissions, replaced, Marking.REMOVED,
				!isEnabled(Features.FIELD_LEVEL));

	}

	private void createNewUserNode(User user, TreeNode root, boolean selectable) {
		TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(),
				ResourceType.USER, user);

		DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
		userNode.setExpanded(true);
		userNode.setSelectable(false);

		createNewUserPermissionNode(userNode, selectable);
	}

	private void createNewUserPermissionNode(DefaultTreeNode userNode,
			boolean selectable) {
		TreeNodeModel nodeModel = (TreeNodeModel) userNode.getData();
		User user = (User) nodeModel.getTarget();

		List<Permission> permissions = permissionManager.getPermissions(user);
		List<Permission> userDataPermissions = EntityPermissionUtils
				.getSeparatedPermissionsByResource(permissions).get(1);

		if (action.equals("grant")) {
			List<Set<Permission>> grant = permissionHandling.getGrantable(
					userDataPermissions, selectedPermissions);
			Set<Permission> granted = grant.get(0);
			dialogBean.setNotGranted(grant.get(1));

			getImmutablePermissionTree(userNode, new ArrayList<Permission>(),
					new ArrayList<Permission>(granted), granted, Marking.NEW);
		} else {
			if (action.equals("revoke")) {
				PermissionChangeSet revokeChangeSet = permissionHandling
						.getRevokableFromRevoked(userDataPermissions,
								selectedPermissions, true);
				Set<Permission> revoked = revokeChangeSet.getRevokes();
				Set<Permission> granted = revokeChangeSet.getGrants();

				Set<Permission> impliedBy = new HashSet<Permission>();
				Set<Permission> toRevoke = new HashSet<Permission>();
				if (!granted.isEmpty()) {
					for (Permission permission : selectedPermissions) {
						if (!PermissionUtils.contains(revoked, permission)
								&& PermissionUtils.implies(revoked,
										permission)) {
							impliedBy.addAll(PermissionUtils.getImpliedBy(
									new ArrayList<Permission>(revoked),
									permission.getAction(),
									permission.getResource()));
							toRevoke.add(permission);
						}
					}
				}
				revoked = new HashSet<Permission>(PermissionUtils.removeAll(
						revoked, impliedBy));
				revoked.addAll(toRevoke);

				// build new tree -> show only new object permissions
				getImmutablePermissionTree(userNode,
						new ArrayList<Permission>(), new ArrayList<Permission>(
								concat(granted, revoked)), revoked, granted,
						Marking.REMOVED, Marking.NEW, false);
			}
		}
	}

	public void confirmUserPermissions() {
		for (TreeNode userNode : newUserPermissionRoot.getChildren()) {
			User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();
			List<Permission> permissions = permissionManager
					.getPermissions(user);
			List<Permission> userDataPermissions = EntityPermissionUtils
					.getSeparatedPermissionsByResource(permissions).get(1);
			if (action.equals("grant")) {

				Set<Permission> grant = permissionHandling.getGrantable(
						userDataPermissions, selectedPermissions).get(0);
				// confirm group permissions
				revokeAndGrant((UserGroup) selectedSubject,
						new HashSet<Permission>(), groupGrantedPermissions,
						false);
				// confirm user permissions
				performOperations(user, permissions, new HashSet<Permission>(),
						grant);

			} else {
				if ("revoke".equals(action)) {

					PermissionChangeSet revokeChangeSet = permissionHandling
							.getRevokableFromRevoked(userDataPermissions,
									selectedPermissions, true);
					Set<Permission> revoked = revokeChangeSet.getRevokes();
					Set<Permission> granted = revokeChangeSet.getGrants();
					// confirm group permissions
					revokeAndGrant((UserGroup) selectedSubject,
							groupRevokedPermissions, groupGrantedPermissions,
							false);
					// confirm user permissions
					performOperations(user, permissions, revoked, granted);
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

	// wizard
	public String permissionWizardListener(FlowEvent event) {
		if (event.getOldStep().equals("selectPermissions")) {
			processSelectedPermissions();
		} else {
			// if it is a group redirect to user tab
			if (isGroup() && event.getOldStep().equals("confirmPermissions")) {
				groupUsers = userGroupDataAccess.collectUsers(
						(UserGroup) selectedSubject,
						isEnabled(Features.GROUP_HIERARCHY));
				if (!groupUsers.isEmpty()) {
					propagateChangesToUsers();
					return "confirmUserPermissions";
				} else {
					return event.getOldStep();
				}
			} else {
				return event.getOldStep();
			}
		}
		return event.getNewStep();
	}

	public TreeNode[] getSelectedUserPermissionNodes() {
		return selectedUserPermissionNodes;
	}

	public void setSelectedUserPermissionNodes(
			TreeNode[] selectedUserPermissionNodes) {
		this.selectedUserPermissionNodes = selectedUserPermissionNodes;
	}

	public void setSelectedObjects(List<RowModel> selectedObjects) {
		this.selectedObjects = selectedObjects;
	}

	public void setSelectedFields(List<FieldModel> selectedFields) {
		this.selectedFields = selectedFields;
	}

	public void setSelectedSubject(IdHolder<?> selectedSubject) {
		this.selectedSubject = selectedSubject;
	}

	public void setSelectedActions(List<EntityAction> selectedActions) {
		this.selectedActions = selectedActions;
	}

	public void setSelectedCollectionActions(
			List<EntityAction> selectedCollectionActions) {
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
		WebUtil.redirect(FacesContext.getCurrentInstance(),
				"/blaze-security-showcase" + prevPath, false);
	}

	private List<Permission> getAllPermissions() {
		if (selectedSubject instanceof Subject) {
			return permissionManager.getPermissions((Subject) selectedSubject);
		} else {
			return permissionManager.getPermissions((Role) selectedSubject);
		}
	}

	private List<Permission> getCurrentPermissions(List<Permission> permissions) {
		return EntityPermissionUtils.getSeparatedPermissionsByResource(permissions)
				.get(0);
	}

	private List<Permission> getCurrentDataPermissions(
			List<Permission> permissions) {
		return EntityPermissionUtils.getSeparatedPermissionsByResource(permissions)
				.get(1);
	}

	public String getPrevPath() {
		return prevPath;
	}

	public void setPrevPath(String prevPath) {
		this.prevPath = prevPath;
	}

}
