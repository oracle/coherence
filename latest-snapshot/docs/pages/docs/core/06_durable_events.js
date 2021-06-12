<doc-view>

<h2 id="_durable_events">Durable Events</h2>
<div class="section">
<p>Coherence has provided the ability for clients to asynchronously observe data
changes for almost two decades. This has proven to be an incredibly powerful
feature allowing Coherence to offer components such as
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/introduction-coherence-caches.html#GUID-5C066CC9-575F-4D7D-9D53-7BB674D69FD1">NearCaches</a>
and <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/introduction-coherence-caches.html#GUID-0A4E65E0-1E92-4B8B-8681-AD9A8CA6D06D">ViewCaches/CQCs</a>,
in addition to allowing customers to build truly event driven systems.</p>

<p>A comprehensive overview of MapEvents in terms of the call back interface in
addition to the registration mechanisms is provided in the
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-map-events.html">official documentation</a>.
However, it is worth drawing attention to some of the guarantees offered by MapEvents
to provide context of why the Durable Events feature is useful.</p>


<h3 id="_mapevent_guarantees">MapEvent Guarantees</h3>
<div class="section">
<p>Coherence guarantees a <code>MapEvent</code>, which represents a change to an <code>Entry</code> in the
source <code>Map</code>, will be delivered exactly once given the client remains a member
of the associated service.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>Each <code>NamedMap</code> is associated to a <code>Service</code> and typically this service is a <code>PartitionedService</code>
therefore provides distributed storage and partitioned/sharded access. The remaining
description of <code>MapEvent</code> guarantees will assume the use of a <code>PartitionedService</code>
and therefore providing resilience to process, machine, rack, or site failure.</p>
</p>
</div>
<p>Importantly this guarantee is maintained regardless of failures in the system
that the services are configured to handle. Therefore a <code>backup-count</code> of 1 results
in the service being able to tolerate the loss of a single unit where unit could
be a node (JVM/member), machine, rack or site. Upon encountering a fault Coherence
will restore data and continue service for the affected partitions. This data
redundancy is extended to MapEvent delivery to clients. Therefore if a member
hosting primary partitions was to die, and said member had sent the backup message
for some change but failed to deliver the MapEvent to the client, the new primary
member (that was a backup member and went through the automatic promotion protocol)
would emit MapEvent messages that had not been confirm by clients. The client
is aware of MapEvent messages it had already processed, therefore if the MapEvent
was received by the client but the primary did not receive the ACK thus causing
the backup to send a duplicate, the client will simply not replay the event.</p>

</div>

<h3 id="_raison_detre">Raison D&#8217;etre</h3>
<div class="section">
<p>This level of redundancy and guarantee is sufficient for many applications and
users of Coherence have been able to live with/workaround a particular shortcoming
such that the product has not attempted to address it.</p>

<p>The aforementioned guarantee of MapEvent delivery are all valid under the assumption
that the member, that has registered for MapEvents, does <strong>not</strong> leave the service.
If it does leave the associated service then these guarantees no longer apply.
This can be problematic under a few contexts (described below) and has led to
the need deliver a more general feature of event replay.</p>


<h4 id="_abnormal_service_termination">Abnormal Service Termination</h4>
<div class="section">
<p>While it is rare, there are some scenarios that result in abnormal service
termination. This has affected Coherence users by causing references to <code>Service</code>s
or <code>NamedMap</code>s to become invalid and therefore unusable. Instead of having
numerous applications, and internal call sites, be defensive to these invalid handles
Coherence chose to introduce a 'safe' layer between the call site and the raw/internal
service. The 'safe' layer remains valid when services abnormally terminate and
additionally may cause the underlying service to restart.</p>

<p>In the case of a 'safe' <code>NamedMap</code> any <code>MapEvent</code> listeners registered will automatically
be re-registered. However, any events that had occurred after the member left
the service and before it re-joined will be lost, or more formally not observed
by this member&#8217;s listener. This has been worked around in many cases by these
members/clients re-synchronizing their local state with the remote data; this
is the approach taken for <code>ContinuousQueryCache</code>s.</p>

</div>

<h4 id="_extend_proxy_failover">Extend Proxy Failover</h4>
<div class="section">
<p>Coherence Extend provides a means for a client to connect to a cluster via a
conduit referred to as a proxy. An extend client wraps up the intended request,
forwards to a proxy which executes said request with the results either streamed
back to the client or sent as a single response. There are many reasons why one
would chose using extend over being a member of the cluster; further documentation
on extend can be found <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-remote-clients/introduction-coherenceextend.html">here</a>.</p>

<p>The liveness of the proxy is incredibly important to the extend clients that
are connected to it. If the proxy does become unresponsive, or simply dies the
extend client will transparently reconnect to another proxy. Similar to the
aforementioned 'abnormal service termination' use case there is a potential of
not observing MapEvents that had occurred at the source due to the proxy leaving
the associated service or the extend client reconnecting to a different proxy
and therefore re-registering the listener.</p>

<p>Once again, there are means to work around this situation by observing the proxy
disconnect / re-connect and causing a re-synchronization of extend client and
the source. However, extend proxy failover is a significantly more likely event
and has been raised by Coherence users.</p>

</div>

<h4 id="_process_death">Process Death</h4>
<div class="section">
<p>A logical client receiving MapEvents may experience a process restart and it
may be desirable to continue receiving MapEvents after the last received event,
opposed to only receiving events after registration. For example, a client may be
responsible for updating some auxiliary system by applying the contents of each
MapEvent to that system. Additionally, it may be tracking the last received events
and therefore <em>could</em> inform the source of the last event it received. A capable
source could replay all events that were missed.</p>

