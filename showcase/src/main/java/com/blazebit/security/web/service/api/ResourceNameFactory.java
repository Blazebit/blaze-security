package com.blazebit.security.web.service.api;

import com.blazebit.security.IdHolder;
import com.blazebit.security.Resource;

public interface ResourceNameFactory {

    Resource createResource(IdHolder entityObject, String field);

    Resource createResource(IdHolder entityObject);

}
