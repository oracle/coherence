/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.reflect;


import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;


/**
* Abstract base class for static, path-based implementations of
* {@link PofNavigator} interface.
*
* @author as  2009.02.14
* @since Coherence 3.5
*/
public abstract class AbstractPofPath
        extends    Base
        implements PofNavigator, PortableObject, ExternalizableLite
    {
    // ----- PofNavigator interface ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public PofValue navigate(PofValue valueOrigin)
        {
        int[]    aiPathElements = getPathElements();
        PofValue valueCurrent   = valueOrigin;

        for (int i = 0, c = aiPathElements.length; i < c && valueCurrent != null; i++)
            {
            valueCurrent = valueCurrent.getChild(aiPathElements[i]);
            }
        return valueCurrent;
        }


    // ----- abstract methods -----------------------------------------------

    /**
    * Return a collection of path elements.
    *
    * @return a collection of path elements
    */
    protected abstract int[] getPathElements();
    }
