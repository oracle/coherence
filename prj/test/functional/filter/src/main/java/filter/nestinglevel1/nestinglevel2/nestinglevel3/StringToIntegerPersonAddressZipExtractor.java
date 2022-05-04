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
public class StringToIntegerPersonAddressZipExtractor
        extends AbstractExtractor
    {
    // ----- ValueExtractor interface ---------------------------------------
    public Integer extract(Object oTarget)
        {
        if (!(oTarget instanceof QueryRecordReporterTests.Person))
            {
            return null;
            }
        return Integer.parseInt(((QueryRecordReporterTests.Person) oTarget).getAddress().getZip());
        }

    public String toString()
        {
        return "filter.nestinglevel1.nestinglevel2.nestinglevel3.StringToIntegerZipExtractor( Person.Address.Zip )";
        }

    public boolean equals(Object o)
            {
            return o instanceof StringToIntegerPersonAddressZipExtractor;
            }

        /**
        * Determine a hash value for the IdentityExtractor object according to
        * the general {@link Object#hashCode()} contract.
        *
        * @return an integer hash value for this IdentityExtractor object
        */
        public int hashCode()
            {
            return 23;
            }

    // ---- constants -------------------------------------------------------
    /**
     * An instance of the extractor.
     */
    public static final StringToIntegerPersonAddressZipExtractor INSTANCE =
        new StringToIntegerPersonAddressZipExtractor();
    }