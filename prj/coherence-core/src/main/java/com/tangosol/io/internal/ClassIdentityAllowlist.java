/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.SecurityConfig;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Package allowlist for wire-supplied {@code ClassIdentity} values.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public final class ClassIdentityAllowlist
    {
    /**
     * Return {@code true} if the specified package and class are allowed.
     *
     * @param sPackage  the package name, using slash or dot separators
     * @param sClass    the class name
     *
     * @return {@code true} if the identity is package-allowlisted
     */
    public static boolean isAllowed(String sPackage, String sClass)
        {
        String sPackageName = normalizePackage(sPackage);
        if (sPackageName == null || sPackageName.isEmpty())
            {
            return false;
            }

        mode();

        return matchesPrefix(sPackageName, DEV_BASELINE_PREFIX)
                || registeredPackages().contains(sPackageName);
        }

    /**
     * Set the registered-package provider.
     *
     * @param supplier  the registered-package supplier
     */
    public static void setRegisteredPackageProvider(Supplier<Set<String>> supplier)
        {
        s_registeredProvider.set(supplier == null ? Set::of : supplier);
        }

    /**
     * Reset test hooks.
     */
    static void reset()
        {
        s_registeredProvider.set(ClassIdentityAllowlist::securityConfigPackages);
        s_modeLogged.set(null);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the detected Coherence mode.
     *
     * @return the current mode
     */
    private static String mode()
        {
        String sMode = CoherenceMode.current().name().toLowerCase();
        if (s_modeLogged.compareAndSet(null, sMode))
            {
            Logger.info(String.format("route=class-identity, gate=class-validation, mode=%s", sMode));
            }
        return sMode;
        }

    /**
     * Return the registered packages.
     *
     * @return the registered packages
     */
    private static Set<String> registeredPackages()
        {
        Set<String> setPackages = s_registeredProvider.get().get();
        if (setPackages == null || setPackages.isEmpty())
            {
            return Set.of();
            }

        return setPackages.stream()
                .map(ClassIdentityAllowlist::normalizePackage)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        }

    private static Set<String> securityConfigPackages()
        {
        return SecurityConfig.current().allowedFqns()
                .stream()
                .map(ClassIdentityAllowlist::packageName)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        }

    private static String packageName(String sName)
        {
        if (sName == null)
            {
            return null;
            }

        int of = sName.lastIndexOf('.');
        return of < 0 ? "" : sName.substring(0, of);
        }

    /**
     * Normalize package separators.
     *
     * @param sPackage  the package name
     *
     * @return the normalized package name
     */
    private static String normalizePackage(String sPackage)
        {
        return sPackage == null ? null : sPackage.replace('/', '.');
        }

    /**
     * Return {@code true} if the package matches a baseline prefix.
     *
     * @param sPackage   the package name
     * @param setPrefix  the baseline prefixes
     *
     * @return {@code true} if the package matches
     */
    private static boolean matchesPrefix(String sPackage, Set<String> setPrefix)
        {
        return setPrefix.stream().anyMatch(sPackage::startsWith);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Built-in package prefixes allowed before Slice E contributes registered
     * packages.
     */
    private static final Set<String> DEV_BASELINE_PREFIX = Set.of(
            "java.",
            "javax.",
            "com.tangosol.",
            "com.oracle.coherence.");

    // ----- data members ---------------------------------------------------

    /**
     * Registered package provider. Slice E will replace the empty default.
     */
    private static final AtomicReference<Supplier<Set<String>>> s_registeredProvider =
            new AtomicReference<>(ClassIdentityAllowlist::securityConfigPackages);

    /**
     * Last logged mode.
     */
    private static final AtomicReference<String> s_modeLogged = new AtomicReference<>();

    /**
     * Utility class.
     */
    private ClassIdentityAllowlist()
        {
        }
    }
