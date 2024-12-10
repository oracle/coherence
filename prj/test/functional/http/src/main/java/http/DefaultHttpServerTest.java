/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package http;

import com.tangosol.coherence.http.AbstractHttpServer;
import com.tangosol.coherence.http.DefaultHttpServer;

public class DefaultHttpServerTest
        extends AbstractHttpServerTest
    {
    @Override
    protected AbstractHttpServer createServer()
        {
        return new DefaultHttpServer();
        }
    }
