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
 * @author Christian Beikov
 */
public interface PermissionFactory {

    /**
     *
     * @param <R>
     * @param <P>
     * @param subject
     * @param action
     * @param resource
     * @return creates a permission for a subject
     */
    public <R extends Role<R>, P extends Permission> P create(Subject<R> subject, Action action, Resource resource);

    /**
     *
     * @param <R>
     * @param <P>
     * @param role
     * @param action
     * @param resource
     * @return creates a permissions for a role
     */
    public <R extends Role<R>, P extends Permission> P create(Role<R> role, Action action, Resource resource);
}
