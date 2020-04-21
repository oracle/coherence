<!--

  Copyright (c) 2000, 2020, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  http://oss.oracle.com/licenses/upl.

-->
# Contributing to Coherence

Oracle welcomes contributions to this repository from anyone.

If you want to submit a pull request to fix a bug or enhance an existing
feature, please first open an issue and link to that issue when you
submit your pull request.

If you have any questions about a possible submission, feel free to open
an issue too.

## Contributing to the Oracle Coherence Community Edition repository

Pull requests can be made under
[The Oracle Contributor Agreement](https://www.oracle.com/technetwork/community/oca-486395.html) (OCA).

For pull requests to be accepted, the bottom of your commit message must have
the following line using your name and e-mail address as it appears in the
OCA Signatories list.

```
Signed-off-by: Your Name <you@example.org>
```

This can be automatically added to pull requests by committing with:

```
  git commit --signoff
```

Only pull requests from committers that can be verified as having
signed the OCA can be accepted.

### Pull request process

1. Fork this repository
1. Create a branch in your fork to implement the changes. We recommend using
the issue number as part of your branch name, e.g. `1234-fixes`
1. Ensure that all changes comply to project coding conventions
1. Ensure that there is at least one test that would fail without the fix and
passes post fix
1. A full build including test execution is required for the PR
1. Submit the pull request. *Do not leave the pull request blank*. Explain exactly
what your changes are meant to do and provide simple steps on how to validate
your changes, ideally referencing the test. Ensure that you reference the issue
you created as well. We will assign the pull request to 2-3 people for review
before it is submitted internally and the PR is closed.
