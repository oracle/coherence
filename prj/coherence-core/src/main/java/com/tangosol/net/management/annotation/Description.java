/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
* The Description annotation allows a description to be specified for a method
* on an MBean, and for an MBean interface itself. The Descriptions are accessed
* at runtime when describing the MBean for the MBeanServer.
* <p>
* This annotation enables developers to use the "Standard MBean" pattern and
* still provide a description for JMX monitoring tools to display. The annotation
* is applicable for operations and attributes as well as the MBean itself.
* <p>
* The Description annotation is designed based on the expected @Description
* annotation in JSR 255.
* <p>
* Usage:
* <p>
* The @Description annotation is applied to the methods in the MBean interface:
* <pre>
* |   @Description("The maximum allowable size, in bytes, of a Binary value.")
* |   int  getMaxValueSize();
* </pre>
*
* as well as for the MBean interface as a whole:
* <pre>
* |   @Description("The RamJournalRMMBean provides RamJournal specific metrics")
* |   public interface RamJournalRMMBean
* </pre>
*
* Note: For attributes, a description is only required for one of the "get" or
* "set" methods. If descriptions are provided for both methods, either
* description could be used.
*
* @author cf 2010-11-28
* @since Coherence 3.7
*/

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})

public @interface Description
    {
    /**
    * Returns the value of the annotation.
    * <p>
    * To allow short form assignments of single member annotations, "value" must
    * be used (Java convention).
    *
    * @return the value of the annotation
    */
    public String value();
    }
