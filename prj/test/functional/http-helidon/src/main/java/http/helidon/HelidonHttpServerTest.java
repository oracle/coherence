/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package http.helidon;

import com.oracle.coherence.http.helidon.HelidonHttpServer;
import com.tangosol.coherence.http.AbstractHttpServer;

import http.AbstractHttpServerTest;

public class HelidonHttpServerTest
        extends AbstractHttpServerTest
    {
    @Override
    protected AbstractHttpServer createServer()
        {
        return new HelidonHttpServer();
        }
    }
