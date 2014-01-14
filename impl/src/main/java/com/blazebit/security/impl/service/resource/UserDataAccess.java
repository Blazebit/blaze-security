package com.blazebit.security.impl.service.resource;

import java.io.Serializable;

import com.blazebit.security.impl.model.User;

public interface UserDataAccess extends Serializable{

    User findUser(Integer id);

    User findUser(String name);

}
