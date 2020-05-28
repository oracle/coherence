/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.extractor;

import com.tangosol.coherence.config.Config;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;
import com.tangosol.util.CopyOnWriteMap;
import com.tangosol.util.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A filter that evaluates if an input class is allowed to be used via reflection.
 * <p>
 * {@link #INSTANCE An ReflectionAllowedFilter INSTANCE} is configured using system property
 * {@link #REFLECT_FILTER_PROPERTY}. If the property is not set or is invalid, the
 * default pattern, {@link #DEFAULT_FILTER_LIST}, is used to initialize the filter.
 * The system property string value consists of a set of one or more patterns
 * that enable specifying explicit white list and/or black list of class names.
 * <p>
 * The input of the filter is a {@link Class}. The input class name is checked for a match against
 * each of the filter's pattern proceeding from left to right to determine if reflection is allowed against instances of
 * the class. Processing over the pattern set completes at the first pattern that matches the input class name.
 * If the pattern begins with the negation character {@code !}, the class is rejected.
 *
 * @author jf  2020.05.15
 * @since 3.7.1
 *
 * @see com.tangosol.util.ClassHelper#REFLECT_FILTER_PROPERTY
 */
public class ReflectionAllowedFilter
    implements Filter<Class<?>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a filter that evaluates an input class name against the provided set of {@code sPatterns}.
     *
     * @param sPatterns  a string containing one or more patterns delimited by {@link #REFLECT_FILTER_SEPARATOR}
     *
     * @throws IllegalArgumentException       if the pattern is malformed or empty
     *
     * @see com.tangosol.util.ClassHelper#REFLECT_FILTER_PROPERTY
     */
    private ReflectionAllowedFilter(String sPatterns)
        {
        Objects.requireNonNull(sPatterns, "sPatterns");
        validate(sPatterns);

        f_sPatterns = sPatterns;
        f_fDisabled = REFLECT_ALLOW_ALL.equals(f_sPatterns);

        String[] asPatterns = f_sPatterns.split(REFLECT_FILTER_SEPARATOR);

        f_listFilter = new ArrayList<>(asPatterns.length);
        for (String sPattern : asPatterns)
            {
            if (sPattern != null && sPattern.length() > 0)
                {
                f_listFilter.add(createPatternFilter(sPattern));
                }
            }
        if (f_listFilter.isEmpty())
            {
            throw new IllegalArgumentException("parameter sPatterns must contain at least one valid pattern");
            }
        }

    // ----- Filter interface -----------------------------------------------

    /**
     * Return {@code true} if {@code clz} can be accessed via reflection.
     *
     * @param clz  class name that is compared agasint filter's patterns
     *
     * @return {@code true} if reflection is allowed for {@code clz}
     */
    @Override
    public boolean evaluate(Class<?> clz)
        {
        if (f_fDisabled)
            {
            return true;
            }
        
        Status status = f_mapClassStatus.get(clz);
        if (status == null)
            {
            // find first filter that allows or rejects the class
            for (AbstractReflectionAllowedFilter filter : f_listFilter)
                {
                status = filter.process(clz);
                if (status != Status.UNDECIDED)
                    {
                    break;
                    }
                }
            f_mapClassStatus.put(clz, status);
            }

        return status != Status.REJECTED;
        }

    // ----- static helpers -------------------------------------------------

    /**
     * Returns a ReflectionAllowedFilter based on the provided pattern {@code sPattern}.
     * <p>
     * If an error occurs during parsing of the pattern this method will log the issue and revert
     * to the DEFAULT_FILTER_LIST.
     *
     * @param sPattern  the pattern string to parse; not null
     *
     * @return a {@link ReflectionAllowedFilter}
     *
     * @see com.tangosol.util.ClassHelper#REFLECT_FILTER_PROPERTY
     */
    public static ReflectionAllowedFilter ensureSafeFilter(String sPattern)
        {
        try
            {
            return ensureFilter(sPattern);
            }
        catch (Throwable t)
            {
            CacheFactory.log("Detected invalid pattern " + t.getMessage() +
                " for system property \"" + REFLECT_FILTER_PROPERTY +
                "\"=\"" + sPattern + "\"; using default pattern: \"" + DEFAULT_FILTER_LIST +
                "\"", Base.LOG_WARN);
            return ensureFilter(DEFAULT_FILTER_LIST);
            }
        }

    /**
     * Return a filter that allows reflection on a class based on matching name against {@code patterns}.
     * <p>
     * The resulting filter tries to match the class, if any.
     * The first pattern that matches, working from left to right, determines
     * the {@link  ReflectionAllowedFilter.Status#ALLOWED Status.ALLOWED}
     * or {@link  ReflectionAllowedFilter.Status#REJECTED Status.REJECTED} result.
     * If no pattern matches the class, reflection will be allowed.
     *
     * @param sPattern  the pattern string to parse; not null
     *
     * @return a filter to validate whether reflection access is allowed for an input class
     *
     * @throws IllegalArgumentException if the pattern string is illegal or
     *         malformed and cannot be parsed.
     *
     * @see com.tangosol.util.ClassHelper#REFLECT_FILTER_PROPERTY
     */
    public static ReflectionAllowedFilter ensureFilter(String sPattern)
        {
        return new ReflectionAllowedFilter(sPattern);
        }

    // ----- ReflectionAllowedFilter methods --------------------------------

    /**
     * Validate only legal characters in {@code sPatterns}.
     *
     * @param sPatterns  set of patterns to check for invalid characters
     *
     * @throws IllegalArgumentException if invalid character identified in {@code sPatterns}
     */
    private void validate(String sPatterns)
        {
        Matcher match    = PATTERN_INVALID_CHARS.matcher(sPatterns);
        boolean fInvalid = match.find();

        if (fInvalid == true)
            {
            throw new IllegalArgumentException("containing invalid character \"" +
                sPatterns.charAt(match.start()) + "\" at offset " + match.start());
            }
        }

    /**
     * Return a Filter for a single {@code sPattern}.
     *
     * @param sPattern  a single pattern
     *
     * @return {@link AbstractReflectionAllowedFilter} for {@code sPattern}
     *
     * @throws {@link IllegalArgumentException} if invalid pattern
     */
    protected AbstractReflectionAllowedFilter createPatternFilter(String sPattern)
        {
        int     cPattern  = sPattern.length();
        boolean fReject   = sPattern.charAt(0) == '!';
        int     ofPattern = fReject ? 1 : 0;

        if (sPattern.endsWith("*"))
            {
            // wildcard cases
            if (sPattern.endsWith(".*"))
                {
                // pattern is a package name with a wildcard
                final String sPkg = sPattern.substring(ofPattern, cPattern - 2);
                if (sPkg.isEmpty())
                    {
                    throw new IllegalArgumentException("package missing in: \"" + f_sPatterns + "\"");
                    }

                // filter that rejects or allows if class is in package, otherwise undecided
                return new ClassInPackageFilter(sPkg, fReject ? Status.REJECTED : Status.ALLOWED);
                }
            else if (sPattern.endsWith(".**"))
                {
                // pattern is a package prefix with a double wildcard
                // matches all classes in package and its subpackages
                final String sPkgs = sPattern.substring(ofPattern, cPattern - 2);
                if (sPkgs.length() < 2)
                    {
                    throw new IllegalArgumentException("package missing in: \"" + f_sPatterns + "\"");
                    }

                // filter that rejects or allows if the class starts with the pattern, otherwise undecided
                return new ClassStartsWithFilter(sPkgs, fReject ? Status.REJECTED : Status.ALLOWED);
                }
            else
                {
                // pattern is a classname (possibly empty) with a trailing wildcard
                final String sClassName = sPattern.substring(ofPattern, cPattern - 1);

                return new ClassStartsWithFilter(sClassName, fReject ? Status.REJECTED : Status.ALLOWED);
                }
            }
        else
            {
            // non wildcard case requires exact match
            final String sName = sPattern.substring(ofPattern);
            if (sName.isEmpty())
                {
                throw new IllegalArgumentException("class or package missing in: \"" + f_sPatterns + "\"");
                }

            // filter that rejects or allows if the class equals the pattern classname, otherwise undecided
            return new ClassFilter(sName, fReject ? Status.REJECTED : Status.ALLOWED);
            }
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Returns the pattern used to create this filter.
     *
     * @return the pattern used to create this filter
     */
    @Override
    public String toString()
        {
        return f_sPatterns;
        }

    // ----- inner class: Status --------------------------------------------
    
    /**
     * The status of a check if reflection is allowed on a class.
     */
    enum Status
        {
        /**
         * The status is allowed.
         */
        ALLOWED,

        /**
         * The status is rejected.
         */
        REJECTED,

        /**
         * Not matched with a pattern.
         */
        UNDECIDED
        }

    // ----- inner class: AbstractReflectionAllowedFilter -------------------

    /**
     * Base class for filter for one pattern.
     */
    protected static abstract class AbstractReflectionAllowedFilter
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct base class for a class filter for a single pattern.
         * Encapsulates whether filter allows or rejects an input class.
         *
         * @param statusPatMatch  {@link Status} to return when {@link #isMatch(Class) is true
         */
        public AbstractReflectionAllowedFilter(Status statusPatMatch)
            {
            f_status = statusPatMatch;
            }

        // ----- AbstractReflectionAllowedFilter methods --------------------

        /**
         * Return {@link #f_status status for match} when {@link #isMatch(Class) input class name matches} filter pattern;
         * otherwise, return {@link Status#UNDECIDED}.
         *
         * @param clz  check class name against filter's pattern
         *
         * @return {@link Status} for filter input {@code clz}
         */
        public Status process(Class<?> clz)
            {
            return isMatch(clz) ? f_status : Status.UNDECIDED;
            }

        /**
         * Return {@code true} if {@code clz} name matches this filter's pattern.
         *
         * @param clz  compare filter string pattern with class name
         *
         * @return true iff there is a pattern match with class name
         */
        protected abstract boolean isMatch(Class<?> clz);

        // ----- data members  ----------------------------------------------

        /**
         * {@link Status} to return when there is a match between class name and this filter's pattern.
         */
        final protected Status f_status;
        }

    // ----- inner class: ClassFilter ---------------------------------------

    /**
     * Filter matches input class name only if input class name is exact match for pattern.
     */
    protected static class ClassFilter
        extends AbstractReflectionAllowedFilter
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a Filter that matches if class name equals {@code sClassName}.
         *
         * @param sClassName      check if input class name equals this parameter
         * @param statusPatMatch  {@link Status} to return if match
         */
        public ClassFilter(String sClassName, Status statusPatMatch)
            {
            super(statusPatMatch);
            f_sClassName = sClassName;
            }

        // ----- AbstractReflectionAllowedFilter methods --------------------

        @Override
        protected boolean isMatch(Class<?> clz)
            {
            return clz.getName().equals(f_sClassName);
            }

        // ----- data members  ----------------------------------------------

        /**
         * The class name from the pattern to match.
         */
        final private String f_sClassName;
        }

    // ----- inner class: ClassStartsWithFilter -----------------------------

    /**
     * Filter matches input class name only if input class name starts with pattern prefix.
     */
    protected static class ClassStartsWithFilter
        extends AbstractReflectionAllowedFilter
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a Filter that matches if class name starts with {@code sPrefix}.
         *
         * @param sPrefix         check if classname starts with this prefix
         * @param statusPatMatch  {@link Status} to return if match
         */
        public ClassStartsWithFilter(String sPrefix, Status statusPatMatch)
            {
            super(statusPatMatch);
            f_sPrefix = sPrefix;
            }

        // ----- AbstractReflectionFilter methods ---------------------------

        @Override
        protected boolean isMatch(Class<?> clz)
            {
            return clz.getName().startsWith(f_sPrefix);
            }

        // ----- data members  ----------------------------------------------

        /**
         * Check if class names starts with this pattern.
         */
        final private String f_sPrefix;
        }

    // ----- inner class: ClassInPackageFilter ------------------------------

    /**
     * Filter matches input class name only if in pattern's package name.
     */
    protected static class ClassInPackageFilter
        extends AbstractReflectionAllowedFilter
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a filter that matches if class is in this package.
         *
         * @param sPackage        check if class is in this package name
         * @param statusPatMatch  {@link Status} to return if match
         */
        public ClassInPackageFilter(String sPackage, Status statusPatMatch)
            {
            super(statusPatMatch);
            f_sPackageName = sPackage;
            }

        // ----- AbstractReflectionFilter methods ---------------------------

        @Override
        protected boolean isMatch(Class<?> clz)
            {
            return f_sPackageName.equals(clz.getPackage().getName());
            }

        // ----- data members  ----------------------------------------------

        /**
         * package name from pattern
         */
        final private String f_sPackageName;
        }

    // ----- constants ------------------------------------------------------
    
    /**
     * Value to set system property {@link #REFLECT_FILTER_PROPERTY} to disable the reflection allowed filter.
     */
    public final static String REFLECT_ALLOW_ALL = "*";

    /**
     * The system property name to configure {@link ReflectionAllowedFilter} filter.
     *
     * @see com.tangosol.util.ClassHelper#REFLECT_FILTER_PROPERTY
     */
    public final static String REFLECT_FILTER_PROPERTY = "coherence.reflect.filter";

    /**
     * Separator for filter patterns.
     */
    public final static String REFLECT_FILTER_SEPARATOR = ";";

    /**
     * Default {@link ReflectionAllowedFilter} blacklist.
     */
    public final static String DEFAULT_REFLECT_ALLOWED_BLACKLIST =
        "!java.lang.Class" + REFLECT_FILTER_SEPARATOR +
        "!java.lang.System" + REFLECT_FILTER_SEPARATOR +
        "!java.lang.Runtime";

    /**
     * @see com.tangosol.util.ClassHelper#DEFAULT_FILTER_LIST
     */
    public final static String DEFAULT_FILTER_LIST =
        DEFAULT_REFLECT_ALLOWED_BLACKLIST + REFLECT_FILTER_SEPARATOR + REFLECT_ALLOW_ALL;

    /**
     * Pattern to detect invalid characters in {@link ReflectionAllowedFilter} pattern.
     */
    private final static Pattern PATTERN_INVALID_CHARS = Pattern.compile("[^A-Za-z0-9\\.;!*$]");

    /**
     * {@link ReflectionAllowedFilter} instance initialized by the value of the system property {@link #REFLECT_FILTER_PROPERTY}.
     */
    public final static ReflectionAllowedFilter INSTANCE =
        ensureSafeFilter(Config.getProperty(REFLECT_FILTER_PROPERTY, DEFAULT_FILTER_LIST));

    // ----- data members ---------------------------------------------------
    
    /**
     * A set of patterns delimited by {@link #REFLECT_FILTER_SEPARATOR} for this filter.
     * The pattern syntax is detailed in {@link #ensureFilter(String)}.
     */
    private final String f_sPatterns;

    /**
     * A list of filters, one filter per non-empty pattern in {@link #f_sPatterns}.
     */
    private final List<AbstractReflectionAllowedFilter> f_listFilter;

    /**
     * Cache map of past {@link ReflectionAllowedFilter} results.
     */
    private final Map<Class, Status> f_mapClassStatus = new CopyOnWriteMap<>(new WeakHashMap<>());

    /**
     * Optimization. Set to true when pattern is just wildcard.
     */
    private final boolean f_fDisabled;
    }
