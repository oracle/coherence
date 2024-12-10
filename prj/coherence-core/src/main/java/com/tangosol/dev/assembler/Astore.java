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
* The ASTORE variable-size op stores a reference variable.
* <p><code><pre>
* JASM op         :  ASTORE    (0x3a)
* JVM byte code(s):  ASTORE    (0x3a)
*                    ASTORE_0  (0x4b)
*                    ASTORE_1  (0x4c)
*                    ASTORE_2  (0x4d)
*                    ASTORE_3  (0x4e)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Astore extends OpStore implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var  the variable to pop
    */
    public Astore(Avar var)
        {
        super(ASTORE, var);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Astore";
    }
