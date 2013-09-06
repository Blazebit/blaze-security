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
package com.blazebit.security.service;

import com.blazebit.security.Action;
import com.blazebit.security.Module;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import java.util.Collection;

/**
 *
 * @author Christian Beikov
 */
public interface SecurityService {

    /**
     *
     * @param subject
     * @param action
     * @return
     */
    public <R extends Role<R>> boolean isGranted(Subject<R> subject, Action action, Resource resource);

    /**
     *
     * @param authorizer
     * @param subject
     * @param action
     * @throws SecurityException
     */
    public <R extends Role<R>> void grant(Subject<R> authorizer, Subject<R> subject, Action action, Resource resource);
        
    /**
     *
     * @param authorizer
     * @param subject
     * @param action
     * @throws SecurityException
     */
    public <R extends Role<R>> void revoke(Subject<R> authorizer, Subject<R> subject, Action action, Resource resource);

    /**
     *
     * @param subject
     * @return
     */
    public <R extends Role<R>> Collection<Action> getAllowedActions(Subject<R> subject, Resource resource);

    public Action getGrantAction();

    public Action getRevokeAction();

    public Collection<Resource> getAllResources();

    public Collection<Resource> getAllResources(Module module);
}
