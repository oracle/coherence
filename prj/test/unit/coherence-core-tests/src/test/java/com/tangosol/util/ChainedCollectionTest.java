/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class ChainedCollectionTest
    {
    @SuppressWarnings("unchecked")
    protected Collection<Integer> collection()
        {
        return new ChainedCollection<>(new Collection[] {List.of(1, 2, 3), Set.of(7, 8, 9)});
        }

    @SuppressWarnings("unchecked")
    protected Collection<Integer> emptyCollection()
        {
        return new ChainedCollection<>(new Collection[] {Collections.emptyList(), Collections.emptySet()});
        }

    @Test
    public void testSize()
        {
        assertThat(collection().size(), is(6));
        assertThat(emptyCollection().size(), is(0));
        }

    @Test
    public void testIsEmpty()
        {
        assertThat(collection().isEmpty(), is(false));
        assertThat(emptyCollection().isEmpty(), is(true));
        }

    @Test
    public void testContains()
        {
        assertThat(collection().contains(1), is(true));
        assertThat(collection().contains(5), is(false));
        assertThat(collection().contains(9), is(true));
        assertThat(emptyCollection().contains(1), is(false));
        assertThat(emptyCollection().contains(5), is(false));
        assertThat(emptyCollection().contains(9), is(false));
        }

    @Test
    public void testContainsAll()
        {
        assertThat(collection().containsAll(Set.of(1, 3, 7, 9)), is(true));
        assertThat(collection().containsAll(Set.of(1, 3, 5, 9)), is(false));
        assertThat(emptyCollection().containsAll(Set.of(1, 2, 3)), is(false));
        }

    @Test
    public void testIterator()
        {
        Set<Integer> set = new HashSet<>(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        for (Integer n : collection())
            {
            set.remove(n);
            }
        assertThat(set, containsInAnyOrder(4, 5, 6));
        }

    @Test
    public void testToArray()
        {
        assertThat(collection().toArray(), arrayContainingInAnyOrder(1, 2, 3, 7, 8, 9));
        assertThat(collection().toArray(Integer[]::new), arrayContainingInAnyOrder(1, 2, 3, 7, 8, 9));
        assertThat(emptyCollection().toArray().length, is(0));
        assertThat(emptyCollection().toArray(Integer[]::new).length, is(0));
        }
    }
