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
* The TABLESWITCH op is a bounded jump table with a default jump.
* <p><code><pre>
* JASM op         :  TABLESWITCH   (0xaa)
* JVM byte code(s):  TABLESWITCH   (0xaa)
* Details         :
* </pre></code>
*
* @version 0.50, 06/17/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Tableswitch extends OpSwitch implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param label  the default label to branch to if no cases match
    */
    public Tableswitch(Label label)
        {
        super(TABLESWITCH, label);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Tableswitch";
    }
