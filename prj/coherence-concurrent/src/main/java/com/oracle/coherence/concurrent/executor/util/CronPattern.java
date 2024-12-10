/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.util;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.tangosol.util.function.Remote.Predicate;

import java.time.Instant;
import java.time.ZonedDateTime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * <p>
 * A UNIX crontab-like pattern is a string split in five space separated parts. Each part is intended as:
 * </p>
 * <ol>
 * <li><strong>Minutes sub-pattern</strong>. During which minutes of the hour
 * should the task been launched? The values range is from 0 to 59.</li>
 * <li><strong>Hours sub-pattern</strong>. During which hours of the day should
 * the task been launched? The values range is from 0 to 23.</li>
 * <li><strong>Days of month sub-pattern</strong>. During which days of the
 * month should the task been launched? The values range is from 1 to 31. The
 * special value L can be used to recognize the last day of month.</li>
 * <li><strong>Months sub-pattern</strong>. During which months of the year
 * should the task been launched? The values range is from 1 (January) to 12
 * (December), otherwise this sub-pattern allows the aliases &quot;jan&quot;,
 * &quot;feb&quot;, &quot;mar&quot;, &quot;apr&quot;, &quot;may&quot;,
 * &quot;jun&quot;, &quot;jul&quot;, &quot;aug&quot;, &quot;sep&quot;,
 * &quot;oct&quot;, &quot;nov&quot; and &quot;dec&quot;.</li>
 * <li><strong>Days of week sub-pattern</strong>. During which days of the week
 * should the task been launched? The values range is from 0 (Sunday) to 6
 * (Saturday), otherwise this sub-pattern allows the aliases &quot;sun&quot;,
 * &quot;mon&quot;, &quot;tue&quot;, &quot;wed&quot;, &quot;thu&quot;,
 * &quot;fri&quot; and &quot;sat&quot;.</li>
 * </ol>
 * <p>
 * The star wildcard character is also admitted, indicating &quot;every minute
 * of the hour&quot;, &quot;every hour of the day&quot;, &quot;every day of the
 * month&quot;, &quot;every month of the year&quot; and &quot;every day of the
 * week&quot;, according to the sub-pattern in which it is used.
 * </p>
 * <p>
 * Once the scheduler is started, a task will be launched when the five parts in
 * its scheduling pattern will be true at the same time.
 * </p>
 * <p>
 * Some examples:
 * </p>
 * <p>
 * <strong>5 * * * *</strong><br>
 * This pattern causes a task to be launched once every hour, at the begin of
 * the fifth minute (00:05, 01:05, 02:05 etc.).
 * </p>
 * <p>
 * <strong>* * * * *</strong><br>
 * This pattern causes a task to be launched every minute.
 * </p>
 * <p>
 * <strong>* 12 * * Mon</strong><br>
 * This pattern causes a task to be launched every minute during the 12th hour
 * of Monday.
 * </p>
 * <p>
 * <strong>* 12 16 * Mon</strong><br>
 * This pattern causes a task to be launched every minute during the 12th hour
 * of Monday, 16th, but only if the day is the 16th of the month.
 * </p>
 * <p>
 * Every sub-pattern can contain two or more comma separated values.
 * </p>
 * <p>
 * <strong>59 11 * * 1,2,3,4,5</strong><br>
 * This pattern causes a task to be launched at 11:59AM on Monday, Tuesday,
 * Wednesday, Thursday and Friday.
 * </p>
 * <p>
 * Values intervals are admitted and defined using the minus character.
 * </p>
 * <p>
 * <strong>59 11 * * 1-5</strong><br>
 * This pattern is equivalent to the previous one.
 * </p>
 * <p>
 * The slash character can be used to identify step values within a range. It
 * can be used both in the form <em>*&#47;c</em> and <em>a-b/c</em>. The
 * subpattern is matched every <em>c</em> values of the range
 * <em>0,maxvalue</em> or <em>a-b</em>.
 * </p>
 * <p>
 * <strong>*&#47;5 * * * *</strong><br>
 * This pattern causes a task to be launched every 5 minutes (0:00, 0:05, 0:10,
 * 0:15 and so on).
 * </p>
 * <p>
 * <strong>3-18&#47;5 * * * *</strong><br>
 * This pattern causes a task to be launched every 5 minutes starting from the
 * third minute of the hour, up to the 18th (0:03, 0:08, 0:13, 0:18, 1:03, 1:08
 * and so on).
 * </p>
 * <p>
 * <strong>*&#47;15 9-17 * * *</strong><br>
 * This pattern causes a task to be launched every 15 minutes between the 9th
 * and 17th hour of the day (9:00, 9:15, 9:30, 9:45 and so on... note that the
 * last execution will be at 17:45).
 * </p>
 * <p>
 * All the fresh described syntax rules can be used together.
 * </p>
 * <p>
 * <strong>* 12 10-16&#47;2 * *</strong><br>
 * This pattern causes a task to be launched every minute during the 12th hour
 * of the day, but only if the day is the 10th, the 12th, the 14th or the 16th
 * of the month.
 * </p>
 * <p>
 * <strong>* 12 1-15,17,20-25 * *</strong><br>
 * This pattern causes a task to be launched every minute during the 12th hour
 * of the day, but the day of the month must be between the 1st and the 15th,
 * the 20th and the 25, or at least it must be the 17th.
 * </p>
 * <p>
 * Finally it lets you combine more scheduling patterns into one, with the
 * pipe character:
 * </p>
 * <p>
 * <strong>0 5 * * *|8 10 * * *|22 17 * * *</strong><br>
 * This pattern causes a task to be launched every day at 05:00, 10:08 and
 * 17:22.
 * </p>
 * <p>
 * Hourly
 * <strong>5 * * * *</strong><br>
 * This pattern causes a task to be launched at 5 minutes past every hour.
 * </p>
 * <p>
 * Daily
 * <strong>0 5 * * *</strong><br>
 * This pattern causes a task to be launched every day at 05:00.
 * </p>
 * <p>
 * Yearly
 * <strong>0 5 1 1 *</strong><br>
 * This pattern causes a task to be launched at 1st month, 1st day, 05:00 every year.
 * </p>
 * <p>
 * Every 5 minutes
 * <strong>*&#47;5 * * * *</strong><br>
 * This pattern causes a task to be launched every 5 minutes (asterisk followed by slash,
 * followed by the 5 minute interval).
 * </p>
 *
 * @author Adapted from the cron4j scheduler by Carlo Pelliccia
 * @author lh, bo
 * @since 21.12
 */
