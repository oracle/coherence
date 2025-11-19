/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.util;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Utility class for measuring elapsed time between events.
 * <p/>
 * This class provides a simple and efficient way to measure execution time
 * for operations in the Coherence RAG framework. It supports both manual start/stop
 * timing and automatic timing using the current system time.
 * <p/>
 * The Timer class is designed to be serializable using Coherence POF (Portable Object Format)
 * for distributed timing measurements across cluster nodes.
 * <p/>
 * Usage examples:
 * <pre>
 * Timer timer = new Timer().start();
 * // ... perform operation ...
 * Duration elapsed = timer.stop().duration();
 * 
 * // Or with custom timestamps
 * Timer timer = new Timer().start(Instant.now()).stop(Instant.now().plusSeconds(5));
 * </pre>
 * 
 * @author Aleks Seovic  2025.06.28
 * @since 25.09
 */
public class Timer extends AbstractEvolvable implements PortableObject
    {
    /**
     * The implementation version for this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    /**
     * The timestamp when the timer was started.
     * <p/>
     * This field records the exact moment when timing began. It is null
     * if the timer has not been started yet.
     */
    private Instant start;

    /**
     * The timestamp when the timer was stopped.
     * <p/>
     * This field records the exact moment when timing ended. It is null
     * if the timer has not been stopped yet or if it's currently running.
     */
    private Instant stop;

    /**
     * Checks if the timer has been started but not yet stopped.
     * <p/>
     * A timer is considered started if it has a start time but no stop time,
     * indicating that timing is currently in progress.
     * 
     * @return true if the timer is currently running, false otherwise
     */
    public boolean isStarted()
        {
        return start != null && stop == null;
        }

    /**
     * Checks if the timer has been started and stopped.
     * <p/>
     * A timer is considered stopped if it has both a start time and a stop time,
     * indicating that timing has been completed.
     * 
     * @return true if the timer has been stopped, false otherwise
     */
    public boolean isStopped()
        {
        return start != null && stop != null;
        }

    /**
     * Gets the timestamp when the timer was started.
     * 
     * @return the start timestamp, or null if the timer has not been started
     */
    public Instant startTime()
        {
        return start;
        }

    /**
     * Starts the timer using the current system time.
     * <p/>
     * This method sets the start time to the current instant and clears
     * any previous stop time, effectively resetting the timer.
     * 
     * @return this Timer instance for method chaining
     */
    public Timer start()
        {
        return start(Instant.now());
        }

    /**
     * Starts the timer using the specified timestamp.
     * <p/>
     * This method sets the start time to the provided instant and clears
     * any previous stop time. This is useful for timing operations that have
     * already begun or for testing purposes.
     * 
     * @param instant the timestamp to use as the start time
     * 
     * @return this Timer instance for method chaining
     * 
     * @throws IllegalArgumentException if the instant is null
     */
    public Timer start(Instant instant)
        {
        this.stop  = null;
        this.start = instant;
        return this;
        }

    /**
     * Gets the timestamp when the timer was stopped.
     * 
     * @return the stop timestamp, or null if the timer has not been stopped
     */
    public Instant stopTime()
        {
        return stop;
        }

    /**
     * Stops the timer using the current system time.
     * <p/>
     * This method sets the stop time to the current instant, completing
     * the timing measurement.
     * 
     * @return this Timer instance for method chaining
     */
    public Timer stop()
        {
        return stop(Instant.now());
        }

    /**
     * Stops the timer using the specified timestamp.
     * <p/>
     * This method sets the stop time to the provided instant. This is useful
     * for timing operations that have already ended or for testing purposes.
     * 
     * @param instant the timestamp to use as the stop time
     * 
     * @return this Timer instance for method chaining
     * 
     * @throws IllegalArgumentException if the instant is null
     */
    public Timer stop(Instant instant)
        {
        this.stop = instant;
        return this;
        }

    /**
     * Calculates the duration between start and stop times.
     * <p/>
     * This method returns the elapsed time as a Duration object. The behavior
     * depends on the timer state:
     * <ul>
     *   <li>If not started: returns Duration.ZERO</li>
     *   <li>If started but not stopped: returns duration from start to current time</li>
     *   <li>If started and stopped: returns duration from start to stop time</li>
     * </ul>
     * 
     * @return the elapsed duration, or Duration.ZERO if not started
     */
    public Duration duration()
        {
        return start == null
               ? Duration.ZERO
               : stop == null
                 ? Duration.between(start, Instant.now())
                 : Duration.between(start, stop);
        }

    /**
     * Compares this Timer with another object for equality.
     * 
     * Two Timer instances are considered equal if they have the same
     * start and stop timestamps.
     * 
     * @param o the object to compare with this Timer
     * 
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        Timer timer = (Timer) o;
        return Objects.equals(start, timer.start) && Objects.equals(stop, timer.stop);
        }

    /**
     * Returns a hash code for this Timer.
     * 
     * The hash code is based on the start and stop timestamp values.
     * 
     * @return a hash code value for this Timer
     */
    @Override
    public int hashCode()
        {
        return Objects.hash(start, stop);
        }

    /**
     * Returns a string representation of this Timer.
     * 
     * The string includes the start time, stop time, and calculated duration
     * in a readable format.
     * 
     * @return a string representation of this Timer
     */
    @Override
    public String toString()
        {
        return "Timer[" +
               "start=" + start +
               ", stop=" + stop +
               ", duration=" + duration() +
               ']';
        }

    // PortableObject implementation

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    @Override
    public void readExternal(PofReader pofReader) throws IOException
        {
        start = (Instant) pofReader.readObject(0);
        stop = (Instant) pofReader.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter pofWriter) throws IOException
        {
        pofWriter.writeObject(0, start);
        pofWriter.writeObject(1, stop);
        }
    }
