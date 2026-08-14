/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
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
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class CapturingAuthorizer
        implements StorageAccessAuthorizer
    {
    @Override
    public void checkRead(BinaryEntry entry, Subject subject, int nReason)
        {
        record(entry, subject, nReason, "read");
        }

    @Override
    public void checkWrite(BinaryEntry entry, Subject subject, int nReason)
        {
        record(entry, subject, nReason, "write");
        }

    @Override
    public void checkReadAny(BackingMapContext context, Subject subject, int nReason)
        {
        record(context, subject, nReason, "readAny");
        }

    @Override
    public void checkWriteAny(BackingMapContext context, Subject subject, int nReason)
        {
        record(context, subject, nReason, "writeAny");
        }

    public static Subject getSubject(Binary binary)
        {
        return s_map.get(binary);
        }

    public static void clear()
        {
        s_map.clear();
        s_listEvents.clear();
        s_setDeniedReasons.clear();
        }

    public static List<Event> getEvents()
        {
        return new ArrayList<>(s_listEvents);
        }

    public static void setDeniedReasons(Set<Integer> setReasons)
        {
        s_setDeniedReasons.clear();
        s_setDeniedReasons.addAll(setReasons);
        }

    protected void record(BinaryEntry entry, Subject subject, int nReason, String sOperation)
        {
        Binary binary = entry.getBinaryKey();
        if (subject == null)
            {
            s_map.remove(binary);
            }
        else
            {
            s_map.put(binary, subject);
            }

        String sCacheName = entry.getBackingMapContext() == null ? null : entry.getBackingMapContext().getCacheName();
        Object oKey = entry.getKey();

        record(new Event(sOperation, nReason, sCacheName, entry.getClass().getName(), oKey, subject));
        }

    protected void record(BackingMapContext context, Subject subject, int nReason, String sOperation)
        {
        String sCacheName = context == null ? null : context.getCacheName();
        String sClassName = context == null ? null : context.getClass().getName();

        record(new Event(sOperation, nReason, sCacheName, sClassName, null, subject));
        }

    protected void record(Event event)
        {
        if (event.getCacheName() != null && event.getCacheName().startsWith("peer-proof-perf-"))
            {
            return;
            }
        s_listEvents.add(event);

        if (s_setDeniedReasons.contains(event.getReason()))
            {
            throw new SecurityException("access denied for " + StorageAccessAuthorizer.reasonToString(event.getReason()));
            }
        }

    // ----- inner class: Event ----------------------------------------------

    public static class Event
            implements Serializable
        {
        public Event(String sOperation, int nReason, String sCacheName, String sOperationClass, Object oKey, Subject subject)
            {
            m_sOperation      = sOperation;
            m_nReason         = nReason;
            m_sReason         = StorageAccessAuthorizer.reasonToString(nReason);
            m_sCacheName      = sCacheName;
            m_sOperationClass = sOperationClass;
            m_oKey            = oKey;
            m_setPrincipals   = subject == null
                    ? null
                    : subject.getPrincipals().stream().map(Principal::getName).collect(Collectors.toCollection(HashSet::new));
            }

        public String getOperation()
            {
            return m_sOperation;
            }

        public int getReason()
            {
            return m_nReason;
            }

        public String getReasonString()
            {
            return m_sReason;
            }

        public String getCacheName()
            {
            return m_sCacheName;
            }

        public String getOperationClass()
            {
            return m_sOperationClass;
            }

        public Object getKey()
            {
            return m_oKey;
            }

        public Set<String> getPrincipals()
            {
            return m_setPrincipals;
            }

        private final String      m_sOperation;

        private final int         m_nReason;

        private final String      m_sReason;

        private final String      m_sCacheName;

        private final String      m_sOperationClass;

        private final Object      m_oKey;

        private final Set<String> m_setPrincipals;
        }

    // ----- data members ---------------------------------------------------

    private static final Map<Binary, Subject> s_map = new ConcurrentHashMap<>();

    private static final List<Event> s_listEvents = new CopyOnWriteArrayList<>();

    private static final Set<Integer> s_setDeniedReasons = ConcurrentHashMap.newKeySet();
    }
