package com.blazebit.security.impl.service.resource;

import com.blazebit.security.impl.model.User;

public interface UserDataAccess {

    User findUser(Integer id);

    User findUser(String name);

}