</div>
</div>

<h3 id="_generic_event_replay">Generic Event Replay</h3>
<div class="section">
<p>In considering how to address these various scenarios that result in event loss
it became evident that the product needs an ability to retain events beyond
receipt of delivery such that they can be replayed on request. This pushes
certain requirements on different parts of the system:</p>

<ul class="ulist">
<li>
<p>storage nodes</p>
<ul class="ulist">
<li>
<p>must track, by storing, the <code>MapEvent</code>s as they are generated</p>

</li>
<li>
<p>expose a monotonically increasing version per partition within a single cache</p>

</li>
<li>
<p>ensure version semantics are maintained regardless of faults</p>

</li>
<li>
<p>[TODO] ensure <code>MapEvent</code> storage is redundant</p>

</li>
<li>
<p>[TODO] provide a <code>MapEvent</code> storage retention policy</p>

</li>
</ul>
</li>
<li>
<p>client nodes</p>
<ul class="ulist">
<li>
<p>have a trivial facility to suggest received <code>MapEvent</code> versions are tracked
and therefore events replayed when faced with restart</p>

</li>
<li>
<p>provide a more advanced means such that the client can control the tracking
of event versions</p>

</li>
</ul>
</li>
</ul>
<p>With the above a <code>MapListener</code> can opt in by suggesting they are version aware
and Coherence will automatically start tracking versions on the client. This
does require a complementing server-side configuration in which the storage servers
are tracking <code>MapEvent</code>s. For example:</p>

<ol style="margin-left: 15px;">
<li>
Start a storage server that is tracking events:
<markup
lang="bash"

>$ java -Dcoherence.distributed.persistence.mode=actice -Dcoherence.distributed.persistence.events.dir=/tmp/events-dir -jar coherence.jar</markup>

</li>
<li>
Start a client that registers a version aware <code>MapEvent</code> listener. A snippet
taken from a functional test in the repo has been provided below:
<markup
lang="java"

>List&lt;MapEvent&gt; listEvents = Collections.synchronizedList(new ArrayList&lt;&gt;());

MapListener&lt;Integer, String&gt; listener = new SimpleMapListener&lt;Integer, String&gt;()
        .addEventHandler(listEvents::add)
        .versioned();

cache.addMapListener(listener, 1, false);</markup>

</li>
</ol>
<p>The above registration results in the client receiving events and tracking the
latest version per partition. Upon abnormal service restart the client will
automatically re-register itself and request versions after the last received
version.</p>

<p>More advanced clients can implement the <a id="" title="" target="_blank" href="https://coherence.community/21.06-SNAPSHOT/api/java//com/tangosol/net/partition/VersionAwareMapListener.html">VersionAwareMapListener</a>
interface directly. Implementors must return an implementation of <a id="" title="" target="_blank" href="https://coherence.community/21.06-SNAPSHOT/api/java//com/tangosol/net/partition/VersionedPartitions.html">VersionedPartitions</a>
which will be interrogated by Coherence upon registration either directly due
to a call to <code>NamedMap.addMapListener</code> or indirectly due to a service restart.
These versions are sent to the relevant storage servers and if a version is returned for
a partition Coherence will return all known versions larger than or equal to
the specified version. Additionally, certain formal constants are defined to
allow a client to request storage servers to send:</p>

<ul class="ulist">
<li>
<p>all known events</p>

</li>
<li>
<p>current head and all future events (previously known as priming events)</p>

</li>
<li>
<p>all future events (the current behavior)</p>

</li>
</ul>
<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>There is a natural harmony between the registration mechanism and the partitions
returned from <code>VersionedPartitions</code> that occurs but is worth noting.
For example, when registering a <code>MapListener</code> against a specific key only <code>MapEvent</code>s
for said key will be received by this <code>MapListener</code> and therefore only versions
for the associated partition will be tracked. The <code>VersionedPartitions</code> returned
by this <code>VersionAwareMapListener</code> will only return a version for a single partition.
However, this is worth being aware of if you do implement your own <code>VersionAwareMapListener</code>
or <code>VersionedPartitions</code> data structure.</p>
</p>
</div>
</div>

<h3 id="_production_ready">Production Ready?</h3>
<div class="section">
<p>This feature is NOT production ready and we do NOT recommend using it in production
at this point. There are some features that we believe are required prior to
this feature graduating to production ready status that we will detail below
for transparency. However, we are making this feature available in its current
form to garner feedback from the community prior to locking down the APIs and
semantics.</p>

<p>The following improvements will be required prior to this feature being production
ready:</p>

<ul class="ulist">
<li>
<p>Redundant MapEvent storage</p>

</li>
<li>
<p>MapEvent retention policy</p>

</li>
<li>
<p>MapEvent delivery flow control</p>

</li>
<li>
<p>Extend and gRPC client support</p>

</li>
<li>
<p>Snapshots of MapEvent storage</p>

</li>
<li>
<p>Allow a logical client to store its <code>VersionedPartitions</code> in Coherence</p>

</li>
<li>
<p>Monitoring metrics</p>

</li>
</ul>
</div>

<h3 id="_get_started">Get Started</h3>
<div class="section">
<p>To get started please take a look at our <router-link to="/examples/guides/145-durable-events/README">guide on Durable Events</router-link>.</p>

</div>
</div>
</doc-view>
