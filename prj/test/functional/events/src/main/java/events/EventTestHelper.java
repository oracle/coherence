/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;

import com.tangosol.net.management.MBeanHelper;

import events.common.AbstractTestInterceptor.ErrorAccumulator;
import events.common.AbstractTestInterceptor.ResetAccumulator;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.fail;

/**
 * EventTestHelper supports gathering errors as reported and held on remote
 * processes via the InvocationService.
 *
 * @author hr  2012.10.16
 * @since Coherence 12.1.2
 */
public class EventTestHelper
    {
    /**
     * Interrogate remote nodes for any failures reporting locally.
     *
     * @param sIdentifier   a unique identifier for the EventInterceptor that
     *                      is accumulating error messages
     * @param ccf           the {@link ConfigurableCacheFactory} to use
     * @prama setRecipients  a set of members that should be interrogated
     *
     * @return the number of invocations of the EventInterceptor
     */
    public int remoteFail(String sIdentifier, ConfigurableCacheFactory ccf, Set<Member> setRecipients)
        {
        InvocationService service = (InvocationService) ccf.ensureService(INVOCATION_SERVICE_NAME);

        Map<Member, List> mapErrorCounts = service.query(new ErrorAccumulator(sIdentifier), setRecipients);
        StringBuilder     sbMsg          = new StringBuilder();

        Integer cActual = null;
        for (Map.Entry<Member, List> entry : mapErrorCounts.entrySet())
            {
            List listErrs = entry.getValue();
            cActual = (Integer) listErrs.get(0);
            if (listErrs.size() > 1)
                {
                sbMsg.append(entry.getKey())
                        .append(" had the following assertion errors:\n\t")
                        .append(listErrs);
                }
            }

        if (sbMsg.length() > 0)
            {
            fail(sbMsg.toString());
            }

        return cActual == null ? 0 : cActual.intValue();
        }

    /**
     * Reset invocation counts for remote nodes.
     *
     * @param sIdentifier   a unique identifier for the EventInterceptor that
     *                      is accumulating error messages
     * @param ccf           the {@link ConfigurableCacheFactory} to use
     * @prama setRecipients  a set of members that should be interrogated
     */
    public static void remoteReset(String sIdentifier, ConfigurableCacheFactory ccf, Set<Member> setRecipients)
        {
        InvocationService service = (InvocationService) ccf.ensureService(INVOCATION_SERVICE_NAME);

        service.query(new ResetAccumulator(sIdentifier), setRecipients);
        }

    public static void ensureRemoteService(String sServiceName, ConfigurableCacheFactory ccf, Set<Member> setRecipients)
        {
        InvocationService service = (InvocationService) ccf.ensureService(INVOCATION_SERVICE_NAME);
        service.query(new EnsureServiceInvocable(sServiceName), setRecipients);
        }

    public static class EnsureServiceInvocable
            extends AbstractInvocable
            implements Serializable
        {
        public EnsureServiceInvocable()
            {
            }

        public EnsureServiceInvocable(String sServiceName)
            {
            m_sServiceName = sServiceName;
            }

        @Override
        public void run()
            {
            CacheFactory.getConfigurableCacheFactory().ensureService(m_sServiceName);
            }

        protected String m_sServiceName;
        }

    /**
     * Obtain the number of primary partitions for the given service
     * owned by the given member.
     *
     * @param service  the service for which to check primary partition ownership
     * @param member   the member to check partition ownership for
     *
     * @return the number of primary partitions owned by the given member for
     *         the given service
     */
    public int getOwnedPartitionCount(CacheService service, Member member)
        {
        int c = -1;

        try
            {
            String      sName  = service.getInfo().getServiceName();
            int         nId    = member.getId();
            MBeanServer server = MBeanHelper.findMBeanServer();

            c = (Integer) server.getAttribute(
                    new ObjectName(String.format("Coherence:type=Service,name=%s,nodeId=%s", sName, nId)),
                    "OwnedPartitionsPrimary");

            Logger.finer(String.format("Member %s owns %s partitions", member, c));
            }
        catch (Exception e)
            {
            Logger.err(e);
            }

        return c;
        }

    /**
     * The name of the InvocationService used by all test methods.
     */
    public static String INVOCATION_SERVICE_NAME = "InvocationService";
    }
