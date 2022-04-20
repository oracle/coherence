/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import com.tangosol.util.Base;
import org.junit.Assume;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;

/**
 * A helper that will allow tests to be skipped if Jacoco
 * is being used.
 *
 * @author jk  2019.12.17
 */
public class JacocoHelper
    {
    public static void skipIfJacocoInstrumented()
        {
        skipIfJacocoInstrumented(Base.class);
        }

    public static void skipIfJacocoInstrumented(Class<?> cls)
        {
        long nMethods = Arrays.stream(cls.getDeclaredMethods()).filter(m -> m.getName().startsWith("$jacoco")).count();
        Assume.assumeThat("Skipping method due to classes being instrumented by Jacoco", nMethods, is(0L));
        }
    }
