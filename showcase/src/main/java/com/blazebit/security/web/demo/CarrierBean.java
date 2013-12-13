package com.blazebit.security.web.demo;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.primefaces.event.TabChangeEvent;

import com.blazebit.security.IdHolder;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.Comment;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.web.bean.base.SecurityBean;
import com.blazebit.security.web.bean.main.resources.ResourceObjectBean;
import com.blazebit.security.web.bean.model.EditModel;
import com.blazebit.security.web.bean.model.FieldModel;
import com.blazebit.security.web.bean.model.FieldModel.Type;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.util.WebUtil;

@Named
@ViewScoped
@Stateless
public class CarrierBean extends SecurityBean {

    @PersistenceContext(unitName = "TestPU")
    EntityManager entityManager;

    @Inject
    private ResourceObjectBean resourceObjectBean;

    private EditModel selectedCarrierModel;

    private List<RowModel> contacts = new ArrayList<RowModel>();
    private EditModel selectedContactModel;
    private boolean selectAll;
    private List<FieldModel> fields = new ArrayList<FieldModel>();

    private List<RowModel> carriers = new ArrayList<RowModel>();

    private EditModel partyModel;

    private String tabIndex;

    public void init() {
        initCarriers();
        fields.clear();
        fields.add(new FieldModel("field1"));
        fields.add(new FieldModel("field2"));
        fields.add(new FieldModel("field3"));
        fields.add(new FieldModel("field4"));
        fields.add(new FieldModel("field5"));

        selectedCarrierModel = new EditModel(new Carrier());
        selectedCarrierModel.getFields().put("field1", new FieldModel("field1", Type.PRIMITIVE));
        selectedCarrierModel.getFields().put("field2", new FieldModel("field2", Type.PRIMITIVE));
        selectedCarrierModel.getFields().put("field3", new FieldModel("field3", Type.PRIMITIVE));
        selectedCarrierModel.getFields().put("field4", new FieldModel("field4", Type.PRIMITIVE));
        selectedCarrierModel.getFields().put("field5", new FieldModel("field5", Type.PRIMITIVE));

        selectedCarrierModel.getFields().put("party", new FieldModel("party", Type.PRIMITIVE));
        selectedCarrierModel.getFields().put("comment", new FieldModel("comment", Type.PRIMITIVE));
        selectedCarrierModel.getFields().put("contacts", new FieldModel("contacts", Type.COLLECTION));

        partyModel = new EditModel(new Party("carrier"));
        partyModel.getFields().put("partyField1", new FieldModel("partyField1"));
        partyModel.getFields().put("partyField2", new FieldModel("partyField2"));

        selectedContactModel = new EditModel(new Contact());
        selectedContactModel.getFields().put("contactField", new FieldModel("contactField"));
        tabIndex = "carrierTab";

    }

    private void initCarriers() {
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
    }

    public void tabChange(TabChangeEvent event) {
        if (partyModel.getEntity() == null) {
            partyModel.setEntity(new Party("Carrier"));
        }
        tabIndex = event.getTab().getId();
    }

    public void selectCarrier(Carrier carrier) {
        Carrier selectedCarrier = entityManager.find(Carrier.class, carrier.getId());
        selectedCarrierModel.setEntity(selectedCarrier);
        partyModel.setEntity(selectedCarrier.getParty());
        List<Contact> allContacts = new ArrayList<Contact>(selectedCarrier.getContacts());
        contacts.clear();
        for (Contact c : allContacts) {
            contacts.add(new RowModel(c, "Contact: " + c.getContactField()));
        }
    }

    public EditModel getSelectedCarrierModel() {
        return selectedCarrierModel;
    }

    public void setSelectedCarrierModel(EditModel selectedCarrierModel) {
        this.selectedCarrierModel = selectedCarrierModel;
    }

    public void selectContact(Contact contact) {
        selectedContactModel.setEntity(contact);
    }

    public void saveCarrier() {
        selectedCarrierModel.setEntity(entityManager.merge(selectedCarrierModel.getEntity()));
        initCarriers();
    }

