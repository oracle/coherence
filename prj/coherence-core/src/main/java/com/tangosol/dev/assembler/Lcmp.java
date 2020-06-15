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
* The LCMP simple op compares the two longs on the top of the stack.
* <p><code><pre>
* JASM op         :  LCMP  (0x94)
* JVM byte code(s):  LCMP  (0x94)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Lcmp extends Op implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Lcmp()
        {
        super(LCMP);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Lcmp";
    }
