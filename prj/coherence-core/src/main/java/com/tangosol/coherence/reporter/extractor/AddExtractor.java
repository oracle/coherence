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
* MultiExtractor implementation to add two ValueExtractors and extract the result.
*
* @author ew 2008.02.28
* @since Coherence 3.4
*/
public class AddExtractor
    extends MultiExtractor
    {
    // ------ constructors ---------------------------------------------------

    /**
    * Construct an AddExtractor.
    *
    * @param aExtractor  An array of ValueExtractors.  The results of the
    *                    first two extractors will be added.  If more
    *                    than two (2) extractors are passed, the excess
    *                    extractors will be ignored.
    */
    public AddExtractor(ValueExtractor[] aExtractor)
        {
        super(aExtractor);
        Base.azzert(aExtractor.length == 2, "Report addition requires " +
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
            if (o1 instanceof String || o2 instanceof String)
                {
                return o1.toString() + o2.toString();
                }
            else if (o1 instanceof Number && o2 instanceof Number)
                {
                return ((Number) o1).doubleValue() + ((Number) o2).doubleValue();
                }
            }

        return null;
        }

}
