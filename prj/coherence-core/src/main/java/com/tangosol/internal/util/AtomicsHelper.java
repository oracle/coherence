/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * A helper class providing supplier method to operate on the supplied Atomic variable.
 *
 * @author hr  2021.10.29
 */
public class AtomicsHelper
    {
    public static LongSupplier newIncrementor(AtomicLong atomic)
        {
        return atomic::incrementAndGet;
        }
    }
