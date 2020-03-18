/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.servlet;

import com.tangosol.net.CacheFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


/**
 * A simple implementation of the <tt>ServletContextListener</tt> interface
 * that calls {@link CacheFactory#shutdown()} during servlet context shutdown.
 * <p>
 * This class should be registered in the web application deployment
 * descriptor of a Coherence REST J2EE web application like so:
 * <pre>
 * &lt;web-app&gt;
 *   ...
 *   &lt;listener&gt;
 *     &lt;listener-class&gt;com.tangosol.coherence.rest.servlet.DefaultServletContextListener&lt;/listener-class&gt;
 *   &lt;/listener&gt;
 *   &lt;servlet&gt;
 *     &lt;servlet-name&gt;Coherence REST&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;org.glassfish.jersey.servlet.ServletContainer&lt;/servlet-class&gt;
 *     &lt;init-param&gt;
 *       &lt;param-name&gt;javax.ws.rs.Application&lt;/param-name&gt;
 *       &lt;param-value&gt;com.tangosol.coherence.rest.server.DefaultResourceConfig&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 *   &lt;/servlet&gt;
 *   ...
 * &lt;/web-app&gt;
 * </pre>
 *
 * @author jh  2011.12.21
 */
public class DefaultServletContextListener
    implements ServletContextListener
    {

    // ----- ServletContextListener interface -------------------------------

    /**
     * Notification that the servlet context is about to be shut down. All
     * servlets have been had their destroy() method called before any
     * ServletContextListeners are notified of context destruction.
     *
     * @param evt  the ServletContextEvent
     */
    public void contextDestroyed(ServletContextEvent evt)
        {
        }

    /**
     * Notification that the web application initialization process is
     * starting. All ServletContextListeners are notified of context
     * initialisation before any filter or servlet in the web application is
     * initialized.
     *
     * @param evt  the ServletContextEvent
     */
    public void contextInitialized(ServletContextEvent evt)
        {
        }
    }
