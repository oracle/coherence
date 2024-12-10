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
* Represents a Java Virtual Machine "Deprecated" attribute which can apply
* to a class, a field, or a method.
* <p>
* The Deprecated Attribute is undocumented.  Its structure appears to be:
* <p>
* <code><pre>
*   Deprecated_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length; (=0)
*       }
* </pre></code>
*
* @version 0.50, 05/18/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class DeprecatedAttribute extends Attribute implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a Deprecated attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected DeprecatedAttribute(VMStructure context)
        {
        super(context, ATTR_DEPRECATED);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "DeprecatedAttribute";
    }
