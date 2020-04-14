/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Set;


/**
* A Set specialization of InflatableCollection.
*
* @author ch  2009.11.22
* @since Coherence 3.6
*/
public class InflatableSet
        extends    InflatableCollection
        implements Set
    {
    // ----- factory methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected InflatedCollection instantiateCollection()
        {
        return new InflatedSet();
        }


    // ----- inner types -----------------------------------------------------

    /**
    * Inflated set implementation. Uses {@link SafeHashSet} and marks the
    * implementation with {@see InflatedCollection}
    */
    private static class InflatedSet
            extends     SafeHashSet
            implements  InflatedCollection
        {
        }
    }
