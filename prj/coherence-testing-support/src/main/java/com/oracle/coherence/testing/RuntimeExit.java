/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

/**
 * A {@link RemoteRunnable} that will call {@link Runtime#exit(int)}.
 *
 * @author bo  2014.11.14
 */
public class RuntimeExit implements RemoteRunnable
    {
    @Override
    public void run()
        {
        System.out.println("Terminating using Runtime.exit(0)");

        Runtime.getRuntime().exit(0);
        }
    }
