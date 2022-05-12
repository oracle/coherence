/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.http.HttpApplication;
import com.tangosol.coherence.http.HttpServer;
import com.tangosol.coherence.http.GenericHttpServer;

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

import org.glassfish.jersey.server.ResourceConfig;

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
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public HttpAcceptorDependencies process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // establish the dependencies into which we'll perform some injection
        DefaultHttpAcceptorDependencies dependencies = new DefaultHttpAcceptorDependencies();

        // resolve the HTTP server
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        GenericHttpServer httpServer;
        if (bldr == null)
            {
            // no builder has been specified, so let's use a default server
            httpServer = HttpServer.create();
            }
        else
            {
            try
                {
                ParameterizedBuilder<GenericHttpServer> bldrHttpServer = (ParameterizedBuilder<GenericHttpServer>) bldr;

                httpServer = bldrHttpServer.realize(context.getDefaultParameterResolver(),
                        context.getContextClassLoader(), null);
                }
            catch (ClassCastException e)
                {
                throw new ConfigurationException("Invalid <" + xmlElement.getName()
                                                 + "> declaration.  The specified element doesn't produce a HttpServer"
                                                 + " as expected in [" + xmlElement + "]", "Please specify a valid <"
                                                     + xmlElement.getName() + ">");
                }
            }

        dependencies.setHttpServer(httpServer);

        // process each of the <resource-config> definitions
        Map<String, Object> mapConfig = new HashMap<>();

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

            Class<?> clzResource = httpServer.getResourceType();
            try
                {
                Object oResourceConfig = bldrResourceConfig.realize(context.getDefaultParameterResolver(),
                                             context.getContextClassLoader(), null);

                // ensure instantiated application is an instance of ResourceConfig
                try
                    {
                    if (oResourceConfig instanceof Application)
                        {
                        oResourceConfig = ResourceConfig.forApplication((Application) oResourceConfig);
                        }
                    }
                catch (NoClassDefFoundError | IllegalAccessError e)
                    {
                    // ignored
                    }

                if (!clzResource.isAssignableFrom(oResourceConfig.getClass()))
                    {
                    throw new IllegalArgumentException("<resource-config> is not an instance of "
                            + clzResource.getCanonicalName());
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
            ServiceLoader<HttpApplication> loaderApps = ServiceLoader.load(HttpApplication.class);
            for (HttpApplication app : loaderApps)
                {
                mapConfig.put(app.getPath(), app.configure());
                }
            }

        dependencies.setResourceConfig(mapConfig);

        // now inject everything else
        context.inject(dependencies, xmlElement);

        return dependencies;
        }

    }
