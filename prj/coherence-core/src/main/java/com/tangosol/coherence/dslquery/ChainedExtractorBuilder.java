/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.util.ValueExtractor;

import java.util.Arrays;
import java.util.List;

/**
 * An {@link ExtractorBuilder} implementation that delegates building
 * {@link ValueExtractor}s to a chain of ExtractorBuilders
 * and returns the result from the first ExtractorBuilder to return a
 * non-null value from its {@link #realize(String, int, String)} method.
 *
 * @author jk 2014.07.15
 * @since Coherence 12.2.1
 */
public class ChainedExtractorBuilder
        implements ExtractorBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ChainedExtractorBuilder that uses the specified chain of
     * {@link ExtractorBuilder}s to realize instances of {@link ValueExtractor}s.
     *
     * @param aBuilders  the chain of ExtractorBuilder to use
     */
    public ChainedExtractorBuilder(ExtractorBuilder...aBuilders)
        {
        f_listBuilders = Arrays.asList(aBuilders);
        }

    // ----- ExtractorBuilder interface -------------------------------------

    /**
     * Call the realize(String, int, String) method on each {@link ExtractorBuilder}
     * in the chain returning the result of the first ExtractorBuilder to return a non-null
     * value.
     *
     * @param sCacheName   the name of the cache the ValueExtractor will be invoked against
     * @param nTarget      the target for the ValueExtractor
     * @param sProperties  the path to the property value to extract
     *
     * @return a {@link ValueExtractor} for the given cache name, target and properties
     *         or null if non of the ExtractorBuilder returned a non-null value
     */
    @Override
    public ValueExtractor realize(String sCacheName, int nTarget, String sProperties)
        {
        for (ExtractorBuilder builder : f_listBuilders)
            {
            ValueExtractor extractor = builder.realize(sCacheName, nTarget, sProperties);

            if (extractor != null)
                {
                return extractor;
                }
            }

        return null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The chain of {@link ExtractorBuilder}s to delegate to.
     */
    protected final List<ExtractorBuilder> f_listBuilders;
    }
