<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in implementation security review round 2

- **Date:** 2026-06-11
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
  (`P4 //dev/main baseline at change 120899`)
- **Working tree reviewed:** current uncommitted implementation diff after the
  first architect/security review fixes, including untracked compatibility plan
  and review documents
- **Verdict:** block until explicit fail-closed feature properties are honored
  at the remote install and persisted topic replay boundaries

## Prior reviews consulted

- `design/features/security-bugs/reviews/2026-06-11-security-mode-property-design-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review.md`

## Files in scope

The full current `git diff --name-only` implementation set was in scope. The
round-2 security review focused on:

- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/security/RemoteExecutionMode.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/security/RemoteInstallGate.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/security/TopicsPersistedPolicyDrift.java`
- `coherence-core-components/src/main/java/com/tangosol/coherence/component/util/daemon/queueProcessor/service/Grid.java`
- representative changed hardening gates in TLS, federation, REST, gRPC, JSON,
  RAG, management, reporting, invocation, concurrent, and serialization limit
  code
- representative changed unit and functional tests for senior metadata proof,
  request subject proof, dynamic remote install gates, and persisted topic
  subscriber replay

## Context

This is a second dedicated security review after the first architect/security
review fixes. The accepted compatibility plan is to keep July
compatibility-sensitive hardening opt-in for this patch. This review therefore
did not re-litigate the default compatibility posture; it checked whether the
implementation creates unintended bypasses beyond that plan.

The first security review's blocking senior-metadata proof finding is fixed in
the current product code. Explicit
`coherence.security.peer.senior-metadata-proof.required=true` now contributes
to `isSeniorMetadataProofRequired(...)`, and `isSeniorMetadataProofEnforced(...)`
no longer adds a global hardening predicate. Both the send-side unavailable
path and the receive-side invalid-proof path use that predicate before
continuing.

## Findings by severity

### P1: Compatibility mode bypasses explicit `coherence.remote.dynamic.unauthenticated=deny` at remote install gates

`RemoteExecutionMode` still resolves an explicit `deny` value to `false`
independent of security mode:
`coherence-core/src/main/java/com/tangosol/internal/util/security/RemoteExecutionMode.java:76`.
That is the intended explicit feature-property behavior.

The attacker-reachable install boundary does not honor it. For synthetic
dynamic classes, `RemoteInstallGate.enforceDynamicInstall(...)` returns as soon
as `coherence.security.mode` is not hardened:
`coherence-core/src/main/java/com/tangosol/internal/util/security/RemoteInstallGate.java:1252`.
The code records `would_reject` telemetry and returns at
`RemoteInstallGate.java:1255`, before it checks
`RemoteExecutionMode.isDynamicRemoteAllowed()` at
`RemoteInstallGate.java:1260`.

Security impact: an operator can explicitly set
`coherence.remote.dynamic.unauthenticated=deny`, but compatibility security
mode still allows dynamic cache processor, aggregator, filter, extractor,
comparator, map-trigger, and topic-subscriber install payloads through the
remote install gate. That violates the compatibility plan's rule that explicit
deny/enforce/safe settings still fail closed in compatibility mode.

The current tests cover the resolver and the default compatibility shadow path,
but not the combined attacker boundary. For example,
`test/unit/coherence-core-tests/src/test/java/com/tangosol/internal/util/security/RemoteExecutionModeTest.java:69`
asserts the resolver returns deny under compatibility mode, while
`test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheNamedCacheInstallGateTest.java:163`
asserts compatibility shadowing only with the dynamic property unset. Similar
compatibility install rows exist for map triggers and topics without an
explicit deny value.

Required fix: make `RemoteInstallGate` reject dynamic installs when
`RemoteExecutionMode.isDynamicRemoteAllowed()` is `false`, regardless of global
security mode. Preserve the default compatibility behavior only when the
resolved dynamic-remote policy allows the payload. Add unit coverage for each
remote install family and at least one functional Extend/topics boundary with
`coherence.security.mode=compatibility` plus
`coherence.remote.dynamic.unauthenticated=deny`.

### P1: Compatibility mode bypasses explicit topic replay `policy-drift=reject`

`TopicsPersistedPolicyDrift` correctly resolves explicit
`coherence.topics.persisted.policy-drift=reject` independent of security mode:
`coherence-core/src/main/java/com/tangosol/internal/util/security/TopicsPersistedPolicyDrift.java:92`.
The unit test at
`test/unit/coherence-core-tests/src/test/java/com/tangosol/internal/util/security/TopicsPersistedPolicyDriftConfigTest.java:58`
confirms the resolver contract.

The replay enforcement path then defeats that explicit opt-in. In
`RemoteInstallGate.enforceReplay(...)`, compatibility mode goes through
`RemoteExecutablePolicy.current().enforce(...)` at
`coherence-core/src/main/java/com/tangosol/internal/util/security/RemoteInstallGate.java:1288`.
Because remote executable policy is itself shadow-only in compatibility mode,
that call returns instead of throwing, so execution returns at
`RemoteInstallGate.java:1289` before the `TopicsPersistedPolicyDrift.isReject()`
failure block at `RemoteInstallGate.java:1293` can run. The preceding dedup
guard is also keyed on `!CoherenceMode.isSecurityHardeningEnabled()` at
`RemoteInstallGate.java:1279`, which makes compatibility mode part of the
allow path even when the replay-drift policy is explicit `reject`.

Security impact: persisted non-executable topic subscriber filter/extractor
metadata can replay under `coherence.security.mode=compatibility` even when an
operator explicitly requested `coherence.topics.persisted.policy-drift=reject`.
This is not just a missing test; current tests encode the bypass. The unit test
`compatibilityShadowsReplayRegardlessOfKnob` sets compatibility plus
`VALUE_REJECT` and expects no rejection at
`test/unit/coherence-core-tests/src/test/java/com/tangosol/util/TopicsSubscriberReplayGateTest.java:125`.
The functional replay test does the same at
`test/functional/topics/src/main/java/topics/TopicsSubscriberReplayIntegrationTest.java:151`.

Required fix: treat `TopicsPersistedPolicyDrift.VALUE_REJECT` as a fail-closed
explicit setting regardless of global security mode. Default compatibility
should still resolve to `warn-allow`, but once the effective drift policy is
`reject`, replay must reject non-executable persisted subscriber classes at the
replay boundary. Replace the current compatibility-plus-reject shadow tests
with negative tests, including a repeat/retry case so dedup cannot become a
fail-open path after the first attempt.

## What looks good

- The prior senior-metadata proof P1 is fixed in both send and receive paths:
  `Grid.java:5438` now throws when the effective proof policy is required, and
  `Grid.java:4775` does the same for receive-side invalid proof.
- `coherence.security.mode` parsing is centralized and rejects blank, unknown,
  shorthand, and boolean-style values rather than accepting hidden aliases.
- Active Java/XML scans found no product or test references to the removed
  `coherence.security.hardened` boolean property, `PROP_SECURITY_HARDENED`,
  `CoherenceMode.LEGACY`, `isLegacy()`, or active
  `coherence.mode=legacy` helper use.
- Request subject proof remains on the intended security-mode boundary, while
  the senior-metadata explicit-required property has the necessary fail-closed
  exception.
- Reviewed TLS hostname verification, federation endpoint validation, REST,
  JSON, gRPC diagnostics/auth/serializer, RAG allowlist, management/reporting,
  invocation, concurrent, serialization-limit, and XML parser defaults now
  consult the security-mode boundary rather than runtime `prod` mode for the
  compatibility-sensitive defaults.

## Required follow-up work

1. Fix `RemoteInstallGate.enforceDynamicInstall(...)` so explicit
   `coherence.remote.dynamic.unauthenticated=deny` rejects at the install
   boundary even when `coherence.security.mode=compatibility`.
2. Fix `RemoteInstallGate.enforceReplay(...)` so explicit
   `coherence.topics.persisted.policy-drift=reject` rejects at the replay
   boundary even when `coherence.security.mode=compatibility`.
3. Replace tests that assert compatibility shadows these explicit reject/deny
   knobs with negative tests.
4. Add attacker-boundary coverage for compatibility plus explicit deny/reject:
   cache named-cache install families, map triggers, topic subscriber install,
   topic subscriber replay, and at least one functional Extend/topics case for
   each class of bug.
5. Re-run the focused unit/functional matrix for the touched gates, plus
   `git diff --check` and stale-reference scans.

## Out of scope

- No implementation source, test, XML, P4 state, commits, submits, backports,
  branches, remotes, or `main` state were modified during this review.
- Full Maven, functional, or Remote Queue verification was not rerun in this
  review pass; the findings are from source/diff review and existing test
  assertions that encode the failing behavior.
- Customer-facing release notes and documentation outside the compatibility
  plan were not reviewed.

## Verification evidence

Review commands run:

```bash
git branch --show-current
git log -1 --format='%H %s'
git status --short
git diff --stat
git diff --name-only
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" coherence-* test/unit test/functional --glob '*.java' --glob '*.xml' --glob '*.properties'
rg -n "CoherenceMode\.isProd\(|CoherenceMode\.isDev\(|CoherenceMode\.current\(\)" coherence-core-components/src/main/java coherence-core/src/main/java coherence-grpc/src/main/java coherence-json/src/main/java coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java --glob '*.java'
rg -n "SECURITY_MODE_COMPATIBILITY.*(VALUE_REJECT|deny)|VALUE_REJECT.*SECURITY_MODE_COMPATIBILITY|deny.*SECURITY_MODE_COMPATIBILITY|compatibility.*VALUE_REJECT|compatibility.*deny" test/unit test/functional --glob '*.java'
```

Observed results:

- Branch was `codex/coherence-mode-security-opt-in`.
- Head was `88e1c8f93e6d664b994e8491edc2db3dc72fe34a P4 //dev/main baseline at change 120899`.
- `git diff --check` produced no output.
- The old boolean property / removed legacy runtime-mode scan had no active
  Java/XML/product hits; remaining broad `"legacy"` literals were unrelated
  binary-format or `LegacyXml` names plus the expected invalid-mode assertion.
