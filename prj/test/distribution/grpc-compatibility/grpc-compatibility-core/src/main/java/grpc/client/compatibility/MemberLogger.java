/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client.compatibility;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

public class MemberLogger
        implements RemoteCallable<Void>
    {
    public MemberLogger(String sMsg)
        {
        m_sMsg = sMsg;
        }

    @Override
    public Void call() throws Exception
        {
        System.err.println(m_sMsg);
        System.err.flush();
        return null;
        }

    // ----- data members ---------------------------------------------------

    private final String m_sMsg;
    }
