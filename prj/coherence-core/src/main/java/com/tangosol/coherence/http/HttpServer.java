/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.net.Session;
import com.tangosol.net.Service;

import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.Application;

/**
 * An interface implemented by embedded HTTP servers that
 * use Jersey {@link ResourceConfig} instances to handle
 * the http endpoints.
 *
 * @author as  2011.06.03
 */
public interface HttpServer
        extends GenericHttpServer<ResourceConfig>
    {
    // ----- GenericHttpServer methods --------------------------------------

    @Override
    default Class<ResourceConfig> getResourceType()
        {
        return ResourceConfig.class;
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Return a {@link HttpServer} implementation to use.
     * <p>
     * The {@link HttpServer} instance will be discovered by the
     * {@link ServiceLoader}. This assumes that there is a single
     * {@link HttpServer} service on the classpath. If multiple
     * implementations are available then the first instance
     * discovered will be returned.
     * <p>
     * If the service loader is unable to discover any {@link HttpServer}
     * implementations then an instance of {@link DefaultHttpServer} will
     * be returned.
     * <p>
     * After creating the {@link HttpServer} this method will use the {@link ServiceLoader}
     * to discover instances of {@link Application JAX-RS Applications} and
     * add them to the {@link HttpServer}.
     *
     * @return  the {@link HttpServer} to use
     */
    public static HttpServer create()
        {
        ServiceLoader<HttpServer> loaderServer = ServiceLoader.load(HttpServer.class);
        Iterator<HttpServer>      itServer     = loaderServer.iterator();

        return itServer.hasNext() ? itServer.next() : new DefaultHttpServer();
        }
    }
