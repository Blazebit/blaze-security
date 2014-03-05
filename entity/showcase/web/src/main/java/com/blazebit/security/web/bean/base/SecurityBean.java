package com.blazebit.security.web.bean.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.entity.EntityActionFactory;
import com.blazebit.security.entity.EntityResourceDefinition;
import com.blazebit.security.entity.EntityResourceFactory;
import com.blazebit.security.entity.EntityResourceMetamodel;
import com.blazebit.security.entity.EntityResourceType;
import com.blazebit.security.entity.UserContext;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.EntityAction;
import com.blazebit.security.model.EntityField;
import com.blazebit.security.model.Features;
import com.blazebit.security.model.IdHolder;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.service.PermissionService;
import com.blazebit.security.showcase.data.PropertyDataAccess;
import com.blazebit.security.showcase.data.UserGroupDataAccess;
import com.blazebit.security.showcase.service.UserService;
import com.blazebit.security.spi.ActionFactory;
import com.blazebit.security.spi.ResourceFactory;
import com.blazebit.security.web.bean.model.FieldModel;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.bean.model.SubjectModel;
import com.blazebit.security.web.context.UserSession;

@Named(value = "securityBaseBean")
@ViewScoped
@Stateless
public class SecurityBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Inject
    protected EntityResourceFactory entityResourceFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected PermissionService permissionService;
    @Inject
    private ResourceFactory resourceFactory;
    @Inject
    protected EntityResourceMetamodel resourceMetamodel;
    @Inject
    protected PropertyDataAccess propertyDataAccess;
    @Inject
    private UserService userService;
    @Inject
    private UserGroupDataAccess userGroupDataAccess;
    @Inject
    private EntityActionFactory actionUtils;
    @Inject
    protected UserContext userContext;
    @Inject
    protected UserSession userSession;

    // WELD-001408 Unsatisfied dependencies for type [ResourceNameFactory] with qualifiers [@Default] at injection point
    // [[field] @Inject private com.blazebit.security.web.bean.SecurityBaseBean.resourceNameFactory]
    // TODO why???
    // @Inject
    // private ResourceNameFactory resourceNameFactory;

    protected List<Object> subjects = new ArrayList<Object>();
    protected List<EntityAction> selectedActions = new ArrayList<EntityAction>();
    protected List<EntityAction> selectedCollectionActions = new ArrayList<EntityAction>();
    protected IdHolder<?> selectedSubject;

    @PersistenceContext(unitName = "TestPU")
    private EntityManager entityManager;

    // /**
    // * reads the configuration for checking whether the given levels are enabled
    // *
    // * @param levels
    // * @return
    // */
    // public boolean isEnabled(String... levels) {
    // if (levels.length == 0) {
    // return false;
    // }
    // for (String level : levels) {
    // if (!Boolean.valueOf(propertyDataAccess.getPropertyValue(level))) {
    // return false;
    // }
    // }
    // return true;
    // }

    public boolean isEnabled(String level) {
        return Boolean.valueOf(propertyDataAccess.getPropertyValue(level));
    }

    public boolean isActAsEnabled(User user) {
        boolean actAsUserEnabled = permissionService.isGranted(actionFactory.createAction(Action.ACT_AS), resourceFactory.createResource(user));
        if (!actAsUserEnabled) {
            List<UserGroup> groups = userGroupDataAccess.getGroupsForUser(user);
            for (UserGroup group : groups) {
                actAsUserEnabled = permissionService.isGranted(actionFactory.createAction(Action.ACT_AS), resourceFactory.createResource(group));
                if (actAsUserEnabled) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * Authorization check for role and actions. At least one of the actions should be available.
     * 
     * @param role
     * @param Action
     * @return
     */
    protected boolean isAuthorized(Role role, String... actions) {
        if (actions.length == 0) {
            return false;
        }
        for (String action : actions) {
            if (isGranted(action, resourceFactory.createResource(role))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Authorization check for subject and actions. At least one of the actions should be available.
     * 
     * @param subject
     * @param Action
     * @return
     */
    protected boolean isAuthorized(Subject subject, String... actions) {
        if (actions.length == 0) {
            return false;
        }
        for (String action : actions) {
            if (isGranted(action, resourceFactory.createResource(subject))) {
                return true;
            }
        }
        return false;
    }

    /**
     * authorization check for a concrete object. if checking update operation, if one of the fields of the entity object is
     * updateable the operation should be allowed.
     * 
     * @param actionConstant
     * @param entityObject
     * @return
     */
    public boolean isAuthorizedResource(String action, Object entityObject) {
        return isAuthorizedResource(action, entityObject, EntityField.EMPTY_FIELD);
    }

    /**
     * authorization check for a concrete object. if checking update operation, if one of the fields of the entity object is
     * updateable the operation should be allowed.
     * 
     * @param actionConstant
     * @param entityObject
     * @return
     */
    public boolean isAuthorizedResources(String action, Collection<Object> entityObjects) {
        for (Object entityObject : entityObjects) {
            boolean authorized = isAuthorizedResource(action, entityObject, EntityField.EMPTY_FIELD);
            if (authorized) {
                return true;
            }
        }
        return false;
    }

    /**
     * authorization check for a field of a concrete object
     * 
     * @param actionConstant
     * @param entityObject
     * @return
     */
    public boolean isAuthorizedResource(String action, Object entityObject, String fieldName) {
        return isAuthorizedResource(action, entityObject, fieldName, false);
    }

    /**
     * authorization check for a field of a concrete object
     * 
     * @param actionConstant
     * @param entityObject
     * @return
     */
    public boolean isAuthorizedResource(String action, Object entityObject, String fieldName, boolean strict) {
        if (entityObject == null || !(entityObject instanceof IdHolder<?>)) {
            return false;
        }
        Resource resource = entityResourceFactory.createResource(entityObject, fieldName);
        if (Action.GRANT.equals(action) || Action.REVOKE.equals(action)) {
            // object permission can only be granted if object level is enabled
            if (!isEnabled(Features.OBJECT_LEVEL) && (!(entityObject instanceof Subject) && !(entityObject instanceof Role))) {
                return false;
            }
        } else if (Action.ADD.equals(action) || Action.REMOVE.equals(action)) {
            if (!isEnabled(Features.FIELD_LEVEL)) {
                return isAuthorizedResource(Action.UPDATE, entityObject);
            }
        } else {
            if (!strict) {
                if (isGranted(action, resource)) {
                    return true;
                } else {
                    // found field that has permission to perform this action
                    boolean foundFieldPermission = false;
                    if (fieldName.equals(EntityField.EMPTY_FIELD)) {
                        foundFieldPermission = findFieldPermissionFor(entityObject, resource, action);
                    }
                    if (foundFieldPermission) {
                        return true;
                    }
                }
            }

        }
        return isGranted(action, resource);
    }

    private boolean findFieldPermissionFor(Object entityObject, Resource resource, String action) {
        List<String> fields;
        try {
            fields = resourceMetamodel.getPrimitiveFields(((EntityField) resource).getEntity());
            boolean foundOneUpdateableField = false;
            for (String field : fields) {
                if (isAuthorizedResource(action, entityObject, field)) {
                    foundOneUpdateableField = true;
                }
            }
            if (foundOneUpdateableField) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    protected boolean isGranted(String action, Resource resource) {
        return permissionService.isGranted(userContext.getUser(), actionFactory.createAction(action), resource);
    }

    public Object getSelectedSubject() {
        return selectedSubject;
    }

    public void setSelectedSubject(IdHolder<?> selectedSubject) {
        this.selectedSubject = selectedSubject;
    }

    public void selectSubject(ValueChangeEvent event) {
        selectedSubject = null;
        String subjectModel = (String) event.getNewValue();
        String className = subjectModel.split("-")[0];
        String id = subjectModel.split("-")[1];
        if (subjectModel != null) {
            try {
                selectedSubject = (IdHolder<?>) entityManager.find(Class.forName(className), Integer.valueOf(id));
            } catch (NumberFormatException e) {
            } catch (ClassNotFoundException e) {
            }
        }
    }

    public void selectAction(ValueChangeEvent event) {
        selectedActions.clear();
        for (String action : (String[]) event.getNewValue()) {
            selectedActions.add((EntityAction) actionFactory.createAction(action));
        }
    }

    public void selectCollectionAction(ValueChangeEvent event) {
        selectedCollectionActions.clear();
        for (String action : (String[]) event.getNewValue()) {
            selectedCollectionActions.add((EntityAction) actionFactory.createAction(action));
        }
    }

    // to grant object permissions
    public List<Action> getEntityActions() {
        List<Action> ret = new ArrayList<Action>();
        ret.addAll(actionUtils.getActionsForEntityObject());
        return ret;
    }

    // to grant object permissions
    public List<Action> getCollectionActions() {
        List<Action> ret = new ArrayList<Action>();
        ret.addAll(actionUtils.getUpdateActionsForCollectionField());
        return ret;
    }

    public List<EntityAction> getSelectedActions() {
        return selectedActions;
    }

    public List<EntityAction> getSelectedCollectionActions() {
        return selectedCollectionActions;
    }

    public List<Object> getSubjects() {
        return subjects;
    }

    public void initSubjects() {
        subjects.clear();
        List<User> users = userService.findUsers(userSession.getSelectedCompany());
        for (User user : users) {
            subjects.add(new SubjectModel(user));
        }
        List<UserGroup> userGroups = userGroupDataAccess.getAllParentGroups(userSession.getSelectedCompany());
        for (UserGroup ug : userGroups) {
            addToList(ug);
        }

    }

    private void addToList(UserGroup ug) {
        subjects.add(new SubjectModel(ug));
        for (UserGroup child : ug.getUserGroups()) {
            addToList(child);
        }
    }

    protected boolean isSelected(List<RowModel> ret) {
        for (RowModel r : ret) {
            if (r.isSelected()) {
                return true;
            }
        }
        return false;
    }

    protected boolean isSelectedFields(Collection<FieldModel> ret) {
        for (FieldModel r : ret) {
            if (r.isSelected()) {
                return true;
            }
        }
        return false;
    }

}
