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

import java.util.Properties;
import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author cuszk
 */
public class RoleSecurityTest extends TestCase {

    @PersistenceContext(unitName = "TestPU", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;
    RoleSecurityServiceImpl roleService;

    @BeforeClass
    public void setUp() throws Exception {
        // create the container with our properties
//        final Properties p = new Properties();
//        p.put("securityDatabase", "new://Resource?type=DataSource");
//        p.put("securityDatabase.JdbcDriver", "org.apache.derby.jdbc.ClientDriver");
//        p.put("securityDatabase.JdbcUrl", "jdbc:derby://localhost:1527/sample");

        final Context context = EJBContainer.createEJBContainer().getContext();

        roleService = (RoleSecurityServiceImpl) context.lookup("java:global/impl/RoleSecurityServiceImpl");

    }

    @Test
    public void test_injected_service() {
        roleService.testInjectedService();
    }

    @Test
    public void test_entityManager() {
        entityManager.clear();
    }
}
