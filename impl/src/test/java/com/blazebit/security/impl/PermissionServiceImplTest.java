/*
 * Copyright 2013 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.security.impl;

import com.blazebit.security.Action;
import com.blazebit.security.PermissionService;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserPermission;
import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Christian Beikov
 */
public class PermissionServiceImplTest {
    
    private PermissionService permissionService;
    private User admin;
    
    @Before
    public void setUp() {
        permissionService = new PermissionServiceImpl();
        admin = new User();
        admin.getPermissions().add(new UserPermission());
    }

    @Test(expected = NullPointerException.class)
    public void testIsGranted_shouldThrowException() {
        permissionService.isGranted(null, null);
    }

    @Test
    public void testIsGranted_shouldBeAllowedToGrant() {
        assertTrue(permissionService.isGranted(admin, permissionService.getGrantAction()));
    }
}