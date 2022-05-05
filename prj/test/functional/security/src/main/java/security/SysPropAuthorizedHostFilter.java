/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import com.tangosol.util.Filter;


/**
* Simple {@link Filter} implementation that authorizes or denies new
* members from joining this cluster based on the
* {@link #DENY_ACCESS_PROPERTY} system property.
*
* @author pp  2010.02.15
*/
public class SysPropAuthorizedHostFilter
        implements Filter
    {
    /**
    * Determine if a new host can join a cluster.
    *
    * @param addr  the address of the member attempting to join
    *
    * @return true if the new member is allowed to join, false otherwise.
    */
    public boolean evaluate(Object addr)
        {
        return !Boolean.getBoolean(DENY_ACCESS_PROPERTY);
        }

    /**
    * System property to determine if a new member can join this cluster.
    * Set to "true" to deny new members access to a running cluster.
    */
    public static final String DENY_ACCESS_PROPERTY = "test.authorized.host.deny";
    }
