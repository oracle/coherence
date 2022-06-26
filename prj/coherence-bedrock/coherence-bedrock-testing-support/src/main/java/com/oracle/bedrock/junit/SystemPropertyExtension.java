/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.junit;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A JUnit 5 extension that sets System properties in the
 * {@link BeforeAllCallback before all phase}.
 * <p>
 * By using this extension with {@link org.junit.jupiter.api.Order @Order}
 * after other extensions, it is possible to configures properties based on
 * the state of other extensions before tests run.
 *
 * @author Jonathan Knight 2022.06.20
 * @since 22.06
 */
public class SystemPropertyExtension
        implements BeforeAllCallback
    {
    @Override
    public void beforeAll(ExtensionContext context) throws Exception
        {
        properties.forEach((k, v) ->
            {
            Object o = v.get();
            if (o != null)
                {
                System.setProperty(k, String.valueOf(o));
                }
            });
        }

    /**
     * Add a property to set.
     * <p>
     * The {@code toString()} of the value returned by the supplier will be used
     * as the property value.
     * <p>
     * If the {@link Supplier} returns {@code null} then no property will be set.
     *
     * @param key    the key of the property
     * @param value  a supplier for the property value
     *
     * @return  this builder
     */
    public SystemPropertyExtension withProperty(String key, Supplier<?> value)
        {
        properties.put(key, value);
        return this;
        }

    /**
     * The {@link Map} of properties to set.
     */
    private final Map<String, Supplier<?>> properties = new HashMap<>();
    }
