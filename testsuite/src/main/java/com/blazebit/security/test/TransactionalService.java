package com.blazebit.security.test;

import java.io.Serializable;
import java.util.concurrent.Callable;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


@Stateless
public class TransactionalService implements Serializable {

    private static final long serialVersionUID = 1L;

    @PersistenceContext(unitName = "TestPU")
    private EntityManager em;

    public <E> E call(TxWork<E> work) {
        return work.doWork(em);
    }
    
}
