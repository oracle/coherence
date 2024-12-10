====
    Copyright (c) 2000, 2020, Oracle and/or its affiliates.

    Licensed under the Universal Permissive License v 1.0 as shown at
    http://oss.oracle.com/licenses/upl.
====

-----------------------------------------------------------------------------
IMPORTANT NOTICE:
-----------------------------------------------------------------------------

This module is not yet included or enabled as part of the Coherence Functional Tests due to the
nature of our Java compliance requirements.

Once Coherence moves to be built with Java 7+, we'll include and enable these tests.

In the mean time these tests may be run by setting up Java 7 following the instructions below.

-----------------------------------------------------------------------------
HOWTO: Running the JSR 107 Technology Compatibility Kit (TCK) against coherence-jcache.

By default the implementations runs with SafeConfigurablePofContext against LocalCache implementation

% mvn -s ../../settings.xml test

By default, test runs against the LocalCache implementation using SafePOF
serialization. (see jcache-compliance/src/test/resources/coherence-jcache-tck-cache-config.xml)
To run against the PartitionedCache implementation, use the following.

% mvn -s ../../settings.xml test -Dimpl=partitioned



To run test with Coherence default serialization mode, run test with default coherence jcache cache config

% mvn -s ../../settings.xml clean test -Dcacheconfig=coherence-jcache-cache-config.xml


-----------------------------------------------------------------------------

To run test using Coherence Extend client, the jsr 107 tck tests are run as a Coherence JCache extend client by starting test with
following steps:

1. start cache servers and proxy server(s)

   The cache servers and proxy server(s) are started with -Dcacheconfig=coherence-jcache-tck-server-cache-config.xml.
   The proxy server(s) must have -Dtangosol.coherence.extend.enabled=true.  The following system properties
   can also be set for proxy server(s): -Dtangosol.coherence.distributed.localstorage=false.
   All the jsr 107 tck test jars must be added to the classpath when starting up the servers.

2. start the jsr 107 tck tests as Coherence extend client.

% mvn -s ../../settings.xml -Dimpl=remote -Dcacheconfig=coherence-jcache-tck-extendclient-cache-config.xml \
-Dtangosol.coherence.remote.address=AA.BBB.CCC.DDD \
-Dtangosol.coherence.clusterport=7778 -Dtangosol.coherence.clusteraddress="225.123.124.125" \
-Dtangosol.coherence.distributed.localstorage=false test

The cache config is configured so Coherence NamedCache is accessed via remote-scheme.
The proxy address and port is really a nameservice address/port and is one of the cluster members or
the extend proxy.

TODO: automate this testing using Oracle Tools to start the cache and proxy server.
Need to figure out how to get all jsr 107 tck junit tests to run after starting cluster
and proxy servers with Oracle Tools.



