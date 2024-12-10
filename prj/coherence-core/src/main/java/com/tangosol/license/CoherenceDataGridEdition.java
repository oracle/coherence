/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.license;


import com.tangosol.net.CacheFactory;


/**
* This class is a license class.
*
* WARNING:  DO NOT MODIFY OR RECOMPILE THIS CLASS!
*
* @since Coherence 3.2
* @author cp  2006.08.29
*/
public class CoherenceDataGridEdition
        extends LicensedObject
    {
    /**
    * @return a description of the LicensedObject
    */
    public String toString()
        {
        return NAME;
        }

    /**
    * Print the product license information.
    */
    static public void printLicense()
        {
        printLicense(CoherenceDataGridEdition.class, NAME);
        }

    /**
    * The product name.
    */
    private static final String NAME = CacheFactory.PRODUCT + ": Grid Edition";
    }
