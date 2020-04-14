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

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;


/**
* Represents a Java Virtual Machine byte-code "LocalVariableTable" attribute
* which cross-references between byte-code variable references by index and
* source file variable references by name; also includes scope information
* of which portion of the code the variable "exists" in.
*
* @version 0.50, 05/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class LocalVariableTableAttribute
    extends AbstractLocalVariableTableAttribute implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a LocalVariableTable attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected LocalVariableTableAttribute(VMStructure context)
        {
        super(context, ATTR_VARIABLES);
        }
    }
