/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

/**
 * @author jk 2015.11.26
 */
public class GetJavaVersion
        implements RemoteCallable<String>
    {
    @Override
    public String call() throws Exception
        {
        return System.getProperty("java.version");
        }
    }
