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
package com.blazebit.security.model;

/**
 * 
 */
public interface Action {

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String GRANT = "GRANT";
    public static final String REVOKE = "REVOKE";
    public static final String READ = "READ";
    public static final String ADD = "ADD";
    public static final String REMOVE = "REMOVE";
    public static final String ACT_AS = "ACT_AS";

    /**
     * Returns the name of the action.
     * 
     * @return the name of the action.
     */
    public String getName();

    /**
     * Returns true if the given action is equal to this action.
     * 
     * @param action the action to which this action should be checked for equality.
     * @return true if the given action is equal to this action
     */
    public boolean equals(Object action);

    /**
     * Returns the hashCode of this action.
     * 
     * This method is included merely for documentation purpose to signal that implementations must honor the equals-hashCode
     * contract.
     * 
     * @return the hashCode of this action.
     */
    public int hashCode();

}
