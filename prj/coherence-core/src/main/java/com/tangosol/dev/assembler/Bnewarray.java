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
* The BNEWARRAY pseudo-op instantiates an array of byte.
* <p><code><pre>
* JASM op         :  BNEWARRAY    (0xf4)
* JVM byte code(s):  NEWARRAY     (0xbc)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Bnewarray extends OpArray implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Bnewarray()
        {
        super(BNEWARRAY);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Bnewarray";
    }
