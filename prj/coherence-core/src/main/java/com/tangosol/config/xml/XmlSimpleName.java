/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that a class is associated with the processing of a 
 * specifically named Xml element or attribute.
 * <p>
 * A <i>simple name</i>, also often called <i>local name</i>, is the part of 
 * an Xml element or attribute name without the specified Xml namespace.  
 * <p>
 * For example: The simple name of the Xml element &lt;example:h1&gt; is "h1".  
 * The simple name of the Xml element &lt;example&gt; is "example". 
 * <p>
 * Typically an {@link XmlSimpleName} annotation is used to identify the 
 * Xml elements/attributes that {@link ElementProcessor}s/{@link AttributeProcessor}s
 * can process with in a {@link NamespaceHandler}. 
 * 
 * @author bo  2012.09.14
 * @since Coherence 12.1.2
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface XmlSimpleName
    {
    /**
     * The simple name of the Xml element or attribute being associated
     * with the class for processing.
     *
     * @return  the simple name of an Xml element or attribute
     */
    String value();
    }
