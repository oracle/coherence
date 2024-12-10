/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.util.ErrorList;


/**
* This class represents the various target statements:  LabelStatement,
* CaseClause, and DefaultClause.
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public abstract class TargetStatement extends Statement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a target statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the first token of the statement
    */
    protected TargetStatement(Statement stmt, Token token)
        {
        super(stmt, token);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "TargetStatement";
    }
