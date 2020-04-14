/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.assembler;

/**
* BootstrapMethodConstant is a convenience subclass of {@link MethodConstant}
* allowing the omission of the well known bootstrap method signature.
* <p>
* Bootstrap methods are permitted to have wider typed arguments or even be a
* variadic method however BootstrapMethodConstant assumes narrower types.
*
* @author hr  2012.08.06
*/
public class BootstrapMethodConstant
        extends MethodConstant
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Constructs a MethodConstant with the common type required by an
    * invokedynamic bootstrap method.
    *
    * @param sClass
    * @param sName
    */
    public BootstrapMethodConstant(String sClass, String sName)
        {
        super(sClass, sName, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");
        }
    }
