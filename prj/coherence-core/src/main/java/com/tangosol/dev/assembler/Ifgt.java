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
* The IFGT op branches to the label if the top integer on the stack is
* greater than zero.
* <p><code><pre>
* JASM op         :  IFGT    (0x9d)
* JVM byte code(s):  IFGT    (0x9d)
* Details         :
* </pre></code>
*
* @version 0.50, 06/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Ifgt extends OpBranch implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param label  the label to branch to
    */
    public Ifgt(Label label)
        {
        super(IFGT, label);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Ifgt";
    }
