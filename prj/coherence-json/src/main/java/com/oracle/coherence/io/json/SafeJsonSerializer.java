/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.MultiplexingSerializer;

import javax.inject.Named;

/**
 * An extension of {@link MultiplexingSerializer} that registers the {@link JsonSerializer}
 * and the {@link DefaultSerializer} (in that order).
 *
 * If an object can't be serialized/deserialized as JSON, then attempt it using Java serialization.
 *
 * @since 20.06
 */
@Named("SafeJson")
public class SafeJsonSerializer
        extends MultiplexingSerializer
    {
    // ----- constructors ---------------------------------------------------

    public SafeJsonSerializer()
        {
        super(new JsonSerializer(), new DefaultSerializer());
        }
    }

