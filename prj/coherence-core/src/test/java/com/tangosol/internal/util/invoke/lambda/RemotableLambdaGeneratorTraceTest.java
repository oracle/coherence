/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke.lambda;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.util.function.Remote;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.lang.invoke.SerializedLambda;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for opt-in shim generation tracing.
 *
 * @author Aleks Seovic  2026.05.04
 * @since 26.04
 */
public class RemotableLambdaGeneratorTraceTest
    {
    @Test
    public void shouldNotTraceByDefault()
        {
        String sTrace = captureTrace(false, "test/generated/TraceLambda$lambda$apply$1$0");

        assertFalse(sTrace.contains("COH-SHIM-TRACE"));
        }

    @Test
    public void shouldTraceGeneratedLambdaWhenEnabled()
        {
        String sTrace = captureTrace(true, "test/generated/TraceLambda$lambda$apply$1$0");

        assertTrue(sTrace.contains("COH-SHIM-TRACE name=test.generated.TraceLambda$lambda$apply$1$0|kind=lambda"));
        }

    private String captureTrace(boolean fEnabled, String sClassName)
        {
        String      sProperty = System.getProperty(PROP_TRACE);
        PrintStream err       = System.err;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
            {
            if (fEnabled)
                {
                System.setProperty(PROP_TRACE, "true");
                }
            else
                {
                System.clearProperty(PROP_TRACE);
                }

            System.setErr(new PrintStream(out, true, StandardCharsets.UTF_8.name()));
            Remote.Function<String, String> function = s -> s.trim();
            SerializedLambda metadata = Lambdas.getSerializedLambda(function);

            RemotableLambdaGenerator.createRemoteLambdaClass(
                    sClassName,
                    metadata,
                    RemotableLambdaGeneratorTraceTest.class.getClassLoader());

            return out.toString(StandardCharsets.UTF_8.name());
            }
        catch (UnsupportedEncodingException e)
            {
            throw new AssertionError(e);
            }
        finally
            {
            System.setErr(err);
            if (sProperty == null)
                {
                System.clearProperty(PROP_TRACE);
                }
            else
                {
                System.setProperty(PROP_TRACE, sProperty);
                }
            }
        }

    private static final String PROP_TRACE = "coherence.internal.invoke.trace.shim";
    }