    public void saveNewCarrier() {
        entityManager.persist(selectedCarrierModel.getEntity());
        selectCarrier((Carrier) selectedCarrierModel.getEntity());
        initCarriers();
    }

    public void newCarrier() {
        selectedCarrierModel.setEntity(new Carrier());
    }

    public void newContact() {
        selectedContactModel.setEntity(new Contact());
    }

    public void saveNewContact() {
        ((Contact) selectedContactModel.getEntity()).setCarrier((Carrier) selectedCarrierModel.getEntity());
        entityManager.persist(selectedContactModel.getEntity());
        entityManager.flush();
        Carrier selectedCarrier = entityManager.find(Carrier.class, selectedCarrierModel.getEntity().getId());

        List<Contact> allContacts = new ArrayList<Contact>(selectedCarrier.getContacts());
        contacts.clear();
        for (Contact c : allContacts) {
            contacts.add(new RowModel(c));
        }

    }

    public void deleteContact(Contact contact) {
        if (contact.equals(selectedContactModel.getEntity())) {
            selectedContactModel.setEntity(null);
        }
        entityManager.remove(entityManager.find(Contact.class, contact.getId()));
        entityManager.flush();
        Carrier selectedCarrier = entityManager.find(Carrier.class, selectedCarrierModel.getEntity().getId());
        
        List<Contact> allContacts = new ArrayList<Contact>(selectedCarrier.getContacts());
        contacts.clear();
        for (Contact c : allContacts) {
            contacts.add(new RowModel(c));
        }
        selectedCarrierModel.setEntity(selectedCarrier);

    }

    public void deleteCarrier(Carrier carrier) {
        if (carrier.equals(((Carrier) selectedCarrierModel.getEntity()))) {
            selectedCarrierModel.setEntity(null);
        }
        entityManager.remove(entityManager.find(Carrier.class, carrier.getId()));
        init();
    }

    public void saveParty() {
        partyModel.setEntity(entityManager.merge(partyModel.getEntity()));
        ((Carrier) selectedCarrierModel.getEntity()).setParty((Party) partyModel.getEntity());
        entityManager.merge(((Carrier) selectedCarrierModel.getEntity()));
        // selectedCarrier = entityManager.find(Carrier.class, selectedCarrier.getId());
    }

    public void saveNewParty() {
        entityManager.persist(partyModel.getEntity());
        ((Carrier) selectedCarrierModel.getEntity()).setParty((Party) partyModel.getEntity());
        selectedCarrierModel.setEntity(entityManager.merge(((Carrier) selectedCarrierModel.getEntity())));
        // selectedCarrier = entityManager.find(Carrier.class, selectedCarrier.getId());
    }

