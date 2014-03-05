package com.blazebit.security.auth.event;

import java.security.acl.Group;

public class GroupRequestEvent {

    private Group userModule;
    private String name;

    public GroupRequestEvent(String name) {
        this.name = name;
    }

    public Group getUserModule() {
        return userModule;
    }

    public void setUserModule(Group userModule) {
        this.userModule = userModule;
    }

    public String getName() {
        return name;
    }

}
