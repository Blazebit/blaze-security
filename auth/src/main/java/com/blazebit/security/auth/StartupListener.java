package com.blazebit.security.auth;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class StartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        AuthConfigFactory factory = AuthConfigFactory.getFactory();
        // We need to specify for which layer we're doing the registration, which needs to be the constant "HttpServlet" for the
        // Servlet Container Profile.
        // appContext = null, which means we're doing the registration for all applications running on the server.
        factory.registerConfigProvider(new TestAuthConfigProvider(), "HttpServlet", null, "The test");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}