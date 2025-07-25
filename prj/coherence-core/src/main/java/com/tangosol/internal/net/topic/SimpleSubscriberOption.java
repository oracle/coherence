/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.topic.Subscriber;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link Subscriber.Option} to enable use of a gRPC simple subscriber.
 *
 * @param <V>  the type of the value on the topic
 * @param <U>  the type of the value returned to the subscriber
 */
public class SimpleSubscriberOption<V, U>
        implements Subscriber.Option<V, U>, ExternalizableLite, PortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public SimpleSubscriberOption() {}

    @Override
    @SuppressWarnings("RedundantMethodOverride")
    public void readExternal(DataInput in) throws IOException
        {
        }

    @Override
    @SuppressWarnings("RedundantMethodOverride")
    public void writeExternal(DataOutput out) throws IOException
        {
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }

    /**
     * The CompleteOnEmpty singleton.
     */
    public static final SimpleSubscriberOption<?, ?> INSTANCE = new SimpleSubscriberOption<>();
    }
