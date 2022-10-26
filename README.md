<!--

  Copyright (c) 2000, 2022, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.

-->

-----
<img src=https://oracle.github.io/coherence/assets/images/logo-red.png><img>

[![CI Build](https://github.com/oracle/coherence/workflows/CI%20Build/badge.svg?branch=master)](https://github.com/oracle/coherence/actions?query=workflow%3A%22CI+Build%22+branch%3Amaster)
[![Maven Central](https://img.shields.io/maven-central/v/com.oracle.coherence.ce/coherence.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.oracle.coherence.ce%22)
[![License](http://img.shields.io/badge/license-UPL%201.0-blue.svg)](https://oss.oracle.com/licenses/upl/)
[![Docker Pulls](https://img.shields.io/docker/pulls/oraclecoherence/coherence-ce)](https://hub.docker.com/r/oraclecoherence/coherence-ce)

# Oracle Coherence Community Edition

## Contents
1. [Introduction](#intro)
1. [How to Get Coherence Community Edition](#acquire)
1. [Coherence Overview](#overview)
1. [Hello Coherence](#get-started)
   1. [Install the Coherence CLI](#install)
   1. [Create a Cluster](#create)
   1. [CohQL Console](#cohql)
   1. [Coherence Console](#coh-console)
   1. [Code Example](#hello-coh)
1. [Building](#build)
1. [Documentation](#documentation)
1. [Contributing](#contrib)

## <a name="intro"></a>Introduction

[Coherence](http://coherence.community/) is a scalable, fault-tolerant, cloud-ready,
distributed platform for building grid-based applications and reliably storing data.
The product is used at scale, for both compute and raw storage, in a vast array of
industries such as critical financial trading systems, high performance telecommunication
products and eCommerce applications.

Typically these deployments do not tolerate any downtime and Coherence is chosen due to its
novel features in death detection, application data evolvability, and the robust,
battle-hardened core of the product that enables it to be seamlessly deployed and
adapted within any ecosystem.

At a high level, Coherence provides an implementation of the familiar `Map<K,V>`
interface but rather than storing the associated data in the local process it is partitioned
(or sharded) across a number of designated remote nodes. This partitioning enables
applications to not only distribute (and therefore scale) their storage across multiple
processes, machines, racks, and data centers but also to perform grid-based processing
to truly harness the CPU resources of the machines.

The Coherence interface `NamedMap<K,V>` (an extension of `Map<K,V>`) provides methods
to query, aggregate (map/reduce style) and compute (send functions to storage nodes
for locally executed mutations) the data set. These capabilities, in addition to
numerous other features, enable Coherence to be used as a framework for writing robust,
distributed applications.

## <a name="acquire"></a>Downloading Coherence Community Edition

As Coherence is generally embedded into an application by using Coherence APIs,
the natural place to consume this dependency is from Maven:

```xml
<dependencies>
    <dependency>
        <groupId>com.oracle.coherence.ce</groupId>
        <artifactId>coherence</artifactId>
        <version>22.06</version>
    </dependency>
</dependencies>
```

You can also get Coherence from the official [Docker site](https://hub.docker.com/r/oraclecoherence/coherence-ce).
For other language clients, use [C++](https://github.com/oracle/coherence-cpp-extend-client) and
[.NET](https://github.com/oracle/coherence-dotnet-extend-client) and for the non-community
edition, see [Oracle Technology Network](https://www.oracle.com/middleware/technologies/coherence-downloads.html).

## <a name="overview"></a>Overview

First and foremost, Coherence provides a fundamental service that is responsible
for all facets of clustering and is a common denominator / building block for all
other Coherence services. This service, referred to as 'service 0' internally,
ensures that the mesh of members is maintained and responsive, taking action to collaboratively
evict, shun, or in some cases, voluntarily depart the cluster when deemed necessary.
As members join and leave the cluster, other Coherence services are notified,
thus enabling those services to react accordingly.

> Note: This part of the Coherence product has been in production for more that 10 years,
>       being the subject of some extensive and imaginative testing. While this feature has
>       been discussed here, it certainly is not something that customers, generally,
>       interact with directly, but is important to be aware of.

Coherence services build on top of the cluster service. The key implementations
to be aware of are PartitionedService, InvocationService, and ProxyService.

In the majority of cases, customers deal with maps. A map is represented
by an implementation of `NamedMap<K,V>`. A `NamedMap` is hosted by a service,
generally the PartitionedService, and is the entry point to store, retrieve,
aggregate, query, and stream data.

Coherence Maps provide a number of features:

* Fundamental **key-based access**: get/put getAll/putAll.
* Client-side and storage-side events:
  * **MapListeners** to asynchronously notify clients of changes to data.
  * **EventInterceptors** (either sync or async) to be notified storage level events, including
mutations, partition transfer, failover, and so on.
* **NearCaches** - Locally cached data based on previous requests with local content
invalidated upon changes in the storage tier.
* **ViewCaches** - Locally stored view of remote data that can be a subset based on a
predicate and is kept in sync, real time.
* **Queries** - Distributed, parallel query evaluation to return matching key, values,
or entries with potential to optimize performance with indices.
* **Aggregations** - A map/reduce style aggregation where data is aggregated in parallel
on all storage nodes, and results streamed back to the client for aggregation of
those results to produce a final result.
* **Data local processing** - Ability to send a function to the relevant storage node
to execute processing logic for the appropriate entries with exclusive access.
* **Partition local transactions** - Ability to perform scalable transactions by
associating data (thus being on the same partition) and manipulating other entries
on the same partition, potentially across different maps.
* **Non-blocking / async NamedMap API**
* **C++ and .NET clients** - Access the same NamedMap API from either C++ or .NET.
* **Portable Object Format** - Optimized serialization format, with the ability to
navigate the serialized form for optimized queries, aggregations, or data processing.
* **Integration with Databases** - Database and third party data integration with
CacheStores, including both synchronous or asynchronous writes.
* **CohQL** - Ansi-style query language with a console for adhoc queries.
* **Topics** - Distributed topics implementation that offers pub/sub messaging with
the storage capacity, the cluster, and parallelizable subscribers.

Coherence also provides a number of non-functional features:

* **Rock solid clustering** - Highly tuned and robust clustering stack that enables
Coherence to scale to thousands of members in a cluster with thousands of partitions
and terabytes of data being accessed, mutated, queried, and aggregated concurrently.
* **Safety first** - Resilient data management that ensures backup copies are
on distinct machines, racks, or sites, and the ability to maintain multiple backups.
* **24/7 Availability** - Zero downtime with rolling redeployment of cluster members
to upgrade application or product versions.
  * Backward and forward compatibility of product upgrades, including major versions.
* **Persistent Maps** - Ability to use local file system persistence (thus
avoid extra network hops) and leverage Coherence consensus protocols to perform
distributed disk recovery when appropriate.
* **Distributed State Snapshot** - Ability to perform distributed point-in-time
snapshot of cluster state, and recover snapshot in this or a different cluster
(leverages persistence feature).
* **Lossy redundancy** - Ability to reduce the redundancy guarantee by making backups
and/or persistence asynchronous from a client perspective.
* **Single Mangement View** - Provides insight into the cluster  with a single
JMX server that provides a view of all members of the cluster.
* **Management over REST** - All JMX data and operations can be performed over REST,
including cluster wide thread dumps and heapdumps.
* **Non-cluster Access** - Provides access to the cluster from the outside via proxies,
for distant (high latency) clients and for non-java languages such as C++ and .NET.
* **Kubernetes friendly** - Enables seamless and safe deployment of applications to k8s with
our own [operator](https://github.com/oracle/coherence-operator).

## <a name="get-started"></a>Hello Coherence

### Prerequisites

  1. Java - JDK 17 or higher
  2. Maven - 3.8.5 or higher
  3. Cohrence CLI Installed (see below)

The following example shows you how to quickly get started with Coherence using the
[Coherence CLI](https://github.com/oracle/coherence-cli) to create a 3 node Coherence cluster scoped to you local machine.
You will then access data using the CohQL and Coherence consoles.

#### <a name="install"></a> Install the Coherence CLI

Install the Coherence CLI by following the instructions for your selected platform [here](https://oracle.github.io/coherence-cli/docs/latest/#/docs/installation/01_installation). Once you have installed the CLI, continue below.

#### <a name="create"></a>Create and start a Cluster

Use the following command to create a 3 node Coherence cluster called `my-cluster`, scoped to your local machine using the default of Coherence CE 22.09.

```shell
$ cohctl create cluster my-cluster

Cluster name:         my-cluster
Cluster version:      22.09
Cluster port:         7574
Management port:      30000
Replica count:        3
Initial memory:       128m
Persistence mode:     on-demand
Group ID:             com.oracle.coherence.ce
Additional artifacts:
Startup Profile:      
Dependency Tool:      mvn
Are you sure you want to create the cluster with the above details? (y/n) y

Checking 3 Maven dependencies...
- com.oracle.coherence.ce:coherence:22.09
- com.oracle.coherence.ce:coherence-json:22.09
- org.jline:jline:3.20.0
Starting 3 cluster members for cluster my-cluster
Starting cluster member storage-0...
Starting cluster member storage-1...
Starting cluster member storage-2...
Current context is now my-cluster
Cluster added and started
```

> Note: If you do not have the Maven artefacts locally, it may take a short while to download them from Maven central.

Once the cluster is created, wait it a couple of seconds, and use the following command to see the members.

```shell
$ cohctl get members

Using cluster connection 'my-cluster' from current context.

Total cluster members: 3
Cluster Heap - Total: 384 MB Used: 114 MB Available: 270 MB (70.3%)
Storage Heap - Total: 128 MB Used: 16 MB Available: 112 MB (87.5%)

NODE ID  ADDRESS     PORT   PROCESS  MEMBER     ROLE             STORAGE  MAX HEAP  USED HEAP  AVAIL HEAP
      1  /127.0.0.1  55654    58270  storage-1  CoherenceServer  true       128 MB      16 MB      112 MB
      2  /127.0.0.1  55655    58271  storage-2  CoherenceServer  false      128 MB      74 MB       54 MB
      3  /127.0.0.1  55656    58269  storage-0  CoherenceServer  false      128 MB      24 MB      104 MB
```

Note: If you do not see the above, then ensure you are using JDK17, and then issue `cohctl start cluster my-cluster` to start the cluster.

#### <a name="cohql"></a>CohQL Console

Start the CohQL Console using the CLI, and run the statements at the `CohQL>` prompt to insert data into your cache.

```shell
$ cohctl start cohql

CohQL> select * from welcomes

CohQL> insert into welcomes key 'english' value 'Hello'

CohQL> insert into welcomes key 'spanish' value 'Hola'

CohQL> insert into welcomes key 'french' value 'Bonjour'

CohQL> select key(), value() from welcomes
Results
["french", "Bonjour"]
["english", "Hello"]
["spanish", "Hola"]

CohQL> bye

# Restart to CohQL to show that the data is still present in the Coherence cluster.

$ cohctl start cohql

CohQL> select key(), value() from welcomes
Results
["french", "Bonjour"]
["english", "Hello"]
["spanish", "Hola"]

CohQL> bye
```

#### <a name="coh-console"></a>Coherence Console

Use the following command to start the Coherence console, which is a different way to interact with the data in a Cache.

```shell
$ cohctl start console

Map (?): cache welcomes

Map (welcomes): get english
Hello

Map (welcomes): list
french = Bonjour
spanish = Hola
english = Hello

Map (welcomes): put australian Gudday
null

Map (welcomes): list
spanish = Hola
english = Hello
australian = Gudday
french = Bonjour

Map (welcomes): bye
```

#### Shutdown your Cluster

**Note**: Ensure you shutdown your Coherence cluster using the following:

```#!/usr/bin/env bash
cohctl stop cluster my-cluster
```

### <a name="hello-coh"></a>Programmatic Hello Coherence Example

The following example illustrates starting a **storage enabled** Coherence server,
followed by running the `HelloCoherence` application. The `HelloCoherence` application
inserts and retrieves data from the Coherence server.

#### Build `HelloCoherence`

1. Create a maven project either manually or by using an archetype such as maven-archetype-quickstart
2. Add a dependency to the pom file:
```xml
    <dependency>
        <groupId>com.oracle.coherence.ce</groupId>
        <artifactId>coherence</artifactId>
        <version>22.06</version>
    </dependency>
```
3. Copy and paste the following source to a file named src/main/java/HelloCoherence.java:
```java
    import com.tangosol.net.CacheFactory;
    import com.tangosol.net.NamedMap;

    public class HelloCoherence
        {
        // ----- static methods -------------------------------------------------

        public static void main(String[] asArgs)
            {
            NamedMap<String, String> map = CacheFactory.getCache("welcomes");

            System.out.printf("Accessing map \"%s\" containing %d entries\n",
                    map.getName(),
                    map.size());

            map.put("english", "Hello");
            map.put("spanish", "Hola");
            map.put("french" , "Bonjour");

            // list
            map.entrySet().forEach(System.out::println);
            }
        }
```
4. Compile the maven project:
```shell
    mvn package
```
5. Start a Storage Server
```shell
    mvn exec:java -Dexec.mainClass="com.tangosol.net.DefaultCacheServer" &
```
6. Run `HelloCoherence`
```shell
    mvn exec:java -Dexec.mainClass="HelloCoherence"
```
7. Confirm you see output including the following:
```shell
    Accessing map "welcomes" containing 3 entries
    ConverterEntry{Key="french", Value="Bonjour"}
    ConverterEntry{Key="spanish", Value="Hola"}
    ConverterEntry{Key="english", Value="Hello"}
```
8. Kill the storage server started previously:
```shell
    kill %1
```

## <a name="build"></a>Building

```shell

$> git clone git@github.com:oracle/coherence.git
$> cd coherence/prj

# build Coherence module
$> mvn clean install

# build Coherence module skipping tests
$> mvn clean install -DskipTests

# build all other modules skipping tests
$> mvn -Pmodules clean install -DskipTests

# build specific module, including all dependent modules and run tests
$> mvn -Pmodules -am -pl test/functional/persistence clean verify

# only build coherence.jar without running tests
$> mvn -am -pl coherence clean install -DskipTests

# only build coherence.jar and skip compilation of CDBs and tests
$> mvn -am -pl coherence clean install -DskipTests -Dtde.compile.not.required
```

## <a name="documentation"></a>Documentation

### Oracle Coherence Documentation

Oracle Coherence product documentation is available
[here](https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/index.html).

### Features Not Included in Coherence Community Edition

The following Oracle Coherence features are not included in Coherence Community Edition:

* Management of Coherence via the Oracle WebLogic Management Framework
* Deployment of Grid Archives (GARs)
* HTTP Session Management for Application Servers (Coherence*Web)
* GoldenGate HotCache
* TopLink-based CacheLoaders and CacheStores
* Elastic Data
* Federation and WAN (wide area network) Support
* Transaction Framework
* CommonJ Work Manager

Below is an overview of features supported in each Coherence edition for comparison purposes:

<img src=https://oracle.github.io/coherence/assets/images/coherence-edition-matrix.png><img>

Please refer to <a href="https://docs.oracle.com/en/middleware/fusion-middleware/fmwlc/application-server-products-new-structure.html#GUID-00982997-3110-4AC8-8729-80F1D904E62B">Oracle Fusion Middleware Licensing Documentation</a> for official documentation of Oracle Coherence commercial editions and licensing details.

## <a name="contrib"></a>Contribute

Interested in contributing?  See our contribution [guidelines](CONTRIBUTING.md) for details.
