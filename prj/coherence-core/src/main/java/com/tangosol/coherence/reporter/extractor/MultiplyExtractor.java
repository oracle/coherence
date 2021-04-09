/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;


import com.tangosol.util.Base;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ImmutableArrayList;


/**
* MultiExtractor extension to muliply the results of the two (2)
* ValueExtractor(s).
*
* @author ew 2008.02.20
* @since Coherence 3.4
*/
public class MultiplyExtractor
    extends MultiExtractor
    {
    // ------ constructors ---------------------------------------------------
    /**
    * Construct a MultiplyExtractor.
    *
    * @param aExtractor  An array of ValueExtractors.  The results of the
    *                    first two extractors will be multiplied.  If more
    *                    than two (2) extractors are passed, the excess
    *                    extractors will be ignored.
    */
    public MultiplyExtractor(ValueExtractor[] aExtractor)
        {
        super(aExtractor);
        Base.azzert(aExtractor.length == 2, "Report multiplication requires " +
                "two and only two arguments");
        }

    public Object extract(Object oTarget)
        {
        ImmutableArrayList arResults = (ImmutableArrayList)super.extract(oTarget);
        if (arResults.size() > 1)
            {
            Object o1 = arResults.get(0);
            Object o2 = arResults.get(1);
            if (o1 instanceof Number && o2 instanceof Number)
                {
                return ((Number) o1).doubleValue() * ((Number) o2).doubleValue();
                }
            }
        return null;
        }
    }
