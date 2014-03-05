/**
 * 
 */
package com.blazebit.security.test;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJBException;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * This class is the base test class for all CDI/EJB-based tests.
 * 
 * <b>Important note: </b> If you want to use a database for a test you have to make use of {@link DatabaseAware} and {@link DatabaseUnaware}.
 * 
 * It provides access to a mutable UserContext and to the proxy of the test. The proxy can be used to call methods on the test
 * that run through the CDI/EJB-chain which gives the tests the possibility to use all possible CDI/EJB-features.
 * 
 * Note that declaring a test as EJB is probably a bad idea and should not be done.
 * 
 * Also note that if {@link Test#expected()} is used and an exception is thrown, the real exception will be unwrapped, meaning
 * that {@link EJBException} and {@link InvocationTargetException} will be unwrapped by calling {@link Throwable#getCause()} and {@link InvocationTargetException#getTargetException()}.
 * 
 * @param <T> The type of the concrete test which is needed for the injection of the proxy to itself.
 * @author Christian Beikov <christian@blazebit.com>
 * 
 */
@RunWith(ContainerRunner.class)
public abstract class AbstractContainerTest<T extends AbstractContainerTest<T>> implements Serializable {

    private static final long serialVersionUID = -7248288932170947951L;

    @Inject
    protected Instance<T> self;
    @PersistenceContext(unitName = "TestPU")
    protected EntityManager entityManager;
    @Inject
    private TransactionalService txService;
    
    protected <E> E persist(final E entity) {
        return txService.call(new TxWork<E>() {

            @Override
            public E doWork(EntityManager em) {
                em.persist(entity);
                em.flush();
                return entity;
            }
        });
                              
    }

    protected <C extends Collection<E>, E> C persist(final C entities) {
        return txService.call(new TxWork<C>() {

            @Override
            public C doWork(EntityManager em) {
                for (E e : entities) {
                    em.persist(e);
                }
                em.flush();
                return entities;
            }
        });
    }

    protected <E> E merge(final E entity) {
        return txService.call(new TxWork<E>() {

            @Override
            public E doWork(EntityManager em) {
                E result = em.merge(entity);
                em.flush();
                return result;
            }
        });
    }

    protected <C extends Collection<E>, E> C merge(final C entities) {
        return txService.call(new TxWork<C>() {

            @Override
            public C doWork(EntityManager em) {
                C result = newCollection(entities);
                for (E e : entities) {
                    result.add(em.merge(e));
                }
                em.flush();
                return result;
            }
        });
    }

    protected <E> void remove(final E entity) {
        txService.call(new TxWork<Void>() {

            @Override
            public Void doWork(EntityManager em) {
                Object entityToRemove;
                
                if (em.contains(entity)) {
                    entityToRemove = entity;
                } else {
                    Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
                    entityToRemove = em.find(entity.getClass(), id);
                }
                
                em.remove(entityToRemove);
                em.flush();
                return null;
            }
        });
    }

    protected <C extends Collection<E>, E> void remove(final C entities) {
        txService.call(new TxWork<E>() {

            @Override
            public E doWork(EntityManager em) {
                for (E e : entities) {
                    Object entityToRemove;
                    
                    if (em.contains(e)) {
                        entityToRemove = e;
                    } else {
                        Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(e);
                        entityToRemove = em.find(e.getClass(), id);
                    }
                    
                    em.remove(entityToRemove);
                }
                em.flush();
                return null;
            }
        });
    }
    
    @SuppressWarnings({ "unchecked" })
    private static <C extends Collection<E>, E> C newCollection(C old) {
        if (old instanceof Set<?>) {
            return (C) new LinkedHashSet<E>(old.size());
        }
        
        return (C) new ArrayList<E>(old.size());
    }
    
//  # Static asserts

    public static void assertUnorderedEquals(List<?> a, List<?> b) {
        Assert.assertUnorderedEquals(a, b);
    }

    public static void assertOrderedEquals(List<?> a, List<?> b) {
        Assert.assertOrderedEquals(a, b);
    }

//  # Static exception asserts
    public static <T, E extends Throwable> T verifyThrowable(T obj, Class<E> clazz) {
        return Assert.verifyThrowable(obj, clazz);
    }
}
