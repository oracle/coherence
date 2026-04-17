/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import com.tangosol.internal.util.invoke.Lambdas;
import org.junit.jupiter.api.Assumptions;

/**
 * A utility class for JUnit assumptions.
 */
public class CoherenceAssumptions
    {
    /**
     * Skip tests if static lambdas is enabled.
     *
     * @throws org.opentest4j.TestAbortedException if static lambdas are enabled
     */
    public static void isNotStaticLambdas()
        {
        Assumptions.assumeFalse(Lambdas.isStaticLambdas(), "Skipping tests when static lambdas are enabled");
        }
    }
