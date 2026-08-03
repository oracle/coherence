/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Shared serialization class policy used by Coherence deserialization gates.
 *
 * @author Aleks Seovic  2026.04.29
 * @since 26.07
 */
public final class SerializationAllowlist
    {
    /**
     * Return {@code true} if the specified class is allowed by the serialization
     * policy.
     *
     * @param clz  the class to check
     *
     * @return {@code true} if the class is allowed
     */
    public static boolean isAllowed(Class<?> clz)
        {
        if (clz == null)
            {
            return true;
            }

        if (isDenied(clz))
            {
            return false;
            }

        return !isProdMode() || isAllowlisted(clz);
        }

    /**
     * Return {@code true} if the specified class is allowlisted.
     *
     * @param clz  the class to check
     *
     * @return {@code true} if the class is allowlisted
     */
    public static boolean isAllowlisted(Class<?> clz)
        {
        if (clz == null)
            {
            return true;
            }

        if (clz.isArray())
            {
            Class<?> clzComponent = clz.getComponentType();
            return clzComponent.isPrimitive() || isAllowlisted(clzComponent);
            }

        String sName = clz.getName();
        return BASELINE_EXACT.contains(sName)
               || matchesPrefix(sName, BASELINE_PREFIX)
               || configuredAllowlist().matches(sName);
        }

    /**
     * Return {@code true} if the specified class is denied.
     *
     * @param clz  the class to check
     *
     * @return {@code true} if the class is denied
     */
    public static boolean isDenied(Class<?> clz)
        {
        if (clz == null)
            {
            return false;
            }

        if (clz.isArray())
            {
            return isDenied(clz.getComponentType());
            }

        String sName = clz.getName();
        return DENY_EXACT.contains(sName) || matchesPrefix(sName, DENY_PREFIX);
        }

    /**
     * Return {@code true} if serialization is running in prod mode.
     *
     * @return {@code true} if prod mode is active
     */
    public static boolean isProdMode()
        {
        return "prod".equalsIgnoreCase(
                System.getProperty("coherence.mode", Config.getProperty("coherence.mode", "dev")).trim());
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the effective manual allowlist.
     *
     * @return the effective manual allowlist
     */
    private static Allowlist configuredAllowlist()
        {
        String    sRaw      = Config.getProperty(PROP_SERIALIZATION_ALLOWED, "");
        Allowlist allowlist = s_allowlist.get();
        if (allowlist != null && allowlist.raw().equals(sRaw))
            {
            return allowlist;
            }

        allowlist = parseAllowlist(sRaw);
        s_allowlist.set(allowlist);
        return allowlist;
        }

    /**
     * Parse the manual allowlist property.
     *
     * @param sRaw  the raw property value
     *
     * @return the parsed allowlist
     */
    private static Allowlist parseAllowlist(String sRaw)
        {
        Set<String> setExact  = Arrays.stream(sRaw.split(";", -1))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> isValidEntry(s, false))
                .collect(Collectors.toUnmodifiableSet());

        Set<String> setPrefix = Arrays.stream(sRaw.split(";", -1))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> isValidEntry(s, true))
                .map(s -> s.substring(0, s.length() - 1))
                .collect(Collectors.toUnmodifiableSet());

        return new Allowlist(sRaw, setExact, setPrefix);
        }

    /**
     * Return {@code true} if an allowlist entry is valid for the requested
     * entry type.
     *
     * @param sEntry   the raw entry
     * @param fPrefix  {@code true} to validate package wildcards
     *
     * @return {@code true} if the entry is valid for the requested type
     */
    private static boolean isValidEntry(String sEntry, boolean fPrefix)
        {
        boolean fValid = fPrefix
                ? PATTERN_PACKAGE_WILDCARD.matcher(sEntry).matches()
                : PATTERN_CLASS_NAME.matcher(sEntry).matches();

        if (!fValid && !fPrefix && !PATTERN_PACKAGE_WILDCARD.matcher(sEntry).matches())
            {
            Logger.warn("Ignored serialization allowlist entry: property=%s, reason=%s, entry=%s"
                    .formatted(PROP_SERIALIZATION_ALLOWED, "serialization-allowlist-entry-invalid",
                            boundedValue(sEntry)));
            }
        return fValid;
        }

    /**
     * Return {@code true} if a class name matches one of the package prefixes.
     *
     * @param sName       the class name
     * @param setPrefix   the prefixes
     *
     * @return {@code true} if the class name matches
     */
    private static boolean matchesPrefix(String sName, Set<String> setPrefix)
        {
        return setPrefix.stream().anyMatch(sName::startsWith);
        }

    /**
     * Bound a configured value for logging.
     *
     * @param sValue  the configured value
     *
     * @return the bounded value
     */
    private static String boundedValue(String sValue)
        {
        if (sValue == null)
            {
            return "";
            }
        String sClean = sValue.replaceAll("[^\\p{Print}]", "?");
        return sClean.length() <= 160 ? sClean : sClean.substring(0, 160);
        }

    // ----- inner class: Allowlist ----------------------------------------

    /**
     * Parsed allowlist.
     *
     * @param raw      the raw property value
     * @param exact    exact class entries
     * @param prefix   package prefix entries
     */
    private record Allowlist(String raw, Set<String> exact, Set<String> prefix)
        {
        /**
         * Return {@code true} if the class name is manually allowlisted.
         *
         * @param sName  the class name
         *
         * @return {@code true} if the class name is manually allowlisted
         */
        boolean matches(String sName)
            {
            return exact.contains(sName) || matchesPrefix(sName, prefix);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Manual serialization allowlist property.
     */
    public static final String PROP_SERIALIZATION_ALLOWED = "coherence.serialization.allowed";

    /**
     * Built-in exact allowlist.
     */
    private static final Set<String> BASELINE_EXACT = Set.of(
            "java.lang.Number",
            "java.lang.String",
            "java.lang.Boolean",
            "java.lang.Character",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            // legitimate management and JNDI value types used by Coherence
            // internals; gadget classes in these namespaces remain exact denies
            "javax.management.Attribute",
            "javax.management.MBeanInfo",
            "javax.management.ObjectName",
            "javax.management.remote.JMXServiceURL",
            "javax.naming.CompositeName",
            "javax.naming.CompoundName");

    /**
     * Built-in package allowlist.
     */
    private static final Set<String> BASELINE_PREFIX = Set.of(
            "com.tangosol.",
            "com.oracle.coherence.",
            "java.util.",
            "java.time.",
            "java.math.",
            "javax.management.openmbean.");

    /**
     * Exact JEP-290 deserialization gadget deny-list. This is intentionally
     * separate from the lambda bytecode sandbox-escape deny-list resource:
     * the gates protect different sinks and overlap only where a class is
     * dangerous at both layers.
     */
    private static final Set<String> DENY_EXACT = Set.of(
            "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl",
            "com.sun.rowset.JdbcRowSetImpl",
            "java.beans.EventHandler",
            "java.lang.ProcessBuilder",
            "java.lang.Runtime",
            "java.lang.invoke.MethodHandles$Lookup",
            "jdk.internal.misc.Unsafe",
            // package denies were too broad for javax.management.* and
            // javax.naming.* because Coherence management and JNDI integrations
            // use legitimate types from those namespaces on internal wires
            "javax.management.BadAttributeValueExpException",
            "javax.naming.LinkRef",
            "javax.naming.Reference",
            "javax.script.ScriptEngineManager",
            "sun.misc.Unsafe");

    /**
     * Package deny-list.
     */
    private static final Set<String> DENY_PREFIX = Set.of(
            "com.sun.jndi.",
            "java.rmi.server.",
            "javax.management.remote.rmi.",
            "jdk.internal.reflect.",
            "org.apache.commons.collections.functors.",
            "org.apache.commons.collections4.functors.",
            "org.codehaus.groovy.runtime.",
            "sun.reflect.");

    /**
     * Java class name pattern.
     */
    private static final Pattern PATTERN_CLASS_NAME = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+");

    /**
     * Package wildcard pattern.
     */
    private static final Pattern PATTERN_PACKAGE_WILDCARD = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+\\.\\*");

    // ----- data members ---------------------------------------------------

    /**
     * Cached manual allowlist.
     */
    private static final AtomicReference<Allowlist> s_allowlist = new AtomicReference<>();
    }
