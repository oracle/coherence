/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.internal.util.invoke.lambda.LambdaIdentity;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Lambdas}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class LambdasTest
    {
    @Test
    public void shouldDetectDynamicLambdaName()
        {
        assertTrue(Lambdas.isDynamicLambdaName("example.Foo$$Lambda/0x0000000800c01440"));
        assertTrue(Lambdas.isDynamicLambdaName("example.Foo$$Lambda$1"));
        }

    @Test
    public void shouldIgnoreNonDynamicLambdaName()
        {
        assertFalse(Lambdas.isDynamicLambdaName("example.Foo"));
        assertFalse(Lambdas.isDynamicLambdaName(null));
        }

    @Test
    public void shouldDetectLambdaIdentityAsDynamic()
        {
        assertTrue(Lambdas.isDynamicLambdaIdentity(new LambdaIdentity("example.Foo", "method", "()V")));
        }

    @Test
    public void shouldDetectDynamicClassIdentityName()
        {
        assertTrue(Lambdas.isDynamicLambdaIdentity(new TestIdentity("example", "Foo$$Lambda/0x0000000800c01440")));
        assertFalse(Lambdas.isDynamicLambdaIdentity(new TestIdentity("example", "Foo")));
        assertFalse(Lambdas.isDynamicLambdaIdentity(null));
        }

    static class TestIdentity
            extends ClassIdentity
        {
        TestIdentity(String sPackage, String sBaseName)
            {
            super(sPackage, sBaseName, "1");
            }
        }
    }
