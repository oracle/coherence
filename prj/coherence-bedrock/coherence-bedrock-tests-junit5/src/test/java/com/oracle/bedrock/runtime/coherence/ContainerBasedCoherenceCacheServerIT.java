/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.java.ContainerBasedJavaApplicationLauncher;
import com.oracle.bedrock.runtime.java.JavaVirtualMachine;
import org.junit.jupiter.api.Disabled;

@Disabled
public class ContainerBasedCoherenceCacheServerIT
        extends AbstractCoherenceCacheServerTest
    {
    @Override
    public Platform getPlatform()
        {
        return JavaVirtualMachine.get();
        }
    }
