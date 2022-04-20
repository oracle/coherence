/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.function;

import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.SimpleParameterList;
import com.tangosol.config.expression.Parameter;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.BigDecimalAverage;
import com.tangosol.util.aggregator.BigDecimalMax;
import com.tangosol.util.aggregator.BigDecimalMin;
import com.tangosol.util.aggregator.BigDecimalSum;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.aggregator.DoubleMax;
import com.tangosol.util.aggregator.DoubleMin;
import com.tangosol.util.aggregator.DoubleSum;
import com.tangosol.util.aggregator.LongMax;
import com.tangosol.util.aggregator.LongMin;
import com.tangosol.util.aggregator.LongSum;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2014.05.07
 */
public class FunctionBuildersTest
    {
    @Test
    public void shouldBuildBigDecimalAverage()
            throws Exception
        {
        ValueExtractor          extractor      = new ReflectionExtractor("getFoo");
        SimpleParameterList     listParameters = new SimpleParameterList(extractor);
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         =
            FunctionBuilders.BIG_DECIMAL_AVERAGE_FUNCTION_BUILDER.realize(resolver, null, listParameters);

        assertThat(result, is(instanceOf(BigDecimalAverage.class)));
        assertThat(((BigDecimalAverage) result).getValueExtractor(), is(sameInstance(extractor)));
        }

    @Test
    public void shouldBuildBigDecimalMax()
            throws Exception
        {
        ValueExtractor          extractor      = new ReflectionExtractor("getFoo");
        SimpleParameterList     listParameters = new SimpleParameterList(extractor);
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         = FunctionBuilders.BIG_DECIMAL_MAX_FUNCTION_BUILDER.realize(resolver,
                                                     null, listParameters);

        assertThat(result, is(instanceOf(BigDecimalMax.class)));
        assertThat(((BigDecimalMax) result).getValueExtractor(), is(sameInstance(extractor)));
        }

    @Test
    public void shouldBuildBigDecimalMin()
            throws Exception
        {
        ValueExtractor          extractor      = new ReflectionExtractor("getFoo");
        SimpleParameterList     listParameters = new SimpleParameterList(extractor);
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         = FunctionBuilders.BIG_DECIMAL_MIN_FUNCTION_BUILDER.realize(resolver,
                                                     null, listParameters);

        assertThat(result, is(instanceOf(BigDecimalMin.class)));
        assertThat(((BigDecimalMin) result).getValueExtractor(), is(sameInstance(extractor)));
        }

    @Test
    public void shouldBuildBigDecimalSum()
            throws Exception
        {
        ValueExtractor          extractor      = new ReflectionExtractor("getFoo");
        SimpleParameterList     listParameters = new SimpleParameterList(extractor);
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         = FunctionBuilders.BIG_DECIMAL_SUM_FUNCTION_BUILDER.realize(resolver,
                                                     null, listParameters);

        assertThat(result, is(instanceOf(BigDecimalSum.class)));
        assertThat(((BigDecimalSum) result).getValueExtractor(), is(sameInstance(extractor)));
        }

    @Test
    public void shouldBuildCount()
            throws Exception
        {
        SimpleParameterList     listParameters = new SimpleParameterList();
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         = FunctionBuilders.COUNT_FUNCTION_BUILDER.realize(resolver, null,
                                                     listParameters);

        assertThat(result, is(instanceOf(Count.class)));
        }

    @Test
    public void shouldBuildDoubleAverage()
            throws Exception
        {
        ValueExtractor          extractor      = new ReflectionExtractor("getFoo");
        SimpleParameterList     listParameters = new SimpleParameterList(extractor);
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         = FunctionBuilders.DOUBLE_AVERAGE_FUNCTION_BUILDER.realize(resolver,
                                                     null, listParameters);

        assertThat(result, is(instanceOf(DoubleAverage.class)));
        assertThat(((DoubleAverage) result).getValueExtractor(), is(sameInstance(extractor)));
        }

    @Test
    public void shouldBuildDoubleMax()
            throws Exception
        {
        ValueExtractor          extractor      = new ReflectionExtractor("getFoo");
        SimpleParameterList     listParameters = new SimpleParameterList(extractor);
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         = FunctionBuilders.DOUBLE_MAX_FUNCTION_BUILDER.realize(resolver, null,
                                                     listParameters);

        assertThat(result, is(instanceOf(DoubleMax.class)));
        assertThat(((DoubleMax) result).getValueExtractor(), is(sameInstance(extractor)));
        }

    @Test
    public void shouldBuildDoubleMin()
            throws Exception
        {
        ValueExtractor          extractor      = new ReflectionExtractor("getFoo");
        SimpleParameterList     listParameters = new SimpleParameterList(extractor);
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         = FunctionBuilders.DOUBLE_MIN_FUNCTION_BUILDER.realize(resolver, null,
                                                     listParameters);

        assertThat(result, is(instanceOf(DoubleMin.class)));
        assertThat(((DoubleMin) result).getValueExtractor(), is(sameInstance(extractor)));
        }

    @Test
    public void shouldBuildDoubleSum()
            throws Exception
        {
        ValueExtractor          extractor      = new ReflectionExtractor("getFoo");
        SimpleParameterList     listParameters = new SimpleParameterList(extractor);
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         = FunctionBuilders.DOUBLE_SUM_FUNCTION_BUILDER.realize(resolver, null,
                                                     listParameters);

        assertThat(result, is(instanceOf(DoubleSum.class)));
        assertThat(((DoubleSum) result).getValueExtractor(), is(sameInstance(extractor)));
        }

    @Test
    public void shouldBuildLongMax()
            throws Exception
        {
        ValueExtractor          extractor      = new ReflectionExtractor("getFoo");
        SimpleParameterList     listParameters = new SimpleParameterList(extractor);
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         = FunctionBuilders.LONG_MAX_FUNCTION_BUILDER.realize(resolver, null,
                                                     listParameters);

        assertThat(result, is(instanceOf(LongMax.class)));
        assertThat(((LongMax) result).getValueExtractor(), is(sameInstance(extractor)));
        }

    @Test
    public void shouldBuildLongMin()
            throws Exception
        {
        ValueExtractor          extractor      = new ReflectionExtractor("getFoo");
        SimpleParameterList     listParameters = new SimpleParameterList(extractor);
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         = FunctionBuilders.LONG_MIN_FUNCTION_BUILDER.realize(resolver, null,
                                                     listParameters);

        assertThat(result, is(instanceOf(LongMin.class)));
        assertThat(((LongMin) result).getValueExtractor(), is(sameInstance(extractor)));
        }

    @Test
    public void shouldBuildLongSum()
            throws Exception
        {
        ValueExtractor          extractor      = new ReflectionExtractor("getFoo");
        SimpleParameterList     listParameters = new SimpleParameterList(extractor);
        ResolvableParameterList resolver       = new ResolvableParameterList();
        Object                  result         = FunctionBuilders.LONG_SUM_FUNCTION_BUILDER.realize(resolver, null,
                                                     listParameters);

        assertThat(result, is(instanceOf(LongSum.class)));
        assertThat(((LongSum) result).getValueExtractor(), is(sameInstance(extractor)));
        }

    @Test
    public void shouldBuildReflectionExtractor()
            throws Exception
        {
        String                  name           = "getFoo";
        SimpleParameterList     listParameters = new SimpleParameterList();
        ResolvableParameterList resolver       = new ResolvableParameterList();

        resolver.add(new Parameter("functionName", name));

        ReflectionExtractor result = FunctionBuilders.METHOD_CALL_FUNCTION_BUILDER.realize(resolver, null,
                                         listParameters);

        assertThat(result, is(new ReflectionExtractor(name)));
        }

    @Test
    public void shouldBuildReflectionExtractorWithArgs()
            throws Exception
        {
        Object[]                args           = new Object[] {1, 2, 3};
        String                  name           = "getFoo";
        SimpleParameterList     listParameters = new SimpleParameterList(1, 2, 3);
        ResolvableParameterList resolver       = new ResolvableParameterList();

        resolver.add(new Parameter("functionName", name));

        ReflectionExtractor result = FunctionBuilders.METHOD_CALL_FUNCTION_BUILDER.realize(resolver, null,
                                         listParameters);

        assertThat(result, is(new ReflectionExtractor(name, args)));
        }

    @Test
    public void shouldBuildIdentityExtractor()
            throws Exception
        {
        SimpleParameterList     listParameters = new SimpleParameterList();
        ResolvableParameterList resolver       = new ResolvableParameterList();
        IdentityExtractor       result         = FunctionBuilders.VALUE_FUNCTION_BUILDER.realize(resolver, null,
                                                     listParameters);

        assertThat(result, is(notNullValue()));
        }
    }
