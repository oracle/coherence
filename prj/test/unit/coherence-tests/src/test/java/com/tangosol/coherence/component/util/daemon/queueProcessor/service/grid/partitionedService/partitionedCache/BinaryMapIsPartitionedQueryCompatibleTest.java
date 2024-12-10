/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache;

import com.tangosol.internal.util.VersionHelper;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
* Unit tests for BinaryMap.isPartitionedQueryCompatible.
*
* @author tam 2024.08.28
*/
public class BinaryMapIsPartitionedQueryCompatibleTest
    {

    /**
     * Test isPartitionedQueryCompatible() on BinaryMap.
     * 
     * Partitioned query was introduced in the following patches/ releases. This check ensures that any
     * breaking change made to VersionHelper will be caught.
     *
     * - 14.1.2.0.0
     * - 23.09.1
     * - 14.1.1.2206.7
     * - 22.06.7
     */
    @Test
    public void testIsPartitionedQueryCompatible()
        {
        assertFalse(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(23, 9, 0)));
        assertTrue(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(23, 9, 1)));
        assertTrue(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(24, 3, 0)));
        assertTrue(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(24, 3, 1)));
        assertTrue(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(24, 9, 0)));
        assertTrue(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(14, 1, 2, 0, 0)));
        assertFalse(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(14, 1, 1, 2206, 6)));
        assertTrue(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(14, 1, 1, 2206, 7)));
        assertFalse(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(22, 6, 6)));
        assertTrue(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(22, 6, 7)));
        assertFalse(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(14, 1, 1, 0, 15)));
        assertFalse(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(14, 1, 1, 0, 16)));
        assertFalse(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(12, 2, 1, 4, 19)));
        assertFalse(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(12, 2, 1, 4, 20)));
        assertFalse(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(12, 2, 1, 6, 5)));
        assertFalse(BinaryMap.isPartitionedQueryCompatible(VersionHelper.encodeVersion(12, 2, 1, 6, 7)));
        }
    }
