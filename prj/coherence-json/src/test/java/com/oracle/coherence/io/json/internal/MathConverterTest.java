/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.oracle.coherence.io.json.genson.JsonBindingException;
import com.oracle.coherence.io.json.genson.reflect.VisibilityFilter;

import java.math.MathContext;

import java.math.RoundingMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Unit tests for {@link MathContextConverter}.
 *
 * @author rl  2023.1.10
 * @since 22.06.2
 */
public class MathConverterTest
    {
    // ----- test setup -----------------------------------------------------

    @BeforeAll
    static void configure()
        {
        s_genson = new GensonBuilder()
                .useClassMetadata(true)
                .useClassMetadataWithStaticType(false)
                .useFields(true, VisibilityFilter.PRIVATE)
                .useMethods(false)
                .useIndentation(false)
                .withConverter(MathContextConverter.INSTANCE, MathContext.class)
                .create();
        }

    // ----- test cases -----------------------------------------------------

    @Test
    public void shouldRoundMathContextWithOnlyPrecision()
        {
        MathContext ctx    = new MathContext(10);
        String      result = s_genson.serialize(ctx);
        MathContext deCtx  = s_genson.deserialize(result, MathContext.class);

        assertThat(deCtx, is(ctx));
        }

    @Test
    public void shouldRoundMathContextWithPrecisionAndRoundingMode()
        {
        MathContext ctx    = new MathContext(10, RoundingMode.CEILING);
        String      result = s_genson.serialize(ctx);
        MathContext deCtx  = s_genson.deserialize(result, MathContext.class);

        assertThat(deCtx, is(ctx));
        }

    @Test
    public void shouldFailIfContainsNoAttributes()
        {
        assertThrows(JsonBindingException.class,
                     () -> s_genson.deserialize("{\"@class\"=\"java.math.MathContext\"}", MathContext.class));
        }

    @Test
    public void shouldFailIfContainsOnlyRoundingModeAttribute()
        {
        assertThrows(JsonBindingException.class,
                     () -> s_genson.deserialize("{\"@class\"=\"java.math.MathContext\",\"roundingMode\"=\"CEILING\"}", MathContext.class));
        }

    @Test
    public void shouldFailIfContainsOnlyRoundingModeAttributeAndIsEmpty()
        {
        assertThrows(JsonBindingException.class,
                     () -> s_genson.deserialize("{\"@class\"=\"java.math.MathContext\",\"roundingMode\"=\"\"}", MathContext.class));
        }

    @Test
    public void shouldFailIfContainsOnlyRoundingModeAttributeIsUnknown()
        {
        assertThrows(JsonBindingException.class,
                     () -> s_genson.deserialize("{\"@class\"=\"java.math.MathContext\",\"roundingMode\"=\"DERP\"}", MathContext.class));
        }

    @Test
    public void shouldFailIfContainsInvalidPrecision()
        {
        assertThrows(JsonBindingException.class,
                     () -> s_genson.deserialize("{\"@class\"=\"java.math.MathContext\",\"precision\"=-1}", MathContext.class));
        }

    // ----- data members ---------------------------------------------------

    protected static Genson s_genson;
    }
