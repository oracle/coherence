/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance.psr;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

/**
 * @author jk 2015.11.25
 */
public class RunnerIsRunning
        implements RemoteCallable<Boolean>
    {
    @Override
    public Boolean call() throws Exception
        {
        return Runner.s_runner.isRunning();
        }
    }
