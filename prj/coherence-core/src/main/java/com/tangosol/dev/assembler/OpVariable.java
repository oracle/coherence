/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


/**
* The OpVariable interface is implemented by ops which reference a variable.
*
* @version 0.50, 06/26/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public interface OpVariable
    {
    /**
    * Determine the variable referenced or affected by this op.
    *
    * @return the variable
    */
    public OpDeclare getVariable();
    }
