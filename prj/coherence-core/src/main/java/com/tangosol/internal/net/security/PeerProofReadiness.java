/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheService;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.ServiceInfo;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.util.ExternalizableHelper;

import java.io.Serializable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Supported topology-bound all-member query for one PEER key rotation.
 *
 * <p>The invocation service is only the observation transport.  The query
 * captures the complete live cluster plus every running proof-relevant Grid
 * service before and after collection.  Each response is bound to the actual
 * responding member and that exact cluster, role, service-membership, and
 * service-senior epoch.  Subject producers are derived from service
 * membership, while subject recipients are independently derived from the
 * authoritative ownership-enabled membership reported consistently by every
 * participant in each partitioned/federated service.  Senior producers are
 * every relevant service senior and senior recipients are derived
 * independently from those services' physical membership.</p>
 *
 * @author Aleks Seovic  2026.07.16
 * @since 26.1.0.0
 */
public final class PeerProofReadiness
    {
    private PeerProofReadiness()
        {
        }

    /** Proof authority role. */
    public enum Role {SUBJECT, SENIOR}

    /** Requested rotation stage. */
    public enum Stage {ADD, ACTIVATE, OVERLAP, RETIRE}

    /**
     * Record an authoritative local cluster, Grid-service, senior, producer,
     * or ownership-recipient transition.  Production transition sources call
     * this before publishing their corresponding member/configuration event.
     */
    public static void recordTopologyChange()
        {
        TOPOLOGY_GENERATION.updateAndGet(value -> value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L);
        }

    /** Return the process-local monotonic topology generation. */
    static long getTopologyGeneration()
        {
        return TOPOLOGY_GENERATION.get();
        }

    /**
     * Execute a production all-member query over an invocation service that
     * runs on every live cluster member.
     *
     * @param cluster  the live cluster
     * @param service  an invocation service present on every live member
     * @param query    the role, stage, and one producer key transition
     *
     * @return a bounded, secret-free result
     */
    @SuppressWarnings("unchecked")
    public static Result query(Cluster cluster, InvocationService service, Query query)
        {
        String sInvalid = validateQuery(query);
        if (!sInvalid.isEmpty())
            {
            return Result.unavailable(sInvalid);
            }
        if (cluster == null || service == null)
            {
            return Result.unavailable("readiness-service-unavailable");
            }

        Topology before;
        try
            {
            before = Topology.capture(cluster, service);
            }
        catch (RuntimeException e)
            {
            return Result.unavailable("topology-unavailable");
            }
        if (!before.f_sFailure.isEmpty())
            {
            return Result.unavailable(before.f_sFailure);
            }

        Map<MemberBinding, Response> mapFirst = collect(service, before);
        if (mapFirst == null)
            {
            return Result.unavailable("observation-collection-failed");
            }
        Topology first = before.withSubjectRecipients(mapFirst);
        if (!first.f_sFailure.isEmpty())
            {
            return Result.unavailable(first.f_sFailure);
            }

        Topology middle;
        try
            {
            middle = Topology.capture(cluster, service);
            }
        catch (RuntimeException e)
            {
            return Result.unavailable("topology-changed");
            }
        if (!before.sameEpoch(middle))
            {
            return Result.unavailable("topology-changed");
            }

        Map<MemberBinding, Response> mapSecond = collect(service, middle);
        if (mapSecond == null)
            {
            return Result.unavailable("observation-collection-failed");
            }
        Topology second = middle.withSubjectRecipients(mapSecond);
        if (!second.f_sFailure.isEmpty())
            {
            return Result.unavailable(second.f_sFailure);
            }

        Topology after;
        try
            {
            after = Topology.capture(cluster, service);
            }
        catch (RuntimeException e)
            {
            return Result.unavailable("topology-changed");
            }
        if (!middle.sameEpoch(after) || !first.sameEpoch(second))
            {
            return Result.unavailable("topology-changed");
            }
        if (!sameTopologyGenerations(mapFirst, mapSecond))
            {
            return Result.unavailable("topology-changed");
            }
        return evaluate(query, second, bindEpoch(mapSecond, second.f_sEpoch), cluster.getTimeMillis());
        }

    private static boolean sameTopologyGenerations(Map<MemberBinding, Response> first,
            Map<MemberBinding, Response> second)
        {
        if (first == null || second == null || !first.keySet().equals(second.keySet()))
            {
            return false;
            }
        for (MemberBinding member : first.keySet())
            {
            Response one = first.get(member);
            Response two = second.get(member);
            if (one == null || two == null || one.f_lTopologyGeneration <= 0L
                    || one.f_lTopologyGeneration != two.f_lTopologyGeneration)
                {
                return false;
                }
            }
        return true;
        }

    @SuppressWarnings("unchecked")
    private static Map<MemberBinding, Response> collect(InvocationService service, Topology topology)
        {
        Map<Member, Object> raw;
        try
            {
            raw = service.query(new ReadinessInvocable(topology), topology.f_setRuntimeMembers);
            }
        catch (RuntimeException e)
            {
            return null;
            }
        Map<MemberBinding, Response> responses = new LinkedHashMap<>();
        if (raw != null)
            {
            for (Map.Entry<Member, Object> entry : raw.entrySet())
                {
                MemberBinding source = MemberBinding.from(entry.getKey());
                Object value = entry.getValue();
                if (!(value instanceof Response) || responses.put(source, (Response) value) != null)
                    {
                    return null;
                    }
                }
            }
        return responses;
        }

    private static Map<MemberBinding, Response> bindEpoch(Map<MemberBinding, Response> responses, String sEpoch)
        {
        Map<MemberBinding, Response> result = new LinkedHashMap<>();
        for (Map.Entry<MemberBinding, Response> entry : responses.entrySet())
            {
            result.put(entry.getKey(), entry.getValue().withEpoch(sEpoch));
            }
        return result;
        }

    /** Evaluate already source-bound responses; retained as a deterministic product test seam. */
    static Result evaluate(Query query, Topology topology, Map<MemberBinding, Response> responses, long lNowMillis)
        {
        String sInvalid = validateQuery(query);
        if (!sInvalid.isEmpty())
            {
            return Result.unavailable(sInvalid);
            }
        if (topology == null || !topology.f_sFailure.isEmpty() || topology.f_mapMembers.isEmpty())
            {
            return Result.unavailable("empty-topology");
            }

        Set<MemberBinding> setRecipients = topology.recipients(query.f_role);
        Set<MemberBinding> setProducers = topology.producers(query.f_role);
        if (setRecipients.isEmpty())
            {
            return Result.unavailable(role(query) + ":recipient-membership-empty");
            }
        if (setProducers.isEmpty())
            {
            return Result.unavailable(role(query) + ":producer-membership-empty");
            }

        if (responses == null || responses.size() != topology.f_mapMembers.size())
            {
            int c = responses == null ? 0 : responses.size();
            return Result.unavailable(role(query) + ":observation-incomplete:"
                    + Math.max(1, topology.f_mapMembers.size() - c));
            }
        if (!responses.keySet().equals(topology.f_mapMembers.keySet()))
            {
            return Result.unavailable(role(query) + ":observation-membership-mismatch");
            }

        Map<MemberBinding, Observation> observations = new LinkedHashMap<>();
        Set<String> issuers = new LinkedHashSet<>();
        Set<MemberBinding> relevant = new LinkedHashSet<>(setRecipients);
        relevant.addAll(setProducers);
        for (Map.Entry<MemberBinding, Response> entry : responses.entrySet())
            {
            MemberBinding source = entry.getKey();
            Response response = entry.getValue();
            MemberBinding expected = topology.f_mapMembers.get(source);
            if (response == null || expected == null || !source.equals(response.f_member)
                    || !expected.equals(response.f_member) || !topology.f_sEpoch.equals(response.f_sEpoch))
                {
                return Result.unavailable(role(query) + ":source-member-mismatch");
                }
            Observation observation = response.f_observation;
            String sExpectedIssuer = X509PeerProofProvider.issuerId(source.f_sMemberName);
            if (observation == null)
                {
                if (relevant.contains(source))
                    {
                    return Result.unavailable(role(query) + ":observation-missing");
                    }
                continue;
                }
            if (!sExpectedIssuer.equals(observation.f_sIssuerId))
                {
                return Result.unavailable(role(query) + ":source-issuer-mismatch");
                }
            if (!issuers.add(observation.f_sIssuerId))
                {
                return Result.unavailable(role(query) + ":duplicate-issuer");
                }
            if (relevant.contains(source) && !observation.isFreshAndUsable(lNowMillis))
                {
                return Result.unavailable(role(query) + ":member-unavailable");
                }
            observations.put(source, observation);
            }

        int cRecipientLag = 0;
        for (MemberBinding recipient : setRecipients)
            {
            Observation observation = observations.get(recipient);
            Map<String, CertificateWindow> authorities = query.f_role == Role.SUBJECT
                    ? observation.f_mapSubjectAuthorities : observation.f_mapSeniorAuthorities;
            boolean fNew = valid(authorities.get(query.f_sNewKeyId), lNowMillis);
            boolean fOld = valid(authorities.get(query.f_sOldKeyId), lNowMillis);
            if (!fNew || query.f_stage == Stage.OVERLAP && !fOld
                    || query.f_stage == Stage.RETIRE && fOld)
                {
                cRecipientLag++;
                }
            }
        if (cRecipientLag > 0)
            {
            return Result.unavailable(role(query) + ":" + stage(query)
                    + ":recipient-lagging:" + cRecipientLag, topology.f_sEpoch,
                    setProducers.size(), setRecipients.size());
            }

        String sExpectedActive = query.f_stage == Stage.ADD ? query.f_sOldKeyId : query.f_sNewKeyId;
        int cTarget = 0;
        int cProducerLag = 0;
        for (MemberBinding producer : setProducers)
            {
            String sActive = observations.get(producer).f_sActiveKeyId;
            if (query.f_sOldKeyId.equals(sActive) || query.f_sNewKeyId.equals(sActive))
                {
                cTarget++;
                if (!sExpectedActive.equals(sActive))
                    {
                    cProducerLag++;
                    }
                }
            }
        if (cTarget != 1)
            {
            return Result.unavailable(role(query) + ":" + stage(query)
                    + ":target-producer-count:" + cTarget, topology.f_sEpoch,
                    setProducers.size(), setRecipients.size());
            }
        if (cProducerLag > 0)
            {
            return Result.unavailable(role(query) + ":" + stage(query)
                    + ":producer-lagging:" + cProducerLag, topology.f_sEpoch,
                    setProducers.size(), setRecipients.size());
            }

        return Result.ready(role(query) + ":" + stage(query) + ":ready", topology.f_sEpoch,
                setProducers.size(), setRecipients.size());
        }

    private static String validateQuery(Query query)
        {
        if (query == null || query.f_role == null || query.f_stage == null)
            {
            return "invalid-readiness-query";
            }
        if (query.f_sOldKeyId.isEmpty() || query.f_sNewKeyId.isEmpty()
                || query.f_sOldKeyId.equals(query.f_sNewKeyId))
            {
            return role(query) + ":invalid-key-transition";
            }
        if (!query.f_setProducerRoles.isEmpty() || !query.f_setRecipientRoles.isEmpty())
            {
            return role(query) + ":caller-topology-hints-unsupported";
            }
        return "";
        }

    private static boolean valid(CertificateWindow window, long now)
        {
        return window != null && window.isValid(now);
        }

    private static String role(Query query)
        {
        return query.f_role == Role.SUBJECT ? "subject" : "senior";
        }

    private static String stage(Query query)
        {
        switch (query.f_stage)
            {
            case ADD:      return "add-new";
            case ACTIVATE: return "activate-new";
            case OVERLAP:  return "overlap";
            case RETIRE:   return "retire-old";
            default:       return "unknown";
            }
        }

    /** Role selection and key transition requested by an operator. */
    public static final class Query
            implements Serializable
        {
        public Query(Role role, Stage stage, String sOldKeyId, String sNewKeyId)
            {
            this(role, stage, sOldKeyId, sNewKeyId, Collections.emptySet(), Collections.emptySet());
            }

        public Query(Role role, Stage stage, String sOldKeyId, String sNewKeyId,
                Collection<String> producerRoles, Collection<String> recipientRoles)
            {
            f_role = role;
            f_stage = stage;
            f_sOldKeyId = normalize(sOldKeyId);
            f_sNewKeyId = normalize(sNewKeyId);
            f_setProducerRoles = immutable(producerRoles);
            f_setRecipientRoles = immutable(recipientRoles);
            }

        private final Role f_role;
        private final Stage f_stage;
        private final String f_sOldKeyId;
        private final String f_sNewKeyId;
        private final Set<String> f_setProducerRoles;
        private final Set<String> f_setRecipientRoles;

        private static final long serialVersionUID = 1L;
        }

    /** Bounded query result. */
    public static final class Result
            implements Serializable
        {
        private Result(boolean fReady, String sStatus, String sEpoch, int cProducers, int cRecipients)
            {
            f_fReady = fReady;
            f_sStatus = bounded(sStatus);
            f_sEpoch = bounded(sEpoch);
            f_cProducers = Math.max(0, cProducers);
            f_cRecipients = Math.max(0, cRecipients);
            }

        public boolean isReady() {return f_fReady;}
        public String getStatus() {return f_sStatus;}
        public String getTopologyEpoch() {return f_sEpoch;}
        public int getProducerCount() {return f_cProducers;}
        public int getRecipientCount() {return f_cRecipients;}

        @Override
        public String toString() {return f_sStatus;}

        private static Result ready(String sStatus, String sEpoch)
            {return ready(sStatus, sEpoch, 0, 0);}
        private static Result ready(String sStatus, String sEpoch, int cProducers, int cRecipients)
            {return new Result(true, sStatus, sEpoch, cProducers, cRecipients);}
        private static Result unavailable(String sStatus)
            {return new Result(false, sStatus, "", 0, 0);}
        private static Result unavailable(String sStatus, String sEpoch)
            {return unavailable(sStatus, sEpoch, 0, 0);}
        private static Result unavailable(String sStatus, String sEpoch, int cProducers, int cRecipients)
            {return new Result(false, sStatus, sEpoch, cProducers, cRecipients);}

        private final boolean f_fReady;
        private final String f_sStatus;
        private final String f_sEpoch;
        private final int f_cProducers;
        private final int f_cRecipients;
        private static final long serialVersionUID = 1L;
        }

    /** Fresh local observation returned by one provider lifecycle generation. */
    public static final class Observation
            implements Serializable
        {
        Observation(String sIssuerId, String sProviderId, String sActiveKeyId, boolean fReady,
                long lObservedAt, long lLifecycleGeneration, CertificateWindow identity,
                Map<String, CertificateWindow> subject, Map<String, CertificateWindow> senior)
            {
            f_sIssuerId = normalize(sIssuerId);
            f_sProviderId = normalize(sProviderId);
            f_sActiveKeyId = normalize(sActiveKeyId);
            f_fReady = fReady;
            f_lObservedAt = lObservedAt;
            f_lLifecycleGeneration = lLifecycleGeneration;
            f_identity = identity;
            f_mapSubjectAuthorities = immutableMap(subject);
            f_mapSeniorAuthorities = immutableMap(senior);
            }

        static Observation unavailable(String sProviderId, long lGeneration)
            {
            return new Observation("", sProviderId, "", false, System.currentTimeMillis(), lGeneration, null,
                    Collections.emptyMap(), Collections.emptyMap());
            }

        public String getIssuerId() {return f_sIssuerId;}
        public String getActiveKeyId() {return f_sActiveKeyId;}
        public boolean isReady() {return f_fReady;}
        public long getLifecycleGeneration() {return f_lLifecycleGeneration;}

        private boolean isFreshAndUsable(long now)
            {
            long earliest = ProofTimePolicy.subtractSaturated(now, MAX_OBSERVATION_AGE_MILLIS);
            long latest = ProofTimePolicy.addSaturated(now, X509PeerProofProvider.CLOCK_SKEW_MILLIS);
            return f_lLifecycleGeneration > 0L && f_fReady
                    && X509PeerProofProvider.PROVIDER_ID.equals(f_sProviderId)
                    && f_lObservedAt >= earliest && f_lObservedAt <= latest
                    && f_identity != null && f_identity.isValid(now);
            }

        private final String f_sIssuerId;
        private final String f_sProviderId;
        private final String f_sActiveKeyId;
        private final boolean f_fReady;
        private final long f_lObservedAt;
        private final long f_lLifecycleGeneration;
        private final CertificateWindow f_identity;
        private final Map<String, CertificateWindow> f_mapSubjectAuthorities;
        private final Map<String, CertificateWindow> f_mapSeniorAuthorities;

        private static final long serialVersionUID = 1L;
        }

    /** Certificate validity evidence without certificate or key material. */
    static final class CertificateWindow
            implements Serializable
        {
        CertificateWindow(long lNotBefore, long lNotAfter)
            {
            f_lNotBefore = lNotBefore;
            f_lNotAfter = lNotAfter;
            }

        private boolean isValid(long now)
            {
            return f_lNotBefore <= ProofTimePolicy.addSaturated(now, X509PeerProofProvider.CLOCK_SKEW_MILLIS)
                    && f_lNotAfter >= ProofTimePolicy.subtractSaturated(now, X509PeerProofProvider.CLOCK_SKEW_MILLIS);
            }

        private final long f_lNotBefore;
        private final long f_lNotAfter;
        private static final long serialVersionUID = 1L;
        }

    /** One exact cluster, role, and proof-producing service epoch. */
    static final class Topology
        {
        private Topology(Map<MemberBinding, MemberBinding> members, Set<Member> runtimeMembers,
                Map<String, ServiceBinding> services, String sCollectorService, String sFailure)
            {
            this(members, runtimeMembers, services, sCollectorService, sFailure, getTopologyGeneration());
            }

        private Topology(Map<MemberBinding, MemberBinding> members, Set<Member> runtimeMembers,
                Map<String, ServiceBinding> services, String sCollectorService, String sFailure,
                long lTopologyGeneration)
            {
            f_mapMembers = Collections.unmodifiableMap(new LinkedHashMap<>(members));
            f_setRuntimeMembers = Collections.unmodifiableSet(new LinkedHashSet<>(runtimeMembers));
            f_mapServices = Collections.unmodifiableMap(new LinkedHashMap<>(services));
            f_sCollectorService = normalize(sCollectorService);
            f_sFailure = normalize(sFailure);
            f_sEpoch = epoch(f_mapMembers.keySet(), f_mapServices.values());
            f_lTopologyGeneration = lTopologyGeneration;
            }

        @SuppressWarnings("unchecked")
        static Topology capture(Cluster cluster, InvocationService collector)
            {
            long lTopologyGeneration = getTopologyGeneration();
            Set<Member> clusterMembers = new LinkedHashSet<>(cluster.getMemberSet());
            Set<Member> collectorMembers = new LinkedHashSet<>((Set<Member>) collector.getInfo().getServiceMembers());
            if (clusterMembers.isEmpty())
                {
                return failed("empty-topology");
                }
            if (!bindings(clusterMembers).equals(bindings(collectorMembers)))
                {
                return failed("readiness-service-membership-incomplete");
                }

            Map<MemberBinding, MemberBinding> members = new LinkedHashMap<>();
            Set<String> issuers = new LinkedHashSet<>();
            for (Member member : clusterMembers)
                {
                MemberBinding binding = MemberBinding.from(member);
                if (!binding.isValid() || members.put(binding, binding) != null
                        || !issuers.add(X509PeerProofProvider.issuerId(binding.f_sMemberName)))
                    {
                    return failed("malformed-topology");
                    }
                }

            Map<String, ServiceBinding> services = new LinkedHashMap<>();
            Enumeration<String> names = cluster.getServiceNames();
            while (names != null && names.hasMoreElements())
                {
                String sName = normalize(names.nextElement());
                ServiceInfo info = cluster.getServiceInfo(sName);
                if (info == null || !isProofRelevantService(info.getServiceType()))
                    {
                    continue;
                    }
                ServiceBinding binding = ServiceBinding.from(info);
                if (binding.f_setMemberUids.isEmpty())
                    {
                    // Registered but currently stopped services cannot
                    // produce or receive proof-bearing Grid traffic.
                    continue;
                    }
                if (!binding.isValid(members) || services.put(sName, binding) != null)
                    {
                    return failed("malformed-service-topology");
                    }
                }
            if (services.isEmpty() || !services.containsKey(normalize(collector.getInfo().getServiceName())))
                {
                return failed("proof-service-topology-empty");
                }
            if (lTopologyGeneration != getTopologyGeneration())
                {
                return failed("topology-changed");
                }
            return new Topology(members, clusterMembers, services, collector.getInfo().getServiceName(), "",
                    lTopologyGeneration);
            }

        static Topology of(Collection<MemberBinding> members, MemberBinding senior)
            {
            Map<MemberBinding, MemberBinding> map = memberMap(members);
            ServiceBinding service = new ServiceBinding("subject-service", CacheService.TYPE_DISTRIBUTED,
                    uids(map.keySet()), senior == null ? "" : senior.f_sUid);
            return new Topology(map, Collections.emptySet(), Collections.singletonMap(service.f_sName, service),
                    service.f_sName, "");
            }

        static Topology of(Collection<MemberBinding> members, Collection<ServiceBinding> services)
            {
            Map<MemberBinding, MemberBinding> map = memberMap(members);
            Map<String, ServiceBinding> serviceMap = new LinkedHashMap<>();
            if (services != null)
                {
                for (ServiceBinding service : services)
                    {
                    if (service != null) {serviceMap.put(service.f_sName, service);}
                    }
                }
            return new Topology(map, Collections.emptySet(), serviceMap,
                    serviceMap.isEmpty() ? "" : serviceMap.keySet().iterator().next(), "");
            }

        private static Map<MemberBinding, MemberBinding> memberMap(Collection<MemberBinding> members)
            {
            Map<MemberBinding, MemberBinding> map = new LinkedHashMap<>();
            if (members != null)
                {
                for (MemberBinding member : members)
                    {
                    if (member != null) {map.put(member, member);}
                    }
                }
            return map;
            }

        private static Topology failed(String s)
            {return new Topology(Collections.emptyMap(), Collections.emptySet(), Collections.emptyMap(), "", s);}

        Topology withSubjectRecipients(Map<MemberBinding, Response> responses)
            {
            if (responses == null || responses.size() != f_mapMembers.size()
                    || !responses.keySet().equals(f_mapMembers.keySet()))
                {
                return failed("subject-recipient-evidence-incomplete");
                }
            Map<String, Set<String>> evidence = new LinkedHashMap<>();
            for (Map.Entry<MemberBinding, Response> entry : responses.entrySet())
                {
                MemberBinding source = entry.getKey();
                Response response = entry.getValue();
                if (response == null || !source.equals(response.f_member)
                        || !f_sEpoch.equals(response.f_sEpoch))
                    {
                    return failed("subject-recipient-evidence-source-mismatch");
                    }
                Set<String> expected = new LinkedHashSet<>();
                for (ServiceBinding service : f_mapServices.values())
                    {
                    if (service.isSubjectSurface() && service.f_setMemberUids.contains(source.f_sUid))
                        {
                        expected.add(service.f_sName);
                        }
                    }
                if (!expected.equals(response.f_mapSubjectRecipients.keySet()))
                    {
                    return failed("subject-recipient-evidence-omitted-or-substituted");
                    }
                for (Map.Entry<String, Set<String>> item : response.f_mapSubjectRecipients.entrySet())
                    {
                    ServiceBinding service = f_mapServices.get(item.getKey());
                    Set<String> recipients = item.getValue();
                    if (service == null || recipients == null || recipients.isEmpty()
                            || !service.f_setMemberUids.containsAll(recipients))
                        {
                        return failed("subject-recipient-evidence-malformed");
                        }
                    Set<String> prior = evidence.putIfAbsent(item.getKey(), recipients);
                    if (prior != null && !prior.equals(recipients))
                        {
                        return failed("subject-recipient-topology-changed");
                        }
                    }
                }
            Map<String, ServiceBinding> services = new LinkedHashMap<>();
            for (ServiceBinding service : f_mapServices.values())
                {
                Set<String> recipients = service.isSubjectSurface() ? evidence.get(service.f_sName)
                        : Collections.emptySet();
                if (service.isSubjectSurface() && (recipients == null || recipients.isEmpty()))
                    {
                    return failed("subject-recipient-membership-empty");
                    }
                services.put(service.f_sName, service.withSubjectRecipients(recipients));
                }
            return new Topology(f_mapMembers, f_setRuntimeMembers, services, f_sCollectorService, "",
                    f_lTopologyGeneration);
            }

        Set<MemberBinding> producers(Role role)
            {
            Set<String> uids = new LinkedHashSet<>();
            for (ServiceBinding service : f_mapServices.values())
                {
                if (role == Role.SUBJECT)
                    {
                    if (service.isSubjectSurface()) {uids.addAll(service.f_setMemberUids);}
                    }
                else if (!service.f_sSeniorUid.isEmpty())
                    {
                    uids.add(service.f_sSeniorUid);
                    }
                }
            return members(uids);
            }

        Set<MemberBinding> recipients(Role role)
            {
            Set<String> uids = new LinkedHashSet<>();
            for (ServiceBinding service : f_mapServices.values())
                {
                if (role == Role.SENIOR || service.isSubjectSurface())
                    {
                    if (role == Role.SENIOR)
                        {
                        uids.addAll(service.f_setMemberUids);
                        }
                    else
                        {
                        uids.addAll(service.f_setSubjectRecipientUids);
                        }
                    }
                }
            return members(uids);
            }

        private Set<MemberBinding> members(Set<String> uids)
            {
            Set<MemberBinding> result = new LinkedHashSet<>();
            for (MemberBinding member : f_mapMembers.keySet())
                {
                if (uids.contains(member.f_sUid)) {result.add(member);}
                }
            return result;
            }

        boolean sameEpoch(Topology other)
            {return other != null && f_sFailure.equals(other.f_sFailure) && f_sEpoch.equals(other.f_sEpoch)
                    && f_lTopologyGeneration == other.f_lTopologyGeneration;}

        String getEpoch() {return f_sEpoch;}

        private final Map<MemberBinding, MemberBinding> f_mapMembers;
        private final Set<Member> f_setRuntimeMembers;
        private final Map<String, ServiceBinding> f_mapServices;
        private final String f_sCollectorService;
        private final String f_sFailure;
        private final String f_sEpoch;
        private final long f_lTopologyGeneration;
        }

    /** One running Grid service membership and senior epoch. */
    static final class ServiceBinding
            implements Serializable
        {
        ServiceBinding(String sName, String sType, Collection<String> memberUids, String sSeniorUid)
            {
            this(sName, sType, memberUids, sSeniorUid,
                    CacheService.TYPE_DISTRIBUTED.equals(normalize(sType))
                            ? memberUids : Collections.emptySet());
            }

        ServiceBinding(String sName, String sType, Collection<String> memberUids, String sSeniorUid,
                Collection<String> subjectRecipientUids)
            {
            f_sName = normalize(sName);
            f_sType = normalize(sType);
            f_setMemberUids = immutable(memberUids);
            f_sSeniorUid = normalize(sSeniorUid);
            f_setSubjectRecipientUids = immutable(subjectRecipientUids);
            }

        @SuppressWarnings("unchecked")
        static ServiceBinding from(ServiceInfo info)
            {
            Set<String> members = new LinkedHashSet<>();
            for (Member member : (Set<Member>) info.getServiceMembers())
                {
                members.add(String.valueOf(member.getUid()));
                }
            Member senior = info.getOldestMember();
            return new ServiceBinding(info.getServiceName(), info.getServiceType(), members,
                    senior == null ? "" : String.valueOf(senior.getUid()), Collections.emptySet());
            }

        boolean isValid(Map<MemberBinding, MemberBinding> members)
            {
            Set<String> clusterUids = uids(members.keySet());
            return !f_sName.isEmpty() && !f_sType.isEmpty() && !f_setMemberUids.isEmpty()
                    && clusterUids.containsAll(f_setMemberUids)
                    && !f_sSeniorUid.isEmpty() && f_setMemberUids.contains(f_sSeniorUid);
            }

        boolean isSubjectSurface()
            {return CacheService.TYPE_DISTRIBUTED.equals(f_sType);}

        ServiceBinding withSubjectRecipients(Collection<String> recipients)
            {return new ServiceBinding(f_sName, f_sType, f_setMemberUids, f_sSeniorUid, recipients);}

        private final String f_sName;
        private final String f_sType;
        private final Set<String> f_setMemberUids;
        private final String f_sSeniorUid;
        private final Set<String> f_setSubjectRecipientUids;
        private static final long serialVersionUID = 1L;
        }

    /** Runtime member identity included in the topology epoch and response binding. */
    static final class MemberBinding
            implements Serializable
        {
        MemberBinding(String sUid, String sMemberName, String sRole)
            {
            f_sUid = normalize(sUid);
            f_sMemberName = normalize(sMemberName);
            f_sRole = normalize(sRole);
            }

        static MemberBinding from(Member member)
            {
            return member == null ? new MemberBinding("", "", "")
                    : new MemberBinding(String.valueOf(member.getUid()), member.getMemberName(), member.getRoleName());
            }

        private boolean isValid()
            {return !f_sUid.isEmpty() && !f_sMemberName.isEmpty() && !f_sRole.isEmpty();}

        @Override
        public boolean equals(Object o)
            {
            if (this == o) {return true;}
            if (!(o instanceof MemberBinding)) {return false;}
            MemberBinding that = (MemberBinding) o;
            return f_sUid.equals(that.f_sUid) && f_sMemberName.equals(that.f_sMemberName)
                    && f_sRole.equals(that.f_sRole);
            }

        @Override
        public int hashCode()
            {return 31 * (31 * f_sUid.hashCode() + f_sMemberName.hashCode()) + f_sRole.hashCode();}

        private final String f_sUid;
        private final String f_sMemberName;
        private final String f_sRole;
        private static final long serialVersionUID = 1L;
        }

    /** Source-bound invocable response. */
    public static final class Response
            implements ExternalizableLite
        {
        public Response()
            {
            }

        Response(MemberBinding member, String sEpoch, Observation observation)
            {this(member, sEpoch, observation, Collections.emptyMap());}

        Response(MemberBinding member, String sEpoch, Observation observation,
                Map<String, Set<String>> subjectRecipients)
            {this(member, sEpoch, observation, subjectRecipients, getTopologyGeneration());}

        Response(MemberBinding member, String sEpoch, Observation observation,
                Map<String, Set<String>> subjectRecipients, long lTopologyGeneration)
            {
            f_member = member;
            f_sEpoch = normalize(sEpoch);
            f_observation = observation;
            f_mapSubjectRecipients = immutableSetMap(subjectRecipients);
            f_lTopologyGeneration = lTopologyGeneration;
            }

        private Response withEpoch(String sEpoch)
            {return new Response(f_member, sEpoch, f_observation, f_mapSubjectRecipients,
                    f_lTopologyGeneration);}

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            f_member = readMember(in);
            f_sEpoch = ExternalizableHelper.readSafeUTF(in);
            f_observation = readObservation(in);
            f_mapSubjectRecipients = readStringSets(in);
            f_lTopologyGeneration = in.readLong();
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            writeMember(out, f_member);
            ExternalizableHelper.writeSafeUTF(out, f_sEpoch);
            writeObservation(out, f_observation);
            writeStringSets(out, f_mapSubjectRecipients);
            out.writeLong(f_lTopologyGeneration);
            }

        private MemberBinding f_member;
        private String f_sEpoch;
        private Observation f_observation;
        private Map<String, Set<String>> f_mapSubjectRecipients = Collections.emptyMap();
        private long f_lTopologyGeneration;
        }

    /** Product invocable; unlike the old functional helper this is the supported collector. */
    public static final class ReadinessInvocable
            extends AbstractInvocable
            implements ExternalizableLite
        {
        public ReadinessInvocable()
            {
            }

        ReadinessInvocable(Topology topology)
            {
            f_sEpoch = topology.f_sEpoch;
            f_sCollectorService = topology.f_sCollectorService;
            f_mapExpected = topology.f_mapMembers;
            Map<String, Set<String>> subjects = new LinkedHashMap<>();
            for (ServiceBinding binding : topology.f_mapServices.values())
                {
                if (binding.isSubjectSurface())
                    {
                    subjects.put(binding.f_sName, binding.f_setMemberUids);
                    }
                }
            f_mapSubjectServices = subjects;
            }

        @Override
        public void run()
            {
            long lTopologyGeneration = getTopologyGeneration();
            try
                {
                Cluster cluster = CacheFactory.getCluster();
                MemberBinding local = MemberBinding.from(cluster.getLocalMember());
                MemberBinding expected = f_mapExpected.get(local);
                Object service = cluster.getService(f_sCollectorService);
                if (expected == null || !(service instanceof InvocationService))
                    {
                    setResult(new Response(local, "", null));
                    return;
                    }
                Topology current = Topology.capture(cluster, (InvocationService) service);
                if (!f_sEpoch.equals(current.f_sEpoch))
                    {
                    setResult(new Response(local, current.f_sEpoch, null));
                    return;
                    }
                Map<String, Set<String>> recipients = new LinkedHashMap<>();
                for (Map.Entry<String, Set<String>> entry : f_mapSubjectServices.entrySet())
                    {
                    if (!entry.getValue().contains(local.f_sUid))
                        {
                        continue;
                        }
                    Object subjectService = cluster.getService(entry.getKey());
                    if (!(subjectService instanceof PartitionedService))
                        {
                        setResult(new Response(local, f_sEpoch, null));
                        return;
                        }
                    Set<String> ownership = new LinkedHashSet<>();
                    for (Member member : ((PartitionedService) subjectService).getOwnershipEnabledMembers())
                        {
                        ownership.add(String.valueOf(member.getUid()));
                        }
                    recipients.put(entry.getKey(), immutable(ownership));
                    }
                Class<?> clz = Class.forName("com.tangosol.coherence.component.net.Security");
                PeerProofProvider provider = (PeerProofProvider) clz.getMethod("getPeerProofProvider").invoke(null);
                if (provider != null)
                    {
                    provider.setLocalMemberName(local.f_sMemberName);
                    List<String> names = new ArrayList<>();
                    for (MemberBinding member : current.f_mapMembers.keySet())
                        {
                        names.add(member.f_sMemberName);
                        }
                    provider.observeMemberNames(names);
                    }
                Observation observation = provider == null ? null : provider.getLocalReadinessObservation();
                long lAfter = getTopologyGeneration();
                setResult(new Response(local, f_sEpoch, lTopologyGeneration == lAfter ? observation : null,
                        recipients, lTopologyGeneration == lAfter ? lAfter : -1L));
                }
            catch (Throwable e)
                {
                setResult(null);
                }
            }

        @Override
        public long getExecutionTimeoutMillis() {return QUERY_TIMEOUT_MILLIS;}
        @Override
        public long getRequestTimeoutMillis() {return QUERY_TIMEOUT_MILLIS;}

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            f_sEpoch = ExternalizableHelper.readSafeUTF(in);
            f_sCollectorService = ExternalizableHelper.readSafeUTF(in);
            int count = in.readInt();
            if (count < 0 || count > MAX_QUERY_MEMBERS)
                {
                throw new IOException("invalid readiness member count");
                }
            Map<MemberBinding, MemberBinding> expected = new LinkedHashMap<>();
            for (int i = 0; i < count; i++)
                {
                MemberBinding member = readMember(in);
                if (member == null || expected.put(member, member) != null)
                    {
                    throw new IOException("invalid readiness member binding");
                    }
                }
            f_mapExpected = expected;
            f_mapSubjectServices = readStringSets(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeSafeUTF(out, f_sEpoch);
            ExternalizableHelper.writeSafeUTF(out, f_sCollectorService);
            out.writeInt(f_mapExpected.size());
            for (MemberBinding member : f_mapExpected.keySet())
                {
                writeMember(out, member);
                }
            writeStringSets(out, f_mapSubjectServices);
            }

        private String f_sEpoch;
        private String f_sCollectorService;
        private Map<MemberBinding, MemberBinding> f_mapExpected;
        private Map<String, Set<String>> f_mapSubjectServices = Collections.emptyMap();
        private static final long serialVersionUID = 1L;
        }

    private static MemberBinding readMember(DataInput in) throws IOException
        {
        if (!in.readBoolean())
            {
            return null;
            }
        return new MemberBinding(ExternalizableHelper.readSafeUTF(in), ExternalizableHelper.readSafeUTF(in),
                ExternalizableHelper.readSafeUTF(in));
        }

    private static void writeMember(DataOutput out, MemberBinding member) throws IOException
        {
        out.writeBoolean(member != null);
        if (member != null)
            {
            ExternalizableHelper.writeSafeUTF(out, member.f_sUid);
            ExternalizableHelper.writeSafeUTF(out, member.f_sMemberName);
            ExternalizableHelper.writeSafeUTF(out, member.f_sRole);
            }
        }

    private static Observation readObservation(DataInput in) throws IOException
        {
        if (!in.readBoolean())
            {
            return null;
            }
        String issuer = ExternalizableHelper.readSafeUTF(in);
        String provider = ExternalizableHelper.readSafeUTF(in);
        String active = ExternalizableHelper.readSafeUTF(in);
        boolean ready = in.readBoolean();
        long observedAt = in.readLong();
        long generation = in.readLong();
        CertificateWindow identity = readWindow(in);
        Map<String, CertificateWindow> subject = readWindows(in);
        Map<String, CertificateWindow> senior = readWindows(in);
        return new Observation(issuer, provider, active, ready, observedAt, generation, identity, subject, senior);
        }

    private static void writeObservation(DataOutput out, Observation observation) throws IOException
        {
        out.writeBoolean(observation != null);
        if (observation != null)
            {
            ExternalizableHelper.writeSafeUTF(out, observation.f_sIssuerId);
            ExternalizableHelper.writeSafeUTF(out, observation.f_sProviderId);
            ExternalizableHelper.writeSafeUTF(out, observation.f_sActiveKeyId);
            out.writeBoolean(observation.f_fReady);
            out.writeLong(observation.f_lObservedAt);
            out.writeLong(observation.f_lLifecycleGeneration);
            writeWindow(out, observation.f_identity);
            writeWindows(out, observation.f_mapSubjectAuthorities);
            writeWindows(out, observation.f_mapSeniorAuthorities);
            }
        }

    private static CertificateWindow readWindow(DataInput in) throws IOException
        {
        return in.readBoolean() ? new CertificateWindow(in.readLong(), in.readLong()) : null;
        }

    private static void writeWindow(DataOutput out, CertificateWindow window) throws IOException
        {
        out.writeBoolean(window != null);
        if (window != null)
            {
            out.writeLong(window.f_lNotBefore);
            out.writeLong(window.f_lNotAfter);
            }
        }

    private static Map<String, CertificateWindow> readWindows(DataInput in) throws IOException
        {
        int count = in.readInt();
        if (count < 0 || count > MAX_QUERY_KEYS)
            {
            throw new IOException("invalid readiness authority count");
            }
        Map<String, CertificateWindow> windows = new LinkedHashMap<>();
        for (int i = 0; i < count; i++)
            {
            String key = ExternalizableHelper.readSafeUTF(in);
            CertificateWindow window = readWindow(in);
            if (normalize(key).isEmpty() || window == null || windows.put(key, window) != null)
                {
                throw new IOException("invalid readiness authority evidence");
                }
            }
        return windows;
        }

    private static void writeWindows(DataOutput out, Map<String, CertificateWindow> windows) throws IOException
        {
        out.writeInt(windows.size());
        for (Map.Entry<String, CertificateWindow> entry : windows.entrySet())
            {
            ExternalizableHelper.writeSafeUTF(out, entry.getKey());
            writeWindow(out, entry.getValue());
            }
        }

    private static Map<String, Set<String>> readStringSets(DataInput in) throws IOException
        {
        int count = in.readInt();
        if (count < 0 || count > MAX_QUERY_SERVICES)
            {
            throw new IOException("invalid readiness service evidence count");
            }
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++)
            {
            String name = normalize(ExternalizableHelper.readSafeUTF(in));
            int members = in.readInt();
            if (name.isEmpty() || members < 0 || members > MAX_QUERY_MEMBERS)
                {
                throw new IOException("invalid readiness service evidence");
                }
            Set<String> uids = new LinkedHashSet<>();
            for (int j = 0; j < members; j++)
                {
                String uid = normalize(ExternalizableHelper.readSafeUTF(in));
                if (uid.isEmpty() || !uids.add(uid))
                    {
                    throw new IOException("invalid readiness service member evidence");
                    }
                }
            if (result.put(name, Collections.unmodifiableSet(uids)) != null)
                {
                throw new IOException("duplicate readiness service evidence");
                }
            }
        return Collections.unmodifiableMap(result);
        }

    private static void writeStringSets(DataOutput out, Map<String, Set<String>> values) throws IOException
        {
        out.writeInt(values.size());
        for (Map.Entry<String, Set<String>> entry : values.entrySet())
            {
            ExternalizableHelper.writeSafeUTF(out, entry.getKey());
            out.writeInt(entry.getValue().size());
            for (String value : entry.getValue())
                {
                ExternalizableHelper.writeSafeUTF(out, value);
                }
            }
        }

    private static Set<MemberBinding> bindings(Collection<Member> members)
        {
        Set<MemberBinding> set = new LinkedHashSet<>();
        for (Member member : members) {set.add(MemberBinding.from(member));}
        return set;
        }

    private static String epoch(Collection<MemberBinding> members, Collection<ServiceBinding> services)
        {
        try
            {
            List<MemberBinding> list = new ArrayList<>(members);
            list.sort(Comparator.comparing(member -> member.f_sUid));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (MemberBinding member : list)
                {
                digest.update(member.f_sUid.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(member.f_sMemberName.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(member.f_sRole.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0xff);
                }
            List<ServiceBinding> listServices = new ArrayList<>(services);
            listServices.sort(Comparator.comparing(service -> service.f_sName));
            for (ServiceBinding service : listServices)
                {
                digest.update(service.f_sName.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(service.f_sType.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                List<String> serviceMembers = new ArrayList<>(service.f_setMemberUids);
                Collections.sort(serviceMembers);
                for (String uid : serviceMembers)
                    {
                    digest.update(uid.getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0);
                    }
                digest.update(service.f_sSeniorUid.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0xfd);
                List<String> recipients = new ArrayList<>(service.f_setSubjectRecipientUids);
                Collections.sort(recipients);
                for (String uid : recipients)
                    {
                    digest.update(uid.getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0);
                    }
                digest.update((byte) 0xfe);
                }
            StringBuilder sb = new StringBuilder(32);
            byte[] value = digest.digest();
            for (int i = 0; i < 16; i++) {sb.append(String.format("%02x", value[i] & 0xff));}
            return sb.toString();
            }
        catch (Exception e)
            {
            throw new IllegalStateException(e);
            }
        }

    private static boolean isProofRelevantService(String sType)
        {
        String s = normalize(sType);
        return !s.isEmpty() && !s.startsWith("Remote") && !CacheService.TYPE_LOCAL.equals(s);
        }

    private static Set<String> uids(Collection<MemberBinding> members)
        {
        Set<String> result = new LinkedHashSet<>();
        for (MemberBinding member : members)
            {
            result.add(member.f_sUid);
            }
        return result;
        }

    private static Set<String> immutable(Collection<String> values)
        {
        Set<String> set = new LinkedHashSet<>();
        if (values != null)
            {
            for (String value : values)
                {
                String s = normalize(value);
                if (!s.isEmpty()) {set.add(s);}
                }
            }
        return Collections.unmodifiableSet(set);
        }

    private static Map<String, CertificateWindow> immutableMap(Map<String, CertificateWindow> values)
        {
        return Collections.unmodifiableMap(values == null
                ? Collections.emptyMap() : new LinkedHashMap<>(values));
        }

    private static Map<String, Set<String>> immutableSetMap(Map<String, Set<String>> values)
        {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        if (values != null)
            {
            for (Map.Entry<String, Set<String>> entry : values.entrySet())
                {
                String name = normalize(entry.getKey());
                if (!name.isEmpty())
                    {
                    result.put(name, immutable(entry.getValue()));
                    }
                }
            }
        return Collections.unmodifiableMap(result);
        }

    private static String normalize(String s) {return s == null ? "" : s.trim();}
    private static String bounded(String s)
        {s = normalize(s); return s.length() <= 128 ? s : s.substring(0, 128);}

    public static final long MAX_OBSERVATION_AGE_MILLIS = 120_000L;
    public static final long QUERY_TIMEOUT_MILLIS = 30_000L;
    private static final int MAX_QUERY_MEMBERS = 16_384;
    private static final int MAX_QUERY_SERVICES = 1_024;
    private static final int MAX_QUERY_KEYS = 1_024;
    private static final AtomicLong TOPOLOGY_GENERATION = new AtomicLong(1L);
    }
