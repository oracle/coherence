/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.Serializable;

import java.util.Comparator;


/**
* Comparator implementation that performs a case-insensitive comparision of
* two strings.
*
* @author jh  2009.12.15
*/
public class CaseInsensitiveComparator
        implements Comparator, PortableObject, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new CaseInsenstiveComparator.
    */
    public CaseInsensitiveComparator()
        {
        }


    // ----- Comparator interface -------------------------------------------

    /**
    * Perform a case-insensitive comparision of two strings.
    */
    public int compare(Object o1, Object o2)
        {
        String s1 = (String) o1;
        String s2 = (String) o2;

        int n1 = s1.length();
        int n2 = s2.length();

        for (int i1 = 0, i2 = 0; i1 < n1 && i2 < n2; ++i1, ++i2)
            {
            char c1 = s1.charAt(i1);
            char c2 = s2.charAt(i2);
            if (c1 != c2)
                {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if (c1 != c2)
                    {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if (c1 != c2)
                        {
                        return c1 - c2;
                        }
                    }
                 }
            }

        return n1 - n2;
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        }
    }
