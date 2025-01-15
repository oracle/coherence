/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.net.topic.Position;

/**
 * The result of a seek operation.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface SeekResult
        extends Comparable<SeekResult>
    {
    /**
     * Obtain the channel head.
     *
     * @return  the channel head
     */
    Position getHead();

    /**
     * Obtain the position seeked to.
     *
     * @return the position seeked to
     */
    Position getSeekPosition();
    }
