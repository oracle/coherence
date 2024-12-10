/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.metrics;

import org.junit.Test;

import java.util.SortedMap;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2020.10.09
 */
public class MBeanMetricTest
    {
    @Test(expected = NullPointerException.class)
    public void shouldNotAllowNullTags()
        {
        new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR, "foo", null);
        }

    @Test
    public void shouldCreateIdentifier()
        {
        String                 sName = "fooBar";
        MBeanMetric.Identifier id    = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR,
                                                                  sName,
                                                                  tags("1", "2", "3", "4"));

        assertThat(id.getScope(), is(MBeanMetric.Scope.VENDOR));
        assertThat(id.getName(), is(sName));
        assertThat(id.getTags(), is(tags("1", "2", "3", "4")));
        }

    @Test
    public void shouldBeEqualIdentifiers()
        {
        String                 sName = "foo.bar";
        MBeanMetric.Identifier one   = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR,
                                                                  sName,
                                                                  tags("1", "2", "3", "4"));
        MBeanMetric.Identifier two   = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR,
                                                                  sName,
                                                                  tags("1", "2", "3", "4"));

        assertThat(one, is(two));
        }

    @Test
    public void shouldGetFormattedIdentifierName()
        {
        String                 sName = "fooBar";
        MBeanMetric.Identifier id    = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR,
                                                                  sName,
                                                                  tags("1", "2", "3", "4"));

        assertThat(id.getFormattedName(), is("foo.bar"));
        }

    @Test
    public void shouldGetLegacyIdentifierName()
        {
        String                 sName = "fooBar";
        MBeanMetric.Identifier id    = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR,
                                                                  sName,
                                                                  tags("1", "2", "3", "4"));

        assertThat(id.getLegacyName(), is("vendor:foo_bar"));
        }

    @Test
    public void shouldGetMicroprofileIdentifierName()
        {
        String                 sName = "fooBar";
        MBeanMetric.Identifier id    = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR,
                                                                  sName,
                                                                  tags("1", "2", "3", "4"));

        assertThat(id.getMicroprofileName(), is("vendor_fooBar"));
        }

    @Test
    public void shouldGetFormattedIdentifierTags()
        {
        String                    sName       = "fooBar";
        SortedMap<String, String> mapTags     = tags("1", "9", "aB", "8", "c_d", "7", "E", "6");
        SortedMap<String, String> mapExpected = tags("1", "9", "a.b", "8", "c.d", "7", "e", "6");
        MBeanMetric.Identifier    id          = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR, sName, mapTags);

        assertThat(id.getFormattedTags(), is(mapExpected));
        }

    @Test
    public void shouldGetPrometheusIdentifierTags()
        {
        String                    sName       = "fooBar";
        SortedMap<String, String> mapTags     = tags("1", "9", "aB", "8", "c_d", "7", "E", "6");
        SortedMap<String, String> mapExpected = tags("1", "9", "a_b", "8", "c_d", "7", "e", "6");
        MBeanMetric.Identifier    id          = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR, sName, mapTags);

        assertThat(id.getPrometheusTags(), is(mapExpected));
        }

    private SortedMap<String, String> tags(String... tags)
        {
        SortedMap<String, String> map = new TreeMap<>();
        for (int i=0; i<tags.length; i++)
            {
            map.put(tags[i++], tags[i]);
            }
        return map;
        }
    }