- Existing tests explicitly demonstrate the topic replay compatibility plus
  `VALUE_REJECT` bypass.

## Codex handoff prompt

~~~markdown
You are working in
`/Users/phfry/dev/perforce/projects/coherence-mode-security-opt-in/prj` on
branch `codex/coherence-mode-security-opt-in`.

Read:
- `AGENTS.md`
- `.ai/dev-environment.md`
- `.ai/branches/codex/coherence-mode-security-opt-in/journal.md`
- `design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round2.md`

Task:
Fix the two round-2 blocking security findings. Compatibility mode must remain
the default for July patch compatibility, but explicit fail-closed
feature-level settings must still fail closed.

Finding 1:
- `coherence.remote.dynamic.unauthenticated=deny` is parsed correctly by
  `RemoteExecutionMode`, but `RemoteInstallGate.enforceDynamicInstall(...)`
  returns early whenever `coherence.security.mode` is not hardened.
- This allows dynamic remote install payloads at cache, map-trigger, and topic
  install boundaries even after the operator explicitly selected `deny`.

Finding 2:
- `coherence.topics.persisted.policy-drift=reject` is parsed correctly by
  `TopicsPersistedPolicyDrift`, but `RemoteInstallGate.enforceReplay(...)`
  still allows replay in compatibility mode because
  `RemoteExecutablePolicy.enforce(...)` shadows and returns before the replay
  drift reject block can run.
- Current unit/functional tests assert this wrong compatibility-plus-reject
  behavior; replace those assertions.

Do:
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

Do not:
- Do not reintroduce `CoherenceMode.LEGACY`, `isLegacy()`, or
  `coherence.mode=legacy`.
- Do not make `coherence.security.hardened` an alias.
- Do not change unrelated REST, TLS, gRPC, JSON, RAG, management, reporting, or
  senior-metadata behavior unless a focused test proves the same explicit
  fail-closed bug exists there too.
- Do not stage, commit, submit P4, fetch, push, use remote syntax, or touch
  `main`.

Suggested verification:
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

Report the code/test changes, verification results, and any remaining stale
reference hits. If fixing these findings would require changing the accepted
compatibility plan, stop and ask for architect/security-reviewer confirmation.
~~~
