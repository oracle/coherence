/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import java.io.Serializable;

/**
 * An opaque representation of the position of an element within a channel of a topic.
 * <p>
 * The exact implementation of a {@link Position} is dependent on the {@link NamedTopic}
 * implementation.
 * <p>
 * Positions are {@link Comparable} so that topic elements can be compared to find
 * their relative positions to one another.
 * <p>
 * Positions are serializable, so they can be stored and recovered in their binary serialized format,
 * which is useful for use-cases where the state of processing of specific topic elements needs
 * to be tracked and stored in a third-party data store (such as a {@link com.tangosol.net.NamedMap}
 * or database).
 *
 * @author Jonathan Knight  2021.05.05
 * @since 21.06
 */
public interface Position
        extends Serializable, Comparable<Position>
    {
    }
