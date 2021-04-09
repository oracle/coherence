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

import java.util.Date;


/**
* MultiExtractor extension to subtract the results of the second ValueExtractor
* by the results of the first ValueExtractor.  If either one of the results is
* not numeric, null will be returned.
*
* @author ew 2008.02.20
* @since Coherence 3.4
*/

public class SubtractExtractor
    extends MultiExtractor
    {
    // ----- constructors ----------------------------------------------------

    /**
    * Construct a SubtractExtractor.
    *
    * @param aExtractor  An array of ValueExtractors.  The results of the second
    *                    extractor will be  subtracted from the results of the
    *                    first.  If more than two (2) extractors are passed.
    *                    the excess extractors will be ignored.  If less than
    *                    two (2) extractors an assert error will occur.
    */
    public SubtractExtractor(ValueExtractor[] aExtractor)
        {
        super(aExtractor);
        Base.azzert(aExtractor.length == 2, "Report subraction requires " +
                "two and only two arguments");
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
            if (o1 instanceof Number && o2 instanceof Number)
                {
                return ((Number) o1).doubleValue() - ((Number) o2).doubleValue();
                }
            if (o1 instanceof Date && o2 instanceof Date)
                {
                return ((Date) o1).getTime() - ((Date) o2).getTime();
                }
            }
        return null;
        }
    }
