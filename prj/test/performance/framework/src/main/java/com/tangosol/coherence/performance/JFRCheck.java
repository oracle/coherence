/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

/**
 * @author jk 2016.04.18
 */
public class JFRCheck
        implements RemoteCallable<String>
    {

    public JFRCheck()
        {
        }

    @Override
    public String call() throws Exception
        {
        return JavaFlightRecorder.checkRecordings();
        }
    }
