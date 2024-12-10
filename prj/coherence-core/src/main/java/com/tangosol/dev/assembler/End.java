/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;


/**
* The END pseudo op closes a variable scope.
* <p><code><pre>
* JASM op         :  END (0xeb)
* JVM byte code(s):  n/a
* Details         :
* </pre></code>
*
* @version 0.50, 06/17/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class End extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public End()
        {
        super(END);
        }


    // ----- Op operations --------------------------------------------------

    /**
    * Determine if the op is discardable.  Begin and End ops are never
    * considered discardable since they may come before labels and after
    * returns/unconditional branches and since they do not affect execution.
    *
    * @return false always
    */
    protected boolean isDiscardable()
        {
        return false;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "End";
    }
