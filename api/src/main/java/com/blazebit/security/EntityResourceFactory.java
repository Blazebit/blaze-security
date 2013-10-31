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

/**
 * 
 * @author cuszk
 * 
 */
public interface EntityResourceFactory {

    /**
     * 
     * @param clazz
     * @return resource created for the given class
     */
    public Resource createResource(Class<?> clazz);

    /**
     * 
     * @param clazz
     * @param field
     * @return resource created for the given class with the given field
     */
    public Resource createResource(Class<?> clazz, String field);

    /**
     * 
     * @param clazz
     * @param id
     * @return resource created for the given class with the given id
     */
    public Resource createResource(Class<?> clazz, Integer id);

    /**
     * 
     * @param clazz
     * @param field
     * @param id
     * @return resource created for the given class with the given field and id
     */
    public Resource createResource(Class<?> clazz, String field, Integer id);

    /**
     * 
     * @param entity
     * @return
     */
    public Resource createResource(String entity);

    public Resource createResource(String entity, String field);

    public Resource createResource(String entity, Integer id);

    public Resource createResource(String entity, String field, Integer id);

}
