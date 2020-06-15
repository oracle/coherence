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
* The GOTO op branches unconditionally to the label.
* <p><code><pre>
* JASM op         :  GOTO    (0xa7)
* JVM byte code(s):  GOTO    (0xa7)
*                    GOTO_W  (0xc8)
* Details         :  The GOTO_W byte code is currently not produced by the
*                    assembler.
* </pre></code>
*
* @version 0.50, 06/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Goto extends OpBranch implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param label  the label to branch to
    */
    public Goto(Label label)
        {
        super(GOTO, label);
        }


    // -----

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        return getName() + ' ' + getLabel().getOffset();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Goto";
    }
