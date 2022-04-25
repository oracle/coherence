/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.http;

import com.sun.net.httpserver.HttpHandler;
import com.tangosol.net.Service;

/**
 * A {@link HttpHandler} that is aware of its parent proxy service.
 *
 * @author Jonathan Knight 2022.01.04
 * @since 22.06
 */
public interface ServiceAwareHandler
        extends HttpHandler
    {
    /**
     * Set the handler's parent service.
     *
     * @param service  the parent service
     */
    void setService(Service service);

    /**
     * Return the handler's parent service.
     *
     * @return  the parent service
     */
    Service getService();
    }
