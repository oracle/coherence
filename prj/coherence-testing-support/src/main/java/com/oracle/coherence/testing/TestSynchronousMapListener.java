/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;


import com.tangosol.util.MapListenerSupport;


/**
* MapListener implementation that exposes the last reported MapEvent.
*
* @author jh  2005.11.29
*/
public class TestSynchronousMapListener
        extends TestMapListener
        implements MapListenerSupport.SynchronousListener
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public TestSynchronousMapListener()
        {
        }
    }
