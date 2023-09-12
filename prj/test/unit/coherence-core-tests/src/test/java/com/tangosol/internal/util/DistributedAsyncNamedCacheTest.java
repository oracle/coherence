/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class DistributedAsyncNamedCacheTest
    {
    public DistributedAsyncNamedCacheTest(String sVersion, int nVersion, boolean fExpected)
        {
        m_nVersion  = nVersion;
        m_fExpected = fExpected;
        }

    @Test
    public void shouldBeBinaryProcessorCompatible()
        {
        assertThat(DistributedAsyncNamedCache.isBinaryProcessorCompatible(m_nVersion), is(m_fExpected));
        }

    @Parameterized.Parameters(name= "{index}: version={0}")
    public static List<Object[]> versions()
        {
        return Arrays.asList(
                new Object[]{"15.1.1.0.0", VersionHelper.encodeVersion(15, 1, 1, 0, 0), true},
                new Object[]{"14.1.1.2206.7", VersionHelper.encodeVersion(14, 1, 1, 2206, 7), true},
                new Object[]{"14.1.1.2206.6", VersionHelper.encodeVersion(14, 1, 1, 2206, 6), true},
                new Object[]{"14.1.1.2206.5", VersionHelper.encodeVersion(14, 1, 1, 2206, 5), false},
                new Object[]{"14.1.1.2206.4", VersionHelper.encodeVersion(14, 1, 1, 2206, 4), false},
                new Object[]{"14.1.1.2206.3", VersionHelper.encodeVersion(14, 1, 1, 2206, 3), false},
                new Object[]{"14.1.1.2206.2", VersionHelper.encodeVersion(14, 1, 1, 2206, 2), false},
                new Object[]{"14.1.1.2206.1", VersionHelper.encodeVersion(14, 1, 1, 2206, 1), false},
                new Object[]{"14.1.1.2206.0", VersionHelper.encodeVersion(14, 1, 1, 2206, 0), false},
                new Object[]{"14.1.2.0.1", VersionHelper.encodeVersion(14, 1, 2, 0, 1), true},
                new Object[]{"14.1.2.0.0", VersionHelper.encodeVersion(14, 1, 2, 0, 0), true},
                new Object[]{"14.1.1.0.16", VersionHelper.encodeVersion(14, 1, 1, 0, 16), false},
                new Object[]{"14.1.1.0.15", VersionHelper.encodeVersion(14, 1, 1, 0, 15), false},
                new Object[]{"14.1.1.0.14", VersionHelper.encodeVersion(14, 1, 1, 0, 14), false},
                new Object[]{"12.2.1.4.14", VersionHelper.encodeVersion(12, 2, 1, 4, 14), false},
                new Object[]{"23.09.0", VersionHelper.encodeVersion(23, 9, 0), true},
                new Object[]{"23.03.1", VersionHelper.encodeVersion(23, 3, 1), false},
                new Object[]{"23.03.0", VersionHelper.encodeVersion(23, 3, 0), false},
                new Object[]{"22.06.6", VersionHelper.encodeVersion(22, 6, 6), true},
                new Object[]{"22.06.0", VersionHelper.encodeVersion(22, 6, 0), false},
                new Object[]{"20.06.1", VersionHelper.encodeVersion(20, 6, 1), false}
            );
        }

    // ----- data members ---------------------------------------------------

    private final int m_nVersion;

    private final boolean m_fExpected;
    }
