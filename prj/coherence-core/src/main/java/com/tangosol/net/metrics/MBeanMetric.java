/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.metrics;

import com.tangosol.net.management.annotation.MetricsScope;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.regex.Pattern;

import java.util.stream.Collectors;

/**
 * A Coherence MBean metric value.
 *
 * @author jk  2019.05.23
 * @since 12.2.1.4
 */
public interface MBeanMetric
    {
    /**
     * Returns the unique {@link Identifier} for this metric.
     *
     * @return the unique {@link Identifier} for this metric
     */
    public Identifier getIdentifier();

    /**
     * Returns the name of this metric.
     *
     * @return  the name of this metric
     */
    default String getName()
        {
        return getIdentifier().getName();
        }

    /**
     * Returns the scope of this metric.
     *
     * @return  the scope of this metric
     */
    default Scope getScope()
        {
        return getIdentifier().getScope();
        }

    /**
     * Returns an immutable {@link Map} of this metric's tags.
     *
     * @return  an immutable {@link Map} of this metric's tags
     */
    default Map<String, String> getTags()
        {
        return getIdentifier().getTags();
        }

    /**
     * Returns the name of the MBean that this metric
     * obtains its value from
     *
     * @return  the name of the MBean that this metric
     *          obtains its value from
     */
    public String getMBeanName();

    /**
     * Returns the description of this metric.
     *
     * @return the description of this metric
     */
    public String getDescription();

    /**
     * Returns the current value of this metric.
     *
     * @return the current value of this metric
     */
    public Object getValue();

    // ----- inner enum: Scope ----------------------------------------------

    /**
     * An enumeration representing the scopes of a metric
     */
    public enum Scope
        {
        /**
         * The Base scoped metric.
         */
        BASE,

        /**
         * The Vendor scoped metric.
         */
        VENDOR,

        /**
         * The Application (default) scoped metric.
         */
        APPLICATION;

        /**
         * Obtain the value to use in an MBean descriptor.
         *
         * @return  the value to use in an MBean descriptor
         */
        public String getDescriptorValue()
            {
            return MetricsScope.KEY + '=' + name();
            }
    }

    // ----- inner class: Identifier ----------------------------------------

    /**
     * The identifier for a metric.
     * <p>
     * A metric identifier is the metric's name, scope and
     * its tags.
     */
    public class Identifier
            implements Comparable<Identifier>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a metric identifier.
         *
         * @param scope    the metric's {@link Scope}
         * @param sName    the metric's name
         * @param mapTags  the metric's tags
         *
         * @throws NullPointerException  if either the scope, name or tags parameters are {@code null}
         */
        public Identifier(Scope scope, String sName, Map<String, String> mapTags)
            {
            Objects.requireNonNull(mapTags);
            f_scope   = Objects.requireNonNull(scope);
            f_sName   = Objects.requireNonNull(sName);
            f_mapTags = new TreeMap<>(mapTags);

            // As the tags map is immutable we can save time later by doing the to-string once
            f_sTags   = f_mapTags.entrySet()
                                 .stream()
                                 .map(entry -> entry.getKey() + "=\"" + entry.getValue() + "\"")
                                 .collect(Collectors.joining(", "));
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return this metric's name.
         *
         * @return  this metric's name
         */
        public String getName()
            {
            return f_sName;
            }

        /**
         * Return this metric's scope.
         *
         * @return  this metric's scope
         */
        public Scope getScope()
            {
            return f_scope;
            }

        /**
         * Return an immutable {@link Map} of this metric's tags.
         *
         * @return  an immutable {@link Map} of this metric's tags
         */
        public Map<String, String> getTags()
            {
            return Collections.unmodifiableMap(f_mapTags);
            }

        /**
         * Returns the formatted dot delimited name of the metric.
         *
         * @return the formatted dot delimited name of the metric
         */
        public String getFormattedName()
            {
            if (m_sFormattedName == null)
                {
                m_sFormattedName    = formatName(f_sName);
                }
            return m_sFormattedName;
            }

        /**
         * Returns the legacy Coherence Prometheus formatted name.
         *
         * @return the legacy Coherence Prometheus formatted name
         */
        public String getLegacyName()
            {
            if (m_sLegacyName == null)
                {
                m_sLegacyName       = prometheusName(f_scope, f_sName);
                }
            return m_sLegacyName;
            }

        /**
         * Returns the Microprofile 2.0 formatted name.
         *
         * @return the Microprofile 2.0 formatted name
         */
        public String getMicroprofileName()
            {
            if (m_sMicroprofileName == null)
                {
                m_sMicroprofileName = microprofileName(f_scope, f_sName);
                }
            return m_sMicroprofileName;
            }

        /**
         * Returns the metric tags with keys in dot delimited format.
         *
         * @return  the metric tags with keys in dot delimited format
         */
        public Map<String, String> getFormattedTags()
            {
            if (f_mapFormattedTags == null)
                {
                SortedMap<String, String> map = new TreeMap<>();
                for (Map.Entry<String, String> entry : f_mapTags.entrySet())
                    {
                    map.put(formatName(entry.getKey()), entry.getValue());
                    }
                f_mapFormattedTags = map;
                }
            return f_mapFormattedTags;
            }

        /**
         * Returns the metric tags with keys in Prometheus underscore delimited format.
         *
         * @return  the metric tags with keys in Prometheus underscore delimited format
         */
        public Map<String, String> getPrometheusTags()
            {
            if (f_mapPrometheusTags == null)
                {
                SortedMap<String, String> map = new TreeMap<>();
                for (Map.Entry<String, String> entry : f_mapTags.entrySet())
                    {
                    map.put(prometheusName(null, entry.getKey()), entry.getValue());
                    }
                f_mapPrometheusTags = map;
                }
            return f_mapPrometheusTags;
            }

        // ----- Comparable methods -----------------------------------------

        @Override
        public int compareTo(Identifier other)
            {
            int nResult = f_scope.compareTo(other.f_scope);

            if (nResult == 0)
                {
                nResult = f_sName.compareTo(other.f_sName);
                }

            if (nResult == 0)
                {
                nResult = f_sTags.compareTo(other.f_sTags);
                }

            return nResult;
            }

        // ----- object methods ---------------------------------------------

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
            Identifier that = (Identifier) o;
            return f_scope.equals(that.f_scope) &&
                    f_sName.equals(that.f_sName) &&
                    f_mapTags.equals(that.f_mapTags);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(f_scope, f_sName, f_mapTags);
            }

        @Override
        public String toString()
            {
            return f_scope + ":" + f_sName + ", tags='" + f_sTags + '\'';
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Create the formatted metric name
         *
         * @param sName the name to format
         *
         * @return the formatted metric name
         */
        String formatName(String sName)
            {
            // ************************************************************
            // Warning: Changing this method may change the format of the
            // metric names and will break any customer that relies on this
            // for things like Grafana dashboards.
            // ************************************************************

            String[] asParts    = sName.replaceAll("_", ".").split("\\.");
            String   sFormatted = Arrays.stream(asParts)
                                        .map(this::camelToDot)
                                        .collect(Collectors.joining("."));


            return sFormatted.toLowerCase();
            }

        private String prometheusName(MBeanMetric.Scope scope, String sName)
            {
            // ************************************************************
            // Warning: Changing this method may change the format of the
            // metric names and will break any customer that relies on this
            // for things like Grafana dashboards.
            // ************************************************************

            // spec 3.2.1

            // Escape any invalid characters by changing them to an underscore
            sName = sName.replaceAll("[^a-zA-Z0-9_]", "_");

            if (scope != null)
                {
                //Scope is always specified at the start of the metric name.
                //Scope and name are separated by colon (:).
                sName = scope.name().toLowerCase() + ":" + sName;
                }

            //camelCase is translated to snake_case
            sName = camelToSnake(sName);

            String orig;
            do
                {
                orig = sName;
                //Double underscore is translated to single underscore
                sName = DOUBLE_UNDERSCORE.matcher(sName).replaceAll("_");
                }
            while (!orig.equals(sName));

            do
                {
                orig = sName;
                //Colon-underscore (:_) is translated to single colon
                sName = COLON_UNDERSCORE.matcher(sName).replaceAll(":");
                }
            while (!orig.equals(sName));

            return sName;
            }

        private String microprofileName(MBeanMetric.Scope scope, String sName)
            {
            // ************************************************************
            // Warning: Changing this method may change the format of the
            // metric names and will break any customer that relies on this
            // for things like Grafana dashboards.
            // ************************************************************

            // Escape any invalid characters by changing them to an underscore
            sName = sName.replaceAll("[^a-zA-Z0-9_]", "_");

            return scope == null ? sName : scope.name().toLowerCase() + "_" + sName;
            }

        /**
         * Split a camel-case string into a dot delimited string.
         *
         * @param s  the string to convert
         *
         * @return the camelcase string converted to a dot delimited string
         */
        private String camelToDot(String s)
            {
            return s.replaceAll(CAMEL_CASE_PATTERN, DOT);
            }

        private static String camelToSnake(String name)
            {
            return CAMEL_CASE.matcher(name).replaceAll("$1_$2").toLowerCase();
            }

        // ----- constants --------------------------------------------------

        private static final String CAMEL_CASE_PATTERN = String.format("%s|%s|%s",
                                                                       "(?<=[A-Z])(?=[A-Z][a-z])",
                                                                       "(?<=[^A-Z])(?=[A-Z])",
                                                                       "(?<=[A-Za-z])(?=[^A-Za-z])");

        private static final String DOT = ".";

        private static final Pattern DOUBLE_UNDERSCORE = Pattern.compile("__");

        private static final Pattern COLON_UNDERSCORE = Pattern.compile(":_");

        private static final Pattern CAMEL_CASE = Pattern.compile("(.)(\\p{Upper})");

        // ----- data members -----------------------------------------------

        /**
         * The metric's scope.
         */
        private final Scope f_scope;

        /**
         * The metric's name.
         */
        private final String f_sName;

        /**
         * The metric's tags.
         */
        private final SortedMap<String, String> f_mapTags;

        /**
         * The metric's tags with the keys formatted to dot notation.
         */
        private SortedMap<String, String> f_mapFormattedTags;

        /**
         * The metric's tags with the keys formatted to Prometheus notation.
         */
        private SortedMap<String, String> f_mapPrometheusTags;

        /**
         * A string representation of the metric's tags.
         */
        private final String f_sTags;

        /**
         * The dot formatted name of the metric.
         */
        private String m_sFormattedName;

        /**
         * The legacy formatted name of the metric.
         */
        private String m_sLegacyName;

        /**
         * The Microprofile 2.0 formatted name of the metric.
         */
        private String m_sMicroprofileName;
        }
    }
