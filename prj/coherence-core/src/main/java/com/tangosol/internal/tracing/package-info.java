/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
/**
 * The classes in this package represent the smallest possible API surface for tracing operations
 * in coherence.  With this API, Coherence can create shims for other tracing implementations such as
 * {@code OpenTracing}, {@code OpenCensus}, and/or {@code OpenTelemetry}.
 * <p>
 * Similarities between the APIs in this package and those of the packages described above is intentional
 * to allow easy conceptual mappings between tracing concepts.
 * <p>
 * For those cases where there is conceptual overlap between the public and our private APIs, there will be explicit
 * attribution to the public APIs.
 * <p>
 * <em>NOTE:</em> the interfaces and classes in this package are not meant for general use by developers using
 * Coherence.
 * <p>
 * For reference:
 * <ul>
 *   <li><a href="http://opentracing.io">OpenTracing</a></li>
 *   <li><a href="http://opencensus.io">OpenCensus</a></li>
 *   <li><a href="http://opentelemetry.io">OpenTelemetry</a></li>
 * </ul>
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
package com.tangosol.internal.tracing;
