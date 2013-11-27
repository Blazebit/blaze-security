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

import com.blazebit.lang.StringUtils;
import com.blazebit.security.Action;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.Resource;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.sample.Comment;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.web.bean.SecurityBaseBean;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.bean.resources.ResourceObjectBean;
import com.blazebit.security.web.util.WebUtil;

@Named
@ViewScoped
@Stateless
public class PartyBean extends SecurityBaseBean {

    @PersistenceContext(unitName = "TestPU")
    EntityManager entityManager;

    @Inject
    private PermissionDataAccess permissionDataAccess;

    private List<RowModel> parties = new ArrayList<RowModel>();
    private Party newParty = new Party("");

    public void init() {
        List<Party> result = entityManager.createQuery("select party from " + Party.class.getCanonicalName() + " party", Party.class).getResultList();
        parties.clear();
        for (Party p : result) {
            parties.add(new RowModel(p, "Party-" + p.getPartyField1() + "," + p.getPartyField2()));
        }
        setNewParty(new Party(""));
    }

    public void saveNewParty() {
        if (!StringUtils.isEmpty(newParty.getType())) {
            entityManager.persist(newParty);

            Resource resource = createResource(newParty);

            for (Action action : actionImplicationProvider.getActionsImpledBy(actionFactory.createAction(ActionConstants.CREATE))) {
                //TODO what to do?
                if (!Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.OBJECT_LEVEL))) {
                    EntityField entityField = (EntityField) resource;
                    resource = entityResourceFactory.createResource(entityField.getEntity());
                }
                if (permissionDataAccess.isGrantable(userSession.getUser(), action, resource)) {
                    permissionService.grant(userSession.getAdmin(), userSession.getUser(), action, resource);
                }
            }
            newParty = new Party("");
            init();
        }
    }

    public void saveParty(Party party) {
        entityManager.merge(party);
        init();
    }

    public void deleteParty(Party party) {
        entityManager.remove(entityManager.find(Party.class, party.getId()));
        init();
    }

    public void grant(Comment comment) {
        WebUtil.redirect(FacesContext.getCurrentInstance(),
                         "/blaze-security-showcase/resource/resources.xhtml?id=" + comment.getId() + "&resource=" + comment.getClass().getName(), false);
    }

    public void revoke(Comment comment) {
        WebUtil.redirect(FacesContext.getCurrentInstance(), "/blaze-security-showcase/resource/resources.xhtml?id=" + comment.getId() + "&resource=" + comment.getClass().getName()
            + "&revoke=true", false);
    }

    public Class<?> getEntityClass() {
        return Party.class;
    }

    @Inject
    ResourceObjectBean resourceObjectBean;

    public void goToPermissions() {
        if (selectedSubject != null && selectedActions != null && isSelected(parties)) {
            resourceObjectBean.setSelectedSubject(selectedSubject);
            resourceObjectBean.setSelectedActions(selectedActions);
            resourceObjectBean.getSelectedFields().clear();

            resourceObjectBean.getSelectedObjects().clear();
            for (RowModel rowModel : parties) {
                if (rowModel.isSelected()) {
                    resourceObjectBean.getSelectedObjects().add(rowModel);
                }
            }
            resourceObjectBean.setPrevPath(FacesContext.getCurrentInstance().getViewRoot().getViewId());
            WebUtil.redirect(FacesContext.getCurrentInstance(), "/blaze-security-showcase/resource/object_resources.xhtml", false);
        } else {
            System.err.println("Must select subject/action/party");
        }
    }

    public Party getNewParty() {
        return newParty;
    }

    public void setNewParty(Party newParty) {
        this.newParty = newParty;
    }

    public List<RowModel> getParties() {
        return parties;
    }

    public void setParties(List<RowModel> parties) {
        this.parties = parties;
    }

    public Party getPartyEntityWithType(String type) {
        if (!StringUtils.isEmpty(type)) {
            return new Party(type);
        } else {
            return null;
        }
    }

    public void handleTypeChange(ValueChangeEvent event) {
        newParty.setType((String) event.getNewValue());
    }

}
