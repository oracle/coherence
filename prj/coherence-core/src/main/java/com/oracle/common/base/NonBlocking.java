/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

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
 *
 * @deprecated use {@link com.oracle.coherence.common.base.NonBlocking} instead
 */
@Deprecated
public class NonBlocking
        extends com.oracle.coherence.common.base.NonBlocking
    {
    /**
     * Return true if the the calling thread has been marked as non-blocking
     *
     * @return true iff the calling thread is marked as non-blocking
     *
     * @deprecated use {@link com.oracle.coherence.common.base.NonBlocking#isNonBlockingCaller()}
     *             instead
     */
    public static boolean isNonBlockingCaller()
        {
        return com.oracle.coherence.common.base.NonBlocking.isNonBlockingCaller();
        }
    }
