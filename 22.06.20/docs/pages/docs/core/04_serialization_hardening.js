<doc-view>

<v-layout row wrap>
<v-flex xs12 sm10 lg10>
<v-card class="section-def" v-bind:color="$store.state.currentColor">
<v-card-text class="pa-3">
<v-card class="section-def__card">
<v-card-text>
<dl>
<dt slot=title>Serialization Hardening (SER-01)</dt>
<dd slot="desc"><p>This document describes Coherence serialization hardening, the secure defaults
introduced by SER-01, and the migration steps for applications that exchange
application classes across Coherence wire boundaries.</p>
</dd>
</dl>
</v-card-text>
</v-card>
</v-card-text>
</v-card>
</v-flex>
</v-layout>

<h2 id="_overview">Overview</h2>
<div class="section">
<p>SER-01 makes Coherence able to fail closed for common deserialization entry
points. It adds bytecode deny-list checks, JEP-290 filter enforcement, strict
POF type handling, dynamic class-definition validation, generated application
allowlists, security-mode-aware defaults, and per-boundary telemetry.</p>

<p>The compatibility model has two independent axes:</p>

<ul class="ulist">
<li>
<p><code>coherence.mode</code> remains the released runtime/license mode switch. It is not
the global security-hardening opt-in.</p>

</li>
<li>
<p><code>coherence.security.mode</code> controls the compatibility-sensitive SER-01
hardening gates. When unset, or set to <code>compatibility</code>, Coherence preserves
prior-patch compatibility. Set <code>-Dcoherence.security.mode=hardened</code> to opt in
to the hardening-on posture.</p>

</li>
</ul>

<h3 id="_why_not_jep_290_alone">Why not JEP-290 alone?</h3>
<div class="section">
<p><code>java.io.ObjectInputFilter</code> (JEP-290) is a transport-level filter wired to
<code>ObjectInputStream</code>. Coherence&#8217;s dominant wire formats (POF, <code>ExternalizableLite</code>,
<code>PortableObject</code>) bypass <code>ObjectInputStream</code> entirely, so a JEP-290 filter alone
would silently miss the majority of deserialization traffic. SER-01 also needs
the same allowlist to drive three distinct gates&#8201;&#8212;&#8201;class allowlist on
deserialization, lambda-bytecode acceptance, and remote-execution policy&#8201;&#8212;&#8201;and
to compose across many modules at build time with annotation-driven generation.
JEP-290&#8217;s single allow/reject bit and flat filter string cannot express that.
Coherence therefore uses <code>security-config.xml</code> as the source of truth, has every
wire-format reader consult it, and registers a JEP-290 filter that delegates
back to the same allowlist for the paths that do run through <code>ObjectInputStream</code>.
JEP-290 is a hook, not a substitute, and customer-supplied <code>jdk.serialFilter</code>
settings still compose normally for non-Coherence streams.</p>

</div>
</div>

<h2 id="_compatibility">Compatibility</h2>
<div class="section">
<p><code>coherence.security.mode</code> controls how SER-01 gates are applied while
applications migrate to generated <code>security-config.xml</code> and explicit
remote-executable classification.</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 10%;">
<col style="width: 20%;">
<col style="width: 20%;">
<col style="width: 20%;">
<col style="width: 30%;">
</colgroup>
<thead>
<tr>
<th>Security mode</th>
<th>Allowlist gate: <code>isAllowlistEnforced()</code></th>
<th>DYNAMIC remote default: <code>isDynamicRemoteDefaultDeny()</code></th>
<th>Remote executable gate: <code>isRemoteExecutableEnforced()</code></th>
<th>Use</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">unset / <code>compatibility</code></td>
<td class=""><code>false</code></td>
<td class=""><code>false</code></td>
<td class=""><code>false</code></td>
<td class="">Default patch-compatible posture. Existing deployments, including
<code>coherence.mode=dev</code> and <code>coherence.mode=prod</code> deployments, continue to run
without configuration changes while selected gates emit
<code>result=would_reject</code> shadow telemetry.</td>
</tr>
<tr>
<td class=""><code>hardened</code></td>
<td class=""><code>true</code></td>
<td class=""><code>true</code></td>
<td class=""><code>true</code></td>
<td class="">Hardening-on target. Generated allowlists, remote-executable classification,
and unauthenticated DYNAMIC lambda default-deny are enforced unless a
feature-specific override explicitly allows the behavior.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h2 id="_quick_migration_guide">Quick Migration Guide</h2>
<div class="section">
<p>For an existing application:</p>

<ul class="ulist">
<li>
<p>Apply the patch with the existing <code>coherence.mode</code> value. Leave
<code>coherence.security.mode</code> unset during the rolling upgrade so the cluster
keeps prior-patch compatibility.</p>

</li>
<li>
<p>Add the Maven or Gradle security-config generation step to every application
artifact that sends filters, processors, extractors, aggregators, or other
application payload classes over Coherence services.</p>

</li>
<li>
<p>Annotate application payload types with <code>@Remote.Allowed</code> or <code>@PortableType</code>,
rebuild, and verify that each artifact contains
<code>META-INF/coherence/security-config.xml</code>.</p>

</li>
<li>
<p>Watch <code>coh.executable.policy_check{result=would_reject}</code> in compatibility
security mode and annotate or XML-list every remote executable class that
appears there.</p>

</li>
<li>
<p>After the full deployment is upgraded and the shadow telemetry is clean, test
client/server workflows with <code>-Dcoherence.security.mode=hardened</code>.</p>

</li>
<li>
<p>Enable <code>coherence.security.mode=hardened</code> only after all members and clients
that need to participate in the hardened workflow are ready.</p>

</li>
</ul>
<p>Applications that skip this migration will most commonly see two
<code>SecurityException</code> reasons at runtime: <code>security-config-missing</code> when an
application payload class is not in any generated allowlist, and
<code>dynamic-remote-denied-by-mode</code> when a DYNAMIC lambda is received while
security mode is hardened and unauthenticated dynamic remote payloads resolve
to <code>deny</code>. Both are documented in <router-link to="#_error_message_reference" @click.native="this.scrollFix('#_error_message_reference')"></router-link>.</p>

</div>

<h2 id="_runtime_modes">Runtime Modes</h2>
<div class="section">
<p>Coherence runtime mode is selected by the <code>coherence.mode</code> system property.
The released values are <code>eval</code>, <code>dev</code>, <code>development</code>, <code>prod</code>, and
<code>production</code>. When <code>coherence.mode</code> is unset, Coherence defaults to <code>dev</code>.
Blank, unknown, <code>legacy</code>, and <code>legacy-compatibility</code> values fail fast.</p>

<p>Runtime mode controls the released license/runtime mode behavior. It does not
enable the compatibility-sensitive SER-01 hardening gates.</p>

<p>Security mode is selected by the <code>coherence.security.mode</code> system property.
Supported values trim whitespace and compare case-insensitively:</p>

<ul class="ulist">
<li>
<p>unset or <code>compatibility</code> keeps the compatibility-sensitive hardening gates
disabled by default and emits bounded shadow telemetry where available.</p>

</li>
<li>
<p><code>hardened</code> enables the compatibility-sensitive hardening gates.</p>

</li>
</ul>
<p>Blank, unknown, shorthand, and boolean-style values such as <code>true</code>, <code>false</code>,
<code>on</code>, <code>off</code>, <code>enabled</code>, or <code>disabled</code> fail fast. The removed pre-release
<code>coherence.security.hardened</code> property is not an alias.</p>

