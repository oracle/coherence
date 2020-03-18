/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


/**
* Represents a Java Virtual Machine "RuntimeVisibleTypeAnnotations" attribute
*
*
* <p>
* The RuntimeVisibleTypeAnnotations Attribute is defined by the JDK 1.8
* documentation as:
* <p>
* <code><pre>
*   RuntimeVisibleTypeAnnotations_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length;
*       u2 num_annotations;
*       type_annotation annotations[num_annotations]
*       }
* </pre></code>
*
* @author hr 2014.05.28
*/
public class RuntimeVisibleTypeAnnotationsAttribute
        extends AbstractAnnotationsAttribute
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a runtime visible type annotations attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected RuntimeVisibleTypeAnnotationsAttribute(VMStructure context)
        {
        super(context, ATTR_RTVISTANNOT);
        }


    // ----- AbstractAnnotationsAttribute operations ------------------------

    @Override
    protected Annotation instantiateAnnotation()
        {
        return new TypeAnnotation();
        }
    }
