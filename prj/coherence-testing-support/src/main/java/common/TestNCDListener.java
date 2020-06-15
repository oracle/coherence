/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package common;

import com.tangosol.internal.net.NamedCacheDeactivationListener;

/**
* MapListener implements NamedCacheDeactivationListener.
*
* @author aag  2013.07.26
*/
public class TestNCDListener extends TestMapListener
        implements NamedCacheDeactivationListener
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public TestNCDListener()
        {
        }
    }
