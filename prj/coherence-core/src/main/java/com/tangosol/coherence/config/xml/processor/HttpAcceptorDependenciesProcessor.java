/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.http.HttpServer;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.internal.net.service.peer.acceptor.DefaultHttpAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.HttpAcceptorDependencies;

import com.tangosol.run.xml.XmlElement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * An {@link ElementProcessor} to produce a {@link HttpAcceptorDependencies}
 * from a &lt;http-acceptor%gt; configuration.
 *
 * @author bo  2013.07.11
 */
@XmlSimpleName("http-acceptor")
public class HttpAcceptorDependenciesProcessor
        implements ElementProcessor<HttpAcceptorDependencies>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public HttpAcceptorDependencies process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // establish the dependencies into which we'll perform some injection
        DefaultHttpAcceptorDependencies dependencies = new DefaultHttpAcceptorDependencies();

        // resolve the HTTP server
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr == null)
            {
            // no builder has been specified, so let's use a default server
            dependencies.setHttpServer(HttpServer.create());
            }
        else
            {
            try
                {
                ParameterizedBuilder<Object> bldrHttpServer = (ParameterizedBuilder<Object>) bldr;

                dependencies.setHttpServer(bldrHttpServer.realize(context.getDefaultParameterResolver(),
                    context.getContextClassLoader(), null));
                }
            catch (ClassCastException e)
                {
                throw new ConfigurationException("Invalid <" + xmlElement.getName()
                                                 + "> declaration.  The specified element doesn't produce a HttpServer"
                                                 + " as expected in [" + xmlElement + "]", "Please specify a valid <"
                                                     + xmlElement.getName() + ">");
                }
            }

        // process each of the <resource-config> definitions
        Map<String, Object> mapConfig = new HashMap<String, Object>();

        for (Iterator iter = xmlElement.getElements("resource-config"); iter.hasNext(); )
            {
            XmlElement xmlResourceConfig = (XmlElement) iter.next();
            String sContextPath = context.getOptionalProperty("context-path", String.class, null, xmlResourceConfig);

            ParameterizedBuilder<?> bldrResourceConfig = ElementProcessorHelper.processParameterizedBuilder(context,
                                                             xmlResourceConfig);

            if (bldrResourceConfig == null)
                {
                bldrResourceConfig =
                    new InstanceBuilder<>("com.tangosol.coherence.rest.server.DefaultResourceConfig");
                }

            try
                {
                // Backporting note: the following referenced Jersey class is Jersey 2.x.  In Jersey 1.x,
                // the same class is in different package: com.sun.jersey.api.core.ResourceConfig.
                // At this time 12.1.3 is still using Jersey 1.x and 12.2.1 is using Jersey 2.x.
                Class<?> jerseyClz =
                    context.getContextClassLoader().loadClass("org.glassfish.jersey.server.ResourceConfig");
                Object oResourceConfig = bldrResourceConfig.realize(context.getDefaultParameterResolver(),
                                             context.getContextClassLoader(), null);

                if (!jerseyClz.isAssignableFrom(oResourceConfig.getClass()))
                    {
                    throw new IllegalArgumentException("<resource-config> is not an instance of " + jerseyClz.getCanonicalName());
                    }

                if (sContextPath == null)
                    {
                    ApplicationPath path = oResourceConfig.getClass().getAnnotation(ApplicationPath.class);
                    sContextPath = path == null ? "/" : path.value();
                    }

                mapConfig.put(sContextPath, oResourceConfig);

                }
            catch (Exception e)
                {
                throw new ConfigurationException("Failed to realize the <resource-config> specified by ["
                    + xmlResourceConfig
                    + "] as the underlying class could not be found", "Please ensure that the class is available on the class path", e);
                }
            }

        if (mapConfig.isEmpty())
            {
            ServiceLoader<Application> loaderApps = ServiceLoader.load(Application.class);

            for (Application application : loaderApps)
                {
                ApplicationPath annotation = application.getClass().getAnnotation(ApplicationPath.class);
                String sPath = annotation == null ? "/" : annotation.value();

                if (sPath.charAt(0) != '/')
                    {
                    sPath = '/' + sPath;
                    }

                mapConfig.put(sPath, application);
                }
            }

        dependencies.setResourceConfig(mapConfig);

        // now inject everything else
        context.inject(dependencies, xmlElement);

        return dependencies;
        }
    }
