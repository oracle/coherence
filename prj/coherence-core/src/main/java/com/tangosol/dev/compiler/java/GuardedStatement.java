/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Label;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* This abstract class is the basis for both the try and synchronized
* statements.
*
* @version 1.00, 09/16/98
* @author  Cameron Purdy
*/
public abstract class GuardedStatement extends Statement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a guarded Java code block.
    *
    * @param outer  the enclosing Java statement
    * @param token  the first token of the statement
    */
    protected GuardedStatement(Statement outer, Token token)
        {
        super(outer, token);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the unwind label.
    *
    * @return  the label of the finally implementation (or null if none)
    */
    public Label getUnwindLabel()
        {
        return lblUnwind;
        }

    /**
    * Set the unwind label.
    *
    * @param lbl  the label of the finally implementation
    */
    protected void setUnwindLabel(Label lbl)
        {
        lblUnwind = lbl;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "GuardedStatement";

    /**
    * The "unwind" label.  (The finally implementation.)
    */
    private Label lblUnwind;
    }
