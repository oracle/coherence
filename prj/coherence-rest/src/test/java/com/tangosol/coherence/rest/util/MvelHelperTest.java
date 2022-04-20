/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;


import org.junit.Test;

import static org.junit.Assert.assertFalse;


public class MvelHelperTest
    {
    @Test
    public void validateDisabledByDefault()
        {
        assertFalse(MvelHelper.isEnabled());
        }

    @Test(expected=UnsupportedOperationException.class)
    public void testGetMvelParserContextThrows()
        {
        MvelHelper.getMvelParserContext();
        }

    @Test(expected=UnsupportedOperationException.class)
    public void testGetMvelExecuteThrows()
        {
        JsonMap map = new JsonMap();

        map.put("age", 41);
        MvelHelper.executeExpression("age", map);
        }

    @Test(expected=UnsupportedOperationException.class)
    public void testGetMvelExecuteSetThrows()
        {
        JsonMap map = new JsonMap();

        map.put("age", 41);
        MvelHelper.executeSetExpression("age", map, 43);
        }
    }
