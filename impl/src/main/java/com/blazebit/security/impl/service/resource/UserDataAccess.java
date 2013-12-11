package com.blazebit.security.impl.service.resource;

import java.util.List;
import java.util.Set;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;

public interface UserDataAccess {

    User findUser(Integer id);

    User findUser(String name);

}
