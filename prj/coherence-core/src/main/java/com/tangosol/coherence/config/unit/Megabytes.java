/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.unit;

import com.oracle.coherence.common.util.MemorySize;

/**
 * {@link Megabytes} is a specialized {@link MemorySize} whose default constructor
 * assumes that the specified units (when a they are not specified) are
 * measured in megabytes.
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
public class Megabytes
        extends MemorySize
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Megabytes} representing a {@link MemorySize} measured in megabytes.
     *
     * @param c  the number of megabytes
     */
    public Megabytes(int c)
        {
        super(c, Magnitude.MB);
        }

    /**
     * Constructs a {@link Megabytes} based on another {@link MemorySize}.
     *
     * @param m  the {@link MemorySize}
     */
    public Megabytes(MemorySize m)
        {
        super(m);
        }

    /**
     * Constructs a {@link Megabytes} representing a {@link MemorySize} measured in megabytes.
     *
     * @param s  the number of megabytes or other {@link MemorySize} when magnitudes are specified
     *
     * @see MemorySize#MemorySize(String, Magnitude)
     */
    public Megabytes(String s)
        {
        super(s, Magnitude.MB);
        }

    // ----- Megabytes methods ----------------------------------------------

    /**
     * Obtain the {@link MemorySize} in megabytes.
     *
     * @return the number of megabytes in the {@link MemorySize}
     */
    public long get()
        {
        return (long) as(Magnitude.MB);
        }
    }
