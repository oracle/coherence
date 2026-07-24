<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in security follow-up round 2

- **Status:** pending
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Authoritative review:**
  `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round2.md`
- **Scope:** fix explicit fail-closed feature-property bypasses introduced by
  the security-mode compatibility conversion

## Required reading

1. `AGENTS.md`
2. `.ai/dev-environment.md`
3. `.ai/branches/codex/coherence-mode-security-opt-in/journal.md`
4. `design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
5. `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review.md`
6. `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round2.md`

## Task

Fix the two round-2 blocking security findings. Compatibility mode must remain
the default for July patch compatibility, but explicit fail-closed
feature-level settings must still fail closed.

## Findings to fix

### Dynamic remote install explicit deny

`coherence.remote.dynamic.unauthenticated=deny` is parsed correctly by
`RemoteExecutionMode`, but `RemoteInstallGate.enforceDynamicInstall(...)`
returns early whenever `coherence.security.mode` is not hardened. This allows
dynamic remote install payloads at cache, map-trigger, and topic install
boundaries even after the operator explicitly selected `deny`.

### Topic replay explicit reject

`coherence.topics.persisted.policy-drift=reject` is parsed correctly by
`TopicsPersistedPolicyDrift`, but `RemoteInstallGate.enforceReplay(...)` still
allows replay in compatibility mode because `RemoteExecutablePolicy.enforce(...)`
shadows and returns before the replay drift reject block can run. Current
unit/functional tests assert this wrong compatibility-plus-reject behavior;
replace those assertions.

## Do

- Preserve default compatibility behavior when the explicit feature property is
  unset.
- Preserve hardened-mode fail-closed defaults.
- Make explicit `coherence.remote.dynamic.unauthenticated=deny` reject dynamic
  install payloads regardless of `coherence.security.mode`.
- Make explicit `coherence.topics.persisted.policy-drift=reject` reject
  non-executable persisted topic subscriber replay regardless of
  `coherence.security.mode`.
- Update unit tests for the install and replay boundaries, not just the
  resolver helpers.
- Add or update functional coverage for at least one Extend install path and
  one topics replay path with `coherence.security.mode=compatibility` plus the
  explicit deny/reject property.

## Do not

- Do not reintroduce `CoherenceMode.LEGACY`, `isLegacy()`, or
  `coherence.mode=legacy`.
- Do not make `coherence.security.hardened` an alias.
- Do not change unrelated REST, TLS, gRPC, JSON, RAG, management, reporting, or
  senior-metadata behavior unless a focused test proves the same explicit
  fail-closed bug exists there too.
- Do not stage, commit, submit P4, fetch, push, use remote syntax, or touch
  `main`.

## Suggested verification

```bash
source ../bin/cfglocal.sh
REVISION=15.1.2-0-0-security-opt-in-SNAPSHOT
make coherence ARGS="-Drevision=${REVISION}"
mvn -Pmodules,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl test/unit/coherence-core-tests \
  -Dtest=RemoteExecutionModeTest,CacheNamedCacheInstallGateTest,MapTriggerInstallGateTest,TopicsSubscriberInstallGateTest,TopicsSubscriberReplayGateTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -Pmodules,modular-tests,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl test/functional/topics,test/functional/extend \
  -Dit.test=TopicsSubscriberReplayIntegrationTest,TopicsSubscriberInstallModeMatrixIntegrationTest,ExtendNamedCacheInstallModeMatrixIntegrationTest,TriggerInstallModeMatrixIntegrationTest \
  -Dcoherence.cluster=security-mode-explicit-overrides-20260611-codex \
  verify
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|isLegacy\(|CoherenceMode\.LEGACY|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" \
  coherence-* test/unit test/functional -g'*.java' -g'*.xml' -g'*.properties'
```

## Reporting

Report the code/test changes, verification results, and any remaining stale
reference hits. If fixing these findings would require changing the accepted
compatibility plan, stop and ask for architect/security-reviewer confirmation.
