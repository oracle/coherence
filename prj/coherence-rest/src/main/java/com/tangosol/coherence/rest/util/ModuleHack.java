/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import jakarta.annotation.Priority;

/**
 * Workaround to ensure Jakarta Annotations are on the module path.
 *
 * @since 22.09
 */
@SuppressWarnings("unused")
class ModuleHack
    {
    private static final Class<Priority> PRIORITY_CLASS = Priority.class;
    }
