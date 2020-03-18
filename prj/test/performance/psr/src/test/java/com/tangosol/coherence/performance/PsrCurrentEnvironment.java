/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.java.ClassPath;

/**
 * @author jk 2016.03.07
 */
public class PsrCurrentEnvironment
        extends PsrPerformanceEnvironment<PsrCurrentEnvironment>
    {
    // ----- constructors ---------------------------------------------------

    public PsrCurrentEnvironment()
        {
        }

    // ----- PerformanceEnvironment methods ---------------------------------

    @Override
    protected ClassPath createClassPath()
        {
        return ClassPath.ofSystem();
        }

    @Override
    protected void modifySchema(Platform platform, OptionsByType schema)
        {
//        super.modifySchema(platform, schema);
        }
    }
