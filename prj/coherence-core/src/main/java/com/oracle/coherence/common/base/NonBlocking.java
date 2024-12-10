/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

/**
 * NonBlocking allows a thread to mark itself as non-blocking and should be exempt from things such as
 * flow-control pauses using a try-with-resource pattern.
 *
 * <pre>
 * try (NonBlocking e = new NonBlocking())
 *     {
 *     // NonBlocking.isNonBlockingCaller() will now be true, and FlowControlled elements should respect this and not block
 *     }
 * // NonBlocking.isNonBlockingCaller() will have been restored to its former value
 * </pre>
 */
public class NonBlocking
    implements AutoCloseable
    {
    public NonBlocking()
        {
        if (!f_fPriorValue)
            {
            s_fNonBlocking.set(Boolean.TRUE);
            }
        // else; prior is true, and we can only set to true no action is necessary
        }

    @Override
    public void close()
        {
        if (!f_fPriorValue)
            {
            s_fNonBlocking.remove();
            }
        // else; prior is true, and we can only set to true no action is necessary
        }

    /**
     * Return true if the calling thread has been marked as non-blocking
     *
     * @return true iff the calling thread is marked as non-blocking
     */
    public static boolean isNonBlockingCaller()
        {
        return s_fNonBlocking.get() == Boolean.TRUE;
        }

    /**
     * The prior value.
     */
    private final boolean f_fPriorValue = isNonBlockingCaller();

    /**
     * ThreadLocal tracking the calling thread's non-blocking status.
     */
    private static final ThreadLocal<Boolean> s_fNonBlocking = new ThreadLocal<>();
    }
