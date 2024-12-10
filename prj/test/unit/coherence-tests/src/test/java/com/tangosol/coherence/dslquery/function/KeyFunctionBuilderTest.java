/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.function;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.SimpleParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.pof.reflect.SimplePofPath;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.junit.Assert.assertThat;

/**
 * @author jk  2014.01.02
 */
public class KeyFunctionBuilderTest
    {
    @Test
    public void shouldBuildCorrectExtractorIfArgsAreNull()
            throws Exception
        {
        ParameterList        listParameters = new SimpleParameterList();
        ParameterResolver    resolver       = new ResolvableParameterList();
        ParameterizedBuilder builder        = FunctionBuilders.KEY_FUNCTION_BUILDER;
        ValueExtractor       result         = (ValueExtractor) builder.realize(resolver, null, listParameters);

        assertThat(result, is(instanceOf(KeyExtractor.class)));
        assertThat(((KeyExtractor) result).getExtractor(), is(instanceOf(IdentityExtractor.class)));
        }

    @Test
    public void shouldBuildExtractorIfArgsIsChainedExtractor()
            throws Exception
        {
        ValueExtractor keyExtractor = new ReflectionExtractor("getFoo", new Object[] {"arg1", "arg2"},
                                          ReflectionExtractor.KEY);
        ValueExtractor extractor = new ChainedExtractor(new ReflectionExtractor("getFoo",
                                       new Object[] {"arg1", "arg2"}), new ReflectionExtractor("getBar",
                                           new Object[] {"arg3",
                "arg4"}));

        ParameterList        listParameters = new SimpleParameterList(extractor);
        ParameterResolver    resolver       = new ResolvableParameterList();
        ParameterizedBuilder builder        = FunctionBuilders.KEY_FUNCTION_BUILDER;

        ValueExtractor       result         = (ValueExtractor) builder.realize(resolver, null, listParameters);

        assertThat(result, is(sameInstance(extractor)));
        assertThat(((ChainedExtractor) result).getExtractors()[0], is(keyExtractor));

        }

    @Test
    public void shouldBuildExtractorIfArgsIsReflectionExtractor()
            throws Exception
        {
        ValueExtractor       keyExtractor   = new ReflectionExtractor("getFoo", new Object[] {"arg1", "arg2"},
                                                                      AbstractExtractor.KEY);
        ValueExtractor       extractor      = new ReflectionExtractor("getFoo", new Object[] {"arg1", "arg2"});
        ParameterList        listParameters = new SimpleParameterList(extractor);
        ParameterResolver    resolver       = new ResolvableParameterList();
        ParameterizedBuilder builder        = FunctionBuilders.KEY_FUNCTION_BUILDER;
        ValueExtractor       result         = (ValueExtractor) builder.realize(resolver, null, listParameters);

        assertThat(result, is(keyExtractor));
        }

    @Test
    public void shouldBuildExtractorIfArgsIsPofExtractor()
            throws Exception
        {
        ValueExtractor       keyExtractor   = new PofExtractor(Integer.class, new SimplePofPath(123), PofExtractor.KEY);
        ValueExtractor       extractor      = new PofExtractor(Integer.class, 123);
        ParameterList        listParameters = new SimpleParameterList(extractor);
        ParameterResolver    resolver       = new ResolvableParameterList();
        ParameterizedBuilder builder        = FunctionBuilders.KEY_FUNCTION_BUILDER;
        ValueExtractor       result         = (ValueExtractor) builder.realize(resolver, null, listParameters);

        assertThat(result, is(keyExtractor));
        }
    }