</div>

<h2 id="_build_time_allowlist_generation">Build-time Allowlist Generation</h2>
<div class="section">
<p>The build-time generator scans compiled classes in the current artifact. It
does not scan dependency jars. Each jar that contributes wire payload types
should generate and package its own <code>META-INF/coherence/security-config.xml</code>.</p>


<h3 id="_maven">Maven</h3>
<div class="section">
<p>The <code>security-config-maven-plugin</code> goal is named <code>generate</code> and is bound to
<code>process-classes</code> by default.</p>

<markup
lang="xml"

>&lt;plugin&gt;
  &lt;groupId&gt;com.oracle.coherence&lt;/groupId&gt;
  &lt;artifactId&gt;security-config-maven-plugin&lt;/artifactId&gt;
  &lt;version&gt;${coherence.version}&lt;/version&gt;
  &lt;executions&gt;
    &lt;execution&gt;
      &lt;id&gt;generate-security-config&lt;/id&gt;
      &lt;phase&gt;process-classes&lt;/phase&gt;
      &lt;goals&gt;
        &lt;goal&gt;generate&lt;/goal&gt;
      &lt;/goals&gt;
    &lt;/execution&gt;
  &lt;/executions&gt;
&lt;/plugin&gt;</markup>

<p>Set <code>-Dcoherence.security-config.skip=true</code> to skip generation for a build.</p>

</div>

<h3 id="_gradle">Gradle</h3>
<div class="section">
<p>The Coherence Gradle plugin registers the <code>securityConfig</code> task when the Java
plugin is applied.</p>

<markup
lang="groovy"

>plugins {
    id 'java'
    id 'com.oracle.coherence.gradle.plugin' version coherenceVersion
}

securityConfig {
    processTestClasses = false
}</markup>

<p>Set <code>processTestClasses = true</code> to also generate
<code>securityConfigTest</code> output for test classes.</p>

</div>

<h3 id="_annotations">Annotations</h3>
<div class="section">
<p>Use <code>com.tangosol.util.function.Remote.Allowed</code> for application classes that
may legitimately cross a wire-deserialization boundary as polymorphic
payloads.</p>

<p>At type level, <code>@Remote.Allowed</code> includes the annotated class and, by default,
its declared nested types. Set <code>recursive = false</code> to include only the
annotated class:</p>

<markup
lang="java"

>// Generates allowlist entries for MyType AND MyType.Nested.
@Remote.Allowed
public class MyType
    {
    static class Nested {}
    }

// Generates an allowlist entry for MyType only; Nested is excluded.
@Remote.Allowed(recursive = false)
public class MyType
    {
    static class Nested {}
    }</markup>

<p>At package level, add <code>@Remote.Allowed</code> to <code>package-info.java</code>. The default
includes classes in that package and subpackages; <code>recursive = false</code> limits
generation to the package itself.</p>

<p><code>com.tangosol.io.pof.schema.annotation.PortableType</code> also generates allowlist
entries. A class does not need both annotations unless your source code uses
both concepts for clarity.</p>

</div>
</div>

<h2 id="_generated_security_config_xml">Generated <code>security-config.xml</code></h2>
<div class="section">
<p>The generated artifact is <code>META-INF/coherence/security-config.xml</code>. At member
start, Coherence loads every such resource visible to the thread context class
loader, validates each resource against <code>coherence-security-config.xsd</code>, and
merges the allowed class names into a single runtime allowlist. A malformed
or invalid resource fails member startup and names the offending URL. The
schema is shipped as <code>coherence-security-config.xsd</code> in the Coherence jar.</p>

<p>A generated file looks like this:</p>

<markup
lang="xml"

>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;security-config xmlns="http://xmlns.oracle.com/coherence/coherence-security-config"&gt;
  &lt;allowed-classes&gt;
    &lt;class name="com.acme.billing.InvoiceFilter"       source="@Remote.Allowed"/&gt;
    &lt;class name="com.acme.billing.InvoiceProcessor"    source="@Remote.Allowed"/&gt;
    &lt;class name="com.acme.billing.model.Invoice"       source="@PortableType"/&gt;
    &lt;class name="com.acme.billing.util.MoneyExtractor" source="@Remote.Allowed(package)"/&gt;
  &lt;/allowed-classes&gt;
&lt;/security-config&gt;</markup>

