/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import java.util.LinkedList;

/**
 * An {@link ExtractorBuilder} implementation that will build
 * instances of {@link ReflectionExtractor}s.
 *
 * @author jk 2014.07.15
 * @since Coherence 12.2.1
 */
public class ReflectionExtractorBuilder
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
            String        sPath = asPath[i];
            StringBuilder sb    = new StringBuilder("get").append(Character.toUpperCase(sPath.charAt(0)));

            if (sPath.length() > 1)
                {
                sb.append(sPath.substring(1));
                }

            listExtractors.add(new ReflectionExtractor(sb.toString(), null, nTarget));
            nTarget = AbstractExtractor.VALUE;
            }

        if (listExtractors.size() == 1)
            {
            return listExtractors.getFirst();
            }

        return new ChainedExtractor(listExtractors.toArray(new ValueExtractor[listExtractors.size()]));
        }
    }
