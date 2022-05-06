/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jaeger1_0;

import io.opentracing.Span;

import org.junit.Test;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import tracing.jaeger.AbstractJaegerPartitionedCacheOperationsTracingTests;

/**
 * Validate {@code PartitionedCache} produces tracing {@link Span spans} when performing cache operations.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
@RunWith(Parameterized.class)
public class PartitionedCacheOperationsTracingTests
        extends AbstractJaegerPartitionedCacheOperationsTracingTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Run the test using the specified cache configuration.
     *
     * @param sSerializerName  the serializer name
     * @param sCacheConfig     cache configuration
     */
    public PartitionedCacheOperationsTracingTests(String sSerializerName, String sCacheConfig)
        {
        super(sSerializerName, sCacheConfig);
        }

    // ----- test methods ---------------------------------------------------

    @Override
    @Test
    public void testGetTracing()
        {
        super.testGetTracing();
        }

    @Override
    @Test
    public void testPutTracing()
        {
        super.testPutTracing();
        }

    @Override
    @Test
    public void testRemoveTracing()
        {
        super.testRemoveTracing();
        }
    }
