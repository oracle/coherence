/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import com.tangosol.util.AssertionException;

import static com.oracle.coherence.common.base.Logger.err;
import static com.oracle.coherence.common.base.StackTrace.getExpression;
import static com.oracle.coherence.common.base.StackTrace.getStackTrace;

/**
 * Class for providing assertion functionality.
 *
 * @author cp  2000.08.02
 * @since Coherence 14.1.2
 */

public abstract class Assertions
    {
    // ----- assertion support ----------------------------------------------

    /**
     * Definite assertion failure.
     */
    public static RuntimeException azzert()
        {
        azzert(false, "Assertion:  Unexpected execution of code at:");
        return null;
        }

    /**
     * Test an assertion.
     */
    public static void azzert(boolean f)
        {
        if (!f)
            {
            azzertFailed(null);
            }
        }

    /**
     * Test an assertion, and print the specified message if the assertion
     * fails.
     */
    public static void azzert(boolean f, String s)
        {
        if (!f)
            {
            azzertFailed(s);
            }
        }

    /**
     * Throw an assertion exception.
     *
     * @param sMsg  the assertion message
     */
    public static void azzertFailed(String sMsg)
        {
        if (sMsg == null)
            {
            // default assertion message
            sMsg = "Assertion failed:";

            // try to load the source to print out the exact assertion
            String sSource = getExpression("azzert");
            if (sSource != null)
                {
                sMsg += "  " + sSource;
                }
            }

        // display the assertion
        err(sMsg);

        // display the code that caused the assertion
        String sStack = getStackTrace();
        err(sStack.substring(sStack.indexOf('\n', sStack.lastIndexOf(".azzert(")) + 1));

        throw new AssertionException(sMsg);
        }
    }
