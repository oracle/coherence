/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.unit;

import com.oracle.coherence.common.util.Duration;

/**
 * {@link Millis} is a specialized {@link Duration} whose default constructor 
 * assumes that the specified units of time (when the unit magnitude is not 
 * specified) are milliseconds.
 * <p>
 * Note:  This class is provided to simplify and support backwards compatibility 
 * during the injection of millisecond-based units of time into appropriate classes.  
 * This class is <strong>not designed</strong> for general purpose representation 
 * of units of time.  For general purpose units of time, please use the 
 * {@link Duration} class.
 *
 * @author bo  2012.01.18
 * @since Coherence 12.1.2
 */
public class Millis
        extends Duration
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Millis} based on another {@link Duration}.
     *
     * @param d  the {@link Duration}
     */
    public Millis(Duration d)
        {
        super(d);
        }

    /**
     * Constructs a {@link Millis} representing a {@link Duration} measured in milliseconds.
     *
     * @param s  the number of milliseconds or other duration when magnitudes are specified
     *
     * @see Duration#Duration(String, Magnitude)
     */
    public Millis(String s)
        {
        super(s, Magnitude.MILLI);
        }

    // ----- Millis methods -------------------------------------------------

    /**
     * Obtain the {@link Duration} in units of milliseconds.
     *
     * @return the number of milliseconds in the {@link Duration}
     */
    public long get()
        {
        return as(Magnitude.MILLI);
        }
    }
