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
import com.blazebit.security.Permission;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

/**
 *
 * @author cuszk
 */
@Stateful
public class RoleSecurityServiceImpl {

    @PersistenceContext(unitName = "TestPU", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    public void testInjectedService() {
        System.out.println("Service Injected and method invoked!");
    }

    public void testEntityManager() {
        entityManager.clear();
    }

    public <R extends Role<R, ?, ?>, P extends Permission<?>, Q extends Permission<?>> void grant(Subject<R, P, Q> authorizer, Subject<R, P, Q> subject, Action action, Resource resource) {
        if (subject == null || authorizer == null || action == null) {
            throw new IllegalArgumentException("Value cannot be null!");
        }
    }

    public <R extends Role<R, ?, ?>, P extends Permission<?>, Q extends Permission<?>> void addUserToGroup(Subject<R, P, Q> authorizer, Subject<R, P, Q> subject, Subject<R, P, Q> subjectGroup, boolean takeOverPermissions) {
        if (subject == null || authorizer == null || subjectGroup == null) {
            throw new IllegalArgumentException("Value cannot be null!");
        }

        if (takeOverPermissions) {
            for (Permission p : subjectGroup.getPermissions()) {
                // subject.getPermissions().add(p);
            }
        }
    }
}