/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package http.config;

import com.tangosol.coherence.http.AbstractHttpServer;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;

/**
 * A stub HTTP server.
 *
 * @author jk  2019.04.27
 */
public class HttpServerStub
        extends AbstractHttpServer
    {
    @Override
    protected void startInternal() throws IOException
        {
        }

    @Override
    protected void stopInternal() throws IOException
        {
        }

    @Override
    protected Object instantiateContainer(ResourceConfig config, ServiceLocator locator)
        {
        return null;
        }
    }
