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
* The LOOKUPSWITCH op is a binary searchable list of values with associated
* jumps with an additional default jump.
* <p><code><pre>
* JASM op         :  LOOKUPSWITCH   (0xab)
* JVM byte code(s):  LOOKUPSWITCH   (0xab)
* Details         :
* </pre></code>
*
* @version 0.50, 06/17/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Lookupswitch extends OpSwitch implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param label  the default label to branch to if no cases match
    */
    public Lookupswitch(Label label)
        {
        super(LOOKUPSWITCH, label);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Lookupswitch";
    }
