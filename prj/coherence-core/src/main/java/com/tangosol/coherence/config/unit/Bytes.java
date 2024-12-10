/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.unit;

import com.oracle.coherence.common.util.MemorySize;

/**
 * {@link Bytes} is a specialized {@link MemorySize} whose default constructor
 * assumes that the specified units (when a magnitude is not specified) are always bytes.
 * <p>
 * Note:  This class is provided to simplify and support backwards compatibility
 * during the injection of {@link MemorySize}s into appropriate classes.  This
 * class is <strong>not designed</strong> for general purpose representation of
 * capacity.  For general purpose representations of memory capacity, please use the
 * {@link MemorySize} class.
 *
 * @author bo  2012.01.19
 * @since Coherence 12.1.2
 */
public class Bytes
        extends MemorySize
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Bytes} representing a {@link MemorySize} measured in bytes.
     *
     * @param c  the number of bytes
     */
    public Bytes(int c)
        {
        super(c, Magnitude.BYTES);
        }

    /**
     * Constructs a {@link Bytes} based on another {@link MemorySize}.
     *
     * @param m  the {@link MemorySize}
     */
    public Bytes(MemorySize m)
        {
        super(m);
        }

    /**
     * Constructs a {@link Bytes} representing a {@link MemorySize} measured in bytes.
     *
     * @param s  the number of bytes or other {@link MemorySize} when magnitudes are specified
     *
     * @see MemorySize#MemorySize(String, Magnitude)
     */
    public Bytes(String s)
        {
        super(s, Magnitude.BYTES);
        }

    // ----- Millis methods -------------------------------------------------

    /**
     * Obtain the {@link MemorySize} in bytes.
     *
     * @return the number of bytes in the {@link MemorySize}
     */
    public long get()
        {
        return getByteCount();
        }
    }
