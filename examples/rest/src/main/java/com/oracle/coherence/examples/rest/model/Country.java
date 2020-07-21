/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.rest.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class to hold country codes as reference data.
 *
 * @author tam  2015.07.21
 * @since 12.2.1
 */
@XmlRootElement(name="country")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Country
        extends ReferenceCode
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for Country.
     *
     * @param sCode the code to identify the country
     * @param sName the descriptive name of the country
     */
    public Country(String sCode, String sName)
        {
        super(sCode, sName);
        }

    /**
     * Default no-args constructor, required for jaxrs access.
     */
    public Country()
        {
        super();
        }
    }
