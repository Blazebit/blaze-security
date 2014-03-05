package com.blazebit.security.test;

import javax.persistence.EntityManager;


public interface TxWork<E> {

    public E doWork(EntityManager em);
}
