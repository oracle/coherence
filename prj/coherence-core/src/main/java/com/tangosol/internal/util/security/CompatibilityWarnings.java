/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Once-per-category guard for security hardening compatibility warnings.
 * <p>
 * Callers must use this guard only for an action permitted by compatibility
 * policy, never for an actual rejection. Telemetry and validation must remain
 * outside the guarded logging statement. Each caller declares fixed category
 * objects in its owning area, normally as static constants, and shares them
 * across routes. There is no global registry or retention of request values.
 *
 * @author phf  2026.10.09
 * @since 26.10
 */
public final class CompatibilityWarnings
    {
    // ----- constructors ---------------------------------------------------

    private CompatibilityWarnings()
        {
        }

    // ----- logging guard --------------------------------------------------

    /**
     * Return whether a compatibility warning should be logged.
     * <p>
     * The decision is atomic and lasts for the lifetime of the category object.
     * A {@code null} category remains unsuppressed, allowing callers to keep
     * unknown categories visible without retaining application-supplied values.
     *
     * @param category  the caller-owned fixed warning category, or {@code null}
     *
     * @return {@code true} for the first occurrence or a {@code null} category;
     *         {@code false} for a repeated occurrence of the same category
     */
    public static boolean shouldLog(Category category)
        {
        return category == null || category.f_logged.compareAndSet(false, true);
        }

    // ----- inner class: Category ------------------------------------------

    /**
     * A caller-owned compatibility warning category, without diagnostic values.
     *
     * @since 26.10
     */
    public static final class Category
        {
        /**
         * Construct an independent warning category.
         */
        public Category()
            {
            }

        /**
         * Whether this category has already permitted a warning.
         */
        private final AtomicBoolean f_logged = new AtomicBoolean();
        }
    }
