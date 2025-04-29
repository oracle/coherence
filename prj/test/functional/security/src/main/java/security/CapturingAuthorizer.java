/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.security.StorageAccessAuthorizer;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;

import javax.security.auth.Subject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CapturingAuthorizer
        implements StorageAccessAuthorizer
    {
    @Override
    public void checkRead(BinaryEntry entry, Subject subject, int nReason)
        {
        s_map.put(entry.getBinaryKey(), subject);
        }

    @Override
    public void checkWrite(BinaryEntry entry, Subject subject, int nReason)
        {
        s_map.put(entry.getBinaryKey(), subject);
        }

    @Override
    public void checkReadAny(BackingMapContext context, Subject subject, int nReason)
        {
        }

    @Override
    public void checkWriteAny(BackingMapContext context, Subject subject, int nReason)
        {
        }

    public static Subject getSubject(Binary binary)
        {
        return s_map.get(binary);
        }

    // ----- data members ---------------------------------------------------

    private static final Map<Binary, Subject> s_map = new ConcurrentHashMap<>();
    }
