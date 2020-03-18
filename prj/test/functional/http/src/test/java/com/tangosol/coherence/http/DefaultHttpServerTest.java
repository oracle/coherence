/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

public class DefaultHttpServerTest
        extends AbstractHttpServerTest
    {
    @Override
    protected AbstractHttpServer createServer()
        {
        return new DefaultHttpServer();
        }
    }