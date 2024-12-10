/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.java.LocalJavaApplicationLauncher;

public class LocalCoherenceCacheServerIT
        extends AbstractCoherenceCacheServerTest
    {
    @Override
    public Platform getPlatform()
        {
        return LocalPlatform.get();
        }
    }
