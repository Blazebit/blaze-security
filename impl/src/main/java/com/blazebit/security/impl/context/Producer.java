/**
 * 
 */
package com.blazebit.security.impl.context;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author Christian Beikov
 * 
 * @company Curecomp Gmbh
 * @date 25.05.2012
 */
@ApplicationScoped
public class Producer {

    @Produces
    @Security
    @PersistenceContext(unitName = "SecurityPU")
    private EntityManager entityManager;

    // @Produces
    // @Default
    // @RequestScoped
    // EntityManager createEntityManager(){
    // return new AuditedEntityManager(entityManager);
    // }

}
