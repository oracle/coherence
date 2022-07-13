/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client;

import com.oracle.coherence.guides.client.model.TenantMetaData;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ApplicationTest
    {
    @Test
    public void shouldLoadTenants() throws Exception
        {
        Map<String, TenantMetaData> expected = new HashMap<>();
        expected.put("foo", new TenantMetaData("foo", "extend", "foo.com", 20000, "java"));
        expected.put("bar", new TenantMetaData("bar", "grpc", "bar.com", 1408, "java"));

        Application main = new Application();
        Map<String, TenantMetaData> result = main.loadTenants("test-tenants.json");
        assertThat(result, is(expected));
        }

    }
