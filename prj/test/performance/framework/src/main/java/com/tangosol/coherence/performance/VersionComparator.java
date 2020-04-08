/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import java.util.Comparator;

/**
 * @author jk 2016.02.04
 */
public class VersionComparator
        implements Comparator<String>
    {
    // ----- constructors ---------------------------------------------------

    public VersionComparator()
        {
        }

    // ----- VersionComparator methods ------------------------------------------------

    @Override
    public int compare(String sVersion1, String sVersion2)
        {
        if (sVersion1 == null && sVersion2 == null)
            {
            return 0;
            }

        if (sVersion1 == null || sVersion2 == null)
            {
            return sVersion1 == null ? -1 : 1;
            }

        String[] asParts1 = sVersion1.split("\\.");
        String[] asParts2 = sVersion2.split("\\.");
        int      nCount   = Math.min(asParts1.length, asParts2.length);

        for (int i=0; i<nCount; i++)
            {
            int n = comparePart(asParts1[i], asParts2[i]);

            if (n != 0)
                {
                return n;
                }
            }

        if (asParts1.length == nCount && asParts2.length == nCount)
            {
            return 0;
            }
        else if (asParts1.length == nCount)
            {
            return -1;
            }

        return 1;
        }

    private int comparePart(String s1, String s2)
        {
        String[] asParts1 = s1.split("-");
        String[] asParts2 = s2.split("-");
        int      nCount   = Math.min(asParts1.length, asParts2.length);

        for (int i=0; i<nCount; i++)
            {
            int nCompare;

            try
                {
                int n1 = Integer.parseInt(asParts1[i]);
                int n2 = Integer.parseInt(asParts2[i]);

                nCompare = Integer.compare(n1, n2);
                }
            catch (NumberFormatException e)
                {
                nCompare = asParts1[i].compareTo(asParts2[i]);
                }

            if (nCompare != 0)
                {
                return nCompare;
                }
            }

        if (asParts1.length == nCount && asParts2.length == nCount)
            {
            return 0;
            }
        else if (asParts1.length == nCount)
            {
            return -1;
            }

        return 1;
        }
    }
