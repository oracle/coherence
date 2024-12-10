/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import java.util.LinkedList;

/**
 * An {@link ExtractorBuilder} implementation that will build
 * instances of {@link UniversalExtractor}s.
 *
 * @author jf 2022.01.19
 *
 * @since 12.2.1.3.18
 */
public class UniversalExtractorBuilder
        implements ExtractorBuilder
    {
    // ----- ExtractorBuilder interface -------------------------------------

    @Override
    public ValueExtractor realize(String sCacheName, int nTarget, String sProperties)
        {
        LinkedList<ValueExtractor> listExtractors = new LinkedList<>();

        String[] asPath = sProperties.split("\\.");
        for (int i = 0; i < asPath.length; i++)
            {
            listExtractors.add(new UniversalExtractor(asPath[i], null, nTarget));
            nTarget = AbstractExtractor.VALUE;
            }

        if (listExtractors.size() == 1)
            {
            return listExtractors.getFirst();
            }

        return new ChainedExtractor(listExtractors.toArray(new ValueExtractor[listExtractors.size()]));
        }
    }