    public void saveContact() {
        selectedContactModel.setEntity(entityManager.merge(selectedContactModel.getEntity()));
        Carrier selectedCarrier = entityManager.find(Carrier.class, ((Carrier) selectedCarrierModel.getEntity()).getId());
        selectedCarrierModel.setEntity(selectedCarrier);
        List<Contact> allContacts = new ArrayList<Contact>(selectedCarrier.getContacts());
        contacts.clear();
        for (Contact c : allContacts) {
            contacts.add(new RowModel(c));
        }

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

    public void goToPermissions(String action) {
        // check if user can grant to selected subject
        boolean allowed = false;
        if (selectedSubject instanceof Subject) {
            allowed = isAuthorized((Subject) selectedSubject, "grant".equals(action) ? ActionConstants.GRANT : ActionConstants.REVOKE);
        } else {
            allowed = isAuthorized((Role) selectedSubject, "grant".equals(action) ? ActionConstants.GRANT : ActionConstants.REVOKE);
        }
        if (!allowed) {
            return;
        }
        if (selectedSubject != null
            && (!selectedActions.isEmpty() || (!selectedCollectionActions.isEmpty() && isSelectedFields(selectedCarrierModel.getCollectionFields().values())))) {

            if (getTabEntity() instanceof Carrier) {
                goToObjectResourceManagement(action, (IdHolder) selectedSubject, selectedActions, selectedCollectionActions, selectedCarrierModel, carriers);
            } else {
                if (getTabEntity() instanceof Party) {
                    if (partyModel.getEntity().getId() != null) {
                        List<RowModel> ret = new ArrayList<RowModel>();
                        ret.add(new RowModel(partyModel.getEntity(), partyModel.isSelected(), "Party-" + ((Party) partyModel.getEntity()).getPartyField1() + ", "
                            + ((Party) partyModel.getEntity()).getPartyField2()));
                        if (isSelected(ret)) {
                            goToObjectResourceManagement(action, (IdHolder) selectedSubject, selectedActions, selectedCollectionActions, partyModel, ret);
                        } else {
                            System.err.println("Select party");
                        }
                    }
                } else {
                    if (getTabEntity() instanceof Contact) {
                        if (isSelected(contacts)) {
                            goToObjectResourceManagement(action, (IdHolder) selectedSubject, selectedActions, selectedCollectionActions, selectedContactModel, contacts);
                        } else {
                            System.err.println("Select contact");
                        }
                    }
                }
            }
        }

    }

    public void goToObjectResourceManagement(String action, IdHolder selectedSubject, List<EntityAction> selectedActions, List<EntityAction> selectedCollectionActions, EditModel selectedEditModel, List<RowModel> rowModelList) {
        resourceObjectBean.setAction(action);
        resourceObjectBean.setSelectedSubject(selectedSubject);
        resourceObjectBean.setSelectedActions(selectedActions);
        resourceObjectBean.setSelectedCollectionActions(selectedCollectionActions);
        resourceObjectBean.getSelectedFields().clear();
        for (FieldModel fieldModel : selectedEditModel.getFields().values()) {
            if (fieldModel.isSelected()) {
                resourceObjectBean.getSelectedFields().add(fieldModel);
            }
        }
        resourceObjectBean.getSelectedObjects().clear();
        for (RowModel rowModel : rowModelList) {
            if (rowModel.isSelected()) {
                resourceObjectBean.getSelectedObjects().add(rowModel);
            }
        }
        resourceObjectBean.setPrevPath(FacesContext.getCurrentInstance().getViewRoot().getViewId());
        WebUtil.redirect(FacesContext.getCurrentInstance(), "/blaze-security-showcase/main/resource/object_resources.xhtml", false);

    }

    public void setSubjects(List<Object> subjects) {
        this.subjects = subjects;
    }

    public void selectField(String field) {
        for (FieldModel model : fields) {
            if (model.getFieldName().endsWith(field)) {
                model.setSelected(true);
            }
        }
    }

    public List<RowModel> getCarriers() {
        return carriers;
    }

    public EditModel getPartyModel() {
        return partyModel;
    }

    public void setPartyModel(EditModel party) {
        this.partyModel = party;
    }

    public EditModel getSelectedContactModel() {
        return selectedContactModel;
    }

    public void setSelectedContactModel(EditModel selectedContact) {
        this.selectedContactModel = selectedContact;
    }

    public List<RowModel> getContacts() {
        return contacts;
    }

    public Object getTabEntity() {
        if ("carrierTab".equals(tabIndex)) {
            return new Carrier();
        } else {
            if ("contactTab".equals(tabIndex)) {
                return new Contact();
            } else {
                if ("partyTab".equals(tabIndex)) {
                    return new Party("Carrier");
                } else {
                    if ("commentTab".equals(tabIndex)) {
                        return new Comment();
                    }
                }
            }
        }
        return null;
    }

    public List<Comment> getComments() {
        List<Comment> result = entityManager.createQuery("select comment from " + Comment.class.getCanonicalName() + " comment where comment.user.company.id="
                                                             + userSession.getSelectedCompany().getId(), Comment.class).getResultList();
        return result;
    }

    public void selectComment(Comment comment) {
        ((Carrier) selectedCarrierModel.getEntity()).setComment(comment);
        selectedCarrierModel.setEntity(entityManager.merge((Carrier) selectedCarrierModel.getEntity()));
    }

}
