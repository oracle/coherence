/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.tracing;

/**
 * Indicates implementing entity is tracing-aware and will
 * accept having the parent span name and span provided
 * in order to associate components within a span graph.
 *
 * @since 15.1.1.0
 */
public interface TracingAware
    {
    /**
     * Sets the parent span name and span that the implementing
     * component may use to build the span graph.
     *
     * @param sSpanName  the parent {@link Span} name
     * @param spanParent the parent {@link Span}
     */
    void setParentSpan(String sSpanName, Span spanParent);
    }
