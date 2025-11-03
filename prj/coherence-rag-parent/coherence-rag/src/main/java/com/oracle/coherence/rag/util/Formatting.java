/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.util;

import java.time.Duration;
import java.time.Instant;

/**
 * Utility class for formatting time-related data in a human-readable format.
 * <p/>
 * This class provides static methods for converting time objects such as
 * {@link Instant} and {@link Duration} into formatted strings suitable for
 * logging, display, or debugging purposes.
 * <p/>
 * The formatting methods handle null values gracefully and produce
 * consistent, readable output across different time ranges.
 * 
 * @author Aleks Seovic  2025.06.28
 * @since 25.09
 */
public class Formatting
    {
    /**
     * Formats an Instant object into a string representation.
     * <p/>
     * This method converts an Instant to its ISO-8601 string representation.
     * If the instant is null, an empty string is returned.
     * <p/>
     * Example outputs:
     * <ul>
     *   <li>2023-12-01T10:30:45.123Z</li>
     *   <li>2023-12-01T10:30:45Z</li>
     *   <li>"" (for null input)</li>
     * </ul>
     * 
     * @param instant the Instant to format, may be null
     * 
     * @return the formatted string representation, or empty string if instant is null
     */
    public static String formatInstant(Instant instant)
        {
        return instant == null ? "" : instant.toString();
        }

    /**
     * Formats a Duration object into a human-readable string representation.
     * <p/>
     * This method converts a Duration into a compact, readable format that
     * shows the most significant time units. The format adapts based on the
     * duration length:
     * <p/>
     * <ul>
     *   <li>For durations ≥ 1 day: "XdXhXmX.Xs" (e.g., "2d5h30m45.123s")</li>
     *   <li>For durations ≥ 1 hour: "XhXmX.Xs" (e.g., "5h30m45.123s")</li>
     *   <li>For durations ≥ 1 minute: "XmX.Xs" (e.g., "30m45.123s")</li>
     *   <li>For durations < 1 minute: "X.Xs" (e.g., "45.123s")</li>
     * </ul>
     * <p/>
     * Example outputs:
     * <ul>
     *   <li>"2d5h30m45.123s" for 2 days, 5 hours, 30 minutes, 45 seconds, 123 milliseconds</li>
     *   <li>"5h30m45.123s" for 5 hours, 30 minutes, 45 seconds, 123 milliseconds</li>
     *   <li>"30m45.123s" for 30 minutes, 45 seconds, 123 milliseconds</li>
     *   <li>"45.123s" for 45 seconds, 123 milliseconds</li>
     * </ul>
     * 
     * @param d the Duration to format
     * 
     * @return a human-readable string representation of the duration
     * 
     * @throws IllegalArgumentException if the duration is null
     */
    public static String formatDuration(Duration d)
        {
        long days    = d.toDays();
        long hours   = d.toHours();
        long minutes = d.toMinutes();

        return days > 0
               ? String.format("%dd%dh%dm%d.%ds", days, d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart(), d.toMillisPart())
               : hours > 0
                 ? String.format("%dh%dm%d.%ds", hours, d.toMinutesPart(), d.toSecondsPart(), d.toMillisPart())
                 : minutes > 0
                   ? String.format("%dm%d.%ds", minutes, d.toSecondsPart(), d.toMillisPart())
                   : String.format("%d.%ds", d.toSeconds(), d.toMillisPart());
        }

    }
