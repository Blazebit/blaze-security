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
package com.blazebit.security;

/**
 *
 * @author cuszk
 */
public interface RoleService {

    /**
     * Creates a relation between role1 and role2. Role2 will be the parent of
     * role1.
     *
     * @param role1
     * @param role2 Add role1 to role2.
     */
    public void addRoleToRole(Role role1, Role role2);

    /**
     * Removes the relation between role1 and role2. Role2 will not be the paren
     * of role1.
     *
     * @param role1
     * @param role2
     */
    public void removeRoleFromRole(Role role1, Role role2);

    /**
     * Authorizer adds Subject to Role with the option to grant the role's
     * permission to Subject or not.
     *
     * @param authorizer
     * @param subject
     * @param role
     * @param copyPermissions
     */
    public void addSubjectToRole(Subject authorizer, Subject subject, Role role, boolean copyPermissions);

    /**
     * Authorizer removes Subject from Role.
     *
     * @param subject
     * @param role
     */
    public void removeSubjectFromRole(Subject authorizer, Subject subject, Role role);

    /**
     * Subject can only be added to role if subject is not already present in the
     * role hierarchy.
     *
     * @param subject
     * @param role
     * @return true if subject is allowed to be added to role
     */
    public boolean canSubjectBeAddedToRole(Subject subject, Role role);
}
