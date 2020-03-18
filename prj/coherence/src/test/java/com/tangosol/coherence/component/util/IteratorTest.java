/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util;


import org.junit.Test;

import static org.junit.Assert.*;


/**
* Unit test of the Iterator component.
*
* @author jh  2010.05.26
*/
public class IteratorTest
    {
    @Test
    public void testIteration()
        {
        Iterator iter = new Iterator();
        iter.setItem(new Object[0]);

        assertFalse(iter.hasNext());

        iter.setItem(new Object[] {Integer.valueOf(0)});
        iter.setNextIndex(0);

        assertTrue(iter.hasNext());
        assertEquals(iter.next(), Integer.valueOf(0));
        assertFalse(iter.hasNext());

        iter.setItem(new Object[] {Integer.valueOf(0), Integer.valueOf(1)});
        iter.setNextIndex(0);

        assertTrue(iter.hasNext());
        assertEquals(iter.next(), Integer.valueOf(0));
        assertTrue(iter.hasNext());
        assertEquals(iter.next(), Integer.valueOf(1));
        assertFalse(iter.hasNext());
        }
    }
