<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Lambda Bytecode Deny-List

Coherence rejects wire-supplied dynamic lambda bytecode that references entries
in `META-INF/coherence/lambda-bytecode-denylist.txt`. The default list blocks
JVM sandbox-escape primitives such as process execution, native library loading,
Unsafe access, private lookup class definition, reflective `setAccessible`,
class-loader definition methods, and unrestricted `MethodHandle` invocation.

The bytecode gate also rejects classes that declare native methods and calls to
`Class.forName` where the class name is not an immediately preceding string
constant. Those structural rules are always active and are not listed in the
resource file.

Customers should not ship lambdas that need these primitives. Move that logic
to a server-side class already installed on the cluster and invoke it through a
documented Coherence API. For short-lived incident response only, operators can
set `coherence.lambda.bytecode.allow` to a comma-separated list of class FQNs to
remove those class entries from the lambda gate. That property does not affect
the serialization `ObjectInputFilter` deny-list and cannot disable structural
bytecode checks.

Additional deny entries can be added by placing UTF-8 resources under
`META-INF/coherence/lambda-bytecode-denylist.d/` on the classpath, using the same
one-entry-per-line format as the default resource.