<p>Each jar on the runtime classpath contributes its own
<code>META-INF/coherence/security-config.xml</code>; the runtime unions all of them
into a single allowlist. This means multi-module applications do not need a
single curated file - each module that sends wire payload classes ships its
own generated resource. A class that is not listed in any generated file and
is not added via <code>coherence.serialization.allowed</code> is rejected at the gate.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>"Wire payload classes" means more than the obvious entry processors,
filters, aggregators, and POF data types. Any class whose state crosses a
Coherence wire boundary is subject to the allowlist gate, including classes
captured by the closure of a serialized lambda. If your code writes
<code>cache.invokeAll(filter, entry &#8594; doSomething(entry, helper))</code> and <code>helper</code>
is an instance of <code>com.acme.PricingHelper</code>, then <code>PricingHelper</code> is part of
the lambda&#8217;s serialized state and must be allowlisted on the receiver. The
typical fix is to annotate the helper class with <code>@Remote.Allowed</code> or apply
a package-level annotation. After upgrade, the most common
<code>security-config-missing</code> rejections come from such capturer classes rather
than from obvious payload types.</p>
</p>
</div>
</div>

<h2 id="_lambda_serialization">Lambda Serialization</h2>
<div class="section">
<p>Coherence serializes Java lambdas in one of two representations. STATIC
lambda serialization sends a symbolic reference to a compiled lambda class
that already exists on both sender and receiver; the receiver resolves it by
name. DYNAMIC lambda serialization synthesizes bytecode on the sender and
ships it to the receiver, which defines the class on arrival. DYNAMIC is
Coherence&#8217;s default and recommended mode because it avoids lockstep
class-version coordination: a new lambda can flow from an upgraded sender to
an older receiver without both sides first deploying the same compiled
class.</p>

<p>When <code>coherence.security.mode=hardened</code>, the DYNAMIC lambda gate denies
wire-supplied bytecode from unauthenticated peers before class definition
unless <code>coherence.remote.dynamic.unauthenticated=allow</code> is set. Applications
that opt in to hardened mode have two supported migration paths:</p>

<ul class="ulist">
<li>
<p>Set <code>-Dcoherence.remote.dynamic.unauthenticated=allow</code> (the recommended
path). This preserves DYNAMIC lambda serialization and the rolling-upgrade
ergonomics that make it Coherence&#8217;s default. It is a long-term option, not
deprecated; a future Coherence release will layer subject-aware permission
checks on top of this path. Monitor rejection telemetry on the
<code>site=lambda</code> counter to confirm traffic shape after enabling.</p>

</li>
<li>
<p>Switch to STATIC by setting <code>-Dcoherence.lambdas=static</code>. This avoids
wire-supplied bytecode entirely but couples every sender and receiver to
the same compiled lambda class: every producer and consumer must redeploy
the same code version before traffic flows. This is appropriate only for
deployments that already manage lockstep rolling upgrades; it trades
upgrade ergonomics for wire-bytecode minimisation.</p>

</li>
</ul>
</div>

<h2 id="_remote_executable_objects">Remote Executable Objects</h2>
<div class="section">
<p>Remote executable objects are application classes whose methods Coherence may
invoke on behalf of a remote request.</p>


<h3 id="_what_counts_as_an_executable">What Counts As An Executable</h3>
<div class="section">
<ul class="ulist">
<li>
<p>Processors ? <code>InvocableMap.EntryProcessor</code> implementations that run against
cache entries.</p>

</li>
<li>
<p>Aggregators ? <code>InvocableMap.EntryAggregator</code> implementations that compute
server-side aggregate results.</p>

</li>
<li>
<p>Invocables ? <code>Invocable</code> implementations submitted to the
<code>InvocationService</code>.</p>

</li>
<li>
<p>Filters ? <code>Filter</code> implementations evaluated against cache, query, topic,
REST, or gRPC data.</p>

</li>
<li>
<p>Value extractors ? <code>ValueExtractor</code> implementations that read values during
server-side filtering, sorting, indexing, or projection.</p>

</li>
<li>
<p>Comparators ? <code>Comparator</code> implementations used for server-side ordering.</p>

</li>
<li>
<p>Triggers ? <code>MapTrigger</code> implementations installed to run during cache
mutation.</p>

</li>
<li>
<p>Server-side event interceptors ? <code>EventInterceptor</code> implementations that
run in the cluster when matching events fire.</p>

</li>
<li>
<p><code>coherence-concurrent</code> tasks / callables / runnables ? executable work
submitted through the Coherence concurrent APIs.</p>

</li>
</ul>
<p>Client-installed <code>MapListener`s are routed, not invoked, on the server; they
do not require `@Remote.Executable</code>. A listener that additionally implements
<code>MapTrigger</code> is governed by the <code>MapTrigger</code> rule.</p>

</div>

<h3 id="_the_annotation">The Annotation</h3>
<div class="section">
<markup
lang="java"

>@Remote.Executable
public class PriceAdjustmentProcessor
        implements InvocableMap.EntryProcessor&lt;String, Order, Void&gt; { ... }</markup>

<p><code>@Remote.Executable</code> has CLASS retention and is class-level only. It implies
<code>@Remote.Allowed</code>: a class that is executable is also a class the receiver may
deserialize. The build-time generator emits a single entry in
<code>security-config.xml</code> with <code>source="@Remote.Executable"</code> and
<code>executable="true"</code>. At runtime, the security-config reader adds that class to
<code>executableFqns()</code> as well as <code>allowedFqns()</code>.</p>

</div>

<h3 id="_xml_alternative">XML Alternative</h3>
<div class="section">
<p>You can also author the same classification by hand in
<code>META-INF/coherence/security-config.xml</code>:</p>

<markup
lang="xml"

>&lt;class name="com.example.PriceAdjustmentProcessor"
       source="@Remote.Executable"
       executable="true"/&gt;</markup>

<p>Runtime merge semantics are the same as generated allowlist entries: Coherence
unions every visible classpath source into one view. See
<router-link to="#_generated_security_config_xml" @click.native="this.scrollFix('#_generated_security_config_xml')"></router-link> for the full merge story.</p>

</div>

<h3 id="_invocationservice">InvocationService</h3>
<div class="section">
<p>When security hardening disables the Coherence*Extend <code>InvocationService</code>
proxy by default, operators who intentionally expose remote invocation can set
<code>coherence.invocation.enabled=true</code> or configure
<code>&lt;proxy-config&gt;&lt;invocation-service-proxy&gt;&lt;enabled&gt;true&lt;/enabled&gt;&lt;/invocation-service-proxy&gt;&lt;/proxy-config&gt;</code>.
The system property overrides operational configuration when both are present.</p>

<p>When the proxy is enabled, every remote <code>Invocable</code> submitted through Extend is
checked by <code>RemoteExecutablePolicy</code> before <code>InvocationService.query(&#8230;&#8203;)</code>
executes it and before any delegated <code>PriorityTask</code> scheduling, timeout, or
cancellation callback can run. The invocable class must carry
<code>@Remote.Executable</code> or appear with <code>executable="true"</code> in merged
<code>security-config.xml</code>. Cluster-internal <code>Invocable</code> dispatch is not gated by
the Extend proxy flag.</p>

<p>If the proxy is disabled, the server log records
<code>InvocationService proxy is DISABLED in &lt;mode&gt; mode by policy</code> once at proxy
startup, while the client receives the same generic service-unavailable shape
used for an unbound Extend receiver. The wire response intentionally does not
name the policy decision. See the Coherence security guide section "Securing
Extend Client Connections" for Extend authentication setup.</p>

</div>

<h3 id="_methodinvocationprocessor_scriptprocessor">MethodInvocationProcessor / ScriptProcessor</h3>
<div class="section">
<p><code>MethodInvocationProcessor</code> and <code>ScriptProcessor</code> are built-in
<code>EntryProcessor</code> implementations that execute behavior described by their
serialized state. Both classes are <code>final</code>, annotated with
<code>@Remote.Executable</code>, and checked by <code>RemoteExecutablePolicy</code> before their
dynamic-remote gates run.</p>

<p>When <code>coherence.security.mode=hardened</code>, both processors are denied by default
unless <code>coherence.remote.dynamic.unauthenticated=allow</code> is set. With
<code>coherence.security.mode</code> unset or set to <code>compatibility</code>, the dynamic-remote
mode gate allows them by default while preserving hard-floor deny-list checks.
The same property therefore controls unauthenticated DYNAMIC lambda bytecode,
reflective method invocation through <code>MethodInvocationProcessor</code>, and script
evaluation through <code>ScriptProcessor</code>.</p>

<p><code>MethodInvocationProcessor</code> also applies the lambda bytecode deny-list before
reflection. The supplier class, the runtime target class, and the
<code>targetClass#methodName</code> pair are each checked against
<code>META-INF/coherence/lambda-bytecode-denylist.txt</code> and extension resources such
as <code>META-INF/coherence/lambda-bytecode-denylist.d/*.txt</code>. Entries such as
<code>java.lang.Runtime</code> and <code>java.lang.System#exit</code> are hard floors in every
security mode. When a processor targets an absent cache entry, the
supplier class is checked first, but the supplier is not executed and the
entry is not populated until the dynamic-remote mode gate allows MIP.</p>

<p><code>ScriptProcessor</code> runs scripts in the singleton Graal <code>Context</code> with explicit
host access. <code>allowAllAccess(true)</code> is not enabled. Host class lookup rejects
deny-listed classes, and loadable deny-listed classes are also blocked through
Graal <code>HostAccess.denyAccess(&#8230;&#8203;)</code>. These HostAccess checks are hard floors in
every mode.</p>

</div>

<h3 id="_maptrigger_remote_install_and_removal">MapTrigger Remote Install and Removal</h3>
<div class="section">
<p>Remote <code>MapTrigger</code> installation and removal through Coherence*Extend are
checked before the listener is registered with or removed from the storage
tier. The trigger class must carry <code>@Remote.Executable</code> or appear with
<code>executable="true"</code> in merged <code>META-INF/coherence/security-config.xml</code>;
unannotated trigger classes are rejected in hardened security mode and shadowed
in compatibility security mode.</p>

<p>After the class policy check, dynamic trigger classes use the same mode gate as
DYNAMIC lambdas, <code>MethodInvocationProcessor</code>, and <code>ScriptProcessor</code>. Hardened
security mode denies dynamic trigger installation by default unless
<code>coherence.remote.dynamic.unauthenticated=allow</code> is set. Compatibility mode
permits dynamic trigger installation by default while emitting
<code>result=would_reject</code> shadow telemetry where the hardened policy would reject.</p>

<p>Classes declared in cache configuration remain server-trusted and are not
blocked at startup. Coherence logs a WARN for each unannotated declared
<code>MapTrigger</code> or <code>EventInterceptor</code> class, de-duplicated by class name and kind,
so operators can classify those classes before allowing equivalent remote
installs. The Slice C audit did not find a remote-supplied <code>EventInterceptor</code>
install path; interceptor coverage is therefore limited to this startup
advisory.</p>

</div>

<h3 id="_topic_subscriber_filter_extractor_install">Topic Subscriber Filter / Extractor Install</h3>
<div class="section">
<p>Remote topic subscriber group and subscriber creation through Coherence*Extend
and gRPC are checked before the subscriber metadata is installed on the storage
tier. Subscriber filters use <code>OperationReason.EVALUATE_FILTER</code>; subscriber
extractors and converters use <code>OperationReason.EXTRACT</code>; both publish
<code>role=TOPICS</code> in telemetry. The filter or extractor class must carry
<code>@Remote.Executable</code> or appear with <code>executable="true"</code> in merged
<code>META-INF/coherence/security-config.xml</code>. Unannotated classes are rejected in
hardened security mode and shadowed in compatibility security mode.</p>

<p>Coherence also walks shipped topic subscriber containers that can hide nested
executable state before install and again during persisted replay. This
includes filter wrappers such as <code>ArrayFilter</code>, <code>ExtractorFilter</code>,
<code>LimitFilter</code>, and <code>FilterWrapper</code>; extractor wrappers such as
<code>ConditionalExtractor</code>, <code>CollectionExtractor</code>, <code>KeyExtractor</code>, fragment
extractors, and transaction wrappers; comparator wrappers such as
<code>ExtractorComparator</code>, <code>ChainedComparator</code>, and <code>SafeComparator</code>; and
script-backed subscriber filters and extractors. Each nested class is checked
with topic subscriber telemetry and replay-drift handling. Customer-defined
composites with opaque fields are not inspected reflectively; annotate or
allowlist the composite class and any nested classes exposed through the
built-in containers.</p>

<p>Dynamic or lambda-shaped subscriber filter and extractor classes use the same
dynamic remote mode gate as DYNAMIC lambdas, <code>MethodInvocationProcessor</code>,
<code>ScriptProcessor</code>, and remote <code>MapTrigger</code> installs. Hardened security mode
denies dynamic subscriber metadata by default unless
<code>coherence.remote.dynamic.unauthenticated=allow</code> is set. Compatibility security
mode permits dynamic subscriber metadata by default while emitting
<code>result=would_reject</code> shadow telemetry where the hardened policy would reject.</p>

<p>Persisted subscriber metadata is rechecked when topic subscription metadata is
activated from persistence. The
<code>coherence.topics.persisted.policy-drift</code> property controls how Coherence
handles metadata whose class was valid at install time but is no longer
executable at replay time:</p>

<ul class="ulist">
<li>
<p><code>reject</code> refuses the replayed subscription and emits
<code>sub_reason=replay_drift</code>.</p>

</li>
<li>
<p><code>warn-allow</code> emits a deduped WARN for the drifted class and allows replay.</p>

</li>
</ul>
<p>The default is <code>reject</code> in hardened security mode and <code>warn-allow</code> in
compatibility security mode. An explicit <code>reject</code> value fails closed in both
security modes. Operators should use
<code>warn-allow</code> only as a migration bridge while annotating the affected
filter/extractor classes, regenerating <code>security-config.xml</code>, and redeploying
the owning artifacts.</p>

<p>Cache-config-declared topic subscriber advisory logging is deferred. The topic
scheme bootstrap paths currently do not materialize the declared filter or
extractor instances early enough to emit a startup advisory without moving
topic subscription realization. Remote topic subscriber installs and persisted
replay rechecks are still enforced as described above.</p>

</div>

<h3 id="_cache_data_plane_processor_aggregator_filter_extractor_comparator_install">Cache Data-Plane Processor / Aggregator / Filter / Extractor / Comparator Install</h3>
<div class="section">
<p>Remote <code>NamedCache</code> data-plane operations through Coherence*Extend, REST, and
gRPC are checked after the wire payload or registry alias resolves to a
concrete executable object and before dispatch to the storage tier. Entry
processors use <code>OperationReason.PROCESS_ENTRY</code>; aggregators use
<code>AGGREGATE</code>; filters use <code>EVALUATE_FILTER</code>; value extractors use <code>EXTRACT</code>;
comparators use <code>COMPARE</code>. Telemetry publishes <code>role=EXTEND_PROXY</code>,
<code>role=REST</code>, or <code>role=GRPC</code> depending on the transport.</p>

<p>The processor, aggregator, filter, extractor, or comparator class must carry
<code>@Remote.Executable</code> or appear with <code>executable="true"</code> in merged
<code>META-INF/coherence/security-config.xml</code>. Unannotated classes are rejected in
hardened security mode and shadowed in compatibility security mode. Dynamic,
lambda-shaped, or script-backed cache data-plane executables also pass through
the dynamic remote mode gate: hardened security mode denies them by default
unless <code>coherence.remote.dynamic.unauthenticated=allow</code> is set; compatibility
security mode permits them by default while emitting <code>result=would_reject</code>
shadow telemetry where the hardened policy would reject. The shared script
gate covers <code>ScriptProcessor</code>, <code>ScriptFilter</code>,
<code>ScriptValueExtractor</code>, and <code>ScriptAggregator</code>; the check runs at remote
install time where the transport exposes the script object and again directly
before script execution or delegate creation.</p>

<p>Coherence also walks the built-in composite containers that can hide nested
executables. This includes processor wrappers such as <code>CompositeProcessor</code>,
<code>ConditionalProcessor</code>, asynchronous / priority processor wrappers,
<code>ExtractorProcessor</code>, <code>PropertyProcessor</code>, and <code>UpdaterProcessor</code>; aggregator
wrappers such as <code>CompositeAggregator</code>, <code>GroupAggregator</code>, <code>ReducerAggregator</code>,
asynchronous / priority aggregators, and <code>TopNAggregator</code>; filter wrappers such
as <code>ArrayFilter</code>, <code>ExtractorFilter</code>, <code>LimitFilter</code>, <code>MapEventTransformerFilter</code>,
<code>WrapperQueryRecorderFilter</code>, and transaction <code>FilterWrapper</code>; extractor
wrappers such as <code>ConditionalExtractor</code>, <code>CollectionExtractor</code>,
<code>DeserializationAccelerator</code>, transaction <code>ExtractorWrapper</code>, and the fragment
extractors; and comparator wrappers such as <code>ExtractorComparator</code>,
<code>ChainedComparator</code>, and <code>SafeComparator</code>. Comparator payloads that are also
<code>ValueExtractor</code> instances, such as <code>KeyExtractor</code>, <code>ChainedExtractor</code>,
<code>ConditionalExtractor</code>, <code>CollectionExtractor</code>, and other <code>AbstractExtractor</code>
subclasses, are routed through the extractor cascade as well as the comparator
gate; this also applies when they are wrapped by <code>SafeComparator</code>,
<code>InverseComparator</code>, <code>EntryComparator</code>, or transaction <code>ComparatorWrapper</code>.
<code>ValueManipulator</code> and <code>ValueUpdater</code> instances nested inside property and
updater processors are treated as executable mutation state under the
processor install check. Each cascaded class records its own policy telemetry
event. Customer-defined composites with opaque fields are not inspected
reflectively; annotate or allowlist the composite class and any nested classes
that are exposed through the built-in containers above.</p>

<p>REST default processor and aggregator factories preflight the configured class
before invoking its constructor. REST resources still check the produced
processor or aggregator instance before dispatch so nested cascade checks are
not lost. Custom REST <code>ProcessorFactory</code> and <code>AggregatorFactory</code>
implementations remain a trusted compatibility boundary: Coherence cannot
preflight arbitrary factory side effects before the custom factory chooses to
construct or return an executable object.</p>

<p>To migrate an application, identify each rejected or would-reject class,
annotate the supported remote executable type with <code>@Remote.Executable</code> or add
an <code>executable="true"</code> entry to generated security config, redeploy the owning
jar, and rerun the workload with <code>-Dcoherence.security.mode=hardened</code>. For REST
registry entries, classify the registry factory output. For CohQL queries and
gRPC bytes-based requests, classify the concrete filter, comparator, extractor,
aggregator, or processor class that is sent by the client.</p>

</div>

<h3 id="_coherence_concurrent_executor">Coherence Concurrent Executor</h3>
<div class="section">
<p>Coherence Concurrent executor submissions are checked before task execution,
scheduling, executor selection, result collection, completion callbacks, and
subscriber callbacks. Submitted <code>Task</code>, <code>Callable</code>, <code>Runnable</code>,
<code>ExecutionStrategy</code>, <code>Task.Collector</code>, completion predicate, completion
runnable, and <code>Task.Subscriber</code> classes must carry <code>@Remote.Executable</code> or
appear with <code>executable="true"</code> in merged
<code>META-INF/coherence/security-config.xml</code>. Enforcement telemetry uses
<code>OperationReason.CONCURRENT_TASK</code> and <code>role=CONCURRENT</code>.</p>

<p>The concurrent gate also walks shipped executor wrappers that can carry nested
callback state. <code>CallableTask</code>, <code>RunnableTask</code>, <code>RunnableWithResultTask</code>,
scheduled task wrappers, and <code>CronTask</code> gate their nested callable, runnable,
or task state after the task-manager subject is available. <code>CronTask</code> gates
both the original and current task. <code>StandardExecutionStrategy</code> gates its
candidate predicate, and <code>ConditionalCollector</code> gates its predicate and nested
collector. Concurrent cache-processor wrappers such as <code>LocalOnlyProcessor</code>
and <code>ClusteredTaskManager.ChainedProcessor</code> gate their nested processors before
they can invoke them. Built-in predicate wrappers such as
<code>Predicates.not(&#8230;&#8203;)</code> and typed composites such as <code>Predicates.and(&#8230;&#8203;)</code> are
unwrapped and their nested predicates are checked before use. Value-style
predicate wrappers gate nested payloads that could otherwise dispatch
application code through <code>equals(&#8230;&#8203;)</code> or <code>toString()</code>: unsafe
<code>Predicates.equalTo(&#8230;&#8203;)</code> values, non-built-in <code>Predicates.has(&#8230;&#8203;)</code>
registration options, and non-JDK <code>Predicates.onException(&#8230;&#8203;)</code> throwables are
checked before the predicate can run. Common scalar values, built-in
registration options, exact product <code>Member</code> registration metadata, and
bootstrap <code>java.*</code> throwables remain data-only. Crafted or non-product nested
members inside <code>Predicates.has(Member.of(&#8230;&#8203;))</code> are checked before comparison.
<code>ValueTask</code> is gated as a task, but its value is result data and is not treated
as an executable callback merely because it implements a callback interface.
Task properties, task options, execution plans, retained results, and tracing
context are data-only for this gate.</p>

<p>Local member submissions capture the current caller <code>Subject</code> as advisory gate
context and serialize it with <code>ClusteredTaskManager</code> at POF index <code>20</code> and as a
final appended ExternalizableLite field. Remote-client submissions through
remote cache services, including <code>RemoteExecutor</code> client paths, serialize a
<code>null</code> subject; client JVM subjects are not authorization-bearing. The subject
is not used with <code>Subject.doAs(&#8230;&#8203;)</code> and does not grant per-subject execution
permission.</p>

<p>The <code>ConcurrentProxy</code> Extend service uses the existing
<code>coherence.concurrent.extend.enabled</code> property with a security-mode-aware
default: enabled in compatibility security mode and disabled in hardened
security mode. A disabled proxy logs the resolved runtime mode, security mode,
property/config source, and enabled state at startup, plus a startup WARN in
hardened mode naming the override property. Clients still see a generic
service-unavailable shape rather than policy-specific wire text.
Cluster-internal executor use is independent of this proxy switch.</p>

</div>

<h3 id="_functional_interface_lambda_targets">Functional-Interface Lambda Targets</h3>
<div class="section">
<p>A lambda target is the functional interface named by a serialized lambda. For
example, a lambda sent as a <code>Remote.Function</code> declares
<code>com.tangosol.util.function.Remote$Function</code> as its functional-interface
target; the generated lambda class is a separate implementation detail.</p>

<p>Coherence pre-checks that functional interface before it runs the lambda
bytecode scanner. The target check answers "may this receiver materialize a
lambda for this API surface?" with a constant-time lookup in merged
<code>security-config.xml</code>. The bytecode scanner still answers the separate
question "does this wire-supplied bytecode contain denied references or
unsupported shape?" Both checks apply; a target allowlist entry does not
disable bytecode scanning.</p>

<p>To allow a custom serializable functional interface as a remote lambda target,
annotate the interface with <code>@Remote.Executable</code> and ensure it also carries
<code>@FunctionalInterface</code>:</p>

<markup
lang="java"

>@FunctionalInterface
@Remote.Executable
public interface PriceFunction
        extends java.io.Serializable
    {
    BigDecimal apply(Order order);
    }</markup>

<p>The build-time scanner emits:</p>

<markup
lang="xml"

>&lt;class name="com.example.PriceFunction"
       source="@Remote.Executable"
       lambda-target="true"/&gt;</markup>

<p>You can also author that XML entry by hand. In that case the entry must name
the functional interface, not the generated lambda implementation class.</p>

<p>Unannotated functional interfaces are denied in hardened security mode before
lambda bytecode is materialized. In compatibility security mode, allowlist and
lambda-target enforcement are disabled; use
<code>-Dcoherence.security.mode=hardened</code> in pre-production to validate
lambda-target readiness before migration. The supported migration is to
annotate the functional interface or add a manual <code>lambda-target="true"</code> entry
to the artifact that owns it.</p>

<p>When the target pre-check rejects a lambda, the reason is
<code>REASON_LAMBDA_TARGET_NOT_ALLOWED</code> and the operator-visible reason string is
<code>lambda-target-not-allowed</code>. Pre-GA adopters who previously relied on custom
serializable functional interfaces should rebuild those artifacts with
<code>@Remote.Executable</code>, verify the generated <code>security-config.xml</code>, and redeploy
receivers before sending lambdas that target the new interface.</p>

</div>

<h3 id="_migration">Migration</h3>
<div class="section">
<p><code>@Remote.Executable</code> covers two distinct cases that have different
runtime-enforcement timelines in 26.04:</p>

<ul class="ulist">
<li>
<p><strong>Functional interfaces</strong> annotated with <code>@Remote.Executable</code> are enforced
today when security mode is hardened. Lambdas that target an unannotated
functional interface are rejected with <code>lambda-target-not-allowed</code> before
bytecode is materialized. Migrate by annotating every custom serializable
functional interface used as a remote lambda target and rebuilding the
artifact that owns it. See <router-link to="#_functional_interface_lambda_targets" @click.native="this.scrollFix('#_functional_interface_lambda_targets')"></router-link>.</p>

</li>
<li>
<p><strong>Concrete executable classes</strong> (entry processors, aggregators, invocables,
filters, value extractors, comparators, triggers, server-side event
interceptors, <code>coherence-concurrent</code> tasks) are classified by the build
in 26.04. Runtime enforcement covers the CACHE-01 remote execution sinks
described above. Classify executable classes now, regenerate
<code>security-config.xml</code>, and use
<code>coh.executable.policy_check{result=would_reject}</code> shadow telemetry to
drive the migration; see <router-link to="#_telemetry" @click.native="this.scrollFix('#_telemetry')"></router-link>.</p>

</li>
</ul>
<p>In both cases the build pipeline is the same. Rebuild every artifact that
owns an executable class or functional interface; the
<code>security-config-maven-plugin</code> and the Gradle <code>securityConfig</code> task pick up
the annotations automatically.</p>

</div>

<h3 id="_why_remote_executable_and_not_just_remote_allowed">Why <code>@Remote.Executable</code> And Not Just <code>@Remote.Allowed</code>?</h3>
<div class="section">
<p><code>@Remote.Allowed</code> answers "can this class be deserialised on the server?";
<code>@Remote.Executable</code> answers "may the server invoke its methods on behalf of
a remote request?" The latter is a strict subset and deserves an explicit
opt-in so that adding a class to the deserialisation allowlist does not
silently grant it execution privileges.</p>

</div>
</div>

<h2 id="_runtime_property_reference">Runtime Property Reference</h2>
<div class="section">

<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 18.182%;">
<col style="width: 9.091%;">
<col style="width: 9.091%;">
<col style="width: 9.091%;">
<col style="width: 36.364%;">
<col style="width: 18.182%;">
</colgroup>
<thead>
<tr>
<th>Property</th>
<th>Type</th>
<th>Default</th>
<th>Since</th>
<th>Description</th>
<th>See</th>
</tr>
</thead>
<tbody>
<tr>
<td class=""><code>coherence.mode</code></td>
<td class="">enum: <code>eval</code>, <code>dev</code>, <code>development</code>, <code>prod</code>, <code>production</code></td>
<td class=""><code>dev</code></td>
<td class="">26.04</td>
<td class="">Selects the released runtime/license mode. It does not enable the
compatibility-sensitive SER-01 hardening gates. Blank, unknown, <code>legacy</code>, and
<code>legacy-compatibility</code> values fail fast.</td>
<td class=""><router-link to="#_runtime_modes" @click.native="this.scrollFix('#_runtime_modes')"></router-link></td>
</tr>
<tr>
<td class=""><code>coherence.security.mode</code></td>
<td class="">enum: <code>compatibility</code>, <code>hardened</code></td>
<td class=""><code>compatibility</code></td>
<td class="">15.1.2.0</td>
<td class="">Selects the global SER-01 security hardening posture. Unset or
<code>compatibility</code> preserves prior-patch behavior and shadow telemetry where
available; <code>hardened</code> enables the compatibility-sensitive hardening gates.
The removed pre-release <code>coherence.security.hardened</code> property is not an alias.</td>
<td class=""><router-link to="#_runtime_modes" @click.native="this.scrollFix('#_runtime_modes')"></router-link></td>
</tr>
<tr>
<td class=""><code>coherence.remote.dynamic.unauthenticated</code></td>
<td class="">enum: <code>allow</code>, <code>deny</code></td>
<td class="">security mode <code>compatibility</code>: <code>allow</code>; security mode <code>hardened</code>: <code>deny</code></td>
<td class="">26.04</td>
<td class="">Controls whether unauthenticated wire-arriving DYNAMIC lambda bytecode,
reflective <code>MethodInvocationProcessor</code> execution, and <code>ScriptProcessor</code>
evaluation are accepted before subject-aware authorization is available. The
property is resolved through Coherence <code>Config</code>, so the
<code>COHERENCE_REMOTE_DYNAMIC_UNAUTHENTICATED</code> environment variable alias is also
honoured. Does not bypass functional-interface target allowlisting, the MIP
deny-list, or Graal HostAccess hardening.</td>
<td class=""><router-link to="#_lambda_serialization" @click.native="this.scrollFix('#_lambda_serialization')"></router-link>, <router-link to="#_methodinvocationprocessor_scriptprocessor" @click.native="this.scrollFix('#_methodinvocationprocessor_scriptprocessor')"></router-link></td>
</tr>
<tr>
<td class=""><code>coherence.invocation.enabled</code></td>
<td class="">boolean</td>
<td class="">operational config value unless explicitly set</td>
<td class="">26.04</td>
<td class="">Overrides whether the Coherence*Extend <code>InvocationService</code> proxy is exposed.
When unset, operational config <code>&lt;invocation-service-proxy&gt;&lt;enabled&gt;</code> is used.
This property does not bypass <code>@Remote.Executable</code> enforcement when the proxy
is enabled.</td>
<td class=""><router-link to="#_invocationservice" @click.native="this.scrollFix('#_invocationservice')"></router-link></td>
</tr>
<tr>
<td class=""><code>coherence.concurrent.extend.enabled</code></td>
<td class="">boolean</td>
<td class="">security mode <code>compatibility</code>: <code>true</code>; security mode <code>hardened</code>: <code>false</code></td>
<td class="">26.04</td>
<td class="">Overrides whether the Coherence Concurrent <code>ConcurrentProxy</code> Extend service is
exposed. When unset, the shipped <code>ConcurrentProxy</code> autostart marker resolves to
the security-mode-aware default. An explicit cache-config autostart override
without the shipped system-property marker wins when the property is absent.
This property does not bypass <code>@Remote.Executable</code> enforcement when the proxy
is enabled.</td>
<td class=""><router-link to="#_coherence_concurrent_executor" @click.native="this.scrollFix('#_coherence_concurrent_executor')"></router-link></td>
</tr>
<tr>
<td class=""><code>coherence.topics.persisted.policy-drift</code></td>
<td class="">enum: <code>reject</code>, <code>warn-allow</code></td>
<td class="">security mode <code>compatibility</code>: <code>warn-allow</code>; security mode <code>hardened</code>: <code>reject</code></td>
<td class="">26.04</td>
<td class="">Controls persisted topic subscriber filter/extractor policy drift on replay.
<code>reject</code> refuses persisted metadata whose class is no longer executable;
<code>warn-allow</code> logs a deduped WARN and allows replay for migration. The property
is resolved through Coherence <code>Config</code>, so the
<code>COHERENCE_TOPICS_PERSISTED_POLICY_DRIFT</code> environment variable alias is also
honoured.</td>
<td class=""><router-link to="#_topic_subscriber_filter_extractor_install" @click.native="this.scrollFix('#_topic_subscriber_filter_extractor_install')"></router-link></td>
</tr>
<tr>
<td class=""><code>coherence.serialization.allowed</code></td>
<td class="">class-fqn-list</td>
<td class="">empty</td>
<td class="">26.04</td>
<td class="">Adds exact class names or package wildcards, such as <code>com.acme.*</code>, to the
runtime serialization allowlist. It supplements generated security config and
does not bypass deny-listed gadget classes.</td>
<td class=""><router-link to="#_generated_security_config_xml" @click.native="this.scrollFix('#_generated_security_config_xml')"></router-link></td>
</tr>
<tr>
<td class=""><code>coherence.lambda.bytecode.allow</code></td>
<td class="">class-fqn-list</td>
<td class="">empty</td>
<td class="">26.04</td>
<td class="">Incident-response override for specific class references in the lambda
bytecode deny-list. Structural bytecode rules such as native methods still
apply.</td>
<td class=""><router-link to="#_error_message_reference" @click.native="this.scrollFix('#_error_message_reference')"></router-link></td>
</tr>
<tr>
<td class=""><code>coherence.security-config.skip</code></td>
<td class="">boolean</td>
<td class=""><code>false</code></td>
<td class="">26.04</td>
<td class="">Maven plugin switch that skips <code>security-config.xml</code> generation for the
current build.</td>
<td class=""><router-link to="#_maven" @click.native="this.scrollFix('#_maven')"></router-link></td>
</tr>
<tr>
<td class=""><code>coherence.lambdas</code></td>
<td class="">enum: <code>dynamic</code>, <code>static</code></td>
<td class=""><code>dynamic</code></td>
<td class="">pre-26.04</td>
<td class="">Lambda serialization mode that has shipped in prior Coherence releases. Set
to <code>static</code> when migrating production applications away from DYNAMIC lambda
bytecode at the cost of lockstep client/server code-version coordination.</td>
<td class=""><router-link to="#_lambda_serialization" @click.native="this.scrollFix('#_lambda_serialization')"></router-link></td>
</tr>
</tbody>
</table>
</div>
</div>

<h2 id="_error_message_reference">Error Message Reference</h2>
<div class="section">
<p>SER-01 rejection reasons fall into four families. When debugging a failing
deployment, identify the family first, then locate the specific row in the
table below.</p>

<ul class="ulist">
<li>
<p><strong>Missing allowlist</strong> ? <code>security-config-missing</code>, <code>ClassIdentity package is
not allowed</code>, <code>unknown user type</code>, <code>Deserialization of class &#8230;&#8203; was
rejected</code>. The class is not known to the receiver; fix by annotating and
regenerating, or by registering with POF.</p>

</li>
<li>
<p><strong>Remote execution policy</strong> ? <code>dynamic-remote-denied-by-mode</code>,
<code>method-invocation-denied-by-mode</code>, <code>script-eval-denied-by-mode</code>,
<code>lambda-target-not-allowed</code>, <code>class-name-on-denylist</code>,
<code>method-on-denylist</code>. A lambda-shaped payload, MIP reflection request, or
script request was refused by mode policy, target-interface policy, or by
the explicit deny-list.</p>

</li>
<li>
<p><strong>Bytecode shape</strong> ? <code>bytecode-references-gadget</code>, <code>native-method-declared</code>,
<code>dynamic-class-forname</code>, <code>invalid-bytecode</code>. Wire-supplied bytecode failed
structural or deny-list checks.</p>

</li>
<li>
<p><strong>Dynamic class definition</strong> ? <code>Invalid class definition: missing-magic</code>,
<code>invalid-version</code>, <code>invalid-shape</code>, <code>trailing-bytes</code>. The class-definition
payload did not pass file-format validation.</p>

</li>
</ul>

<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 22.222%;">
<col style="width: 11.111%;">
<col style="width: 33.333%;">
<col style="width: 33.333%;">
</colgroup>
<thead>
<tr>
<th>Reason or message shape</th>
<th>Subsystem</th>
<th>Typical cause</th>
<th>Fix</th>
</tr>
</thead>
<tbody>
<tr>
<td class=""><code>Lambda bytecode rejected: site=%s, reason=security-config-missing, denied=%s</code></td>
<td class="">Allowlist</td>
<td class="">An application class arrived as a class-name payload but is not in generated
security config or the manual allowlist.</td>
<td class="">Annotate the producing class with <code>@Remote.Allowed</code> or <code>@PortableType</code>, run
the Maven or Gradle generator, and package the generated config.</td>
</tr>
<tr>
<td class=""><code>Lambda bytecode rejected: site=LAMBDA, reason=dynamic-remote-denied-by-mode</code></td>
<td class="">Lambda mode gate</td>
<td class="">A DYNAMIC lambda arrived while security mode was hardened and
<code>coherence.remote.dynamic.unauthenticated</code> resolved to <code>deny</code>.</td>
<td class="">Switch to STATIC lambda serialization or explicitly set
<code>-Dcoherence.remote.dynamic.unauthenticated=allow</code>.</td>
</tr>
<tr>
<td class=""><code>method-invocation-denied-by-mode</code></td>
<td class="">MIP mode gate</td>
<td class="">A <code>MethodInvocationProcessor</code> arrived while security mode was hardened and
<code>coherence.remote.dynamic.unauthenticated</code> resolved to <code>deny</code>.</td>
<td class="">Set <code>-Dcoherence.remote.dynamic.unauthenticated=allow</code> only where reflective
method invocation is intentionally exposed, or replace the call with a
purpose-built <code>@Remote.Executable</code> processor.</td>
</tr>
<tr>
<td class=""><code>script-eval-denied-by-mode</code></td>
<td class="">Script mode gate</td>
<td class="">A <code>ScriptProcessor</code> arrived while security mode was hardened and
<code>coherence.remote.dynamic.unauthenticated</code> resolved to <code>deny</code>.</td>
<td class="">Set <code>-Dcoherence.remote.dynamic.unauthenticated=allow</code> only where remote
script evaluation is intentionally exposed, or replace the script with a
purpose-built <code>@Remote.Executable</code> processor.</td>
</tr>
<tr>
<td class=""><code>Lambda bytecode rejected: site=%s, reason=lambda-target-not-allowed, denied=%s</code></td>
<td class="">Lambda target</td>
<td class="">A serialized lambda targets a functional interface that is not marked as a
lambda target in merged security config. The denied value is the functional
interface FQN, not the lambda capture class.</td>
<td class="">Annotate the functional interface with <code>@Remote.Executable</code> and regenerate
security config, or add a manual <code>&lt;class name="&#8230;&#8203;" lambda-target="true"/&gt;</code>
entry for that interface.</td>
</tr>
<tr>
<td class=""><code>reason=bytecode-references-gadget</code></td>
<td class="">Bytecode scan</td>
<td class="">Generated or supplied lambda bytecode references a deny-listed gadget class or
method.</td>
<td class="">Remove the reference or use a safe server-side class. Use
<code>coherence.lambda.bytecode.allow</code> only for emergency class-reference overrides.</td>
</tr>
<tr>
<td class=""><code>reason=class-name-on-denylist</code></td>
<td class="">Bytecode scan / MIP reflection</td>
<td class="">A static lambda, class-name payload, MIP supplier, or MIP target value names a
deny-listed class.</td>
<td class="">Remove the deny-listed class from the payload path or reflective MIP target.</td>
</tr>
<tr>
<td class=""><code>reason=method-on-denylist</code></td>
<td class="">MIP reflection</td>
<td class="">A <code>MethodInvocationProcessor</code> targets a deny-listed <code>class#method</code> pair.</td>
<td class="">Remove the reflective method call and use a purpose-built safe server-side
processor.</td>
</tr>
<tr>
<td class="">Graal <code>PolyglotException</code> or script error naming a denied Java type</td>
<td class="">Script HostAccess</td>
<td class="">A script attempted <code>Java.type(&#8230;&#8203;)</code> or guest-side host access for a
deny-listed class such as <code>java.lang.Runtime</code> or <code>java.lang.ProcessBuilder</code>.</td>
<td class="">Remove the host access from the script. <code>coherence.remote.dynamic.unauthenticated</code>
does not bypass the HostAccess hard floor.</td>
</tr>
<tr>
<td class=""><code>reason=native-method-declared</code></td>
<td class="">Bytecode scan</td>
<td class="">Wire-supplied bytecode declares a native method.</td>
<td class="">Do not send dynamic bytecode that declares native methods.</td>
</tr>
<tr>
<td class=""><code>reason=dynamic-class-forname</code></td>
<td class="">Bytecode scan</td>
<td class="">Wire-supplied bytecode calls <code>Class.forName</code> with a non-constant value.</td>
<td class="">Resolve the class server-side or remove dynamic class loading from the lambda.</td>
</tr>
<tr>
<td class=""><code>reason=invalid-bytecode</code></td>
<td class="">Bytecode scan / Class definition</td>
<td class="">The supplied class bytes are empty, malformed, or cannot be parsed.</td>
<td class="">Regenerate the payload with a supported Coherence version and check for
transport corruption.</td>
</tr>
<tr>
<td class=""><code>Invalid class definition: missing-magic</code></td>
<td class="">Class definition</td>
<td class="">A class-definition payload does not start with the Java class-file magic
number.</td>
<td class="">Reject the payload source; resend only valid class files.</td>
</tr>
<tr>
<td class=""><code>Invalid class definition: invalid-version</code></td>
<td class="">Class definition</td>
<td class="">A class-definition payload uses an unsupported class-file version.</td>
<td class="">Build the producing artifact for a Java version supported by the receiving
member.</td>
</tr>
<tr>
<td class=""><code>Invalid class definition: invalid-shape</code></td>
<td class="">Class definition</td>
<td class="">A class-definition payload has an unexpected constant-pool, method, or class
shape.</td>
<td class="">Regenerate the payload and verify client/server version compatibility.</td>
</tr>
<tr>
<td class=""><code>Invalid class definition: trailing-bytes</code></td>
<td class="">Class definition</td>
<td class="">A class-definition payload contains bytes after the parsed class file.</td>
<td class="">Reject the payload source; resend a clean class definition.</td>
</tr>
<tr>
<td class=""><code>ClassIdentity package is not allowed: %s</code></td>
<td class="">Class identity / Allowlist</td>
<td class="">A dynamic class identity names a package not present in generated security
config or the runtime allowlist.</td>
<td class="">Annotate the producing type or package and regenerate security config.</td>
</tr>
<tr>
<td class=""><code>unknown user type: %s. Unregistered POF type &#8230;&#8203; strict mode</code></td>
<td class="">POF strict / Allowlist</td>
<td class="">A POF payload names a user type that is not registered in the receiver&#8217;s POF
configuration.</td>
<td class="">Register the class in <code>&lt;pof-config&gt;</code>, annotate it with <code>@PortableType</code>, or add
<code>@Remote.Allowed</code> and regenerate security config.</td>
</tr>
<tr>
<td class=""><code>Deserialization of class %s was rejected</code></td>
<td class="">JEP-290 filter</td>
<td class="">The JEP-290 filter rejected a Java-serialization class in a fail-closed path.</td>
<td class="">Use a registered safe type or add the application type to generated security
config or <code>coherence.serialization.allowed</code>.</td>
</tr>
<tr>
<td class=""><code>coh.executable.policy_check{result=would_reject, class=&lt;FQN&gt;, reason=&lt;OperationReason&gt;, role=&lt;SerializationRole&gt;}</code></td>
<td class="">Executable policy (shadow)</td>
<td class="">Compatibility security mode classified a remote executable object that would
be rejected by <code>DefaultRemoteExecutablePolicy.enforce(&#8230;&#8203;)</code> in hardened
security mode.</td>
<td class="">Treat each tuple as migration work: annotate the class with
<code>@Remote.Executable</code>, regenerate <code>security-config.xml</code>, or add a manual
<code>executable="true"</code> entry before setting
<code>-Dcoherence.security.mode=hardened</code>.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h2 id="_telemetry">Telemetry</h2>
<div class="section">
<p>Serialization gate decisions are visible through <code>SerializationGates</code> MBeans.
ObjectNames include the route, gate, result, reason, and mode tuple, for
example:</p>

<markup
lang="text"

>Coherence:type=SerializationGates,route=GRPC,gate=filter,result=rejected,reason=serialization-allowlist-rejected,mode=prod</markup>

<p>Lambda bytecode checks also maintain the in-process counter key:</p>

<markup
lang="text"

>coh.lambda.bytecode_check{result=rejected,reason=dynamic-remote-denied-by-mode,site=lambda}</markup>

<p>Use these counters to identify which boundary is rejecting traffic before
loosening any runtime property.</p>

<p>Live remote executable enforcement checks in hardened security mode maintain:</p>

<markup
lang="text"

>coh.executable.policy_check{reason=&lt;OperationReason&gt;, role=&lt;SerializationRole&gt;, result=allowed|rejected, mode=&lt;runtime-mode&gt;}</markup>

<p>For Extend <code>InvocationService</code>, the tuple uses <code>reason=INVOKE</code> and
<code>role=EXTEND_PROXY</code>. <code>MethodInvocationProcessor</code> and <code>ScriptProcessor</code> use
<code>reason=PROCESS_ENTRY</code> and <code>reason=SCRIPT_EVAL</code>, respectively. Remote
<code>MapTrigger</code> installation uses <code>reason=TRIGGER</code> and <code>role=EXTEND_PROXY</code>.
Topic subscriber filter/extractor installation and persisted replay use
<code>reason=EVALUATE_FILTER|EXTRACT</code> and <code>role=TOPICS</code>. Cache data-plane
processor, aggregator, filter, extractor, and comparator installs through
Extend, REST, and gRPC use
<code>reason=PROCESS_ENTRY|AGGREGATE|EVALUATE_FILTER|EXTRACT|COMPARE</code> and
<code>role=EXTEND_PROXY|REST|GRPC</code>. Coherence Concurrent executor task and callback
installs use <code>reason=CONCURRENT_TASK</code> and <code>role=CONCURRENT</code>. Live tuples may also include
<code>sub_reason=policy|mode_gate|denylist|replay_drift</code>; the tag is additive and
callers must not assume a closed set. Successful policy checks use
<code>sub_reason=policy</code>; mode and deny-list sub-reasons are emitted only for
refusals. Topic replay under <code>warn-allow</code> records
<code>result=allowed,sub_reason=replay_drift</code> to make migration drift visible.</p>

<p>Remote executable policy shadow checks in compatibility security mode maintain:</p>

<markup
lang="text"

>coh.executable.policy_check{result=would_reject, class=&lt;FQN&gt;, reason=&lt;OperationReason&gt;, role=&lt;SerializationRole&gt;}</markup>

<p>This counter is emitted only by <code>DefaultRemoteExecutablePolicy.enforce(&#8230;&#8203;)</code>.
It tells you which remote executable classes would be rejected after migration
to hardened security mode. The <code>mode</code> tag is intentionally absent from these
shadow tuples, while <code>class</code> is included to make per-class dry-run diagnostics
possible. It does not measure serialization allowlist or lambda bytecode
readiness; validate those gates by running the workload under
<code>-Dcoherence.security.mode=hardened</code> in a non-production environment. Topic
subscriber replay records one <code>would_reject</code> tuple per compatibility shadow;
install-time dynamic shadows can record policy and mode-gate tuples because
replay has no dynamic mode-gate analog.</p>

</div>
</doc-view>
