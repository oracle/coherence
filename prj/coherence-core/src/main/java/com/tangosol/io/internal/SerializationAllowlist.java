/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.SecurityConfig;
import com.tangosol.io.SerializationGeneratedClasses;

import java.lang.invoke.MethodHandles;
import java.lang.StackWalker;
import java.lang.StackWalker.StackFrame;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Shared serialization class policy used by Coherence deserialization gates.
 *
 * @author Aleks Seovic  2026.04.29
 * @since 26.04
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

        return isRegisteredRestGeneratedPartialClass(clz)
               || isAllowlistedName(clz.getName(), clz.isSynthetic());
        }

    /**
     * Return {@code true} if the specified class is explicitly allowlisted by
     * security configuration or the manual serialization allowlist property.
     *
     * @param clz  the class to check
     *
     * @return {@code true} if the class is explicitly allowlisted
     */
    public static boolean isExplicitlyAllowlisted(Class<?> clz)
        {
        if (clz == null)
            {
            return true;
            }

        if (clz.isArray())
            {
            Class<?> clzComponent = clz.getComponentType();
            return clzComponent.isPrimitive() || isExplicitlyAllowlisted(clzComponent);
            }

        String sName = explicitAllowlistName(clz.getName(), clz.isSynthetic());
        return sName != null && isDirectlyExplicitlyAllowlisted(sName);
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
     * Return {@code true} if serialization allowlist enforcement is active.
     *
     * @return {@code true} if serialization allowlist enforcement is active
     */
    public static boolean isProdMode()
        {
        return CoherenceMode.isAllowlistEnforced();
        }

    /**
     * Register a REST-generated partial projection class.
     *
     * @param lookup  caller proof from the REST {@code PartialObject} class
     * @param clz     generated partial projection class
     */
    public static void registerRestGeneratedPartialClass(MethodHandles.Lookup lookup, Class<?> clz)
        {
        Class<?> clzAnchor = validateRestGeneratedPartialClass(lookup, clz);
        synchronized (s_mapRestGeneratedPartialClasses)
            {
            Set<Class<?>> setClasses = s_mapRestGeneratedPartialClasses.get(clzAnchor);
            if (setClasses == null)
                {
                setClasses = Collections.newSetFromMap(new WeakHashMap<>());
                s_mapRestGeneratedPartialClasses.put(clzAnchor, setClasses);
                }
            setClasses.add(clz);
            }
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
     * Return {@code true} if a class is a registered REST-generated projection
     * type.
     *
     * @param clz  the class to check
     *
     * @return {@code true} if the class was registered by REST generation
     */
    private static boolean isRegisteredRestGeneratedPartialClass(Class<?> clz)
        {
        if (!clz.getName().startsWith(REST_PARTIAL_CLASS_PREFIX))
            {
            return false;
            }

        Class<?> clzAnchor = clz.getSuperclass();
        if (clzAnchor == null)
            {
            return false;
            }

        synchronized (s_mapRestGeneratedPartialClasses)
            {
            Set<Class<?>> setClasses = s_mapRestGeneratedPartialClasses.get(clzAnchor);
            return setClasses != null && setClasses.contains(clz);
            }
        }

    /**
     * Validate a REST-generated projection class registration.
     *
     * @param lookup  caller proof from the REST {@code PartialObject} class
     * @param clz     generated partial projection class
     *
     * @return trusted REST {@code PartialObject} anchor class
     */
    private static Class<?> validateRestGeneratedPartialClass(MethodHandles.Lookup lookup, Class<?> clz)
        {
        if (lookup == null || clz == null)
            {
            throw new IllegalArgumentException("lookup and class are required");
            }

        Class<?> clzAnchor = lookup.lookupClass();
        if (!REST_PARTIAL_OBJECT_CLASS.equals(clzAnchor.getName())
                || !isTrustedRestPartialObject(clzAnchor))
            {
            throw new SecurityException("REST generated partial registration caller is not trusted");
            }

        if (!isTrustedRestGeneratedPartialRegistrationCaller(clzAnchor)
                || !lookup.hasFullPrivilegeAccess())
            {
            throw new SecurityException("REST generated partial registration caller is not trusted");
            }

        ClassLoader loader = clz.getClassLoader();
        if (!clz.getName().startsWith(REST_PARTIAL_CLASS_PREFIX)
                || clz.getSuperclass() != clzAnchor
                || loader == null
                || !REST_PARTIAL_CLASS_LOADER.equals(loader.getClass().getName())
                || loader.getClass().getDeclaringClass() != clzAnchor)
            {
            throw new SecurityException("REST generated partial class shape is not trusted");
            }

        return clzAnchor;
        }

    /**
     * Return {@code true} if the supplied class is the real REST
     * {@code PartialObject} class.
     *
     * @param clz  the class to check
     *
     * @return {@code true} if the class is trusted
     */
    private static boolean isTrustedRestPartialObject(Class<?> clz)
        {
        Module module = clz.getModule();
        if (module.isNamed())
            {
            ModuleLayer layer = SerializationAllowlist.class.getModule().getLayer();
            if (layer == null)
                {
                return false;
                }

            Module moduleRest = layer.findModule(REST_MODULE).orElse(null);
            return moduleRest == module
                   && Class.forName(moduleRest, REST_PARTIAL_OBJECT_CLASS) == clz;
            }

        ClassLoader loader = SerializationGeneratedClasses.class.getClassLoader();
        try
            {
            return Class.forName(REST_PARTIAL_OBJECT_CLASS, false, loader) == clz;
            }
        catch (ClassNotFoundException e)
            {
            return false;
            }
        }

    /**
     * Return {@code true} if the registration call stack proves that the real
     * REST anchor called through the generated-class facade.
     *
     * @param clzAnchor  trusted REST {@code PartialObject} anchor class
     *
     * @return {@code true} if the caller path is trusted
     */
    private static boolean isTrustedRestGeneratedPartialRegistrationCaller(Class<?> clzAnchor)
        {
        boolean[] afSawFacade = new boolean[1];
        Class<?>  clzCaller   = STACK_WALKER.walk(stream -> stream
                .map(StackFrame::getDeclaringClass)
                .filter(clz -> !isIgnoredRestGeneratedPartialRegistrationFrame(clz, afSawFacade))
                .findFirst()
                .orElse(null));

        return afSawFacade[0] && clzCaller == clzAnchor;
        }

    /**
     * Return {@code true} if the specified frame is registration plumbing or
     * JDK invocation machinery that should not own the trust decision.
     *
     * @param clz          frame declaring class
     * @param afSawFacade  one-element flag set when the public facade is seen
     *
     * @return {@code true} if the frame should be ignored
     */
    private static boolean isIgnoredRestGeneratedPartialRegistrationFrame(Class<?> clz, boolean[] afSawFacade)
        {
        if (clz == SerializationAllowlist.class)
            {
            return true;
            }

        if (clz == SerializationGeneratedClasses.class)
            {
            afSawFacade[0] = true;
            return true;
            }

        String sName = clz.getName();
        return sName.startsWith("java.lang.StackWalker")
               || sName.startsWith("java.lang.reflect.")
               || sName.startsWith("jdk.internal.reflect.")
               || sName.startsWith("java.lang.invoke.");
        }

    /**
     * Return {@code true} if the specified class name is allowlisted.
     *
     * @param sName       the class name
     * @param fSynthetic  {@code true} if the class is synthetic
     *
     * @return {@code true} if the class name is allowlisted
     */
    static boolean isAllowlistedName(String sName, boolean fSynthetic)
        {
        if (isDirectlyAllowlisted(sName))
            {
            return true;
            }

        String sCapturingClass = fSynthetic ? generatedCapturingClassName(sName) : null;
        return sCapturingClass != null && isDirectlyAllowlisted(sCapturingClass);
        }

    /**
     * Return {@code true} if the specified class name is directly allowlisted.
     *
     * @param sName  the class name
     *
     * @return {@code true} if the class name is directly allowlisted
     */
    private static boolean isDirectlyAllowlisted(String sName)
        {
        return BASELINE_EXACT.contains(sName)
               || matchesPrefix(sName, BASELINE_PREFIX)
               || isDirectlyExplicitlyAllowlisted(sName);
        }

    /**
     * Return {@code true} if the specified class name is directly explicitly
     * allowlisted.
     *
     * @param sName  the class name
     *
     * @return {@code true} if the class name is explicitly allowlisted
     */
    private static boolean isDirectlyExplicitlyAllowlisted(String sName)
        {
        return SecurityConfig.current().contains(sName)
               || configuredAllowlist().matches(sName);
        }

    /**
     * Return the class name to use for explicit allowlist checks.
     *
     * @param sName       the class name
     * @param fSynthetic  {@code true} if the class is synthetic
     *
     * @return the class name, or {@code null} if it cannot be allowlisted
     */
    private static String explicitAllowlistName(String sName, boolean fSynthetic)
        {
        if (isDirectlyExplicitlyAllowlisted(sName))
            {
            return sName;
            }

        return fSynthetic ? generatedCapturingClassName(sName) : null;
        }

    /**
     * Return the capturing class for a JVM-generated or Coherence-generated
     * lambda class name.
     *
     * @param sName  the class name
     *
     * @return the capturing class name, or {@code null} if not a lambda class
     */
    private static String generatedCapturingClassName(String sName)
        {
        String sCapturingClass = lambdaCapturingClassName(sName);
        return sCapturingClass == null ? methodReferenceCapturingClassName(sName) : sCapturingClass;
        }

    /**
     * Return the capturing class for a JVM-generated or Coherence-generated
     * lambda class name.
     *
     * @param sName  the class name
     *
     * @return the capturing class name, or {@code null} if not a lambda class
     */
    private static String lambdaCapturingClassName(String sName)
        {
        if (sName == null)
            {
            return null;
            }

        int ofMarker = sName.indexOf(LAMBDA_PROXY_MARKER);
        if (ofMarker > 0)
            {
            return sName.substring(0, ofMarker);
            }

        ofMarker = sName.indexOf(LAMBDA_GENERATED_MARKER);
        return ofMarker > 0 ? sName.substring(0, ofMarker) : null;
        }

    /**
     * Return the capturing class for a Coherence-generated method-reference
     * class name.
     *
     * @param sName  the class name
     *
     * @return the capturing class name, or {@code null} if not a method reference
     */
    private static String methodReferenceCapturingClassName(String sName)
        {
        if (sName == null)
            {
            return null;
            }

        int ofVersion = sName.lastIndexOf('$');
        if (ofVersion <= 0 || !PATTERN_METHOD_REFERENCE_VERSION.matcher(sName.substring(ofVersion + 1)).matches())
            {
            return null;
            }

        int ofMethod = sName.lastIndexOf('$', ofVersion - 1);
        if (ofMethod <= 0)
            {
            return null;
            }

        String sMethod = sName.substring(ofMethod + 1, ofVersion);
        if (!PATTERN_METHOD_REFERENCE_METHOD.matcher(sMethod).matches())
            {
            return null;
            }

        String sOwner = sName.substring(0, ofMethod);
        if (sOwner.startsWith(METHOD_REFERENCE_JAVA_PACKAGE_PREFIX))
            {
            sOwner = sOwner.substring(METHOD_REFERENCE_JAVA_PACKAGE_PREFIX.length());
            }

        return PATTERN_CLASS_NAME.matcher(sOwner).matches() ? sOwner : null;
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
     * Coherence-generated remotable lambda class marker.
     */
    private static final String LAMBDA_GENERATED_MARKER = "$lambda$";

    /**
     * Built-in exact allowlist.
     */
    private static final Set<String> BASELINE_EXACT = Set.of(
            "java.lang.Number",
            "java.lang.Object",
            "java.lang.String",
            "java.lang.Boolean",
            "java.lang.Character",
            "java.lang.CharSequence",
            "java.lang.Class",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Math",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Throwable",
            "java.lang.Exception",
            "java.lang.RuntimeException",
            "java.lang.IllegalStateException",
            "java.lang.SecurityException",
            "java.lang.Error",
            "java.lang.AssertionError",
            "java.lang.Enum",
            "java.lang.StackTraceElement",
            "java.io.IOException",
            "java.io.ObjectStreamException",
            "java.io.InvalidClassException",
            "java.net.InetAddress",
            "java.net.Inet4Address",
            "java.net.Inet6Address",
            "com.tangosol.internal.net.security.PeerProofReadiness$ReadinessInvocable",
            "com.tangosol.internal.net.security.PeerProofReadiness$Response",
            // legitimate management and JNDI value types used by Coherence
            // internals; gadget classes in these namespaces remain exact denies
            "javax.management.Attribute",
            "javax.management.ImmutableDescriptor",
            "javax.management.MBeanAttributeInfo",
            "javax.management.MBeanConstructorInfo",
            "javax.management.MBeanFeatureInfo",
            "javax.management.MBeanInfo",
            "javax.management.MBeanNotificationInfo",
            "javax.management.MBeanOperationInfo",
            "javax.management.MBeanParameterInfo",
            "javax.management.ObjectName",
            "javax.management.modelmbean.DescriptorSupport",
            "javax.management.remote.JMXServiceURL",
            "javax.naming.CompositeName",
            "javax.naming.CompoundName");

    /**
     * Built-in package allowlist.
     */
    private static final Set<String> BASELINE_PREFIX = Set.of(
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

    /**
     * Coherence-generated method-reference method-name pattern.
     */
    private static final Pattern PATTERN_METHOD_REFERENCE_METHOD = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*|<init>");

    /**
     * Coherence-generated method-reference version pattern.
     */
    private static final Pattern PATTERN_METHOD_REFERENCE_VERSION = Pattern.compile("[0-9A-Fa-f]{32}");

    /**
     * Package prefix used for Coherence-generated method references to JDK
     * classes.
     */
    private static final String METHOD_REFERENCE_JAVA_PACKAGE_PREFIX = "lambda.";

    /**
     * JVM-generated lambda proxy class name marker.
     */
    private static final String LAMBDA_PROXY_MARKER = "$$Lambda";

    /**
     * REST module name.
     */
    private static final String REST_MODULE = "com.oracle.coherence.rest";

    /**
     * REST partial object class name.
     */
    private static final String REST_PARTIAL_OBJECT_CLASS =
            "com.tangosol.coherence.rest.util.PartialObject";

    /**
     * REST partial class loader class name.
     */
    private static final String REST_PARTIAL_CLASS_LOADER =
            "com.tangosol.coherence.rest.util.PartialObject$PartialClassLoader";

    /**
     * REST-generated partial class package prefix.
     */
    private static final String REST_PARTIAL_CLASS_PREFIX =
            "com.tangosol.coherence.rest.util.gen.partial.";

    /**
     * Stack walker used to verify the REST generated-partial registration
     * caller identity.
     */
    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    // ----- data members ---------------------------------------------------

    /**
     * Cached manual allowlist.
     */
    private static final AtomicReference<Allowlist> s_allowlist = new AtomicReference<>();

    /**
     * Registered REST-generated partial projection classes grouped by
     * {@code PartialObject} identity.
     */
    private static final Map<Class<?>, Set<Class<?>>> s_mapRestGeneratedPartialClasses = new WeakHashMap<>();
    }
