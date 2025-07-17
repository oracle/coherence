/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.tracing;

/**
 * Indicates a component is interested in decorating a {@link Span.Builder}
 * with metadata specific to that component.
 *
 * @since 15.1.1.0
 */
public interface SpanDecorator
    {
    /**
     * Decorates the given {@link Span.Builder} with metadata
     * from the implementing component.
     *
     * @param spanBuilder  the {@link Span.Builder}
     *
     * @return the given {@link Span.Builder} after decoration
     */
    default Span.Builder decorate(Span.Builder spanBuilder)
        {
        return spanBuilder;
        }
    }
