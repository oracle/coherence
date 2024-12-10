/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* Implementation of a resource bundle whose resources are supplied to the
* constructor.
*
* @version 1.00, 02/10/99
* @author  Cameron Purdy
*/
public class SimpleResources
        extends Resources
    {
    public SimpleResources(Object[][] contents)
        {
        m_contents = contents;
        }

    public Object[][] getContents()
        {
        return m_contents;
        }

    private Object[][] m_contents;
    }
