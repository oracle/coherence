/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Java 21 virtual-thread implementation.
 *
 * @author Aleks Seovic  2023.06.20
 * @since 26.07
 */
class VirtualThreadsTest
    {
    @Test
    void shouldCreateVirtualThreadWithoutInheritedThreadLocals() throws InterruptedException
        {
        InheritableThreadLocal<String> local  = new InheritableThreadLocal<>();
        AtomicReference<String>       result = new AtomicReference<>();

        local.set("submitter-context");
        try
            {
            Thread thread = VirtualThreads.makeThread(null, () -> result.set(local.get()), "test-vt");

            assertTrue(VirtualThreads.isVirtual(thread));
            thread.start();
            thread.join();

            assertNull(result.get());
            }
        finally
            {
            local.remove();
            }
        }
    }
