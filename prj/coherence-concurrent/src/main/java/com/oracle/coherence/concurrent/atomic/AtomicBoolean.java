/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

/**
 * A {@code boolean} value that may be updated atomically.
 * <p>
 * An {@code AtomicBoolean} is used in applications such as atomically updated
 * flags, and cannot be used as a replacement for a {@link java.lang.Boolean}.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public interface AtomicBoolean
    {
    /**
     * Return non-blocking API for this atomic value.
     *
     * @return non-blocking API for this atomic value
     */
    AsyncAtomicBoolean async();

    /**
     * Returns the current value.
     *
     * @return the current value
     */
    boolean get();

    /**
     * Sets the value to {@code newValue}.
     *
     * @param fNewValue  the new value
     */
    void set(boolean fNewValue);

    /**
     * Atomically sets the value to {@code newValue} and returns the old value.
     *
     * @param fNewValue  the new value
     *
     * @return the previous value
     */
    boolean getAndSet(boolean fNewValue);

    /**
     * Atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue}.
     *
     * @param fExpectedValue  the expected value
     * @param fNewValue       the new value
     *
     * @return {@code true} if successful. False return indicates that
     *         the actual value was not equal to the expected value.
     */
    boolean compareAndSet(boolean fExpectedValue, boolean fNewValue);

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue}.
     *
     * @param fExpectedValue  the expected value
     * @param fNewValue       the new value
     *
     * @return the witness value, which will be the same as the expected value
     *         if successful
     */
    boolean compareAndExchange(boolean fExpectedValue, boolean fNewValue);

    // ----- inner class: Serializer ----------------------------------------

    /**
     * POF serializer implementation.
     */
    class Serializer
            implements PofSerializer<java.util.concurrent.atomic.AtomicBoolean>
        {
        // ----- PofSerializer interface ------------------------------------

        @Override
        public void serialize(PofWriter out, java.util.concurrent.atomic.AtomicBoolean value)
                throws IOException
            {
            out.writeBoolean(0, value.get());
            out.writeRemainder(null);
            }

        @Override
        public java.util.concurrent.atomic.AtomicBoolean deserialize(PofReader in)
                throws IOException
            {
            boolean fValue = in.readBoolean(0);

            in.readRemainder();
            return new java.util.concurrent.atomic.AtomicBoolean(fValue);
            }
        }
    }
