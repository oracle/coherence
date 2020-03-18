/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.metrics.internal;


import java.io.IOException;
import java.io.Writer;


/**
 * An interface that should be implemented by concrete metrics formatters
 * we want to support (Prometheus, JSON, etc.)
 *
 * @author as  2019.06.29
 * @since 12.2.1.4.0
 */
public interface MetricsFormatter
    {
    void writeMetrics(Writer writer) throws IOException;
    }
