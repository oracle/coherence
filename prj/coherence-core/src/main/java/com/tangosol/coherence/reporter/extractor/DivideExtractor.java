/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;


import com.tangosol.util.Base;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.ValueExtractor;


/**
* MultiExtractor extension to divide the results of the first ValueExtractor
* by the results of the second ValueExtractor.  If the demominator is zero (0)
* or not numeric, null will be returned.
*
* @author ew 2008.02.20
* @since Coherence 3.4
*/
public class DivideExtractor
    extends MultiExtractor
    {
    // ----- constructors ----------------------------------------------------

    /**
    * Construct a DivideExtractor.
    *
    * @param aExtractor  An array of ValueExtractors.  The results of the first
    *                    extractor will be divided by the results of the
    *                    second.  If more than two (2) extractors are passed.
    *                    the excess extractors will be ignored.  If less than
    *                    two (2) extractors null will be returned.
    */
    public DivideExtractor(ValueExtractor[] aExtractor)
        {
        super(aExtractor);
        Base.azzert(aExtractor.length == 2, "Report division requires " +
                "two and only two arguments.");

        }

    // ----- ValueExtractor interface ----------------------------------------

    /**
    * @inheritDoc
    */
    public Object extract(Object oTarget)
        {
        ImmutableArrayList arResults = (ImmutableArrayList)super.extract(oTarget);
        if (arResults.size() > 1)
            {
            Object o1 = arResults.get(0);
            Object o2 = arResults.get(1);

            if (o2 == null || ((Number) o2).doubleValue() == 0)
                {
                return null;
                }
            else if (o1 instanceof Number && o2 instanceof Number)
                {
                return ((Number) o1).doubleValue() / ((Number) o2).doubleValue();
                }
            }
        return null;
        }
    }
