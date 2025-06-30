/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.oracle.coherence.grpc.messages.proxy.v1.ClientMemberIdentity;
import com.tangosol.net.MemberIdentity;

/**
 * A {@link MemberIdentity} implementation that wraps a
 * gRPC {@link ClientMemberIdentity}.
 *
 * @author Jonathan Knight  2025.06.27
 */
public class WrapperMemberIdentity
        implements MemberIdentity
    {
    public WrapperMemberIdentity(ClientMemberIdentity identity)
        {
        f_identity = identity;
        }

    @Override
    public String getClusterName()
        {
        return f_identity == null ? null : f_identity.getClusterName();
        }

    @Override
    public int getMachineId()
        {
        return f_identity == null ? 0 : f_identity.getMachineId();
        }

    @Override
    public String getMachineName()
        {
        return f_identity == null ? null : f_identity.getMachineName();
        }

    @Override
    public String getMemberName()
        {
        return f_identity == null ? null : f_identity.getMemberName();
        }

    @Override
    public int getPriority()
        {
        return f_identity == null ? 0 : f_identity.getPriority();
        }

    @Override
    public String getProcessName()
        {
        return f_identity == null ? null : f_identity.getProcessName();
        }

    @Override
    public String getRackName()
        {
        return f_identity == null ? null : f_identity.getRackName();
        }

    @Override
    public String getSiteName()
        {
        return f_identity == null ? null : f_identity.getSiteName();
        }

    @Override
    public String getRoleName()
        {
        return f_identity == null ? null : f_identity.getRoleName();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped protobuf client identity.
     */
    private final ClientMemberIdentity f_identity;
    }
