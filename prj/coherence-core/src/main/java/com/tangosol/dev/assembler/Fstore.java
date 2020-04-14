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
* The FSTORE variable-size op stores a float variable.
* <p><code><pre>
* JASM op         :  FSTORE    (0x38)
* JVM byte code(s):  FSTORE    (0x38)
*                    FSTORE_0  (0x43)
*                    FSTORE_1  (0x44)
*                    FSTORE_2  (0x45)
*                    FSTORE_3  (0x46)
* Details         :
* </pre></code>
*
* @version 0.50, 06/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Fstore extends OpStore implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param var  the variable to pop
    */
    public Fstore(Fvar var)
        {
        super(FSTORE, var);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Fstore";
    }
