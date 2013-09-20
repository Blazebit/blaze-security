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
package com.blazebit.security.impl;

import com.blazebit.exception.ExceptionUtils;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.deltaspike.cdise.api.CdiContainer;
import org.apache.deltaspike.cdise.api.CdiContainerLoader;
import org.apache.deltaspike.cdise.api.ContextControl;
import org.apache.deltaspike.core.api.literal.DefaultLiteral;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 *
 * @author Christian
 */
public class TestRunner extends BlockJUnit4ClassRunner {

    private static final List<TestRunner> instances = new ArrayList<TestRunner>();
    private CdiContainer container;
    private ContextControl contextControl;
    private int executedTests = 0;

    public TestRunner(Class<?> klass) throws InitializationError {
        super(klass);
        System.setProperty("openejb.deploymentId.format", "{moduleId}/{ejbName}");
        System.setProperty("openejb.embedded.initialcontext.close", "destroy");
        System.setProperty("openejb.validation.output.level", "verbose");

        try {
            System.setProperty("openejb.conf.file", TestRunner.class.getClassLoader().getResource("META-INF/openejb.xml").toURI().toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException("could not load openejb.xml");
        }
        container = CdiContainerLoader.getCdiContainer();
        container.boot();
        contextControl = container.getContextControl();
        contextControl.startContexts();
        instances.add(this);
    }

    @Override
    protected Object createTest() throws Exception {
        return BeanProvider.getContextualReference(getTestClass().getJavaClass(), new DefaultLiteral());
    }

    /**
     * Stops all contexts and shuts the CDI container down.
     */
    private void close() throws Throwable {
        if (container != null) {
            contextControl.stopContexts();
            contextControl = null;
            container.shutdown();
            container = null;
        }
    }

    @Override
    protected Statement withBefores(final FrameworkMethod method, final Object target, final Statement statement) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    contextControl.startContext(RequestScoped.class);
                    TestRunner.super.withBefores(method, target, statement).evaluate();
                } finally {
                    contextControl.stopContext(RequestScoped.class);
                }
            }
        };
    }

    @Override
    protected List<TestRule> getTestRules(Object target) {
        List<TestRule> rules = new ArrayList<TestRule>(super.getTestRules(target));
        rules.add(new TestRule() {
            @Override
            public Statement apply(final Statement base, Description description) {

                return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        EntityManagerFactory emf = null;
                        Map<String, Object> properties = new HashMap<String, Object>();

                        properties.put("javax.persistence.transactionType", "RESOURCE_LOCAL");
                        properties.put("javax.persistence.jtaDataSource", null);

                        properties.put("javax.persistence.jdbc.driver", "org.h2.Driver");
                        properties.put("javax.persistence.jdbc.url", "jdbc:h2:mem:test;INIT=CREATE SCHEMA IF NOT EXISTS USERROLES;");
                        properties.put("javax.persistence.jdbc.password", "test");
                        properties.put("javax.persistence.jdbc.user", "test");

                        try {
                            contextControl.startContext(RequestScoped.class);
                            emf = Persistence.createEntityManagerFactory("TestPU", properties);
                            base.evaluate();
                        } catch (Throwable e) {
                            e = ExceptionUtils.unwrap(e, InvocationTargetException.class);
                            throw e;
                        } finally {
                            if (emf != null) {
                                emf.close();
                            }

                            contextControl.stopContext(RequestScoped.class);
                            executedTests++;

                            if (executedTests == testCount()) {
                                instances.remove(TestRunner.this);
                            }
                            if (instances.isEmpty()) {
                                TestRunner.this.close();
                            }
                        }
                    }
                };
            }
        });
        return rules;
    }
}
