/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.function;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.dslquery.CohQLException;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;

import com.tangosol.util.Base;
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

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

/**
 * This class contains a number of {@link ParameterizedBuilder}
 * implementations for the standard built-in CohQL functions.
 *
 * @author jk 2014.05.07
 * @since Coherence 12.2.1
 */
public final class FunctionBuilders
    {
    /**
     * This builder will realize instances of the {@link BigDecimalAverage} aggregator.
     * This builder is called as a result of the CohQL bd_avg() function.
     */
    public static ParameterizedBuilder<BigDecimalAverage> BIG_DECIMAL_AVERAGE_FUNCTION_BUILDER =
        new ParameterizedBuilder<BigDecimalAverage>()
            {
            @Override
            public BigDecimalAverage realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new BigDecimalAverage(getFirstParameter(resolver, listParameters, ValueExtractor.class));
                }
            };

    /**
     * This builder will realize instances of the {@link BigDecimalMax} aggregator.
     * This builder is called as a result of the CohQL bd_max() function.
     */
    public static ParameterizedBuilder<BigDecimalMax> BIG_DECIMAL_MAX_FUNCTION_BUILDER =
        new ParameterizedBuilder<BigDecimalMax>()
            {
            @Override
            public BigDecimalMax realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new BigDecimalMax(getFirstParameter(resolver, listParameters, ValueExtractor.class));
                }
            };

    /**
     * This builder will realize instances of the {@link BigDecimalMin} aggregator.
     * This builder is called as a result of the CohQL bd_min() function.
     */
    public static ParameterizedBuilder<BigDecimalMin> BIG_DECIMAL_MIN_FUNCTION_BUILDER =
        new ParameterizedBuilder<BigDecimalMin>()
            {
            @Override
            public BigDecimalMin realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new BigDecimalMin(getFirstParameter(resolver, listParameters, ValueExtractor.class));
                }
            };

    /**
     * This builder will realize instances of the {@link BigDecimalSum} aggregator.
     * This builder is called as a result of the CohQL bd_sum() function.
     */
    public static ParameterizedBuilder<BigDecimalSum> BIG_DECIMAL_SUM_FUNCTION_BUILDER =
        new ParameterizedBuilder<BigDecimalSum>()
            {
            @Override
            public BigDecimalSum realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new BigDecimalSum(getFirstParameter(resolver, listParameters, ValueExtractor.class));
                }
            };

    /**
     * This builder will result in a concatenation of all function arguments to a single string result.
     * This builder is called as a result of the CohQL concat() function.
     *
     * @since 21.06
     */
    public static ParameterizedBuilder<String> CONCAT_FUNCTION_BUILDER = (resolver, loader, listParameters) ->
        {
        StringBuilder sb = new StringBuilder();

        if (listParameters.size() < 2)
            {
            throw new CohQLException("CONCAT requires at least two arguments");
            }

        for (Parameter listParameter : listParameters)
            {
            sb.append(listParameter.evaluate(resolver).get().toString());
            }
        return sb.toString();
        };

    /**
     * This builder will realize instances of the {@link Count} aggregator.
     * This builder is called as a result of the CohQL count() function.
     */
    public static ParameterizedBuilder<Count> COUNT_FUNCTION_BUILDER = new ParameterizedBuilder<Count>()
            {
            @Override
            public Count realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new Count();
                }
            };

    /**
     * This builder will realize instances of the {@link DoubleAverage} aggregator.
     * This builder is called as a result of the CohQL avg() function.
     */
    public static ParameterizedBuilder<DoubleAverage> DOUBLE_AVERAGE_FUNCTION_BUILDER =
        new ParameterizedBuilder<DoubleAverage>()
            {
            @Override
            public DoubleAverage realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new DoubleAverage(getFirstParameter(resolver, listParameters, ValueExtractor.class));
                }
            };

    /**
     * This builder will realize instances of the {@link DoubleMax} aggregator.
     * This builder is called as a result of the CohQL max() function.
     */
    public static ParameterizedBuilder<DoubleMax> DOUBLE_MAX_FUNCTION_BUILDER = new ParameterizedBuilder<DoubleMax>()
            {
            @Override
            public DoubleMax realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new DoubleMax(getFirstParameter(resolver, listParameters, ValueExtractor.class));
                }
            };

    /**
     * This builder will realize instances of the {@link DoubleMin} aggregator.
     * This builder is called as a result of the CohQL min() function.
     */
    public static ParameterizedBuilder<DoubleMin> DOUBLE_MIN_FUNCTION_BUILDER = new ParameterizedBuilder<DoubleMin>()
            {
            @Override
            public DoubleMin realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new DoubleMin(getFirstParameter(resolver, listParameters, ValueExtractor.class));
                }
            };

    /**
     * This builder will realize instances of the {@link DoubleSum} aggregator.
     * This builder is called as a result of the CohQL sum() function.
     */
    public static ParameterizedBuilder<DoubleSum> DOUBLE_SUM_FUNCTION_BUILDER = new ParameterizedBuilder<DoubleSum>()
            {
            @Override
            public DoubleSum realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new DoubleSum(getFirstParameter(resolver, listParameters, ValueExtractor.class));
                }
            };

    /**
     * This builder will realize instances of the {@link LongMax} aggregator.
     * This builder is called as a result of the CohQL long_max() function.
     */
    public static ParameterizedBuilder<LongMax> LONG_MAX_FUNCTION_BUILDER = new ParameterizedBuilder<LongMax>()
            {
            @Override
            public LongMax realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new LongMax(getFirstParameter(resolver, listParameters, ValueExtractor.class));
                }
            };

    /**
     * This builder will realize instances of the {@link LongMin} aggregator.
     * This builder is called as a result of the CohQL long_min() function.
     */
    public static ParameterizedBuilder<LongMin> LONG_MIN_FUNCTION_BUILDER = new ParameterizedBuilder<LongMin>()
            {
            @Override
            public LongMin realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new LongMin(getFirstParameter(resolver, listParameters, ValueExtractor.class));
                }
            };

    /**
     * This builder will realize instances of the {@link LongSum} aggregator.
     * This builder is called as a result of the CohQL long_sum() function.
     */
    public static ParameterizedBuilder<LongSum> LONG_SUM_FUNCTION_BUILDER = new ParameterizedBuilder<LongSum>()
            {
            @Override
            public LongSum realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new LongSum(getFirstParameter(resolver, listParameters, ValueExtractor.class));
                }
            };

    /**
     * This builder will realize instances of a {@link ReflectionExtractor} that will call
     * a specific method.
     */
    public static ParameterizedBuilder<ReflectionExtractor> METHOD_CALL_FUNCTION_BUILDER =
        new ParameterizedBuilder<ReflectionExtractor>()
            {
            @Override
            public ReflectionExtractor realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                Parameter paramFunction = resolver.resolve("functionName");
                Value     value         = paramFunction.evaluate(resolver);
                String    sFunction     = (String) value.get();

                if (listParameters.isEmpty())
                    {
                    return new ReflectionExtractor(sFunction);
                    }

                Object[] ao = new Object[listParameters.size()];
                int      i  = 0;

                for (Parameter parameter : listParameters)
                    {
                    ao[i++] = parameter.evaluate(resolver).get();
                    }

                return new ReflectionExtractor(sFunction, ao);
                }
            };

    /**
     * This builder will realize instances of the {@link IdentityExtractor} aggregator.
     * This builder is called as a result of the CohQL value() function.
     */
    public static ParameterizedBuilder<IdentityExtractor> VALUE_FUNCTION_BUILDER =
        new ParameterizedBuilder<IdentityExtractor>()
            {
            @Override
            public IdentityExtractor realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return IdentityExtractor.INSTANCE;
                }
            };

    /**
     * This {@link ParameterizedBuilder} handles the key() function.
     * The type of {@link ValueExtractor} realized will depend on
     * the type of the first element in the args array passed to the
     * realize method.
     *
     * This builder is called as a result of the CohQL key() function.
     */
    public static ParameterizedBuilder<ValueExtractor> KEY_FUNCTION_BUILDER =
        new ParameterizedBuilder<ValueExtractor>()
            {
            @Override
            public ValueExtractor realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                ValueExtractor extractor = listParameters.isEmpty()
                                           ? IdentityExtractor.INSTANCE
                                           : getFirstParameter(resolver, listParameters, ValueExtractor.class);

                return realizeExtractor(extractor);
                }

            /**
             * Make sure that specified {@link ValueExtractor} targets the
             * key if a cache entry.
             *
             * @param extractorOrig  the ValueExtractor to target at the key
             *
             * @return the ValueExtractor targeted at a cache entry's key
             */
            protected ValueExtractor realizeExtractor(ValueExtractor extractorOrig)
                {
                Class<? extends ValueExtractor> clsExtractor = extractorOrig.getClass();

                if (UniversalExtractor.class.equals(clsExtractor))
                    {
                    UniversalExtractor extractor = (UniversalExtractor) extractorOrig;

                    return new UniversalExtractor(extractor.getMethodName() + "()", extractor.getParameters(),
                                                   UniversalExtractor.KEY);
                    }

                if (ReflectionExtractor.class.equals(clsExtractor))
                    {
                    ReflectionExtractor extractor = (ReflectionExtractor) extractorOrig;

                    return new ReflectionExtractor(extractor.getMethodName(), extractor.getParameters(),
                                                   ReflectionExtractor.KEY);
                    }

                if (PofExtractor.class.equals(clsExtractor))
                    {
                    PofExtractor extractor = (PofExtractor) extractorOrig;

                    return new PofExtractor(extractor.getClassExtracted(), extractor.getNavigator(), PofExtractor.KEY);
                    }

                if (ChainedExtractor.class.equals(clsExtractor))
                    {
                    ValueExtractor[] extractors = ((ChainedExtractor) extractorOrig).getExtractors();

                    // ensure that the first ValueExtractor in the chain is targeted at the key
                    // by recursively calling back into this method
                    extractors[0] = realizeExtractor(extractors[0]);
                    ((ChainedExtractor) extractorOrig).ensureTarget();

                    // return the ChainedExtractor with the first ValueExtractor re-targeted at the Key
                    return extractorOrig;
                    }

                return new KeyExtractor(extractorOrig);
                }
            };
    
    // ---- helper methods --------------------------------------------------

    /**
     * Extract the first parameter from the listParameters argument.
     *
     * @param <V>             the parameter type
     * @param resolver        the {@link ParameterResolver} for resolving named {@link Parameter}s
     * @param listParameters  an optional {@link ParameterList} (may be <code>null</code>) to be used
     *                        for realizing the instance, eg: used as constructor parameters
     * @param clzExpected     the expected type of the first parameter
     *
     * @return the first parameter from the listParameters argument
     *
     * @throws com.tangosol.util.AssertionException if the listParameters list is empty or if the
     *         first parameter resolved from the list is null or not of the expected type.
     */
    protected static <V> V getFirstParameter(ParameterResolver resolver, ParameterList listParameters, 
                                             Class<V> clzExpected)
        {
        Base.azzert(!listParameters.isEmpty());

        Parameter parameter = listParameters.iterator().next();
        Value     value     = parameter.evaluate(resolver);
        Object    oResult   = value.get();

        Base.azzert(oResult != null);
        Base.azzert(clzExpected.isAssignableFrom(oResult.getClass()));

        return (V) oResult;
        }
    
    }
