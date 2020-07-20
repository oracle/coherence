/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.examples.rest.processor;

import com.tangosol.coherence.rest.util.processor.ProcessorFactory;
import com.tangosol.examples.rest.model.Product;
import com.tangosol.util.InvocableMap;

/**
 * Example {@link ProcessorFactory} implementation that returns an
 * {@link InvocableMap.EntryProcessor} for adjusting the price of a product.
 * <p>
 * Called via rest by using POST
 * <pre>
 * http://localhost:8080/cache/product/price-adjust(1.1)
 * </pre>
 * or for a single product:
 * <pre>
 * http://localhost:8080/cache/product/10/price-adjust(1.1)
 * </pre>
 *
 * @author tam 2015.07.10
 * @since 12.2.1
 */
public class PriceAdjustProcessorFactory
        implements ProcessorFactory<Integer, Product, Void>
    {
    // ----- ProcessorFactory methods ---------------------------------------

    @Override
    public InvocableMap.EntryProcessor<Integer, Product, Void> getProcessor(String[] asArgs)
        {
        if (asArgs.length != 1)
            {
            throw new IllegalArgumentException("Must supply an argument to processor");
            }

        return (entry) ->
            {
            Product product = entry.getValue();
            product.setPrice(Float.valueOf(asArgs[0]) * product.getPrice());
            entry.setValue(product);
            return null;
            };
        }
    }
