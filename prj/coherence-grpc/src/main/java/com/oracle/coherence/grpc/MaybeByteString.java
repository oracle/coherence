/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.google.protobuf.ByteString;

/**
 * A wrapper that may contain a {@link ByteString}.
 */
public class MaybeByteString
    {
    /**
     * Create a {@link MaybeByteString}.
     *
     * @param value  the optional {@link ByteString} to wrap
     */
    private MaybeByteString(ByteString value)
        {
        m_value = value;
        }

    /**
     * Determine whether this {@link MaybeByteString} contains a {@link ByteString}.
     *
     * @return {@code true} if this wrapper contains a {@link ByteString}
     */
    public boolean isPresent()
        {
        return m_value != null;
        }

    /**
     * Return the wrapped value.
     *
     * @return the wrapped value
     */
    public ByteString value()
        {
        return m_value;
        }

    /**
     * Create a {@link MaybeByteString}.
     *
     * @param s  the {@link ByteString} to wrap
     *
     * @return a {@link MaybeByteString} wrapping the specified {@link ByteString}
     */
    public static MaybeByteString ofNullable(ByteString s)
        {
        return new MaybeByteString(s == null ? ByteString.EMPTY : s);
        }

    /**
     * Create an empty {@link MaybeByteString}.
     *
     * @return an empty {@link MaybeByteString}
     */
    public static MaybeByteString empty()
        {
        return new MaybeByteString(null);
        }

    // ----- data members -----------------------------------------------

    /**
     * The optional {@link ByteString} value.
     */
    private final ByteString m_value;
    }
