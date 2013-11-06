/*
 * Copyright 2013 Blazebit.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.blazebit.security;

import java.util.Collection;

/**
 * 
 * @author Christian
 */
public interface Resource {

    /**
     * 
     * @param resource
     * @return true if existing resource implies given resource
     */
    public boolean implies(Resource resource);

    /**
     * 
     * @param resource
     * @return true if existing resource can be replaced by the given resource
     */
    public boolean isReplaceableBy(Resource resource);

    /**
     * collection of connected resources
     * 
     * @return
     */
    public Collection<Resource> connectedResources();

    /**
     * decides whether the given action is applicable to this resource
     * 
     * @param action
     * @return
     */
    public boolean isApplicable(Action action);
}
