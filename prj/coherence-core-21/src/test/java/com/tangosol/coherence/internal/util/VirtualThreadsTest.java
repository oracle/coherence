/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.internal.util;

import com.tangosol.internal.util.VirtualThreads;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class VirtualThreadsTest
    {
    @Test
    public void ensureThreadIsVirtual()
        {
        Thread t = VirtualThreads.makeThread(null, () -> {}, "MyThread");

        assertThat(t.isVirtual(), is(true));
        assertThat(t.getName(), is("MyThread"));
        }
    }
