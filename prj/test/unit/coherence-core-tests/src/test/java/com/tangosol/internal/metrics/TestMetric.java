/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;


import java.util.Map;


/**
 * An implementation of {@code MBeanMetric} that can be used for testing.
 *
 * @author as  2019.06.30
 */
public class TestMetric extends BaseMBeanMetric
    {
    public TestMetric(Scope scope, String name, Map<String, String> mapTags, String description, Object value)
        {
        super(new Identifier(scope, name, mapTags), null, description);
        f_value = value;
        }

    public Object getValue()
        {
        return f_value;
        }

    private final Object f_value;
    }
