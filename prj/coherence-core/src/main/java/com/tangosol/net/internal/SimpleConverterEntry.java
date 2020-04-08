/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;


import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections.ConverterEntry;
import com.tangosol.util.SimpleMapEntry;


/**
 * Simple read-only {@link java.util.Map.Entry Entry} implementation that lazily
 * converts the underlying key and value.
 *
 * @author gg 2013.01.07
 * @since Coherence 12.1.3
 */
public class SimpleConverterEntry
        extends ConverterEntry
    {
    /**
     * Construct the SimpleConverterEntry based on the key, value in internal
     * format and the converter to convert from that format.
     */
    public SimpleConverterEntry(Object oKey, Object oValue, Converter converter)
        {
        super(new SimpleMapEntry(oKey, oValue), converter, converter, null);
        }

    @Override
    public Object setValue(Object value)
        {
        throw new UnsupportedOperationException();
        }
    }
