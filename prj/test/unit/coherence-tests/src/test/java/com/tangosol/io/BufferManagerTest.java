/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.io.nio.AbstractBufferManager;

import com.tangosol.util.Base;

import org.junit.Test;


/**
* Test AbstractBufferManager.
*
* @author dag  2009.04.02
*/
public class BufferManagerTest
        extends Base
    {
    // ----- test methods ---------------------------------------------------

    /**
    * Test if grow() can finish (no infinite loop)
    */
    @Test
    public void testGrow()
        {
        int cbInit = 1024 * 1024;
        int cbMax  = Integer.MAX_VALUE - 1023;

        BufferManagerTester mgr = new BufferManagerTester(cbInit, cbMax);

        // Trying to grow to values near Integer.MAX_VALUE used to cause
        // an infinite loop.
        mgr.grow(mgr.getMaxCapacity());
        }

        // ----- inner class ------------------------------------------------

        /**
        * Extend AbstactBufferManager in order to detect an infinite loop.
        *
        * {@inheritDoc}
        */
        public class BufferManagerTester
                extends AbstractBufferManager
            {
            /**
            * Construct an AbstractBufferManager that supports a buffer of a
            * certain initial and maximum size.
            *
            * @param cbInitial the initial size
            * @param cbMaximum the maximum size
            */
            protected BufferManagerTester(int cbInitial, int cbMaximum)
                {
                super(cbInitial, cbMaximum);
                }

            protected void allocateBuffer()
                {
                // Don't allocate anything. Just for test.
                }

            /**
            * Gets called for every iteration of the allocation size
            * calculation in AbstractBufferManager.grow(), so keep
            * track of iterations here.
            *
            * @param cb  the capacity of the managed ByteBuffer
            */
            protected void setCapacity(int cb)
                {
                super.setCapacity(cb);
                // if it iterates too many times, assume infinite loop
                azzert(m_cIter++ < 50);
                }


            /**
            * Count of how many times the grow method size calculation has
            * iterated.
            */
            int m_cIter = 0;
            }
    }
