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

import com.blazebit.security.Action;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.Resource;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.sample.Comment;
import com.blazebit.security.web.bean.SecurityBaseBean;
import com.blazebit.security.web.util.WebUtil;

@Named
@ViewScoped
@Stateless
public class CommentBean extends SecurityBaseBean {

    @PersistenceContext(unitName = "TestPU")
    EntityManager entityManager;

    @Inject
    private PermissionDataAccess permissionDataAccess;

    private List<Comment> comments = new ArrayList<Comment>();
    private Comment newComment = new Comment();

    public void init() {
        setComments(entityManager.createQuery("select comment from " + Comment.class.getCanonicalName() + " comment where comment.user.company.id="
                                                  + userSession.getSelectedCompany().getId()).getResultList());
        newComment = new Comment();
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public Comment getNewComment() {
        return newComment;
    }

    public void setNewComment(Comment newComment) {
        this.newComment = newComment;
    }

    public void saveNewComment() {
        User subject = userSession.getUser();
        newComment.setUser(subject);
        entityManager.persist(newComment);
        setComments(entityManager.createQuery("select comment from " + Comment.class.getCanonicalName() + " comment").getResultList());
        Resource resource = entityFieldFactory.createResource(newComment.getClass(), newComment.getId());
        Action readAction = actionFactory.createAction(ActionConstants.READ);
        if (permissionDataAccess.isGrantable(subject, readAction, resource)) {
            permissionService.grant(userSession.getAdmin(), subject, readAction, resource);
        }
        Action updateAction = actionFactory.createAction(ActionConstants.UPDATE);
        if (permissionDataAccess.isGrantable(subject, updateAction, resource)) {
            permissionService.grant(userSession.getAdmin(), subject, updateAction, resource);
        }

        Action deleteAction = actionFactory.createAction(ActionConstants.DELETE);
        if (permissionDataAccess.isGrantable(subject, deleteAction, resource)) {
            permissionService.grant(userSession.getAdmin(), subject, deleteAction, resource);
        }
        newComment = new Comment();
    }

    public void saveComment(Comment comment) {
        entityManager.merge(comment);
        setComments(entityManager.createQuery("select comment from " + Comment.class.getCanonicalName() + " comment").getResultList());
    }

    public void deleteComment(Comment comment) {
        entityManager.remove(entityManager.find(Comment.class, comment.getId()));
        setComments(entityManager.createQuery("select comment from " + Comment.class.getCanonicalName() + " comment").getResultList());
    }

    public void grant(Comment comment) {
        WebUtil.redirect(FacesContext.getCurrentInstance(),
                         "/blaze-security-showcase/resource/resources.xhtml?id=" + comment.getId() + "&resource=" + comment.getClass().getName(), false);
    }

    public void revoke(Comment comment) {
        WebUtil.redirect(FacesContext.getCurrentInstance(), "/blaze-security-showcase/resource/resources.xhtml?id=" + comment.getId() + "&resource=" + comment.getClass().getName()
            + "&revoke=true", false);
    }
}
