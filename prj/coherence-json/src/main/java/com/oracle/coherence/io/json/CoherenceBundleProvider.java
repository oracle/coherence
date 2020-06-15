/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.oracle.coherence.io.json.genson.ext.GensonBundle;

import com.oracle.coherence.io.json.internal.NullSetConverter;

import com.tangosol.util.NullImplementation;

/**
 * A {@link GensonBundleProvider} providing a {@link GensonBundle} that configures package aliases.
 *
 * @since 20.06
 */
public class CoherenceBundleProvider
        implements GensonBundleProvider
    {
    // ----- GensonBundleProvider interface ---------------------------------

    @Override
    public GensonBundle provide()
        {
        return new CoherenceBundle();
        }

    // ----- inner class: CoherenceBundle -----------------------------------

    /**
     * A {@link GensonBundle} that adds package aliases.
     */
    protected static final class CoherenceBundle
            extends GensonBundle
        {
        // ----- GensonBundle interface -------------------------------------

        @Override
        public void configure(GensonBuilder builder)
            {
            builder.withConverter(NullSetConverter.INSTANCE, NullImplementation.NullSet.class)
                    .addPackageAlias("aggregator", "com.tangosol.util.aggregator")
                    .addPackageAlias("comparator", "com.tangosol.util.comparator")
                    .addPackageAlias("extractor",  "com.tangosol.util.extractor")
                    .addPackageAlias("filter",     "com.tangosol.util.filter")
                    .addPackageAlias("processor",  "com.tangosol.util.processor");
            }
        }
    }
