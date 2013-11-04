package com.blazebit.security.web.demo;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.reflection.ReflectionUtils;
import com.blazebit.security.Action;
import com.blazebit.security.IdHolder;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.web.bean.PermissionManager;
import com.blazebit.security.web.bean.SecurityBaseBean;
import com.blazebit.security.web.bean.model.EditModel;
import com.blazebit.security.web.bean.model.FieldModel;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.bean.model.SubjectModel;
import com.blazebit.security.web.service.api.ActionUtils;
import com.blazebit.security.web.service.impl.UserGroupService;
import com.blazebit.security.web.service.impl.UserService;
import com.blazebit.security.web.util.WebUtil;

@Named
@ViewScoped
@Stateless
public class CarrierBean extends SecurityBaseBean {

    @Inject
    private UserService userService;

    @Inject
    private UserGroupService userGroupService;

    private List<RowModel> carriers = new ArrayList<RowModel>();

    @PersistenceContext(unitName = "TestPU")
    EntityManager entityManager;

    @Inject
    private PermissionManager permissionManager;

    @Inject
    private ActionUtils actionUtils;

    private Carrier newCarrier = new Carrier();
    private Carrier selectedCarrier;
    private EditModel selectedCarrierModel;

    private List<Contact> contacts = new ArrayList<Contact>();
    private Contact newContact = new Contact();
    private Contact selectedContact;

    private List<CarrierGroup> groups = new ArrayList<CarrierGroup>();
    private CarrierGroup newGroup = new CarrierGroup();
    private boolean selectAll;
    private List<FieldModel> fields = new ArrayList<FieldModel>();

    private List<Object> subjects = new ArrayList<Object>();
    private List<EntityAction> selectedActions = new ArrayList<EntityAction>();
    private Object selectedSubject;

    public Object getSelectedSubject() {
        return selectedSubject;
    }

    public void setSelectedSubject(Object selectedSubject) {
        this.selectedSubject = selectedSubject;
    }

