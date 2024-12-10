/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;


import com.tangosol.util.ValueExtractor;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;


/**
* ValueExtractor implementation to extract the difference between the current
* extracted value and the prior extracted value.  This will always return
* non-negative values.  If the prior value is greater than the current value,
* the current value is returned.
*
* @author ew 2008.03.10
* @since Coherence 3.4
*/
public class DeltaExtractor
        implements ValueExtractor
    {
    // ----- constructors ----------------------------------------------------

    /**
    * Construct a DeltaExtractor
    *
    * @param mapPrior  A map of the prior values. The key is determined by the
    *                  mapKey and the value is the prior value.
    * @param veSource  the source ValueExtractor.
    * @param mapKey    A map of ValueExtractors used to determine the key for
    *                  the prior map (mapPrior). If mapKey is null,  the prior
    *                  value key will be the extraction target.
    */
    public DeltaExtractor(Map mapPrior, ValueExtractor veSource, Map mapKey)
        {
        m_veSource = veSource;
        m_mapPrior = mapPrior;
        m_mapKey   = mapKey;
        }


    // ----- ValueExtractor interface ----------------------------------------

    /**
    * @inheritDoc
    */
    public Object extract(Object oTarget)
        {
        Object oValue = m_veSource.extract(oTarget);
        Object oPrior = m_mapPrior.get(getKey(oTarget));
        if (oPrior == null)
            {
            return oValue;
            }

        if (oValue instanceof Number && oPrior instanceof Number)
            {
            Double dValue = ((Number) oValue).doubleValue();
            Double dPrior = ((Number) oPrior).doubleValue();
            return dValue < dPrior ? dValue : dValue - dPrior;
            }

        return null;
        }


    // ----- helpers ---------------------------------------------------------

    /**
    * Determine the key for the prior map based on the
    *
    * @param oTarget  the MBean Object name to convert to the internal delta key.
    *
    * @return the internal delta key
    */
    public Object getKey(Object oTarget)
        {
        Map mapKey = m_mapKey;

        // Most likely instance.
        if (mapKey == null || mapKey.size() == 0)
            {
            return oTarget;
            }
        else
            {
            Set          setKey = mapKey.entrySet();
            StringBuffer sb     = new StringBuffer();
            for (Iterator iter = setKey.iterator(); iter.hasNext();)
                {
                Map.Entry entry  = (Map.Entry) iter.next();
                String    oKey   = (String) entry.getKey();
                Object    oValue = ((ValueExtractor) entry.getValue()).extract(oTarget);
                String    sValue = oValue == null ? "n/a" : oValue.toString();
                sb.append(oKey).append('=').append(sValue).append(',');
                }
            return sb.toString();
            }
        }


    // ----- data members ----------------------------------------------------
    /**
    * The value extractor that will provide the values for the delta
    */
    protected ValueExtractor m_veSource;

    /**
    * A Map containing the prior values for the source
    */
    protected Map m_mapPrior;

    /**
    * A Map containing the aggregate keys for each target
    */
    protected Map m_mapKey;
    }
