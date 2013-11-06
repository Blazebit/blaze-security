package com.blazebit.security.web.bean;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.inject.Inject;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.service.api.ActionUtils;
import com.blazebit.security.web.util.Constants;
import com.blazebit.security.web.util.FieldUtils;

public class ResourceHandlingBaseBean extends PermissionHandlingBaseBean {

    @Inject
    private ResourceNameExtension resourceNameExtension;

    @Inject
    protected EntityResourceFactory entityFieldFactory;

    @Inject
    protected ActionUtils actionUtils;

    protected DefaultTreeNode getResourceTree() {
        return getResourceTree(new HashSet<Permission>());
    }

    /**
     * builds up a resource tree
     * 
     * @return
     */
    protected DefaultTreeNode getResourceTree(Collection<Permission> selectedPermissions) {
        DefaultTreeNode root = new DefaultTreeNode();
        // gets all the entities that are annotated with resource name -> groups by module name
        Map<String, List<AnnotatedType<?>>> modules = resourceNameExtension.getResourceNamesByModule();
        for (String module : modules.keySet()) {
            if (!modules.get(module).isEmpty()) {
                // creates module as root
                DefaultTreeNode moduleNode = new DefaultTreeNode(new TreeNodeModel(module, TreeNodeModel.ResourceType.MODULE, null), root);
                moduleNode.setExpanded(true);
                for (AnnotatedType<?> type : modules.get(module)) {
                    Class<?> entityClass = (Class<?>) type.getBaseType();
                    // second level must always be an entity resource with the entity name
                    EntityField entityField = (EntityField) entityFieldFactory.createResource(entityClass);
                    // check if logged in user can grant or revoke these resources
                    if (isGranted(ActionConstants.GRANT, entityField) || isGranted(ActionConstants.REVOKE, entityField)) {
                        // entity
                        DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entityField.getEntity(), TreeNodeModel.ResourceType.ENTITY, entityField), moduleNode);
                        entityNode.setExpanded(true);
                        // first come action available only for entity
                        createActionNodes(actionUtils.getActionsForEntity(), entityField, selectedPermissions, entityNode);
                        // fields for entity
                        List<Field> primitiveFields = FieldUtils.getPrimitiveFields(entityClass);
                        if (!primitiveFields.isEmpty()) {
                            // actions for primitive fields
                            createActionNodes(actionUtils.getActionsForPrimitiveField(), primitiveFields, entityField, selectedPermissions, entityNode);
                        }
                        List<Field> collectionFields = FieldUtils.getCollectionFields(entityClass);
                        if (!collectionFields.isEmpty()) {
                            // actions for collection fields
                            createActionNodes(actionUtils.getActionsForCollectionField(), collectionFields, entityField, selectedPermissions, entityNode);
                        }
                        if (!primitiveFields.isEmpty() || !collectionFields.isEmpty()) {
                            // actions for all fields
                            primitiveFields.addAll(collectionFields);
                            createActionNodes(actionUtils.getActionsForGeneralField(), primitiveFields, entityField, selectedPermissions, entityNode);
                        }
                    }
                }
            }
        }
        return root;
    }

    private void createActionNodes(List<Action> actions, EntityField entityField, Collection<Permission> selectedPermissions, TreeNode entityNode) {
        for (Action action : actions) {
            EntityAction entityAction = (EntityAction) action;
            DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction), entityNode);
            actionNode.setExpanded(true);
            // selected
            Permission found = findWithoutIdDifferentiation(selectedPermissions, permissionFactory.create(entityAction, entityField));
            actionNode.setSelected(found != null);
        }
    }

    private void createActionNodes(List<Action> actions, List<Field> fields, EntityField entityField, Collection<Permission> selectedPermissions, TreeNode entityNode) {
        for (Action action : actions) {
            EntityAction entityAction = (EntityAction) action;
            DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction), entityNode);
            actionNode.setExpanded(true);
            // fields
            createFieldNodes(fields, actionNode, entityAction, entityField, selectedPermissions);
            propagateNodePropertiesUpwards(actionNode);
        }
    }

    private void createFieldNodes(List<Field> fields, DefaultTreeNode actionNode, EntityAction entityAction, EntityField entityField, Collection<Permission> selectedPermissions) {
        for (Field field : fields) {
            EntityField entityFieldWithField = entityField.getChild(field.getName());
            DefaultTreeNode fieldNode = new DefaultTreeNode(null, actionNode);
            TreeNodeModel fieldNodeModel = new TreeNodeModel(field.getName(), TreeNodeModel.ResourceType.FIELD, entityFieldWithField);
            // decide how field node should be marked (red, blue, green)
            Permission foundWithField = findWithoutIdDifferentiation(selectedPermissions, permissionFactory.create(entityAction, entityFieldWithField));
            Permission foundWithoutField = findWithoutIdDifferentiation(selectedPermissions, permissionFactory.create(entityAction, entityField));

            if (foundWithField != null || foundWithoutField != null) {
                // entity object resources are blue
                if ((foundWithField != null && foundWithField.getResource() instanceof EntityObjectField)
                    || (foundWithoutField != null && foundWithoutField.getResource() instanceof EntityObjectField)) {
                    fieldNodeModel.setMarking(Marking.BLUE);
                    fieldNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
                } else {
                    fieldNode.setSelected(true);
                }
                // fieldNode.setSelectable(isAuthorized(ActionConstants.REVOKE, entityField));
            } else {
                // permission can be granted if logged in user has permission to do it
                // fieldNode.setSelectable(isAuthorized(ActionConstants.GRANT, entityField));
            }
            fieldNode.setData(fieldNodeModel);
            fieldNode.setExpanded(true);
        }
    }
}
