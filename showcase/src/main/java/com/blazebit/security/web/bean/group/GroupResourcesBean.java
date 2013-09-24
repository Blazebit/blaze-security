/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.group;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.apache.deltaspike.core.util.ReflectionUtils;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.lang.StringUtils;
import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityFieldFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.ResourceName;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.PermissionViewUtils;
import com.blazebit.security.web.bean.UserSession;
import com.blazebit.security.web.bean.model.EntityModel;
import com.blazebit.security.web.bean.model.PermissionModel;
import com.blazebit.security.web.bean.model.ResourceAction;
import com.blazebit.security.web.bean.model.ResourceModel;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.impl.UserGroupService;
import com.blazebit.security.web.service.impl.UserService;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "groupResourcesBean")
public class GroupResourcesBean extends PermissionViewUtils implements PermissionView, Serializable {

    @Inject
    private UserService userService;
    @Inject
    private UserGroupService userGroupService;
    @Inject
    private RoleService roleService;
    @Inject
    private UserContext userContext;
    @Inject
    private PermissionService permissionService;
    @Inject
    private PermissionDataAccess permissionDataAccess;
    @Inject
    private PermissionManager permissionManager;
    @Inject
    private UserSession userSession;
    private TreeNode[] selectedResourceNodes;
    private TreeNode[] selectedActionNodes;
    private DefaultTreeNode actionRoot;
    private DefaultTreeNode resourceRoot;
    private Set<EntityField> selectedResources = new HashSet<EntityField>();
    private Set<ResourceAction> selectedResourceActions = new HashSet<ResourceAction>();
    private List<PermissionModel> groupPermissions = new ArrayList<PermissionModel>();
    private boolean permissionTreeView;
    private TreeNode permissionViewRoot;
    private List<Permission> permisions;
    @Inject
    private EntityFieldFactory entityFieldFactory;

    @Inject
    private ActionFactory actionFactory;

    public void init() {
        List<EntityModel> availableEntities = new ArrayList<EntityModel>();
        for (Class<?> clazz : entityFieldFactory.getEntityClasses()) {
            ResourceName resourceName = AnnotationUtils.findAnnotation(clazz, ResourceName.class);
            if (resourceName != null) {
                availableEntities.add(new EntityModel(clazz, resourceName.name()));
            }

        }
        resourceRoot = new DefaultTreeNode("root", null);
        for (EntityModel model : availableEntities) {
            // entity
            DefaultTreeNode entityRoot = new DefaultTreeNode(new ResourceModel(model.getName(), ResourceModel.ResourceType.ENTITY, entityFieldFactory.createResource(model
                .getEntityClass(), EntityField.EMPTY_FIELD)), resourceRoot);
            // fields for entity
            Set<Field> allFields = ReflectionUtils.getAllDeclaredFields(model.getEntityClass());
            for (Field field : allFields) {
                if (!ReflectionUtils.isStatic(field)) {
                    new DefaultTreeNode(new ResourceModel(field.getName(), ResourceModel.ResourceType.FIELD, entityFieldFactory.createResource(model.getEntityClass(),
                                                                                                                                               field.getName())), entityRoot);
                }
            }
        }
        if (getSelectedGroup() != null) {
            initGroupPermissions();
        }
    }

    public void selectResources() {
        selectedResources.clear();
        for (TreeNode resourceNode : selectedResourceNodes) {
            ResourceModel model = (ResourceModel) resourceNode.getData();
            EntityField resource = (EntityField) model.getTarget();

            switch (model.getType()) {
                case ENTITY:
                    // entity is selected => permission for entity (all fields) and all actions
                    selectedResources.add(resource);
                    break;
                case FIELD:
                    // field is selected => permission for entity + field and all actions
                    EntityField resourceWithoutField = new EntityField(resource.getEntity(), EntityField.EMPTY_FIELD);

                    if (!selectedResources.contains(resourceWithoutField)) {
                        selectedResources.add(resource);
                    }
                    break;
            }
        }
        actionRoot = new DefaultTreeNode("Root", null);

        for (EntityField resource : selectedResources) {
            DefaultTreeNode resourceChild;
            if (StringUtils.isEmpty(resource.getField())) {
                resourceChild = new DefaultTreeNode(new ResourceModel(resource.getEntity(), ResourceModel.ResourceType.ENTITY, resource), actionRoot);
                for (Action _action : actionFactory.getActionsForEntity()) {
                    EntityAction action = (EntityAction) _action;
                    new DefaultTreeNode(new ResourceModel(action.getActionName(), ResourceModel.ResourceType.ACTION, action), resourceChild);
                }
            } else {
                resourceChild = new DefaultTreeNode(new ResourceModel(resource.getEntity() + " " + resource.getField(), ResourceModel.ResourceType.FIELD, resource), actionRoot);
                for (Action _action : actionFactory.getActionsForField()) {
                    EntityAction action = (EntityAction) _action;
                    new DefaultTreeNode(new ResourceModel(action.getActionName(), ResourceModel.ResourceType.ACTION, action), resourceChild);
                }
            }
        }

    }

