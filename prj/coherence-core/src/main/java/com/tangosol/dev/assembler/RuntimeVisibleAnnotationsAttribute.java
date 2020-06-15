/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


/**
* Represents a Java Virtual Machine "RuntimeVisibleAnnotations" attribute
* 
*
* <p>
* The RuntimeVisibleAnnotations Attribute is defined by the JDK 1.5
* documentation as:
* <p>
* <code><pre>
*   RuntimeVisibleAnnotations_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length;
*       u2 num_annotations;
*       annotation annotations[num_annotations]
*       }
* </pre></code>
*
* @author rhl 2008.09.23
*/
public class RuntimeVisibleAnnotationsAttribute
        extends AbstractAnnotationsAttribute
        implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a runtime visible annotations attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected RuntimeVisibleAnnotationsAttribute(VMStructure context)
        {
        super(context, ATTR_RTVISANNOT);
        }
    }
