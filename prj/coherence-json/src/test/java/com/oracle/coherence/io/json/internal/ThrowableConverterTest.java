/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for {@link ThrowableConverter}.
 *
 * @author Jonathan Knight  2018.11.28
 * @since 20.06
 */
class ThrowableConverterTest
    {
    // ----- test cases -----------------------------------------------------

    @Test
    void shouldSerializeThrowable()
        {
        JsonSerializer serializer = new JsonSerializer();
        Throwable      throwable  = getThrowable();
        Binary         binary     = ExternalizableHelper.toBinary(throwable, serializer);
        Throwable      result     = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getMessage(), is(throwable.getMessage()));

        // Assert that the deserialized stack trace has the original stack at the top

        StringWriter writerOriginal = new StringWriter();
        StringWriter writerResult   = new StringWriter();

        throwable.printStackTrace(new PrintWriter(writerOriginal));
        result.printStackTrace(new PrintWriter(writerResult));

        List<String> listOriginal = Arrays.stream(writerOriginal.getBuffer().toString().split("\n"))
                .skip(1)
                .collect(Collectors.toList());

        List<String> listResult = Arrays.stream(writerResult.getBuffer().toString().split("\n"))
                .skip(1)
                .limit(listOriginal.size())
                .collect(Collectors.toList());

        assertThat(listResult, is(listOriginal));
        }

    // ----- helper methods -------------------------------------------------

    protected Throwable getThrowable()
        {
        try
            {
            throw new RuntimeException("Computer says No!");
            }
        catch (Throwable thrown)
            {
            return thrown;
            }
        }
    }
