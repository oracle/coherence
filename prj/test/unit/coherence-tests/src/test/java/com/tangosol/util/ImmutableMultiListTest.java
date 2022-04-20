/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.List;


/**
* Unit test for ImmutableMultiList
*
* @author mf  08.17.2009
*/
public class ImmutableMultiListTest
    extends ImmutableArrayListTest
    {
    // ----- internal test methods ------------------------------------------

    public List _makeList(Long[] aL)
        {
        final int[] iBreak = {3, 45, 45, 123, 436, 684, 1027, 5326, 8132, 9999};

        // compute number of sub-arrays to create
        int ca = 1;
        int cL = aL.length;
        for (int i = 0, c = iBreak.length; i < c && iBreak[i] < cL; ++i, ++ca);

        Long[][] aaL = new Long[ca][];
        int cpLast = 0;
        for (int i = 0; i < ca; ++i)
            {
            int cp = (i == ca - 1)
                ? cL - cpLast
                : iBreak[i] - cpLast;

            System.arraycopy(aL, cpLast, aaL[i] = new Long[cp], 0, cp);
            cpLast += cp;
            }

        return new ImmutableMultiList(aaL);
        }
    }
