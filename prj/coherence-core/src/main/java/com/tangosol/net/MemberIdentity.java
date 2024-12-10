/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;


import com.tangosol.util.Base;
import com.tangosol.util.NullImplementation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * The MemberIdentity interface represents the identity of a cluster member.
 *
 * @author pfm 2011.05.12
 * @since Coherence 3.7.1
 */
public interface MemberIdentity
    {
    /**
     * Return the name of the cluster with which this member is associated.
     *
     * @return the cluster name
     */
    public String getClusterName();

    /**
     * Return the Member's machine Id.
     *
     * This identifier should be the same for Members that are on the same
     * physical machine, and ideally different for Members that are on different
     * physical machines.
     *
     * @return the Member's machine Id
     */
    public int getMachineId();

    /**
     * Return the configured name for the Machine (such as a host name) in which
     * this Member resides. This name is used for logging purposes and to
     * differentiate among multiple servers, and may be used as the basis for
     * determining the MachineId property.
     *
     * @return the configured Machine name or null
     */
    public String getMachineName();

    /**
     * Return the configured name for the Member. This name is used for logging
     * purposes and to differentiate among Members running within a particular
     * process.
     *
     * @return the configured Member name or null
     */
    public String getMemberName();

    /**
     * Return the priority (or "weight") of the local Member.
     *
     * @return the member priority
     */
    public int getPriority();

    /**
     * Return the configured name for the Process (such as a JVM) in which this
     * Member resides. This name is used for logging purposes and to
     * differentiate among multiple processes on a a single machine.
     *
     * @return the configured Process name or null
     */
    public String getProcessName();

    /**
     * Return the configured name for the Rack (such as a physical rack, cage or
     * blade frame) in which this Member resides. This name is used for logging
     * purposes and to differentiate among multiple racks within a particular
     * data center, for example.
     *
     * @return the configured Rack name or null
     */
    public String getRackName();

    /**
     * Return the configured name for the Site (such as a data center) in which
     * this Member resides. This name is used for logging purposes and to
     * differentiate among multiple geographic sites.
     *
     * @return the configured Site name or null
     */
    public String getSiteName();

    /**
     * Return the configured role name for the Member. This role is completely
     * definable by the application, and can be used to determine what Members
     * to use for specific purposes.
     *
     * @return the configured role name for the Member or null
     */
    public String getRoleName();

    /**
     * Return a set of role names for the Member. The role names are
     * parsed from a comma-delimited role name definable by the application.
     *
     * @return the set of role names for the Member
     *
     * @since Coherence 12.2.3
     */
    public default Set<String> getRoles()
        {
        String sRole = getRoleName();
        return sRole == null ? NullImplementation.getSet() :
            sRole.indexOf(',') < 0 ? Collections.singleton(sRole) :
            new HashSet<>(Arrays.asList(Base.parseDelimitedString(sRole, ',')));
        }


    // ----- constants ------------------------------------------------------

    /**
     * The member identity limit used to restrict the size of various fields.
     */
    // the chosen number is sufficient to accommodate the minimal limit on host names
    // http://en.wikipedia.org/wiki/Hostname rfc1123
    // and long enough to hold a UUID in string form which could serve as a mechanism
    // to generate truly unique cluster names
    public static final int MEMBER_IDENTITY_LIMIT = 66;
    }
