package com.blazebit.security.showcase.data;

import java.io.Serializable;

import com.blazebit.security.model.User;

public interface UserDataAccess extends Serializable{

    User findUser(Integer id);

    User findUser(String name);

}
