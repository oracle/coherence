/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package common;

import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

/**
 * A {@link RemoteRunnable} that will call {@link Runtime#halt(int)}.
 *
 * @author bo  2014.11.14
 */
public class RuntimeHalt implements RemoteRunnable
    {
    @Override
    public void run()
        {
        System.out.println("Terminating using Runtime.halt(0)");

        Runtime.getRuntime().halt(0);
        }
    }
