/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.metrics;

import org.eclipse.microprofile.metrics.MetricID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link CoherenceTagsConfigSource}.
 *
 * @author Aleks Seovic  2020.03.26
 */
public class CoherenceTagsConfigSourceTest
    {
    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.cluster", "cluster");
        System.setProperty("coherence.site", "site");
        System.setProperty("coherence.role", "role");
        System.setProperty(MetricID.GLOBAL_TAGS_VARIABLE, "tag_1=value_1,tag_2=VALUE_2");
        }

    @Test
    @Disabled("BLOCKER: regression in Helidon 2.0")
    void testGlobalTags()
        {
        MetricID id = new MetricID("test");
        assertThat(id.getTags().get("cluster"), is("cluster"));
        assertThat(id.getTags().get("site"), is("site"));
        assertThat(id.getTags().get("role"), is("role"));
        assertThat(id.getTags().get("node_id"), notNullValue());
        assertThat(id.getTags().get("machine"), nullValue());
        assertThat(id.getTags().get("member"), nullValue());
        assertThat(id.getTags().get("tag_1"), is("value_1"));
        assertThat(id.getTags().get("tag_2"), is("VALUE_2"));
        }
    }
