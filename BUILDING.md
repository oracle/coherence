<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Build Coherence from source

These instructions apply to the CE 26.10 source line. Use Java 21 or later and
Apache Maven 3.6.0 or later, as required by the project's Maven configuration.
The targets below also require `make`.

## Build the core and modules

Clone the repository and run the build from `prj`:

```shell
git clone https://github.com/oracle/coherence.git
cd coherence/prj
make coherence
```

Build the additional modules after the core build:

```shell
make modules
```

Alternatively, `make all` runs both stages. These build targets skip tests.
See the [CI build workflow](.github/workflows/build.yaml) for the project's
staged verification and test configuration.

## Build the documentation

From `prj`, run:

```shell
make docs
```

The generated site is written to `docs/target/docs` and packaged as
`docs/target/docs.zip`. Run `make help` to list the available build targets.

See the [contribution guide](CONTRIBUTING.md) and
[development guidelines](DEV-GUIDELINES.md) before preparing a contribution.
