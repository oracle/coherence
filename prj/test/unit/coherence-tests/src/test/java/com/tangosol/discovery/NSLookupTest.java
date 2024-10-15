/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.discovery;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Unit test of the NSLookup class.
 *
 * @author jf  2024.10.07
 */
public class NSLookupTest
    {
    @Test
    public void shouldReportNameWarning()
        {
        for (String sName : NSLookup.VALID_PREDEFINED_LOOKUP_NAMES)
            {
            StringBuffer sbTypo = new StringBuffer(sName);

            sbTypo.setCharAt(0, Character.toUpperCase(sName.charAt(0)));
            sbTypo.setCharAt(1, Character.toLowerCase(sName.charAt(1)));
            sbTypo.setCharAt(2, Character.toUpperCase(sName.charAt(2)));
            assertNotNull("assert predefined lookup name " + sbTypo + " with injected typo results in a warning",
                          NSLookup.validateName(sbTypo.toString()));
            }
        }

    @Test
    public void mustNotReportNameWarning()
        {
        for (String sName : NSLookup.VALID_PREDEFINED_LOOKUP_NAMES)
            {
            assertNull("assert no warning for predefined lookup names", NSLookup.validateName(sName));
            }
        }
    }