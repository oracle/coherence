/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package coherence.mp.metrics.testing;

import com.oracle.coherence.mp.metrics.CoherenceTagsConfigSource;
import org.eclipse.microprofile.metrics.MetricID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link CoherenceTagsConfigSource}.
 *
 * @author Aleks Seovic  2020.03.26
 */
public class CoherenceTagsConfigSourceIT
    {
    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.cluster", "cluster");
        System.setProperty("coherence.site", "site");
        System.setProperty("coherence.role", "role");
        System.setProperty("coherence.machine", "machine");
        System.setProperty(MetricID.GLOBAL_TAGS_VARIABLE, "tag_1=value_1,tag_2=VALUE_2");
        }

    @Test
    void testGlobalTags()
        {
        MetricID id = new MetricID("test");
        assertThat(id.getTags().get("cluster"), is("cluster"));
        assertThat(id.getTags().get("site"), is("site"));
        assertThat(id.getTags().get("role"), is("role"));
        assertThat(id.getTags().get("node_id"), notNullValue());
        assertThat(id.getTags().get("machine"), is("machine"));
        assertThat(id.getTags().get("member"), nullValue());
        assertThat(id.getTags().get("tag_1"), is("value_1"));
        assertThat(id.getTags().get("tag_2"), is("VALUE_2"));
        }
    }
