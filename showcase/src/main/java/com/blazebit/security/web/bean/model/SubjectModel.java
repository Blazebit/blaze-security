package com.blazebit.security.web.bean.model;

import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.model.BaseEntity;

public class SubjectModel {

    private BaseEntity subject;

    public SubjectModel(BaseEntity subject) {
        this.subject = subject;
    }

    public BaseEntity getSubject() {
        return subject;
    }

    public void setSubject(BaseEntity subject) {
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
