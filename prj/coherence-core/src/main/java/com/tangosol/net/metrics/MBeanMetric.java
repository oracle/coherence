/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.metrics;

import com.tangosol.net.management.annotation.MetricsScope;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

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
            f_scope   = Objects.requireNonNull(scope);
            f_sName   = Objects.requireNonNull(sName);
            f_mapTags = new TreeMap<>(Objects.requireNonNull(mapTags));

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
         * A string representation of the metric's tags.
         */
        private final String f_sTags;
        }
    }
