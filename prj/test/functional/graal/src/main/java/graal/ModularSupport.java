/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package graal;

import org.graalvm.polyglot.Context;

/**
 * Add transitive dependency modules needed by this test suite, so that failsafe will add them to the
 * module path.
 */
public abstract class ModularSupport
    {
    /**
     * Adds "requires org.graalvm.sdk;" to module-info
     */
    public static Context s_context;
    }
