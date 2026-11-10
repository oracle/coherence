/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement.persistence;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

/**
 * Tests for snapshot date macros on {@link AbstractSnapshotStatement}.
 */
public class AbstractSnapshotStatementTest
    {
    /**
     * %w and %M expand to the weekday and month name.
     */
    @Test
    public void testDateMacrosReplaceWeekdayAndMonth()
        {
        Calendar calendar  = Calendar.getInstance();
        String   sExpected = "pre-" + weekday(calendar) + "-" + month(calendar) + "-post";

        assertThat(AbstractSnapshotStatement.replaceDateMacros("pre-%w-%M-post"), is(sExpected));
        }

    /**
     * Concurrent snapshot commands must not share <tt>SimpleDateFormat</tt> state.
     */
    @Test
    public void testDateMacrosAreThreadSafe()
            throws Exception
        {
        Calendar january  = calendar(2011, Calendar.JANUARY);
        Calendar july     = calendar(2011, Calendar.JULY);
        String   sJanuary = "snap-" + weekday(january) + "-" + month(january);
        String   sJuly    = "snap-" + weekday(july) + "-" + month(july);
        int             cThreads = 16;
        ExecutorService pool     = Executors.newFixedThreadPool(cThreads);
        CountDownLatch  start    = new CountDownLatch(1);
        Future<?>[]     aFuture  = new Future<?>[cThreads];

        try
            {
            for (int i = 0; i < cThreads; i++)
                {
                Calendar calendar  = (Calendar) ((i % 2 == 0) ? january : july).clone();
                String   sExpected = (i % 2 == 0) ? sJanuary : sJuly;

                aFuture[i] = pool.submit(() ->
                    {
                    start.await();
                    for (int n = 0; n < 400; n++)
                        {
                        String sName = AbstractSnapshotStatement.replaceDateMacros("snap-%w-%M", calendar);
                        if (!sExpected.equals(sName))
                            {
                            throw new AssertionError("formatted " + sName);
                            }
                        }
                    return null;
                    });
                }
            start.countDown();
            for (Future<?> future : aFuture)
                {
                future.get();
                }
            }
        finally
            {
            pool.shutdownNow();
            }
        }

    /**
     * Calendar for a day in the given month.
     *
     * @param nYear   the year
     * @param nMonth  the {@link Calendar} month
     *
     * @return a calendar on the 15th of that month
     */
    private static Calendar calendar(int nYear, int nMonth)
        {
        Calendar calendar = Calendar.getInstance();

        calendar.set(nYear, nMonth, 15, 12, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
        }

    /**
     * Weekday name for <tt>calendar</tt>.
     *
     * @param calendar  the calendar to format
     *
     * @return the value <tt>%w</tt> expands to
     */
    private static String weekday(Calendar calendar)
        {
        synchronized (AbstractSnapshotStatement.WEEKDAY_NAME)
            {
            return AbstractSnapshotStatement.WEEKDAY_NAME.format(calendar.getTime());
            }
        }

    /**
     * Month name for <tt>calendar</tt>.
     *
     * @param calendar  the calendar to format
     *
     * @return the value <tt>%M</tt> expands to
     */
    private static String month(Calendar calendar)
        {
        synchronized (AbstractSnapshotStatement.MONTH_NAME)
            {
            return AbstractSnapshotStatement.MONTH_NAME.format(calendar.getTime());
            }
        }
    }
