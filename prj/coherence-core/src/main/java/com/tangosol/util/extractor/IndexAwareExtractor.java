/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.net.BackingMapContext;

import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.util.Comparator;
import java.util.Map;


/**
* IndexAwareExtractor is an extension to the {@link ValueExtractor} interface
* that supports the creation and destruction of an {@link MapIndex index}.
* Instances of this interface are intended to be used with the
* {@link com.tangosol.util.QueryMap#addIndex} and
* {@link com.tangosol.util.QueryMap#removeIndex} API to support the creation
* of custom indexes.
*
* @author tb 2010.02.08
* @since Coherence 3.6
*/
public interface IndexAwareExtractor<T, E>
        extends ValueExtractor<T, E>
    {
    /**
    * Create an index and associate it with the corresponding extractor.
    * Important note: it is a responsibility of this method's implementations
    * to place the necessary &lt;ValueExtractor, MapIndex&gt; entry into the
    * given map of indexes.
    *
    * @param fOrdered    true iff the contents of the indexed information
    *                    should be ordered; false otherwise
    * @param comparator  the Comparator object which imposes an ordering
    *                    of entries in the index contents; or <tt>null</tt>
    *                    if the entries' values natural ordering should be
    *                    used
    * @param mapIndex    Map&lt;ValueExtractor, MapIndex&gt; to be updated with the
    *                    created index
    * @param ctx         The {@link BackingMapContext context} the index is
    *                    associate with.
    *
    * @return the created index; null if the index has not been created
    */
    public MapIndex createIndex(boolean fOrdered, Comparator comparator,
                                Map<ValueExtractor<T, E>, MapIndex> mapIndex,
                                BackingMapContext ctx);

    /**
    * Destroy an existing index and remove it from the given map of indexes.
    *
    * @param mapIndex   map&lt;ValueExtractor, MapIndex&gt; to be updated by
    *                   removing the index being destroyed
    *
    * @return the destroyed index; null if the index does not exist
    */
    public MapIndex destroyIndex(Map<ValueExtractor<T, E>, MapIndex> mapIndex);

    /**
    * Obtain the underlying ValueExtractor that was added to the index map
    * during the {@link #createIndex index creation}.
    *
    * @param mapIndex   Map&lt;ValueExtractor, MapIndex&gt; containing the index
    *                   {@link #createIndex created by this extractor}
    * @param index      the index {@link #createIndex created by this extractor}
    *
    * @return the corresponding ValueExtractor
    *
    * @since Coherence 12.2.1.1
    */
    default public ValueExtractor getExtractor(Map<ValueExtractor<T, E>, MapIndex> mapIndex, MapIndex index)
        {
        for (Map.Entry<ValueExtractor<T, E>, MapIndex> entry : mapIndex.entrySet())
            {
            if (entry.getValue() == index)
                {
                return entry.getKey();
                }
            }
        return null;
        }
    }
