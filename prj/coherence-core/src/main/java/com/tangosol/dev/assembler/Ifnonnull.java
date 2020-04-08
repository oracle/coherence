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
* The IFNONNULL op branches to the label if the top reference on the stack is
* not null.
* <p><code><pre>
* JASM op         :  IFNONNULL    (0xc7)
* JVM byte code(s):  IFNONNULL    (0xc7)
* Details         :
* </pre></code>
*
* @version 0.50, 06/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Ifnonnull extends OpBranch implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param label  the label to branch to
    */
    public Ifnonnull(Label label)
        {
        super(IFNONNULL, label);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Ifnonnull";
    }
