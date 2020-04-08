/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.reflect;


/**
* The PofNavigator interface represents an algorithm for navigating a PofValue
* hierarchy in order to locate a contained PofValue for extraction, modification
* or removal purposes.
*
* @author as 2009.02.14
* @since Coherence 3.5
*/
public interface PofNavigator
    {
    /**
    * Locate the {@link PofValue} identified by this PofNavigator within the
    * passed PofValue. If one of the intermediate navigation values is null,
    * return the null value immediately.
    *
    * @param valueOrigin  the origin from which navigation starts
    *
    * @return the resulting PofValue
    *
    * @throws PofNavigationException  if the navigation fails; for example one
    *         of the intermediate nodes in this path is a "terminal" PofValue
    *         such as {@link SimplePofValue}
    */
    public PofValue navigate(PofValue valueOrigin);
    }
