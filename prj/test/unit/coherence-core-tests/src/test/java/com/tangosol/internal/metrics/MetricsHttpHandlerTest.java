/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MetricsHttpHandlerTest
    {
    @Test
    public void shouldHaveCorrectDefaultFormat()
        {
        MetricsHttpHandler resource = new MetricsHttpHandler();
        MetricsHttpHandler.Format format = resource.getFormat();
        assertThat(format, is(MetricsHttpHandler.Format.Default));
        }
    }
