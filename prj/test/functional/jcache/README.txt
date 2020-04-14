====
    Copyright (c) 2000, 2020, Oracle and/or its affiliates.

    Licensed under the Universal Permissive License v 1.0 as shown at
    http://oss.oracle.com/licenses/upl.
====

By default, runs against LocalCache and pof.

To test partitioned, add -Dimpl=partitioned.
To test java serialization, add -Denablepof=false.

Thus to test java serialization and partitionedcache, run the following:

%mvn -s ../../settings.xml test -Dimpl=partitioned -Denablepof=false

