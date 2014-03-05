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

import com.blazebit.security.data.PermissionDataAccess;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.User;
import com.blazebit.security.model.sample.Comment;
import com.blazebit.security.web.bean.base.SecurityBean;
import com.blazebit.security.web.bean.main.resources.ResourceObjectBean;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.util.WebUtil;

@Named
@ViewScoped
@Stateless
public class CommentBean extends SecurityBean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @PersistenceContext(unitName = "TestPU")
    EntityManager entityManager;

    @Inject
    private PermissionDataAccess permissionDataAccess;

    private List<RowModel> comments = new ArrayList<RowModel>();
    private Comment newComment = new Comment();

    public void init() {
        List<Comment> result = entityManager.createQuery("select comment from " + Comment.class.getCanonicalName() + " comment where comment.user.company.id="
                                                             + userSession.getSelectedCompany().getId(), Comment.class).getResultList();
        comments.clear();
        for (Comment c : result) {
            comments.add(new RowModel(c, "Comment-" + c.getText()));
        }
        newComment = new Comment();
    }

    public List<RowModel> getComments() {
        return comments;
    }

    public Comment getNewComment() {
        return newComment;
    }

    public void setNewComment(Comment newComment) {
        this.newComment = newComment;
    }

    public void saveNewComment() {
        User subject = (User) userContext.getUser();
        newComment.setUser(subject);
        entityManager.persist(newComment);
        init();

        Resource resource = entityResourceFactory.createResource(newComment.getClass(), newComment.getId());
        Action readAction = actionFactory.createAction(Action.READ);
        if (permissionDataAccess.isGrantable(subject, readAction, resource)) {
            permissionService.grant(userSession.getAdmin(), subject, readAction, resource);
        }
        Action updateAction = actionFactory.createAction(Action.UPDATE);
        if (permissionDataAccess.isGrantable(subject, updateAction, resource)) {
            permissionService.grant(userSession.getAdmin(), subject, updateAction, resource);
        }

        Action deleteAction = actionFactory.createAction(Action.DELETE);
        if (permissionDataAccess.isGrantable(subject, deleteAction, resource)) {
            permissionService.grant(userSession.getAdmin(), subject, deleteAction, resource);
        }
        newComment = new Comment();
    }

    public void saveComment(Comment comment) {
        entityManager.merge(comment);
        init();
    }

    public void deleteComment(Comment comment) {
        entityManager.remove(entityManager.find(Comment.class, comment.getId()));
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

    public Object getTabEntity() {
        return new Comment();
    }

    @Inject
    ResourceObjectBean resourceObjectBean;

    public void goToPermissions() {
        if (selectedSubject != null && selectedActions != null && isSelected(comments)) {
            resourceObjectBean.setSelectedSubject(selectedSubject);
            resourceObjectBean.setSelectedActions(selectedActions);
            resourceObjectBean.getSelectedFields().clear();

            resourceObjectBean.getSelectedObjects().clear();
            for (RowModel rowModel : comments) {
                if (rowModel.isSelected()) {
                    resourceObjectBean.getSelectedObjects().add(rowModel);
                }
            }
            resourceObjectBean.setPrevPath(FacesContext.getCurrentInstance().getViewRoot().getViewId());
            WebUtil.redirect(FacesContext.getCurrentInstance(), "/blaze-security-showcase/resource/object_resources.xhtml", false);
        } else {
            System.err.println("Must select subject/action/comment");
        }
    }

}
