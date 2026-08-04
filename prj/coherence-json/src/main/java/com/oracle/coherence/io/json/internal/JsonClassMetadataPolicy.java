/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.JsonBindingException;

import com.tangosol.internal.util.CoherenceMode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Central policy for JSON class-name metadata.
 * <p>
 * Prompt 05 at
 * design/features/security-bugs/plans/rest-01/prompts/05-slice-d-json-class-metadata-implementation.md
 * makes hardened class metadata default-deny unless the class comes from an
 * operator-configured alias and passes the shared serialization gate.
 *
 * @author Vaso Putica  2026.05.09
 * @since 26.07
 */
public final class JsonClassMetadataPolicy
    {
    private JsonClassMetadataPolicy()
        {
        }

    /**
     * Resolve a JSON {@link Class} literal.
     *
     * @param sClassName  the class name from JSON
     * @param loader      the loader to use in compatibility mode
     *
     * @return the resolved class
     *
     * @throws ClassNotFoundException if compatibility resolution cannot find
     *         the class
     */
    public static Class<?> resolveClassLiteral(String sClassName, ClassLoader loader)
            throws ClassNotFoundException
        {
        if (!CoherenceMode.isSecurityHardeningEnabled())
            {
            return loader == null ? Class.forName(sClassName, false, null) : loader.loadClass(sClassName);
            }

        throw new JsonBindingException("JSON Class literal deserialization is not allowed when security hardening is enabled");
        }

    /**
     * Resolve Genson {@code @class} metadata.
     *
     * @param sAlias              the metadata value from JSON
     * @param mapConfiguredAlias  the exact aliases configured by the server
     * @param mapAliasCache       the mutable Genson alias/cache map
     * @param mapPackageAlias     the configured package aliases
     * @param mapCompatAlias      the configured compatibility aliases
     * @param fEnforceAliases     {@code true} when Genson type alias enforcement is enabled
     * @param loader              the loader to use for compatibility fallback
     *
     * @return the resolved class
     *
     * @throws ClassNotFoundException if compatibility resolution cannot find
     *         the class
     */
    public static Class<?> resolveClassMetadata(String sAlias,
                                                Map<String, Class<?>> mapConfiguredAlias,
                                                Map<String, Class<?>> mapAliasCache,
                                                Map<String, String> mapPackageAlias,
                                                Map<String, String> mapCompatAlias,
                                                boolean fEnforceAliases,
                                                ClassLoader loader)
            throws ClassNotFoundException
        {
        if (CoherenceMode.isSecurityHardeningEnabled())
            {
            return resolveConfiguredAlias(sAlias, mapConfiguredAlias, mapCompatAlias, new HashSet<>());
            }

        return resolveLegacyMetadata(sAlias, mapAliasCache, mapPackageAlias, mapCompatAlias, fEnforceAliases, loader);
        }


    /**
     * Resolve only exact server-configured aliases in hardened modes.
     *
     * @param sAlias              the metadata value from JSON
     * @param mapConfiguredAlias  the exact aliases configured by the server
     * @param mapCompatAlias      the configured compatibility aliases
     * @param setVisited          compatibility aliases visited while resolving
     *
     * @return the resolved class
     */
    private static Class<?> resolveConfiguredAlias(String sAlias,
                                                   Map<String, Class<?>> mapConfiguredAlias,
                                                   Map<String, String> mapCompatAlias,
                                                   Set<String> setVisited)
        {
        Class<?> clz = mapConfiguredAlias.get(sAlias);
        if (clz == null)
            {
            String sAliasFor = mapCompatAlias.get(sAlias);
            if (sAliasFor != null && setVisited.add(sAlias))
                {
                clz = resolveConfiguredAlias(sAliasFor, mapConfiguredAlias, mapCompatAlias, setVisited);
                }
            }

        if (clz == null)
            {
            throw new JsonBindingException("JSON @class metadata must use a configured type alias in "
                    + "security hardened mode");
            }

        if (!SerializationGate.isValid(clz))
            {
            throw new JsonBindingException("Unable to de-serialize " + clz.getName());
            }
        return clz;
        }

    /**
     * Preserve the historical Genson resolution path in compatibility mode.
     *
     * @param sAlias           the metadata value from JSON
     * @param mapAliasCache    the mutable Genson alias/cache map
     * @param mapPackageAlias  the configured package aliases
     * @param mapCompatAlias   the configured compatibility aliases
     * @param fEnforceAliases  {@code true} when Genson type alias enforcement is enabled
     * @param loader           the loader to use for fallback
     *
     * @return the resolved class
     *
     * @throws ClassNotFoundException if fallback resolution cannot find the class
     */
    private static Class<?> resolveLegacyMetadata(String sAlias,
                                                  Map<String, Class<?>> mapAliasCache,
                                                  Map<String, String> mapPackageAlias,
                                                  Map<String, String> mapCompatAlias,
                                                  boolean fEnforceAliases,
                                                  ClassLoader loader)
            throws ClassNotFoundException
        {
        Class<?> clz = mapAliasCache.get(sAlias);
        if (clz == null)
            {
            String sAliasFor = mapCompatAlias.get(sAlias);
            if (sAliasFor != null)
                {
                return resolveLegacyMetadata(sAliasFor, mapAliasCache, mapPackageAlias, mapCompatAlias,
                        fEnforceAliases, loader);
                }

            String sResolvedName = resolveLegacyName(sAlias, mapPackageAlias, fEnforceAliases);
            clz = Class.forName(sResolvedName, false, loader);

            mapAliasCache.put(sResolvedName, clz);
            if (!sResolvedName.equals(sAlias))
                {
                mapAliasCache.put(sAlias, clz);
                }
            }
        return clz;
        }

    /**
     * Apply package alias and FQCN compatibility rules.
     *
     * @param sAlias           the metadata value from JSON
     * @param mapPackageAlias  the configured package aliases
     * @param fEnforceAliases  {@code true} when Genson type alias enforcement is enabled
     *
     * @return the class name to load
     */
    private static String resolveLegacyName(String sAlias, Map<String, String> mapPackageAlias, boolean fEnforceAliases)
        {
        int cIdx = sAlias.lastIndexOf('.');
        if (cIdx != -1)
            {
            String sPackageAlias = sAlias.substring(0, cIdx);
            String sPackageToUse = mapPackageAlias.get(sPackageAlias);
            if (sPackageToUse != null)
                {
                return sPackageToUse + sAlias.substring(cIdx);
                }
            else if (fEnforceAliases && !sAlias.startsWith("java.") && !sAlias.startsWith("javax."))
                {
                throw new JsonBindingException(String.format("Unable to find matching type or package alias for %s",
                        sAlias));
                }
            }
        return sAlias;
        }
    }