    public List<CarrierGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<CarrierGroup> groups) {
        this.groups = groups;
    }

    public CarrierGroup getNewGroup() {
        return newGroup;
    }

    public void setNewGroup(CarrierGroup newGroup) {
        this.newGroup = newGroup;
    }

    public CarrierGroup getSelectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(CarrierGroup selectedGroup) {
        this.selectedGroup = selectedGroup;
    }

    private CarrierGroup selectedGroup;

    private Party party;

    public void init() {
        carriers.clear();
        List<Carrier> existingCarriers = entityManager.createQuery("select carrier from " + Carrier.class.getCanonicalName() + " carrier", Carrier.class).getResultList();
        for (Carrier carrier : existingCarriers) {
            carriers.add(new RowModel(carrier, new StringBuilder()
                .append(carrier.getClass().getSimpleName())
                .append("-")
                .append(carrier.getField1())
                .append(",")
                .append(carrier.getField2())
                .append(",")
                .append(carrier.getField3())
                .append(",")
                .append(carrier.getField4())
                .append(",")
                .append(carrier.getField5())
                .toString()));
        }
        selectedCarrier = new Carrier();
        fields.clear();
        fields.add(new FieldModel("field1"));
        fields.add(new FieldModel("field2"));
        fields.add(new FieldModel("field3"));
        fields.add(new FieldModel("field4"));
        fields.add(new FieldModel("field5"));

        List<User> users = userService.findUsers(userSession.getSelectedCompany());
        for (User user : users) {
            subjects.add(new SubjectModel(user));
        }
        List<UserGroup> userGroups = userGroupService.getAllParentGroups(userSession.getSelectedCompany());
        for (UserGroup ug : userGroups) {
            addToList(ug);
        }

        selectedCarrierModel = new EditModel();
        selectedCarrierModel.getFields().put("field1", new FieldModel("field1"));
        selectedCarrierModel.getFields().put("field2", new FieldModel("field2"));
        selectedCarrierModel.getFields().put("field3", new FieldModel("field3"));
        selectedCarrierModel.getFields().put("field4", new FieldModel("field4"));
        selectedCarrierModel.getFields().put("field5", new FieldModel("field5"));

    }

    private void addToList(UserGroup ug) {
        subjects.add(new SubjectModel(ug));
        for (UserGroup child : ug.getUserGroups()) {
            addToList(child);
        }
    }

    public void tabChange() {
        if (party == null) {
            party = new Party();
        }
    }

    public void selectCarrier(Carrier carrier) {
        selectedCarrier = entityManager.find(Carrier.class, carrier.getId());
        selectedCarrierModel.setEntity(selectedCarrier);
        party = selectedCarrier.getParty();
        contacts = new ArrayList<Contact>(selectedCarrier.getContacts());
        groups = new ArrayList<CarrierGroup>(selectedCarrier.getGroups());
    }

    public EditModel getSelectedCarrierModel() {
        return selectedCarrierModel;
    }

    public void setSelectedCarrierModel(EditModel selectedCarrierModel) {
        this.selectedCarrierModel = selectedCarrierModel;
    }

    public void selectContact(Contact contact) {
        setSelectedContact(contact);
    }

    public void selectGroup(CarrierGroup group) {
        selectedGroup = group;
    }

    public void saveNewCarrier() {
        entityManager.persist(newCarrier);
        init();
        newCarrier = new Carrier();
    }

    public void saveCarrier() {
        entityManager.merge(selectedCarrier);
        init();

    }

    public void saveNewContact() {
        entityManager.persist(newContact);
        selectedCarrier.getContacts().add(newContact);
        selectedCarrier = entityManager.merge(selectedCarrier);
        contacts = new ArrayList<Contact>(selectedCarrier.getContacts());
        newContact = new Contact();
    }

    public void saveNewGroup() {
        newGroup.getCarriers().add(selectedCarrier);
        entityManager.persist(newGroup);
        selectedCarrier = entityManager.find(Carrier.class, selectedCarrier.getId());
        selectedCarrier.getGroups().add(newGroup);
        selectedCarrier = entityManager.merge(selectedCarrier);
        groups = new ArrayList<CarrierGroup>(selectedCarrier.getGroups());
        newGroup = new CarrierGroup();
    }

    public void deleteGroup(CarrierGroup carrierGroup) {
        if (carrierGroup.equals(selectedGroup)) {
            selectedGroup = null;
        }
        entityManager.remove(entityManager.find(CarrierGroup.class, carrierGroup.getId()));
        selectedCarrier.getGroups().remove(carrierGroup);
        selectedCarrier = entityManager.merge(selectedCarrier);
        groups = new ArrayList<CarrierGroup>(selectedCarrier.getGroups());
    }

    public void deleteContact(Contact contact) {
        if (contact.equals(selectedContact)) {
            selectedContact = null;
        }
        entityManager.remove(entityManager.find(Contact.class, contact.getId()));
        selectedCarrier.getContacts().remove(contact);
        selectedCarrier = entityManager.merge(selectedCarrier);
        contacts = new ArrayList<Contact>(selectedCarrier.getContacts());

    }

    public void deleteCarrier(Carrier carrier) {
        if (carrier.equals(selectedCarrier)) {
            selectedCarrier = null;
        }
        entityManager.remove(entityManager.find(Carrier.class, carrier.getId()));
        init();
    }

    public void saveParty() {
        if (party.getId() == null) {
            entityManager.persist(party);
        } else {
            entityManager.merge(party);
        }
        selectedCarrier.setParty(party);
        entityManager.merge(selectedCarrier);
        // selectedCarrier = entityManager.find(Carrier.class, selectedCarrier.getId());
    }

    public void saveContact() {
        entityManager.merge(selectedContact);
        selectedCarrier = entityManager.find(Carrier.class, selectedCarrier.getId());
        setContacts(new ArrayList<Contact>(selectedCarrier.getContacts()));
    }

    public void saveGroup() {
        entityManager.merge(selectedGroup);
        selectedCarrier = entityManager.find(Carrier.class, selectedCarrier.getId());
        groups = new ArrayList<CarrierGroup>(selectedCarrier.getGroups());
    }

    public Carrier getSelectedCarrier() {
        return selectedCarrier;
    }

    public void setSelectedCarrier(Carrier selectedCarrier) {
        this.selectedCarrier = selectedCarrier;
    }

    public List<RowModel> getCarriers() {
        return carriers;
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public Contact getSelectedContact() {
        return selectedContact;
    }

    public void setSelectedContact(Contact selectedContact) {
        this.selectedContact = selectedContact;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public Carrier getNewCarrier() {
        return newCarrier;
    }

    public Contact getNewContact() {
        return newContact;
    }

    public void setNewContact(Contact newContact) {
        this.newContact = newContact;
    }

    public void setNewCarrier(Carrier newCarrier) {
        this.newCarrier = newCarrier;
    }

    public void grant(Carrier carrier, String field) {
        WebUtil.redirect(FacesContext.getCurrentInstance(), "/blaze-security-showcase/resource/resources.xhtml?id=" + carrier.getId() + "&resource=" + carrier.getClass().getName()
            + "&field=" + field, false);
    }

    public void grant(Carrier carrier) {
        WebUtil.redirect(FacesContext.getCurrentInstance(),
                         "/blaze-security-showcase/resource/resources.xhtml?id=" + carrier.getId() + "&resource=" + carrier.getClass().getName(), false);
    }

    public void revoke(Carrier carrier) {
    }

    public void revoke(Carrier carrier, String field) {
    }

    public void selectAllCarriers() {
        for (RowModel rowModel : carriers) {
            rowModel.setSelected(selectAll);
        }
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public List<FieldModel> getFields() {
        return fields;
    }

    public boolean isSelectedField(String field) {
        for (FieldModel fieldModel : fields) {
            if (fieldModel.getFieldName().equals(field)) {
                return fieldModel.isSelected();
            }
        }
        return false;
    }

    public void setFields(List<FieldModel> fields) {
        this.fields = fields;
    }

    public void goToPermissions() {
        permissionManager.setSelectedSubject((IdHolder) selectedSubject);
        permissionManager.setSelectedActions(selectedActions);
        permissionManager.getSelectedFields().clear();
        for (FieldModel fieldModel : selectedCarrierModel.getFields().values()) {
            if (fieldModel.isSelected()) {
                permissionManager.getSelectedFields().add(fieldModel.getFieldName());
            }
        }
        permissionManager.getSelectedObjects().clear();
        for (RowModel rowModel : carriers) {
            if (rowModel.isSelected()) {
                permissionManager.getSelectedObjects().add(rowModel);
            }
        }
        WebUtil.redirect(FacesContext.getCurrentInstance(), "/blaze-security-showcase/permissionManager.xhtml", false);

    }

    public List<Object> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Object> subjects) {
        this.subjects = subjects;
    }

    public void selectSubject(ValueChangeEvent event) {
        selectedSubject = null;
        String subjectModel = (String) event.getNewValue();
        String className = subjectModel.split("-")[0];
        String id = subjectModel.split("-")[1];
        if (subjectModel != null) {
            try {
                selectedSubject = entityManager.find(Class.forName(className), Integer.valueOf(id));
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

    public List<Action> getActions() {
        return actionUtils.getActionsForEntityObject();
    }

    public List<EntityAction> getSelectedActions() {
        return selectedActions;
    }

    public void selectField(String field) {
        for (FieldModel model : fields) {
            if (model.getFieldName().endsWith(field)) {
                model.setSelected(true);
            }
        }
    }

}
