/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.tangosol.io.DefaultSerializer;

import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.util.Arrays;
import java.util.Collection;

import javax.cache.processor.EntryProcessorException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class CoherenceEntryProcessorResultTest
    {
    public CoherenceEntryProcessorResultTest(String sSerializer)
        {
        f_serializer = sSerializer.equalsIgnoreCase("pof")
                       ? new ConfigurablePofContext("coherence-jcache-junit-pof-config.xml")
                       : new DefaultSerializer();
        }

    @Parameterized.Parameters(name = "f_serializer={0}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]
            {
            {"pof"}, {"java"}
            });
        }

    // ----- tests -------------------------------------------------------------

    @Test
    public void testSerializationOfResult()
        {
        CoherenceEntryProcessorResult value  = new CoherenceEntryProcessorResult("aStringResult");
        Binary                        bin    = ExternalizableHelper.toBinary(value, f_serializer);
        CoherenceEntryProcessorResult result = (CoherenceEntryProcessorResult) ExternalizableHelper.fromBinary(bin, f_serializer);

        assertThat(result.get(), is(value.get()));
        }

    @Test
    public void testSerializationOfException()
        {
        Exception                     ex    = new IllegalStateException("failed in entry processor");
        CoherenceEntryProcessorResult value = new CoherenceEntryProcessorResult(ex);

        Binary binary = ExternalizableHelper.toBinary(value, f_serializer);
        Object result = ExternalizableHelper.fromBinary(binary, f_serializer);

        try
            {
            ((CoherenceEntryProcessorResult) result).get();
            fail("CoherenceEntryProcesorResult " + result + " should have thrown exception");
            }
        catch (EntryProcessorException e)
            {
            assertNotNull(e.getCause());
            assertThat(e.getCause().getMessage(), is(ex.getMessage()));
            assertThat(e.getCause().getClass().getName(), is(ex.getClass().getName()));
            }
        }

    // ----- constants ------------------------------------------------------

    private final Serializer f_serializer;
    }