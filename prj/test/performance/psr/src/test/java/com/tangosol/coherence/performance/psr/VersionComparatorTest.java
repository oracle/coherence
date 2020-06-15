/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance.psr;

import com.tangosol.coherence.performance.VersionComparator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2016.02.04
 */
public class VersionComparatorTest
    {
    @Test
    public void shouldBeSorted() throws Exception
        {
        List<String> listVersion = new ArrayList<>();

        listVersion.add("1.0");
        listVersion.add(null);
        listVersion.add("12.1.3-0-0");
        listVersion.add("12.2.2-0-0");
        listVersion.add("12.1.3-0-1");
        listVersion.add("2.0.0");
        listVersion.add("12.2.2");
        listVersion.add("12.2.2-0-1");
        listVersion.add("1.0.0");

        Collections.sort(listVersion, new VersionComparator());

        assertThat(listVersion.get(0), is(nullValue()));
        assertThat(listVersion.get(1), is("1.0"));
        assertThat(listVersion.get(2), is("1.0.0"));
        assertThat(listVersion.get(3), is("2.0.0"));
        assertThat(listVersion.get(4), is("12.1.3-0-0"));
        assertThat(listVersion.get(5), is("12.1.3-0-1"));
        assertThat(listVersion.get(6), is("12.2.2"));
        assertThat(listVersion.get(7), is("12.2.2-0-0"));
        assertThat(listVersion.get(8), is("12.2.2-0-1"));
        }

    }
