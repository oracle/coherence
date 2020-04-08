/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.collection;


import com.tangosol.net.NamedCache;

import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections.ConverterNamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;

import com.tangosol.util.filter.InKeySetFilter;


/**
 * Internal extension of {@link ConverterNamedCache} that is aware of the
 * conversion conventions around the {@link InKeySetFilter} use for the
 * {@link NamedCache#addMapListener(MapListener, Filter, boolean)} API.
 *
 * @author gg 2016.04.19
 */
public class ConvertingNamedCache
        extends ConverterNamedCache
    {
    /**
    * Constructor.
    *
    * @param cache        the underlying NamedCache
    * @param convKeyUp    the Converter to view the underlying
    *                     NamedCache's keys through
    * @param convKeyDown  the Converter to use to pass keys down to the
    *                     underlying NamedCache
    * @param convValUp    the Converter to view the underlying
    *                     NamedCache's values through
    * @param convValDown  the Converter to use to pass values down to the
    *                     underlying NamedCache
    */
    public ConvertingNamedCache(NamedCache cache, Converter convKeyUp,
            Converter convKeyDown, Converter convValUp, Converter convValDown)
        {
        super(cache, convKeyUp, convKeyDown, convValUp, convValDown);
        }

    @Override
    public void addMapListener(MapListener listener, Filter filter, boolean fLite)
        {
        if (filter instanceof InKeySetFilter)
            {
            // The mere fact that we are here, indicates that
            // (a) the keys are in Binary format, converted by the extend client
            // serializer, and
            // (b) that serializer not compatible with the serializer used by the
            // underlying NamedCache (see CacheServiceProxy#ensureTypedCache).
            //
            // Therefore, we need to deserialize the keys in the filter using
            // the "down" converter and mark the filter as "unconverted".

            ((InKeySetFilter) filter).ensureUnconverted(getConverterKeyDown());
            }

        super.addMapListener(listener, filter, fLite);
        }

    @Override
    public void removeMapListener(MapListener listener, Filter filter)
        {
        if (filter instanceof InKeySetFilter)
            {
            ((InKeySetFilter) filter).ensureUnconverted(getConverterKeyDown());
            }

        super.removeMapListener(listener, filter);
        }
    }
