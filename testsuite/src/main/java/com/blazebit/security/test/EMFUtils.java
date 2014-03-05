/**
 * 
 */
package com.blazebit.security.test;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

/**
 * @author Moritz Becker <m.becker@curecomp.com>
 * @company curecomp
 * @date 09.01.2014
 */
public class EMFUtils {

    @SuppressWarnings("deprecation")
    public static EntityManagerFactory create(String unitName, Map<String, String> properties) {
        return new MyEjb3Configuration(System.getProperty("openejb.altdd.prefix")).configure(unitName, properties).buildEntityManagerFactory();
        // return Persistence.createEntityManagerFactory(databaseAwareAnnotation.unitName(), properties);
    }
}
