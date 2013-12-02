package com.blazebit.security.web.bean;

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
import com.blazebit.security.impl.el.utils.ELUtils;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.utils.ActionUtils;
import com.blazebit.security.metamodel.ResourceMetamodel;
import com.blazebit.security.service.api.PropertyDataAccess;
import com.blazebit.security.spi.EntityResource;
import com.blazebit.security.spi.ResourceDefinition;
import com.blazebit.security.web.bean.model.FieldModel;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.bean.model.SubjectModel;
import com.blazebit.security.web.service.api.UserGroupDataAccess;
import com.blazebit.security.web.service.api.UserService;

@Named
@ViewScoped
@Stateless
public class SecurityBaseBean {

    @Inject
    protected EntityResourceFactory entityResourceFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected PermissionService permissionService;
    @Inject
    private ResourceFactory resourceFactory;
    @Inject
    protected UserSession userSession;
    @Inject
    protected ResourceMetamodel resourceMetamodel;
    @Inject
    protected PropertyDataAccess propertyDataAccess;
    @Inject
    private UserService userService;
    @Inject
    private UserGroupDataAccess userGroupDataAccess;
    @Inject
    private ActionUtils actionUtils;
    // WELD-001408 Unsatisfied dependencies for type [ResourceNameFactory] with qualifiers [@Default] at injection point
    // [[field] @Inject private com.blazebit.security.web.bean.SecurityBaseBean.resourceNameFactory]
    // TODO why???
    // @Inject
    // private ResourceNameFactory resourceNameFactory;

    protected List<Object> subjects = new ArrayList<Object>();
    protected List<EntityAction> selectedActions = new ArrayList<EntityAction>();
    protected List<EntityAction> selectedCollectionActions = new ArrayList<EntityAction>();
    protected IdHolder selectedSubject;

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

    /**
     * Authorization check for role and actions. At least one of the actions should be available.
     * 
     * @param role
     * @param actionConstants
     * @return
     */
    protected boolean isAuthorized(Role role, ActionConstants... actionConstants) {
        if (actionConstants.length == 0) {
            return false;
        }
        for (ActionConstants actionConstant : actionConstants) {
            if (isGranted(actionConstant, resourceFactory.createResource(role))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Authorization check for subject and actions. At least one of the actions should be available.
     * 
     * @param subject
     * @param actionConstants
     * @return
     */
    protected boolean isAuthorized(Subject subject, ActionConstants... actionConstants) {
        if (actionConstants.length == 0) {
            return false;
        }
        for (ActionConstants actionConstant : actionConstants) {
            if (isGranted(actionConstant, resourceFactory.createResource(subject))) {
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
    public boolean isAuthorizedResource(ActionConstants actionConstant, Object entityObject) {
        return isAuthorizedResource(actionConstant, entityObject, EntityField.EMPTY_FIELD);
    }

    /**
     * authorization check for a field of a concrete object
     * 
     * @param actionConstant
     * @param entityObject
     * @return
     */
    public boolean isAuthorizedResource(ActionConstants actionConstant, Object entityObject, String fieldName) {
        return isAuthorizedResource(actionConstant, entityObject, fieldName, false);
    }

    // TODO workaround because resourceNameFactory injection didnt work
    public Resource createResource(IdHolder entityObject) {
        return createResource(entityObject, EntityField.EMPTY_FIELD);
    }

    // TODO resourceNameFactory injection didnt work
    public Resource createResource(IdHolder entityObject, String field) {
        if (entityObject == null) {
            return null;
        }
        List<ResourceDefinition> resourceDefinitions = resourceMetamodel.getEntityResources().get(new EntityResource(entityObject.getClass().getName()));
        Map<String, Object> variableMap = new HashMap<String, Object>();

        variableMap.put("object", entityObject);

        if (resourceDefinitions.size() == 1) {
            ResourceDefinition def = resourceDefinitions.get(0);

            if (com.blazebit.lang.StringUtils.isEmpty(def.getTestExpression())) {
                return entityResourceFactory.createResource(def.getResourceName(), field, entityObject.getId());
            }
        }

        for (ResourceDefinition def : resourceDefinitions) {
            if (Boolean.TRUE.equals(ELUtils.getValue(def.getTestExpression(), Boolean.class, variableMap))) {
                return entityResourceFactory.createResource(def.getResourceName(), field, entityObject.getId());
            }
        }

        throw new RuntimeException("No resource definition found!!");
    }

    /**
     * authorization check for a field of a concrete object
     * 
     * @param actionConstant
     * @param entityObject
     * @return
     */
    public boolean isAuthorizedResource(ActionConstants actionConstant, Object entityObject, String fieldName, boolean strict) {
        if (entityObject == null || !(entityObject instanceof IdHolder)) {
            return false;
        }
        Resource resource = createResource((IdHolder) entityObject, fieldName);
        switch (actionConstant) {
        // object permission can only be granted if object level is enabled
            case GRANT:
            case REVOKE:
                if (!Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.OBJECT_LEVEL)) && (!(entityObject instanceof Subject) && !(entityObject instanceof Role))) {
                    return false;
                }
                break;
            case ADD:
            case REMOVE:
                if (!Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.FIELD_LEVEL))) {
                    return isAuthorizedResource(ActionConstants.UPDATE, entityObject);
                }
                break;
            default:
                if (!strict) {
                    if (isGranted(actionConstant, resource)) {
                        return true;
                    } else {
                        // found field that has permission to perform this action
                        boolean foundFieldPermission = false;
                        if (fieldName.equals(EntityField.EMPTY_FIELD)) {
                            foundFieldPermission = findFieldPermissionFor(entityObject, resource, actionConstant);
                        }
                        if (foundFieldPermission) {
                            return true;
                        }
                    }
                }
                break;

        }
        return isGranted(actionConstant, resource);
    }

    private boolean findFieldPermissionFor(Object entityObject, Resource resource, ActionConstants action) {
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
