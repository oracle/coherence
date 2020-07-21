/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.rest.model;

import com.oracle.coherence.examples.pof.ContactId;
import com.tangosol.coherence.rest.KeyConverter;

import java.util.StringTokenizer;

/**
 * Implementation of {@link KeyConverter} for using {@link ContactId} composite
 * key via REST.
 *
 * @author tam  2015.07.10
 * @since 12.2.1
 */
public class ContactIdKeyConverter
        implements KeyConverter
    {

    // ----- KeyConverter methods -------------------------------------------

    @Override
    public Object fromString(String sKey)
        {
        StringTokenizer st         = new StringTokenizer(sKey, TOKEN);
        String          sFirstName = st.hasMoreTokens() ? st.nextToken() : null;
        String          sLastName  = st.hasMoreTokens() ? st.nextToken() : null;
        if (sFirstName == null || sLastName == null)
            {
            throw new IllegalArgumentException("Invalid string " + sKey);
            }

        return new ContactId(sFirstName, sLastName);
        }

    @Override
    public String toString(Object oKey)
        {
        if (oKey instanceof ContactId)
            {
            ContactId key = (ContactId) oKey;
            return key.getFirstName() + TOKEN +
                   key.getLastName();
            }
        throw new IllegalArgumentException("Must supply ContactId class");
        }

    // ----- constants ------------------------------------------------------

    /**
     * Separator for first name and last name.
     */
    private static final String TOKEN = "_";
    }