public class CronPattern
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Builds a CronPattern based on the provided input argument formatted as
     * a crontab-like string.
     *
     * @param sPattern  the pattern as a crontab-like string
     *
     * @throws IllegalArgumentException if the supplied string is not a valid pattern
     */
    public CronPattern(String sPattern) throws IllegalArgumentException
        {
        f_sPattern = sPattern;

        StringTokenizer st1 = new StringTokenizer(sPattern, "|");

        if (st1.countTokens() < 1)
            {
            throw new IllegalArgumentException("invalid pattern: \"" + sPattern + "\"");
            }
        while (st1.hasMoreTokens())
            {
            String          sLocalPattern = st1.nextToken();
            StringTokenizer st2           = new StringTokenizer(sLocalPattern, " \t");

            if (st2.countTokens() != 5)
                {
                throw new IllegalArgumentException("invalid pattern: \"" + sLocalPattern + "\"");
                }

            try
                {
                m_listMinuteMatchers.add(buildPredicate(st2.nextToken(), MINUTE_VALUE_PARSER));
                }
            catch (Exception e)
                {
                throw new IllegalArgumentException("invalid pattern \""
                                                   + sLocalPattern + "\". Error parsing minutes field: "
                                                   + e.getMessage() + ".");
                }

            try
                {
                m_listHourMatchers.add(buildPredicate(st2.nextToken(), HOUR_VALUE_PARSER));
                }
            catch (Exception e)
                {
                throw new IllegalArgumentException("invalid pattern \""
                                                   + sLocalPattern + "\". Error parsing hours field: "
                                                   + e.getMessage() + ".");
                }

            try
                {
                m_listDayOfMonthMatchers.add(buildPredicate(st2.nextToken(), DAY_OF_MONTH_VALUE_PARSER));
                }
            catch (Exception e)
                {
                throw new IllegalArgumentException("invalid pattern \""
                                                   + sLocalPattern
                                                   + "\". Error parsing days of month field: "
                                                   + e.getMessage() + ".");
                }

            try
                {
                m_listMonthMatchers.add(buildPredicate(st2.nextToken(), MONTH_VALUE_PARSER));
                }
            catch (Exception e)
                {
                throw new IllegalArgumentException("invalid pattern \""
                                                   + sLocalPattern + "\". Error parsing months field: "
                                                   + e.getMessage() + ".");
                }

            try
                {
                m_listDayOfWeekMatchers.add(buildPredicate(st2.nextToken(), DAY_OF_WEEK_VALUE_PARSER));
                }
            catch (Exception e)
                {
                throw new IllegalArgumentException("invalid pattern \""
                                                   + sLocalPattern
                                                   + "\". Error parsing days of week field: "
                                                   + e.getMessage() + ".");
                }

            m_cMatcherSize++;
            }
        }

    // ----- public methods -------------------------------------------------

    /**
     * Returns the next execution time in milliseconds from the crontab scheduling pattern, according to the
     * given time zone.
     *
     * @param timezone  a time zone
     * @param cMillis   the timestamp, as a UNIX-era millis value
     *
     * @return the next execute time
     */
    public long getNextExecuteTime(TimeZone timezone, long cMillis)
        {
        ZonedDateTime zdt         = ZonedDateTime.ofInstant(Instant.ofEpochMilli(cMillis), timezone.toZoneId());
        int           nMinute     = zdt.getMinute();
        int           nHour       = zdt.getHour();
        int           nDayOfMonth = zdt.getDayOfMonth();
        int           nMonth      = zdt.getMonth().getValue();
        int           nDayOfWeek  = zdt.getDayOfWeek().getValue();
        int           nYear       = zdt.getYear();

        for (int i = 0; i < m_cMatcherSize; i++)
            {
            Predicate<?> minuteMatcher = m_listMinuteMatchers.get(i);

            int nextMinute = getNextMinute(nMinute, minuteMatcher);
            if (nextMinute > nMinute)
                {
                return zdt.withMinute(nextMinute).toInstant().toEpochMilli();
                }

            Predicate<?> hourMatcher = m_listHourMatchers.get(i);
            int nextHour = getNextHour(nHour, hourMatcher);

            if (nextHour > nHour)
                {
                return zdt.withMinute(nextMinute).withHour(nextHour).toInstant().toEpochMilli();
                }

            Predicate<?> dayOfMonthMatcher = m_listDayOfMonthMatchers.get(i);
            Predicate<?> dayOfWeekMatcher  = m_listDayOfWeekMatchers.get(i);
            boolean      fDayOfMonthSet    = false;
            int          nNextDayOfMonth   = 0;

            if (dayOfMonthMatcher instanceof Predicates.AlwaysPredicate
                && dayOfWeekMatcher instanceof Predicates.AlwaysPredicate
                || dayOfMonthMatcher instanceof IntArrayPredicate)
                {
                nNextDayOfMonth = getNextDayOfMonth(zdt, dayOfMonthMatcher);
                if (nNextDayOfMonth > nDayOfMonth)
                    {
                    if (dayOfWeekMatcher instanceof Predicates.AlwaysPredicate)
                        {
                        return zdt.withMinute(nextMinute).withHour(nextHour).withDayOfMonth(nNextDayOfMonth)
                                .toInstant().toEpochMilli();
                        }
                    fDayOfMonthSet = true;
                    }
                }

            if (dayOfWeekMatcher instanceof IntArrayPredicate)
                {
                int nNextDayOfWeek = getNextDayOfWeek(nDayOfWeek, dayOfWeekMatcher);
                int cOffset        = nNextDayOfWeek > nDayOfWeek
                                     ? nNextDayOfWeek - nDayOfWeek
                                     : nNextDayOfWeek + 7 - nDayOfWeek;

                if (fDayOfMonthSet)
                    {
                    if (cOffset > (nNextDayOfMonth - nDayOfMonth))
                        {
                        cOffset = nNextDayOfMonth - nDayOfMonth;
                        }
                    return zdt.withMinute(nextMinute).withHour(nextHour).plusDays(cOffset).toInstant().toEpochMilli();
                    }
                else if (zdt.plusDays(cOffset).getDayOfMonth() > nDayOfMonth)
                    {
                    return zdt.withMinute(nextMinute).withHour(nextHour).plusDays(cOffset).toInstant().toEpochMilli();
                    }

                ZonedDateTime nextZdt = zdt.plusDays(cOffset);

                if (nNextDayOfMonth > 0)
                    {
                    int nextDay = nextZdt.getDayOfMonth();
                    if (nextDay < nNextDayOfMonth)
                        {
                        nNextDayOfMonth = nextDay;
                        }
                    }
                else
                    {
                    nNextDayOfMonth = nextZdt.getDayOfMonth();
                    }
                }

            Predicate<?> monthMatcher = m_listMonthMatchers.get(i);
            int          nNextMonth   = getNextMonth(nMonth, monthMatcher);

            if (nNextMonth > nMonth)
                {
                return zdt.withMinute(nextMinute).withHour(nextHour).withDayOfMonth(nNextDayOfMonth)
                        .withMonth(nNextMonth).toInstant().toEpochMilli();
                }

            int nextYear = nYear + 1;

            zdt = zdt.withMinute(nextMinute).withHour(nextHour)
                    .withDayOfMonth(nNextDayOfMonth).withMonth(nNextMonth).withYear(nextYear);
            }

        return zdt.toInstant().toEpochMilli();
        }

    /**
     * Returns the next execution time in milliseconds given timestamp (expressed as a UNIX-era millis value) using the
     * system default time zone.
     *
     * @param cMillis  the timestamp, as a UNIX-era millis value
     *
     * @return the next execution time in milliseconds
     */
    public long getNextExecuteTime(long cMillis)
        {
        return getNextExecuteTime(TimeZone.getDefault(), cMillis);
        }

    /**
     * Returns the next minute to execute the task.
     *
     * @param nMinute    the current minute
     * @param predicate  the predicate for getting the next minute
     *
     * @return the next minute to execute the task
     */
    public int getNextMinute(int nMinute, Predicate<?> predicate)
        {
        if (predicate instanceof Predicates.AlwaysPredicate)
            {
            return nMinute > 58 ? 0 : nMinute + 1;
            }

        return ((IntArrayPredicate) predicate).getNext(nMinute);
        }

    /**
     * Returns the next hour to execute the task.
     *
     * @param nHour      the current hour
     * @param predicate  the predicate for getting the next hour
     *
     * @return the next hour to execute the task
     */
    public int getNextHour(int nHour, Predicate<?> predicate)
        {
        if (predicate instanceof Predicates.AlwaysPredicate)
            {
            return nHour > 22 ? 0 : nHour + 1;
            }
        else
            {
            return ((IntArrayPredicate) predicate).getNext(nHour);
            }
        }

    /**
     * Returns the next dayOfMonth to execute the task.
     *
     * @param zdt         the current ZonedDateTime
     * @param predicate   the predicate for getting the next dayOfMonth
     *
     * @return the next dayOfMonth to execute the task
     */
    public int getNextDayOfMonth(ZonedDateTime zdt, Predicate<?> predicate)
        {
        if (predicate instanceof Predicates.AlwaysPredicate)
            {
            return zdt.toLocalDate().plusDays(1).getDayOfMonth();
            }
        else
            {
            return ((IntArrayPredicate) predicate).getNext(zdt.getDayOfMonth());
            }
        }

    /**
     * Returns the next dayOfWeek (0(Sunday) - 6(Saturday)) to execute the task.
     *
     * @param nDayOfWeek  the current dayOfWeek
     * @param predicate   the predicate for getting the next dayOfWeek
     *
     * @return the next dayOfWeek to execute the task
     */
    public int getNextDayOfWeek(int nDayOfWeek, Predicate<?> predicate)
        {
        if (predicate instanceof Predicates.AlwaysPredicate)
            {
            return nDayOfWeek > 5 ? 0 : nDayOfWeek + 1;
            }
        else
            {
            return ((IntArrayPredicate) predicate).getNext(nDayOfWeek);
            }
        }

    /**
     * Returns the next dayOfWeek to execute the task.
     *
     * @param nMonth     the current month
     * @param predicate  the predicate for getting the next month
     *
     * @return the next month to execute the task
     */
    public int getNextMonth(int nMonth, Predicate<?> predicate)
        {
        if (predicate instanceof Predicates.AlwaysPredicate)
            {
            return nMonth > 11 ? 1 : nMonth + 1;
            }
        else
            {
            return ((IntArrayPredicate) predicate).getNext(nMonth);
            }
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Returns the pattern as a string.
     *
     * @return the pattern as a string
     */
    @Override
    public String toString()
        {
        return f_sPattern;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * A Predicate utility builder.
     *
     * @param sPattern  the pattern part for the Predicate creation
     * @param parser    the parser used to parse the values
     *
     * @return the requested {@link Predicate}
     *
     * @throws Exception if the supplied pattern part is not valid
     */
    protected Predicate<?> buildPredicate(String sPattern, ValueParser parser)
            throws Exception
        {
        if (sPattern.length() == 1 && sPattern.charAt(0) == '*')
            {
            return Predicates.AlwaysPredicate.get();
            }

        List<Integer>   listValues = new ArrayList<>();
        StringTokenizer st         = new StringTokenizer(sPattern, ",");

        while (st.hasMoreTokens())
            {
            String sElement = st.nextToken();
            List<Integer> listLocal;
            try
                {
                listLocal = parseListElement(sElement, parser);
                }
            catch (Exception e)
                {
                throw new Exception("invalid field \"" + sPattern
                                    + "\", invalid element \"" + sElement + "\", "
                                    + e.getMessage());
                }
            for (Iterator<Integer> i = listLocal.iterator(); i.hasNext(); )
                {
                Integer value = i.next();
                if (!listValues.contains(value))
                    {
                    listValues.add(value);
                    }
                }
            }

        if (listValues.isEmpty())
            {
            throw new Exception("invalid field \"" + sPattern + "\"");
            }

        if (parser == DAY_OF_MONTH_VALUE_PARSER)
            {
            return new DayOfMonthPredicate(listValues);
            }
        else
            {
            return new IntArrayPredicate(listValues);
            }
        }

    /**
     * Parses an individual part/element of the crontab configuration.
     *
     * @param sElement the element string
     * @param parser   the parser used to parse the values
     *
     * @return a {@link List} of {@link Integer integers} representing the allowed values
     *
     * @throws Exception if the supplied pattern part is not valid
     */
    protected List<Integer> parseListElement(String sElement, ValueParser parser)
            throws Exception
        {
        StringTokenizer st    = new StringTokenizer(sElement, "/");
        int             cSize = st.countTokens();

        if (cSize < 1 || cSize > 2)
            {
            throw new Exception("syntax error");
            }

        List<Integer> listValues;
        try
            {
            listValues = parseRange(st.nextToken(), parser);
            }
        catch (Exception e)
            {
            throw new Exception("invalid range, " + e.getMessage());
            }

        if (cSize == 2)
            {
            String dStr = st.nextToken();
            int    nDiv;

            try
                {
                nDiv = Integer.parseInt(dStr);
                }
            catch (NumberFormatException e)
                {
                throw new Exception("invalid divisor \"" + dStr + "\"");
                }

            if (nDiv < 1)
                {
                throw new Exception("non positive divisor \"" + nDiv + "\"");
                }

            List<Integer> listValues2 = new ArrayList<>();

            for (int i = 0; i < listValues.size(); i += nDiv)
                {
                listValues2.add(listValues.get(i));
                }

            return listValues2;
            }
        else
            {
            return listValues;
            }
        }

    /**
     * Parses a range of values.
     *
     * @param sRange  the range string
     * @param parser  the parser used to parse the values
     *
     * @return a {@link List} of {@link Integer integers} representing the allowed values
     *
     * @throws Exception if the supplied pattern part is not valid
     */
    protected List<Integer> parseRange(String sRange, ValueParser parser)
            throws Exception
        {
        if (sRange.length() == 1 && sRange.charAt(0) == '*')
            {
            int cMin = parser.getMinValue();
            int cMax = parser.getMaxValue();
            List<Integer> values = new ArrayList<>();
            for (int i = cMin; i <= cMax; i++)
                {
                values.add(i);
                }
            return values;
            }

        StringTokenizer st    = new StringTokenizer(sRange, "-");
        int             cSize = st.countTokens();

        if (cSize < 1 || cSize > 2)
            {
            throw new Exception("syntax error");
            }

        String v1Str = st.nextToken();
        int    nV1;

        try
            {
            nV1 = parser.parse(v1Str);
            }
        catch (Exception e)
            {
            throw new Exception("invalid value \"" + v1Str + "\", "
                                + e.getMessage());
            }

        if (cSize == 1)
            {
            List<Integer> listValues = new ArrayList<>();
            listValues.add(nV1);
            return listValues;
            }
        else
            {
            String v2Str = st.nextToken();
            int    nV2;

            try
                {
                nV2 = parser.parse(v2Str);
                }
            catch (Exception e)
                {
                throw new Exception("invalid value \"" + v2Str + "\", "
                                    + e.getMessage());
                }

            List<Integer> listValues = new ArrayList<>();

            if (nV1 < nV2)
                {
                for (int i = nV1; i <= nV2; i++)
                    {
                    listValues.add(i);
                    }
                }
            else if (nV1 > nV2)
                {
                int cMin = parser.getMinValue();
                int cMax = parser.getMaxValue();
                for (int i = nV1; i <= cMax; i++)
                    {
                    listValues.add(i);
                    }
                for (int i = cMin; i <= nV2; i++)
                    {
                    listValues.add(i);
                    }
                }
            else
                {
                // v1 == v2
                listValues.add(nV1);
                }

            return listValues;
            }
        }

    /**
     * This utility method changes an alias to an int value.
     *
     * @param sValue     the value
     * @param asAliases  the aliases list
     * @param cOffset    the offset applied to the aliases list indices
     *
     * @return the parsed value
     *
     * @throws Exception if the expressed values doesn't match any alias
     */
    protected static int parseAlias(String sValue, String[] asAliases, int cOffset)
            throws Exception
        {
        for (int i = 0; i < asAliases.length; i++)
            {
            if (asAliases[i].equalsIgnoreCase(sValue))
                {
                return cOffset + i;
                }
            }
        throw new Exception("invalid alias \"" + sValue + "\"");
        }

    // ----- inner interface: ValueParser -----------------------------------

    /**
     * Definition for a value parser.
     */
    protected interface ValueParser
        {
        /**
         * Attempts to parse a value.
         *
         * @param sValue  the value
         *
         * @return the parsed value
         *
         * @throws Exception if the value can't be parsed
         */
        int parse(String sValue)
                throws Exception;

        /**
         * Returns the minimum value accepted by the parser.
         *
         * @return The minimum value accepted by the parser
         */
        int getMinValue();

        /**
         * Returns the maximum value accepted by the parser.
         *
         * @return The maximum value accepted by the parser
         */
        int getMaxValue();
        }

    // ----- inner class: SimpleValueParser ---------------------------------

    /**
     * A simple value parser.
     */
    private static class SimpleValueParser
            implements ValueParser
        {
        // ----- constructors -----------------------------------------------

        /**
         * Builds the value parser.
         *
         * @param minValue  the minimum allowed value
         * @param nMaxValue  the maximum allowed value
         */
        public SimpleValueParser(int minValue, int nMaxValue)
            {
            m_nMinValue = minValue;
            m_nMaxValue = nMaxValue;
            }

        // ----- ValueParser interface --------------------------------------

        @Override
        public int parse(String sValue) throws Exception
            {
            int i;
            try
                {
                i = Integer.parseInt(sValue);
                }
            catch (NumberFormatException e)
                {
                throw new Exception("invalid integer value");
                }
            if (i < m_nMinValue || i > m_nMaxValue)
                {
                throw new Exception("value out of range");
                }
            return i;
            }

        @Override
        public int getMinValue()
            {
            return m_nMinValue;
            }

        @Override
        public int getMaxValue()
            {
            return m_nMaxValue;
            }

        // ----- data members -----------------------------------------------

        /**
         * The minimum allowed value.
         */
        protected int m_nMinValue;

        /**
         * The maximum allowed value.
         */
        protected int m_nMaxValue;
        }

    // ----- inner class: MinuteValueParser ---------------------------------

    /**
     * The minutes value parser.
     */
    private static class MinuteValueParser
            extends SimpleValueParser
        {
        // ----- constructors -----------------------------------------------
        /**
         * Builds the value parser.
         */
        public MinuteValueParser()
            {
            super(0, 59);
            }
        }

    // ----- inner class: HourValueParser -----------------------------------

    /**
     * The hours value parser.
     */
    private static class HourValueParser
            extends SimpleValueParser
        {
        // ----- constructors -----------------------------------------------

        /**
         * Builds the value parser.
         */
        public HourValueParser()
            {
            super(0, 23);
            }
        }

    // ----- inner class: DayOfMonthValueParser -----------------------------

    /**
     * The days of month value parser.
     */
    private static class DayOfMonthValueParser
            extends SimpleValueParser
        {
        // ----- constructors -----------------------------------------------

        /**
         * Builds the value parser.
         */
        public DayOfMonthValueParser()
            {
            super(1, 31);
            }

        // ----- methods from SimpleValueParser -----------------------------

        /**
         * Added to support last-day-of-month.
         *
         * @param sValue  the value to be parsed
         *
         * @return the integer day of the month or 32 for last day of the month
         *
         * @throws Exception if the input value is invalid
         */
        public int parse(String sValue)
                throws Exception
            {
            if (sValue.equalsIgnoreCase("L"))
                {
                return 32;
                }
            else
                {
                return super.parse(sValue);
                }
            }
        }

    // ----- inner class: MonthValueParser ----------------------------------

    /**
     * The value parser for the months field.
     */
    private static class MonthValueParser
            extends SimpleValueParser
        {
        // ----- constructors -----------------------------------------------

        /**
         * Builds the months value parser.
         */
        public MonthValueParser()
            {
            super(1, 12);
            }

        // ----- SimpleValueParser methods ----------------------------------

        /**
         * Parses the given string value as a calendar month.
         *
         * @param sValue the value to parse
         *
         * @return the parsed result
         *
         * @throws Exception if the argument can't be parsed
         */
        public int parse(String sValue) throws Exception
            {
            try
                {
                // try as a simple value
                return super.parse(sValue);
                }
            catch (Exception e)
                {
                // try as an alias
                return parseAlias(sValue, ALIASES, 1);
                }
            }

        // ----- constants --------------------------------------------------

        /**
         * Months aliases.
         */
        private static final String[] ALIASES = {"jan", "feb", "mar", "apr", "may",
                                                 "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        }

    // ----- inner class: DayOfWeekValueParser ------------------------------

    /**
     * The value parser for the months field.
     */
    private static class DayOfWeekValueParser
        extends SimpleValueParser
        {
        // ----- constructors -----------------------------------------------

        /**
         * Builds the months value parser.
         */
        public DayOfWeekValueParser()
            {
            super(0, 7);
            }

        /**
         * Parses the given string value as a day of the week.
         *
         * @param sValue the value to parse
         *
         * @return the parsed result
         *
         * @throws Exception if the argument can't be parsed
         */
        public int parse(String sValue) throws Exception
            {
            try
                {
                // try as a simple value
                return super.parse(sValue) % 7;
                }
            catch (Exception e)
                {
                // try as an alias
                return parseAlias(sValue, ALIASES, 0);
                }
            }

        // ----- constants --------------------------------------------------

        /**
         * Days of week aliases.
         */
        private static final String[] ALIASES = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};
        }

    // ----- constants --------------------------------------------------

    /**
     * The parser for the minute values.
     */
    protected static final ValueParser MINUTE_VALUE_PARSER = new MinuteValueParser();

    /**
     * The parser for the hour values.
     */
    protected static final ValueParser HOUR_VALUE_PARSER = new HourValueParser();

    /**
     * The parser for the day of month values.
     */
    protected static final ValueParser DAY_OF_MONTH_VALUE_PARSER = new DayOfMonthValueParser();

    /**
     * The parser for the month values.
     */
    protected static final ValueParser MONTH_VALUE_PARSER = new MonthValueParser();

    /**
     * The parser for the day of week values.
     */
    protected static final ValueParser DAY_OF_WEEK_VALUE_PARSER = new DayOfWeekValueParser();

    // ----- data members -----------------------------------------------

    /**
     * The pattern as a string.
     */
    protected final String f_sPattern;

    /**
     * The Predicate list for the "minute" field.
     */
    protected List<Predicate<?>> m_listMinuteMatchers = new ArrayList<>();

    /**
     * The Predicate list for the "hour" field.
     */
    protected List<Predicate<?>> m_listHourMatchers = new ArrayList<>();

    /**
     * The Predicate list for the "day of month" field.
     */
    protected List<Predicate<?>> m_listDayOfMonthMatchers = new ArrayList<>();

    /**
     * The Predicate list for the "month" field.
     */
    protected List<Predicate<?>> m_listMonthMatchers = new ArrayList<>();

    /**
     * The Predicate list for the "day of week" field.
     */
    protected List<Predicate<?>> m_listDayOfWeekMatchers = new ArrayList<>();

    /**
     * How many predicate groups in this pattern?
     */
    protected int m_cMatcherSize = 0;
    }
