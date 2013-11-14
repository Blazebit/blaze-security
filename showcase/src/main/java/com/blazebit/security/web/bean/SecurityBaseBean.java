package com.blazebit.security.web.bean;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.IdHolder;
import com.blazebit.security.PermissionService;
import com.blazebit.security.Resource;
import com.blazebit.security.ResourceFactory;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserDataPermission;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupDataPermission;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.bean.model.SubjectModel;
import com.blazebit.security.web.service.api.ActionUtils;
import com.blazebit.security.web.service.api.UserGroupService;
import com.blazebit.security.web.service.api.UserService;
import com.blazebit.security.web.util.FieldUtils;

@Named
@ViewScoped
@Stateless
public class SecurityBaseBean {

    @Inject
    protected EntityResourceFactory entityFieldFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected PermissionService permissionService;
    @Inject
    protected UserSession userSession;

    protected List<Object> subjects = new ArrayList<Object>();
    protected List<EntityAction> selectedActions = new ArrayList<EntityAction>();
    protected List<EntityAction> selectedCollectionActions = new ArrayList<EntityAction>();
    protected IdHolder selectedSubject;

    @Inject
    private ActionUtils actionUtils;

    @PersistenceContext(unitName = "TestPU")
    EntityManager entityManager;

    /**
     * authorization check for resource name with field. usage from EL
     * 
     * @param actionConstant
     * @param resourceName
     * @param field
     * @return
     */
    public boolean isAuthorized(ActionConstants actionConstant, String resourceName, String field) {
        try {
            return isGranted(actionConstant, entityFieldFactory.createResource(Class.forName(resourceName), field));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * authorization check for resource name. usage from EL
     * 
     * @param actionConstant
     * @param resourceName
     * @return
     */
    public boolean isAuthorized(ActionConstants actionConstant, String resourceName) {
        try {
            return isGranted(actionConstant, entityFieldFactory.createResource(Class.forName(resourceName)));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * authorization check for a concrete object. if checking update operation, if one of the fields of the entity object is
     * updateable the operation should be allowed.
     * 
     * @param actionConstant
     * @param entityObject
     * @return
     */
    public boolean isAuthorizedResource(ActionConstants actionConstant, Object entityObject) {
        switch (actionConstant) {
            case READ:
                if (isGranted(ActionConstants.READ, entityFieldFactory.createResource(entityObject.getClass(), ((IdHolder) entityObject).getId()))) {
                    return true;
                } else {
                    List<Field> primitives = FieldUtils.getPrimitiveFields(entityObject.getClass());
                    boolean foundOneUpdateableField = false;
                    for (Field field : primitives) {
                        if (isAuthorizedResource(ActionConstants.READ, entityObject, field.getName())) {
                            foundOneUpdateableField = true;
                        }
                    }
                    if (foundOneUpdateableField) {
                        return true;
                    }
                }
                // if action is to read or update at least one field has to be readable or updatable
            case UPDATE:
                List<Field> primitives = FieldUtils.getPrimitiveFields(entityObject.getClass());
                boolean foundOneUpdateableField = false;
                for (Field field : primitives) {
                    if (isAuthorizedResource(ActionConstants.UPDATE, entityObject, field.getName())) {
                        foundOneUpdateableField = true;
                    }
                }
                if (foundOneUpdateableField) {
                    return true;
                }
                break;
            case ADD:
            case REMOVE:
                if (!userSession.getSelectedCompany().isFieldLevelEnabled()) {
                    isAuthorizedResource(ActionConstants.UPDATE, entityObject);
                }

        }

        if (entityObject != null && entityObject instanceof IdHolder) {
            Integer id = null;
            if (((IdHolder) entityObject).getId() != null) {
                id = ((IdHolder) entityObject).getId();
            }
            return isGranted(actionConstant, entityFieldFactory.createResource(entityObject.getClass(), id));
        } else {
            return false;
            // throw new IllegalArgumentException("entityobject empty for " + actionConstant);
        }
    }

    @Inject
    private ResourceFactory resourceFactory;

    protected boolean isAuthorizedToGrantRevoke(Subject subject) {
        if (!isGranted(ActionConstants.GRANT, resourceFactory.createResource(subject)) && !isGranted(ActionConstants.REVOKE, resourceFactory.createResource(subject))) {
            return false;
        }

        return true;
    }

    protected boolean isAuthorizedToGrantRevoke(Role role) {
        if (!isGranted(ActionConstants.GRANT, resourceFactory.createResource(role)) && !isGranted(ActionConstants.REVOKE, resourceFactory.createResource(role))) {
            return false;
        }
        return true;
    }

    /**
     * authorization check for a field of a concrete object
     * 
     * @param actionConstant
     * @param entityObject
     * @return
     */
    public boolean isAuthorizedResource(ActionConstants actionConstant, Object entityObject, String field) {
        if (entityObject != null && entityObject instanceof IdHolder) {
            return isGranted(actionConstant, entityFieldFactory.createResource(entityObject.getClass(), field, ((IdHolder) entityObject).getId()));
        } else {
            // throw new IllegalArgumentException("entityobject empty for " + actionConstant + "field: " + field);
            return false;
        }
    }

    protected boolean isGranted(ActionConstants actionConstant, Resource resource) {
        return permissionService.isGranted(userSession.getUser(), actionFactory.createAction(actionConstant), resource);
    }

    public Object getSelectedSubject() {
        return selectedSubject;
    }

    public void setSelectedSubject(IdHolder selectedSubject) {
        this.selectedSubject = selectedSubject;
    }

    public void selectSubject(ValueChangeEvent event) {
        selectedSubject = null;
        String subjectModel = (String) event.getNewValue();
        String className = subjectModel.split("-")[0];
        String id = subjectModel.split("-")[1];
        if (subjectModel != null) {
            try {
                selectedSubject = (IdHolder) entityManager.find(Class.forName(className), Integer.valueOf(id));
            } catch (NumberFormatException e) {
            } catch (ClassNotFoundException e) {
            }
        }
    }

    public void selectAction(ValueChangeEvent event) {
        selectedActions.clear();
        for (String action : (String[]) event.getNewValue()) {
            selectedActions.add((EntityAction) actionFactory.createAction(ActionConstants.valueOf(action)));
        }
    }

    public void selectCollectionAction(ValueChangeEvent event) {
        selectedCollectionActions.clear();
        for (String action : (String[]) event.getNewValue()) {
            selectedCollectionActions.add((EntityAction) actionFactory.createAction(ActionConstants.valueOf(action)));
        }
    }

    public List<Action> getActions() {
        List<Action> ret = new ArrayList<Action>();
        ret.addAll(actionUtils.getActionsForEntityObject());
        return ret;
    }

    public List<Action> getCollectionActions() {
        List<Action> ret = new ArrayList<Action>();
        ret.addAll(actionUtils.getActionsForCollectionField());
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

    @Inject
    private UserService userService;

    @Inject
    private UserGroupService userGroupService;

    public void initSubjects() {
        subjects.clear();
        List<User> users = userService.findUsers(userSession.getSelectedCompany());
        for (User user : users) {
            subjects.add(new SubjectModel(user));
        }
        List<UserGroup> userGroups = userGroupService.getAllParentGroups(userSession.getSelectedCompany());
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

    public boolean isRelatedTabEntity() {
        return false;
    }

    protected boolean isSelected(List<RowModel> ret) {
        for (RowModel r : ret) {
            if (r.isSelected()) {
                return true;
            }
        }
        return false;
    }

}
