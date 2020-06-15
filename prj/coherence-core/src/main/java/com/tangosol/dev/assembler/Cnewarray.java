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
* The CNEWARRAY pseudo-op instantiates an array of char.
* <p><code><pre>
* JASM op         :  CNEWARRAY    (0xf5)
* JVM byte code(s):  NEWARRAY     (0xbc)
* Details         :
* </pre></code>
*
* @version 0.50, 06/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Cnewarray extends OpArray implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Cnewarray()
        {
        super(CNEWARRAY);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Cnewarray";
    }
