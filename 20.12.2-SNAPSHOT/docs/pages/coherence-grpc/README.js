<doc-view>

<h2 id="_coherence_grpc">Coherence gRPC</h2>
<div class="section">
<p>Coherence gRPC provides the protobuf definitions necessary to interact with a Coherence data management services over gRPC.
This library also provides utilities for making low-level cache requests over gRPC, converting between gRPC and
Coherence binary implementations.</p>

<p>Given this, unless there is a plan to develop a Coherence gRPC client in another language or to create new services
in Java, there is little need for developers to depend on this library.</p>

</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use Coherence gRPC, you need to declare it as a dependency in your <code>pom.xml</code>:</p>

<markup
lang="xml"

>&lt;dependency&gt;
  &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
  &lt;artifactId&gt;coherence-grpc&lt;/artifactId&gt;
  &lt;version&gt;20.12.2-SNAPSHOT&lt;/version&gt;
&lt;/dependency&gt;</markup>

</div>

<h2 id="_protobuf_definitions">Protobuf Definitions</h2>
<div class="section">

<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Proto File</th>
<th>Usage</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">services.proto</td>
<td class="">defines the RPCs for interacting with a Coherence data management services</td>
</tr>
<tr>
<td class="">requests.proto</td>
<td class="">defines the request/response structs for making requests to and receiving responses from Coherence
data management services</td>
</tr>
</tbody>
</table>
</div>
</div>
</doc-view>
