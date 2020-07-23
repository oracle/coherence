/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.examples.rest.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class to hold state codes as reference data.
 *
 * @author tam  2015.07.21
 * @since 12.2.1
 */
@XmlRootElement(name="state")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class State
        extends ReferenceCode
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for State.
     *
     * @param sCode the code to identify the state
     * @param sName the descriptive name of the state
     */
    public State(String sCode, String sName)
        {
        super(sCode, sName);
        }

    /**
     * Default no-args constructor, required for jaxrs access.
     */
    public State()
        {
        super();
        }
    }
