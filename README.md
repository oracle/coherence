<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

<img src="https://oracle.github.io/coherence/assets/images/logo-red.png" alt="Oracle Coherence" width="320">

[![CI Pipeline](https://github.com/oracle/coherence/actions/workflows/ci.yaml/badge.svg?branch=main)](https://github.com/oracle/coherence/actions/workflows/ci.yaml)
[![Maven Central](https://img.shields.io/maven-central/v/com.oracle.coherence.ce/coherence.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.oracle.coherence.ce/coherence)
[![License](https://img.shields.io/badge/license-UPL%201.0-blue.svg)](https://oss.oracle.com/licenses/upl/)

# Oracle Coherence Community Edition

<a id="introduction"></a>

**Fast, resilient state for distributed applications.**

Coherence is an open source, distributed data platform that brings application data and
processing together. Keep frequently used data close to your services, run business
logic where that data lives, and scale storage and computation across a cluster.

Use it to accelerate an existing database, share state across microservices, maintain
live business summaries, or build search and AI applications over your own data.
Coherence combines distributed maps, queries, computation, messaging, and persistence
in a platform designed for applications that need to keep running as demand and
infrastructure change.

[Documentation](https://docs.coherence.community/coherence/docs/latest/) ·
[Try Coherence](https://docs.coherence.community/coherence/docs/latest/examples/guides/050-bootstrap/README) ·
[Releases](https://github.com/oracle/coherence/releases) ·
[Community](https://coherence.community/)

<a id="overview"></a>

## Why Coherence?

- **Scale data and processing together.** Add cluster members to increase capacity.
  Coherence partitions data and routes operations to the members that own it, so
  applications can process data without pulling the entire working set across the network.
- **Build on dependable shared state.** Configurable backups, automatic failover, and
  rebalancing help applications stay available through member failures and planned
  maintenance. Persistence and snapshots provide recovery options beyond live replicas.
- **Turn changing data into useful answers.** Query, aggregate, and react to data in the
  same cluster. Continuous Aggregation in CE 26.10 maintains partial results as entries
  change, reducing repeated scans for frequently requested summaries.
- **Bring search and AI to application data.** Combine vector similarity search with
  filters on business attributes. Add the Lucene and RAG modules for full-text search,
  document ingestion, and integration with embedding and language models.
- **Keep operational control.** Use metrics, tracing, health checks, JMX, and REST
  management to understand the cluster. Deploy on your own infrastructure or on
  Kubernetes with the Coherence Operator.

## What can you build?

| Application need | How Coherence helps |
| --- | --- |
| Fast customer, product, or reference-data access | Cache frequently used data, keep local views current, and integrate with existing databases. |
| Stateful services at scale | Share application state and process updates alongside the data, with affinity for related records. |
| Live dashboards and operational decisions | Run distributed queries and aggregations; use Continuous Aggregation in CE 26.10 for recurring summaries. |
| Event-driven applications and background work | React to map changes, publish and consume topics, and distribute tasks through the executor framework. |
| Semantic search and retrieval-augmented generation | Search vectors alongside application data and use optional Lucene and RAG integrations. |

## Fit Coherence into your application

Java applications use familiar map and concurrency concepts through APIs such as
`NamedMap` and the Repository API. Applications can join a cluster or connect as remote
clients, allowing storage and application tiers to scale independently.

Clients are available for [Java](prj/examples/guides/070-coherence-extend/README.adoc),
[Go](https://github.com/oracle/coherence-go-client),
[Python](https://github.com/oracle/coherence-py-client),
[JavaScript](https://github.com/oracle/coherence-js-client),
[.NET](https://github.com/oracle/coherence-dotnet-extend-client), and
[C++](https://github.com/oracle/coherence-cpp-extend-client).
Integrations with [Helidon](https://helidon.io/),
[Spring](https://github.com/coherence-community/coherence-spring), and
[Micronaut](https://micronaut-projects.github.io/micronaut-coherence/latest/guide/)
bring Coherence into existing application frameworks.

<a id="getting-started"></a>
<a id="acquire"></a>
<a id="documentation"></a>
<a id="examples"></a>

## Start exploring

- **Understand the platform:** read the [introduction](prj/docs/about/02_introduction.adoc).
- **Build a first application:** follow the [bootstrap guide](prj/examples/guides/050-bootstrap/README.adoc),
  then explore the [examples and tutorials](prj/examples).
- **Choose a distribution:** browse [published releases](https://github.com/oracle/coherence/releases),
  [Maven artifacts](https://central.sonatype.com/namespace/com.oracle.coherence.ce), or
  [container images](https://github.com/oracle/coherence/pkgs/container/coherence-ce).
- **Prepare for CE 26.10:** review the [Core Improvements](prj/docs/core/01_overview.adoc)
  and [Security Hardening](prj/docs/core/04_security_hardening.adoc) guides.
  CE 26.10 requires Java 21 or later and enables hardened mode by default; plan the
  security migration before upgrading an existing application.

## Community Edition and commercial editions

Community Edition is released under the Universal Permissive License (UPL) 1.0.
Oracle Coherence commercial editions offer additional capabilities and support.
Features such as WAN federation and WebLogic-specific integration are outside CE;
see [edition availability](prj/docs/about/02_introduction.adoc#community-edition)
and the [Oracle product documentation](https://docs.oracle.com/en/middleware/standalone/coherence/15.1.1/)
when evaluating those requirements.

<a id="build"></a>
<a id="contributing"></a>

## Build and contribute

Contributions, issue reports, documentation improvements, and examples are welcome.
Start with the [contribution guide](CONTRIBUTING.md),
[build instructions](BUILDING.md), and [development guidelines](DEV-GUIDELINES.md).
For vulnerability reports, follow the [security policy](SECURITY.md).

## License

Copyright (c) 2000, 2026, Oracle and/or its affiliates.

Released under the [Universal Permissive License v1.0](https://oss.oracle.com/licenses/upl/).
