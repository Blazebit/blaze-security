/**
 * 
 */
package com.blazebit.security.test;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionRolledbackException;

import org.apache.deltaspike.cdise.api.CdiContainer;
import org.apache.deltaspike.cdise.api.CdiContainerLoader;
import org.apache.deltaspike.cdise.api.ContextControl;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.junit.Test;
import org.junit.Test.None;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.exception.ExceptionUtils;

/**
 * Custom runner for CDI/EJB backed tests.
 * 
 * @author Christian Beikov
 * 
 * @company Curecomp Gmbh
 * @date 14.12.2012
 */
public class ContainerRunner extends BlockJUnit4ClassRunner {

    private static boolean inUse = false;
    private static int testRun = 1;
    private static CdiContainer container;
    private static ContextControl contextControl;
    
    /**
     * @param klass
     * @throws InitializationError
     */
    public ContainerRunner(Class<?> klass) throws InitializationError {
        super(klass);
        
        if(!inUse) {
            inUse = true;
            init();
        }
    }

    private static void init() {
        // Logging configuration
        Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
        while(loggerNames.hasMoreElements()) {
            Logger.getLogger(loggerNames.nextElement()).setLevel(Level.SEVERE);
        }
        
        // Including the ejbJar-name and ejbName in the deploymentId 
        // avoids duplicate EJB names and also is the default behavior of most containers.
        System.setProperty("openejb.deploymentId.format", "{moduleId}/{ejbName}");
        System.setProperty("openejb.embedded.initialcontext.close", "destroy");
        
        // Load the default EJB configuration file
        try {
            System.setProperty("openejb.conf.file", ContainerRunner.class.getClassLoader().getResource("META-INF/openejb.xml").toURI().toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException("could not load openejb.xml");
        }
        
        // System.setProperty("openejb.validation.output.level", "verbose");
        
        // Actually start the container
        container = CdiContainerLoader.getCdiContainer();
        container.boot();
        contextControl = container.getContextControl();
        contextControl.startContexts();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                ContainerRunner.close();
            }
            
        });
    }

    /**
     * Stops all contexts and shuts the CDI container down.
     */
    private static void close() {
        if (container != null) {
            contextControl.stopContexts();
            contextControl = null;
            container.shutdown();
            container = null;
        }
    }

    /**
     * This is just a quick hack to make the new test system work next to the old one.
     * 
     * @return
     */
    public static boolean inUse() {
        return inUse;
    }

    /**
     * Creates a CDI proxy of the test class.
     * 
     * @return the CDI proxy of the test class.
     */
    @Override
    protected Object createTest() throws Exception {
        return BeanProvider.getContextualReference(getTestClass().getJavaClass());
    }

    /**
     * Additionally to the JUnit logic, this method unwraps thrown EJB-Container related exceptions so that the real exception can be used for further processing.
     */
    @Override
    protected Statement possiblyExpectingExceptions(FrameworkMethod method, Object test, final Statement next) {
        final Test annotation = method.getAnnotation(Test.class);
        
        if (annotation == null || annotation.expected() == None.class) {
            return next;
        } else {
            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    Class<?> expected = annotation.expected();
                    boolean complete = false;
                    try {
                        next.evaluate();
                        complete = true;
                    } catch (AssumptionViolatedException e) {
                        throw e;
                    } catch (Throwable e) {
                        // We do some exception unwrapping here to get the real exception
                        @SuppressWarnings("unchecked")
                        Throwable ex = ExceptionUtils.unwrap(e, InvocationTargetException.class, EJBException.class);

                        // If there is no root cause other than an EJBException we use the original exception
                        if(ex == null) {
                            ex = e;
                        }

                        if (!expected.isAssignableFrom(ex.getClass())) {
                            String message = "Unexpected exception, expected<" + expected.getName() + "> but was<"
                                + ex.getClass().getName() + ">";
                            throw new AssertionError(message).initCause(ex);
                        }
                    }
                    if (complete) {
                        throw new AssertionError("Expected exception: " + expected.getName());
                    }
                }
            };
        }
    }

    /**
     * Manages CDI-Contexts to be started before entering end stopped after exiting a Statement.
     * Additionally it is aware of the DatabaseAware, DatabaseUnaware and BeforeDatabaseAware annotations.
     * It creates an EntityManagerFactory before entering the test method and closes it after exiting
     * to always have a clean state of the database.
     */
    @Override
    protected List<TestRule> getTestRules(final Object target) {
        List<TestRule> rules = new ArrayList<TestRule>(super.getTestRules(target));
        rules.add(new TestRule() {

            @Override
            public Statement apply(final Statement base, final Description description) {
                final DatabaseAware databaseAwareAnnotation;

                if (description.getAnnotation(DatabaseUnaware.class) != null) {
                    databaseAwareAnnotation = null;
                } else if (description.getAnnotation(DatabaseAware.class) != null) {
                    databaseAwareAnnotation = description.getAnnotation(DatabaseAware.class);
                } else {
                    databaseAwareAnnotation = AnnotationUtils.findAnnotation(target.getClass(), DatabaseAware.class);
                }

                List<FrameworkMethod> befores = new ArrayList<FrameworkMethod>(0);

                if (databaseAwareAnnotation != null) {
                    for (FrameworkMethod m : getTestClass().getAnnotatedMethods(BeforeDatabaseAware.class)) {
                        if (databaseAwareAnnotation.unitName().equals(m.getAnnotation(BeforeDatabaseAware.class).unitName())) {
                            befores.add(m);
                        }
                    }
                }

                final Statement statement = befores.isEmpty() ? base : new RunBefores(base, befores, target);

                return new Statement() {

                    @Override
                    public void evaluate() throws Throwable {
                        EntityManagerFactory emf = null;

                        try {
                            contextControl.startContext(RequestScoped.class);

                            if (databaseAwareAnnotation != null) {
                                Map<String, String> properties = new HashMap<String, String>();
                                properties.put("hibernate.hbm2ddl.auto", "create-drop");
                                properties.put("hibernate.ejb.entitymanager_factory_name", databaseAwareAnnotation.unitName()
                                    + testRun++);
                                properties.put("javax.persistence.jtaDataSource", null);
                                properties.put("javax.persistence.transactionType", "RESOURCE_LOCAL");
                                properties.put("javax.persistence.jdbc.driver", "org.h2.Driver");
                                properties.put("javax.persistence.jdbc.url", "jdbc:h2:mem:test;INIT=CREATE SCHEMA IF NOT EXISTS USERROLES;");
                                properties.put("javax.persistence.jdbc.password", "test");
                                properties.put("javax.persistence.jdbc.user", "test");
                                
                                emf = EMFUtils.create(databaseAwareAnnotation.unitName(), properties);
                            }

                            statement.evaluate();
                        } catch (Throwable e) {
                            // We do some exception unwrapping here to get the real exception
                            @SuppressWarnings("unchecked")
                            Throwable ex = ExceptionUtils.unwrap(e, InvocationTargetException.class, EJBException.class,
                                                                 TransactionRolledbackException.class);
                            throw ex;
                        } finally {
                            if (emf != null && emf.isOpen()) {
                                emf.close();
                            }

                            contextControl.stopContext(RequestScoped.class);
                        }
                    }
                };
            }
        });
        return rules;
    }

}
