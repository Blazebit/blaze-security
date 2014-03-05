/**
 * 
 */
package com.blazebit.security.showcase.impl.data;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.entity.Security;

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
}
