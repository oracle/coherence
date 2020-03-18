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
* The IF_ACMPEQ op branches to the label if the top two references on the
* stack are equal.
* <p><code><pre>
* JASM op         :  IF_ACMPEQ    (0xa5)
* JVM byte code(s):  IF_ACMPEQ    (0xa5)
* Details         :
* </pre></code>
*
* @version 0.50, 06/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class If_acmpeq extends OpBranch implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param label  the label to branch to
    */
    public If_acmpeq(Label label)
        {
        super(IF_ACMPEQ, label);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "If_acmpeq";
    }
