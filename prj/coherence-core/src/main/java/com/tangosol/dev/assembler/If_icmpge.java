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
* The IF_ICMPGE op branches to the label if the second integer on the stack
* is greater than or equal to the first integer on the stack.
* <p><code><pre>
* JASM op         :  IF_ICMPGE    (0xa2)
* JVM byte code(s):  IF_ICMPGE    (0xa2)
* Details         :
* </pre></code>
*
* @version 0.50, 06/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class If_icmpge extends OpBranch implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param label  the label to branch to
    */
    public If_icmpge(Label label)
        {
        super(IF_ICMPGE, label);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "If_icmpge";
    }
