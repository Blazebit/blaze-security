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
package com.blazebit.security.impl.utils;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.lang.StringUtils;
import com.blazebit.security.Resource;
import com.blazebit.security.impl.model.EntityConstants;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.ResourceName;
import com.blazebit.security.impl.model.SubjectRoleConstants;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserDataPermission;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupDataPermission;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Party;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cuszk
 */
public class EntityUtils {

    public static EntityField getEntityFieldFor(EntityConstants entity, String field) {
        return new EntityField(entity.getClassName(), field);
    }

    public static EntityField getEntityFieldFor(SubjectRoleConstants entity, String field) {
        return new EntityField(entity.getClassName(), field);
    }

    public static EntityObjectField getEntityObjectFieldFor(EntityConstants entity, String field, String id) {
        return new EntityObjectField(entity.getClassName(), field, id);
    }

    public static EntityObjectField getEntityObjectFieldFor(SubjectRoleConstants entity, String field, String id) {
        return new EntityObjectField(entity.getClassName(), field, id);
    }

    public static EntityObjectField getEntityObjectFieldFor(Class<?> clazz, String field, String id) {
        ResourceName annotation = (ResourceName) AnnotationUtils.findAnnotation(clazz, ResourceName.class);
        if (annotation != null) {
            return new EntityObjectField(annotation.name(), field, id);
        } else {
            throw new IllegalArgumentException("Class " + clazz + " does not have a ResourceName annotation, therefore it cannot be a resource!");
        }
    }

    public static EntityField getEntityFieldFor(Class<?> clazz, String field) {
        ResourceName annotation = (ResourceName) AnnotationUtils.findAnnotation(clazz, ResourceName.class);
        if (annotation != null) {
            return new EntityField(annotation.name(), field);
        } else {
            throw new IllegalArgumentException("Class " + clazz + " does not have a ResourceName annotation, therefore it cannot be a resource!");
        }
    }

    public static Resource getEntityResourceFor(Class<?> clazz, String fieldName, String entityId) {
        if (!StringUtils.isEmpty(entityId)) {
            return getEntityObjectFieldFor(clazz, fieldName, entityId);
        } else {
            return getEntityFieldFor(clazz, fieldName);
        }
    }

    public static List<Class<?>> getEntityClasses() {
        List<Class<?>> ret = new ArrayList<Class<?>>();
        ret.add(Carrier.class);
        ret.add(Party.class);
        ret.add(Contact.class);
        ret.add(CarrierGroup.class);
        ret.add(UserPermission.class);
        ret.add(UserDataPermission.class);
        ret.add(UserGroupPermission.class);
        ret.add(UserGroupDataPermission.class);
        ret.add(User.class);
        ret.add(UserGroup.class);
        
//        try {
//            for (Class<?> clazz : getClassesForPackage("com.blazebit.security.impl.model")) {
//
//                ret.add(clazz);
//
//            }
//            for (Class<?> clazz : getClassesForPackage("com.blazebit.security.impl.model.sample")) {
//
//                ret.add(clazz);
//
//            }
//        } catch (ClassNotFoundException ex) {
//        }
        return ret;
    }

    /**
     * Attempts to list all the classes in the specified package as determined
     * by the context class loader
     *
     * @param pckgname the package name to search
     * @return a list of classes that exist within that package
     * @throws ClassNotFoundException if something went wrong
     */
    //"\WEB-INF\lib\blaze-security-impl-0.1.0-SNAPSHOT.jar"
    private static List<Class> getClassesForPackage(String pckgname) throws ClassNotFoundException {
        // This will hold a list of directories matching the pckgname. There may be more than one if a package is split over multiple jars/paths
        ArrayList<File> directories = new ArrayList<File>();
        try {
            ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            String path = pckgname.replace('.', '/');
            // Ask for all resources for the path
            Enumeration<URL> resources = cld.getResources(path);
            while (resources.hasMoreElements()) {
                directories.add(new File(URLDecoder.decode(resources.nextElement().getPath(), "UTF-8")));
            }
        } catch (NullPointerException x) {
            throw new ClassNotFoundException(pckgname + " does not appear to be a valid package (Null pointer exception)");
        } catch (UnsupportedEncodingException encex) {
            throw new ClassNotFoundException(pckgname + " does not appear to be a valid package (Unsupported encoding)");
        } catch (IOException ioex) {
            throw new ClassNotFoundException("IOException was thrown when trying to get all resources for " + pckgname);
        }

        ArrayList<Class> classes = new ArrayList<Class>();
        // For every directory identified capture all the .class files
        for (File directory : directories) {
            if (directory.exists()) {
                // Get the list of the files contained in the package
                String[] files = directory.list();
                for (String file : files) {
                    // we are only interested in .class files
                    if (file.endsWith(".class")) {
                        // removes the .class extension
                        try {
                            classes.add(Class.forName(pckgname + '.' + file.substring(0, file.length() - 6)));
                        } catch (NoClassDefFoundError e) {
                            // do nothing. this class hasn't been found by the loader, and we don't care.
                        }
                    }
                }
            } else {
                throw new ClassNotFoundException(pckgname + " (" + directory.getPath() + ") does not appear to be a valid package");
            }
        }
        return classes;
    }
}
