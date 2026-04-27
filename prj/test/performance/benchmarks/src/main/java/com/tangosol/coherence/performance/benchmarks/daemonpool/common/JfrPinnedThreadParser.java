/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool.common;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.time.Instant;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for {@code jdk.VirtualThreadPinned} events in dumped JFR recordings.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public final class JfrPinnedThreadParser
    {
    private JfrPinnedThreadParser()
        {
        }

    public static PinSummary summarize(Path pathJfr, Instant instantWindowStart, Instant instantWindowEnd)
            throws IOException
        {
        if (!Files.isRegularFile(pathJfr))
            {
            return new PinSummary(pathJfr, instantWindowStart, instantWindowEnd, 0, 0, 0, List.of(), List.of());
            }

        int          cTotal    = 0;
        int          cMeasured = 0;
        int          cOutside  = 0;
        List<String> listMeasuredSamples = new ArrayList<>();
        List<String> listOutsideSamples  = new ArrayList<>();

        try (RecordingFile recording = new RecordingFile(pathJfr))
            {
            while (recording.hasMoreEvents())
                {
                RecordedEvent event = recording.readEvent();
                if (!PINNED_EVENT.equals(event.getEventType().getName()))
                    {
                    continue;
                    }

                cTotal++;
                boolean fMeasured = isInMeasurementWindow(event.getStartTime(), instantWindowStart, instantWindowEnd);
                if (fMeasured)
                    {
                    cMeasured++;
                    addSample(listMeasuredSamples, event);
                    }
                else
                    {
                    cOutside++;
                    addSample(listOutsideSamples, event);
                    }
                }
            }

        return new PinSummary(pathJfr, instantWindowStart, instantWindowEnd,
                cTotal, cMeasured, cOutside, listMeasuredSamples, listOutsideSamples);
        }

    private static boolean isInMeasurementWindow(Instant instantEvent, Instant instantWindowStart,
            Instant instantWindowEnd)
        {
        if (instantWindowStart == null || instantWindowEnd == null)
            {
            return true;
            }

        return !instantEvent.isBefore(instantWindowStart) && !instantEvent.isAfter(instantWindowEnd);
        }

    private static void addSample(List<String> listSamples, RecordedEvent event)
        {
        if (listSamples.size() < MAX_SAMPLES)
            {
            listSamples.add(formatEvent(event));
            }
        }

    private static String formatEvent(RecordedEvent event)
        {
        StringBuilder sb = new StringBuilder();
        sb.append(PINNED_EVENT)
                .append(" startTime=").append(event.getStartTime())
                .append(", duration=").append(event.getDuration())
                .append(", blockingOperation=").append(readString(event, "blockingOperation"))
                .append(", pinnedReason=").append(readString(event, "pinnedReason"));

        RecordedThread thread = event.getThread();
        if (thread != null)
            {
            sb.append(", eventThread=").append(thread.getJavaName());
            }

        RecordedStackTrace stack = event.getStackTrace();
        if (stack != null)
            {
            int cFrames = 0;
            for (RecordedFrame frame : stack.getFrames())
                {
                if (cFrames++ >= MAX_STACK_FRAMES)
                    {
                    sb.append(System.lineSeparator()).append("  ...");
                    break;
                    }

                RecordedMethod method = frame.getMethod();
                if (method == null)
                    {
                    continue;
                    }
                sb.append(System.lineSeparator())
                        .append("  at ")
                        .append(method.getType().getName())
                        .append('.')
                        .append(method.getName());
                int nLine = frame.getLineNumber();
                if (nLine >= 0)
                    {
                    sb.append(':').append(nLine);
                    }
                }
            }

        return sb.toString();
        }

    private static String readString(RecordedEvent event, String sName)
        {
        try
            {
            return event.getString(sName);
            }
        catch (IllegalArgumentException e)
            {
            return "<unavailable>";
            }
        }

    public static class PinSummary
        {
        public PinSummary(Path pathJfr, Instant instantWindowStart, Instant instantWindowEnd,
                int cTotal, int cMeasured, int cOutside, List<String> listMeasuredSamples,
                List<String> listOutsideSamples)
            {
            f_pathJfr             = pathJfr;
            f_instantWindowStart  = instantWindowStart;
            f_instantWindowEnd    = instantWindowEnd;
            f_cTotal              = cTotal;
            f_cMeasured           = cMeasured;
            f_cOutside            = cOutside;
            f_listMeasuredSamples = List.copyOf(listMeasuredSamples);
            f_listOutsideSamples  = List.copyOf(listOutsideSamples);
            }

        public boolean hasMeasuredPins()
            {
            return f_cMeasured > 0;
            }

        public String formatSummary()
            {
            return "JFR pinned virtual-thread summary: file=" + f_pathJfr
                    + ", windowStart=" + f_instantWindowStart
                    + ", windowEnd=" + f_instantWindowEnd
                    + ", totalPins=" + f_cTotal
                    + ", measuredPins=" + f_cMeasured
                    + ", outsideWindowPins=" + f_cOutside;
            }

        public String formatFailure()
            {
            StringBuilder sb = new StringBuilder(formatSummary());
            for (String sSample : f_listMeasuredSamples)
                {
                sb.append(System.lineSeparator()).append(sSample);
                }
            return sb.toString();
            }

        public int getTotalPins()
            {
            return f_cTotal;
            }

        public int getMeasuredPins()
            {
            return f_cMeasured;
            }

        public int getOutsideWindowPins()
            {
            return f_cOutside;
            }

        public List<String> getOutsideSamples()
            {
            return f_listOutsideSamples;
            }

        private final Path         f_pathJfr;
        private final Instant      f_instantWindowStart;
        private final Instant      f_instantWindowEnd;
        private final int          f_cTotal;
        private final int          f_cMeasured;
        private final int          f_cOutside;
        private final List<String> f_listMeasuredSamples;
        private final List<String> f_listOutsideSamples;
        }

    private static final String PINNED_EVENT     = "jdk.VirtualThreadPinned";
    private static final int    MAX_SAMPLES      = 3;
    private static final int    MAX_STACK_FRAMES = 24;
    }
