/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package filter.nestinglevel1.nestinglevel2.nestinglevel3;


import com.tangosol.util.extractor.AbstractExtractor;
import filter.QueryRecordReporterTests;


/**
 *  A dummy ValueExtractor with a long canonical name.
 */
public class IntegerToStringPersonKeyExtractor
        extends AbstractExtractor
    {
    // ----- ValueExtractor interface ---------------------------------------
    public Object extract(Object oTarget)
        {
        if (!(oTarget instanceof QueryRecordReporterTests.Person))
            {
            return null;
            }
        return Integer.toString(((QueryRecordReporterTests.Person) oTarget).getKey());
        }

    public String toString()
        {
        return "filter.nestinglevel1.nestinglevel2.nestinglevel3.IntegerToStringPersonKeyExtractor( Person.Key )";
        }

    public boolean equals(Object o)
        {
        return o instanceof IntegerToStringPersonKeyExtractor;
        }

    /**
    * Determine a hash value for the IdentityExtractor object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this IdentityExtractor object
    */
    public int hashCode()
        {
        return 17;
        }

    // ---- constants -------------------------------------------------------
    /**
     * An instance of the extractor.
     */
    public static final IntegerToStringPersonKeyExtractor INSTANCE = new IntegerToStringPersonKeyExtractor();
    }