    public void selectActions() {
        selectedResourceActions.clear();
        for (TreeNode actionNode : selectedActionNodes) {
            ResourceModel model = (ResourceModel) actionNode.getData();
            switch (model.getType()) {
                case ENTITY:
                    EntityField entityResource = (EntityField) model.getTarget();
                    for (Action action : actionFactory.getActionsForEntity()) {
                        selectedResourceActions.add(new ResourceAction(entityResource, (EntityAction) action));
                    }
                    break;
                case FIELD:
                    EntityField fieldResource = (EntityField) model.getTarget();
                    for (Action action : actionFactory.getActionsForField()) {
                        selectedResourceActions.add(new ResourceAction(fieldResource, (EntityAction) action));
                    }
                    break;
                case ACTION:
                    EntityAction action = (EntityAction) model.getTarget();
                    TreeNode parent = actionNode.getParent();

                    ResourceModel parentModel = (ResourceModel) parent.getData();
                    EntityField parentResource = (EntityField) parentModel.getTarget();

                    selectedResourceActions.add(new ResourceAction(parentResource, action));
                    break;
            }
        }
        // user permissions
        for (ResourceAction ra : selectedResourceActions) {
            if (permissionDataAccess.isGrantable(getSelectedGroup(), ra.getAction(), ra.getResource())) {
                Set<Permission> toRevoke = permissionDataAccess.getRevokablePermissionsWhenGranting(getSelectedGroup(), ra.getAction(), ra.getResource());
                for (PermissionModel userPermissionModel : groupPermissions) {
                    if (toRevoke.contains(userPermissionModel.getPermission())) {
                        userPermissionModel.setSelected(true);
                    }
                }
            }
        }
    }

    public String resourceWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("resources")) {
            selectResources();
        } else {
            if (event.getOldStep().equals("actions")) {
                selectActions();
            }
        }
        return event.getNewStep();
    }

    /**
     * confirm button when adding permissions to user
     * 
     */
    public void confirmSelectedResourcesAndActions() {
        for (ResourceAction ra : selectedResourceActions) {
            permissionService.grant(userContext.getUser(), getSelectedGroup(), ra.getAction(), ra.getResource());
        }

        this.selectedResourceActions.clear();
        this.groupPermissions.clear();
        this.selectedActionNodes = new TreeNode[0];
        this.selectedResourceNodes = new TreeNode[0];
        this.selectedResources.clear();
        initGroupPermissions();
    }

    public TreeNode[] getSelectedResourceNodes() {
        return selectedResourceNodes;
    }

    public void setSelectedResourceNodes(TreeNode[] selectedResourceNodes) {
        this.selectedResourceNodes = selectedResourceNodes;
    }

    public TreeNode[] getSelectedActionNodes() {
        return selectedActionNodes;
    }

    public void setSelectedActionNodes(TreeNode[] selectedActionNodes) {
        this.selectedActionNodes = selectedActionNodes;
    }

    public DefaultTreeNode getActionRoot() {
        return actionRoot;
    }

    public DefaultTreeNode getResourceRoot() {
        return resourceRoot;
    }

    public Set<EntityField> getSelectedResources() {
        return selectedResources;
    }

    public void setSelectedResources(Set<EntityField> selectedResources) {
        this.selectedResources = selectedResources;
    }

    public List<ResourceAction> getSelectedResourceActions() {
        List<ResourceAction> ret = new ArrayList<ResourceAction>(selectedResourceActions);
        Collections.sort(ret, new Comparator<ResourceAction>() {

            @Override
            public int compare(ResourceAction o1, ResourceAction o2) {
                return o1.getResource().getEntity().compareToIgnoreCase(o2.getResource().getEntity());

            }
        });
        return ret;

    }

    @Override
    public List<PermissionModel> getPermissions() {
        return groupPermissions;
    }

    @Override
    public String getPermissionHeader() {
        return "Usergroup -" + (getSelectedGroup() != null ? getSelectedGroup().getName() : "");
    }

    public UserGroup getSelectedGroup() {
        return userSession.getSelectedUserGroup();
    }

    private void initGroupPermissions() {
        this.groupPermissions.clear();
        permisions = permissionManager.getAllPermissions(getSelectedGroup());
        for (Permission permission : permisions) {
            this.groupPermissions.add(new PermissionModel(permission, false));
        }
        this.permissionViewRoot = new DefaultTreeNode("root", null);
        buildPermissionTree(permisions, permissionViewRoot);
        this.permissionTreeView = true;
    }

    @Override
    public TreeNode getPermissionViewRoot() {
        return permissionViewRoot;
    }

    @Override
    public boolean isShowPermissionTreeView() {
        return true;
    }

    @Override
    public void setShowPermissionTreeView(boolean set) {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools |
                                                                       // Templates.
    }
}
