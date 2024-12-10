/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import java.util.Collections;
import java.util.Set;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ChainedSetTest
        extends ChainedCollectionTest
    {
    @SuppressWarnings("unchecked")
    protected Set<Integer> collection()
        {
        return new ChainedSet<>(Set.of(1, 2, 3), Set.of(7, 8, 9));
        }

    @SuppressWarnings("unchecked")
    protected Set<Integer> emptyCollection()
        {
        return new ChainedSet<>(Collections.emptySet(), Collections.emptySet());
        }


    @Test
    public void testEquality()
        {
        assertThat(collection(), is(Set.of(1, 2, 3, 7, 8, 9)));
        assertThat(collection().hashCode(), is(Set.of(1, 2, 3, 7, 8, 9).hashCode()));
        }
    }
