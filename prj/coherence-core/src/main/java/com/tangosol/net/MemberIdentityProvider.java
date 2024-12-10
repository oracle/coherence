/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

/**
 * A provider of values for a member's identity.
 *
 * @author Jonathan Knight  2022.05.25
 * @since 22.06
 */
public interface MemberIdentityProvider
    {
    /**
     * Return the name for the Machine (such as a host name) in which
     * this Member resides. This name is used for logging purposes and to
     * differentiate among multiple servers, and may be used as the basis for
     * determining the MachineId property.
     *
     * @return the configured Machine name or {@code null}
     */
    String getMachineName();

    /**
     * Return the name for the Member. This name is used for logging
     * purposes and to differentiate among Members running within a
     * particular process.
     *
     * @return the configured Member name or {@code null}
     */
    String getMemberName();

    /**
     * Return the name for the Rack (such as a physical rack, cage or blade frame)
     * in which this Member resides. This name is used for logging purposes and to
     * differentiate among multiple racks within a particular data center, for
     * example.
     *
     * @return the configured Rack name or {@code null}
     */
    String getRackName();

    /**
     * Return the name for the Site (such as a data center) in which
     * this Member resides. This name is used for logging purposes
     * and to differentiate among multiple geographic sites.
     *
     * @return the configured Site name or {@code null}
     */
    String getSiteName();

    /**
     * Return the role name for the Member. This role is completely
     * definable by the application, and can be used to determine
     * what Members to use for specific purposes.
     *
     * @return the configured role name for the Member or {@code null}
     */
    String getRoleName();

    /**
     * Set the cluster dependencies.
     *
     * @param deps the cluster dependencies
     */
    default void setDependencies(ClusterDependencies deps)
        {
        }

    /**
     * The System property to use to set the name of the identity provider
     * class to use.
     * <p>
     * This will take precedence over any providers discovered by the
     * {@link java.util.ServiceLoader}
     */
    String PROPERTY = "coherence.identity.provider";
    }
