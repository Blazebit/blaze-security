package com.blazebit.security.web.integration.service;

import java.io.Serializable;

import com.blazebit.security.impl.model.User;

public interface UserDataAccess extends Serializable{

    User findUser(Integer id);

    User findUser(String name);

}
