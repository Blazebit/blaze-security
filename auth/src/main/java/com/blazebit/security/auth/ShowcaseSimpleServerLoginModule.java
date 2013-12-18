package com.blazebit.security.auth;

import java.security.acl.Group;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.spi.SimpleServerLoginModule;

import com.blazebit.security.Permission;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.service.resource.UserDataAccess;

public class ShowcaseSimpleServerLoginModule extends SimpleServerLoginModule {

    @Override
    protected Group[] getRoleSets() throws LoginException {
        PermissionManager permissionManager = BeanProvider.getContextualReference(PermissionManager.class);
        UserDataAccess userDataAccess = BeanProvider.getContextualReference(UserDataAccess.class);
        User user = userDataAccess.findUser(Integer.valueOf(getIdentity().getName()));
        if (user != null) {
            Group[] roleSets = { new SimpleGroup("Roles") };
            List<String> resources = permissionManager.getPermissionResources(user);
            //TODO store role name with action from permissions
            List<Permission> permissions = permissionManager.getPermissions(user);
            for (String resource : resources) {
                roleSets[0].addMember(new SimplePrincipal(resource));
            }
            return roleSets;
        }
        throw new LoginException("Permissions cannot be loaded");
    }

    @Override
    protected boolean validatePassword(String inputPassword, String expectedPassword) {
        // add password validation
        return true;
    }
}
