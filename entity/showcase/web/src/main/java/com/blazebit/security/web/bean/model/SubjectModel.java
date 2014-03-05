package com.blazebit.security.web.bean.model;

import com.blazebit.security.model.IdHolder;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserGroup;

public class SubjectModel {

    private IdHolder<?> subject;

    public SubjectModel(IdHolder<?> subject) {
        this.subject = subject;
    }

    public IdHolder<?> getSubject() {
        return subject;
    }

    public void setSubject(IdHolder<?> subject) {
        this.subject = subject;
    }

    public String getName() {
        if (subject instanceof User) {
            return ((User) subject).getUsername();
        } else {
            if (subject instanceof UserGroup) {
                return ((UserGroup) subject).getName();
            }
        }
        return "";
    }

    public String getId() {
        return subject.getClass().getName() + "-" + subject.getId();
    }

}
