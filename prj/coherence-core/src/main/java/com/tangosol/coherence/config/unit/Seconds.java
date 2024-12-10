/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.unit;

import com.oracle.coherence.common.util.Duration;

/**
 * {@link Seconds} is a specialized {@link Duration} whose default constructor 
 * assumes that the specified units of time (when the unit magnitude is not 
 * specified) are seconds.
 * <p>
 * Note:  This class is provided to simplify and support backwards compatibility 
 * during the injection of second-based units of time into appropriate classes.  
 * This class is <strong>not designed</strong> for general purpose representation 
 * of units of time.  For general purpose units of time, please use the {@link Duration} class.
 *
 * @author bo  2012.01.18
 * @since Coherence 12.1.2
 */
public class Seconds
        extends Duration
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Seconds} based on another {@link Duration}.
     *
     * @param d  the {@link Duration}
     */
    public Seconds(Duration d)
        {
        super(d);
        }

    /**
     * Constructs a {@link Seconds} representing a {@link Duration} measured in seconds.
     *
     * @param c  the number of seconds
     */
    public Seconds(int c)
        {
        super(c, Magnitude.SECOND);
        }

    /**
     * Constructs a {@link Seconds} representing a {@link Duration} measured in seconds.
     *
     * @param s  the number of seconds or other duration when magnitudes are specified
     *
     * @see Duration#Duration(String, Magnitude)
     */
    public Seconds(String s)
        {
        super(s, Magnitude.SECOND);
        }

    // ----- Seconds methods ------------------------------------------------

    /**
     * Obtain the {@link Duration} in units of seconds.
     *
     * @return the number of seconds in the {@link Duration}
     */
    public long get()
        {
        return as(Magnitude.SECOND);
        }
    }
