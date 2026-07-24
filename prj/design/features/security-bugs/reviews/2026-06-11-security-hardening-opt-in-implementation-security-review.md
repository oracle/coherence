<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in implementation security review

- **Date:** 2026-06-11
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
  (`P4 //dev/main baseline at change 120899`)
- **Working tree reviewed:** unstaged implementation diff for
  `coherence.security.mode` opt-in compatibility, plus untracked branch plan
  and prior design review documents
- **Verdict:** block until the senior-metadata proof explicit-required
  enforcement regression is fixed

## Prior reviews consulted

- `design/features/security-bugs/reviews/2026-06-11-security-mode-property-design-review.md`

## Files in scope

The full current `git diff --name-only` implementation set was in scope. The
deep security review focused on:

- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
- `coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java`
- `coherence-testing-support/src/main/java/com/oracle/coherence/testing/util/CoherenceModeHelper.java`
- `coherence-core-components/src/main/java/com/tangosol/coherence/component/util/daemon/queueProcessor/service/Grid.java`
- `coherence-core/src/main/java/com/tangosol/internal/federation/service/ConnectRequestHandler.java`
- `coherence-core/src/main/java/com/tangosol/net/internal/NameServiceValuePolicy.java`
- `coherence-core/src/main/java/com/oracle/coherence/common/net/SSLSocketProvider.java`
- `coherence-core/src/main/java/com/tangosol/coherence/config/builder/SSLSocketProviderDependenciesBuilder.java`
- `coherence-core/src/main/java/com/tangosol/net/security/DefaultController.java`
- `coherence-core/src/main/java/com/tangosol/io/SerializationLimitPolicy.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/security/RemoteInstallGate.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/security/LambdaBytecodeGate.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/processor/CacheProcessors.java`
- `coherence-core/src/main/java/com/tangosol/run/xml/SaxParser.java`
- `coherence-core/src/main/java/com/tangosol/net/grpc/GrpcDiagnosticsPolicy.java`
- `coherence-grpc/src/main/java/com/oracle/coherence/grpc/GrpcAuthentication.java`
- `coherence-grpc/src/main/java/com/oracle/coherence/grpc/GrpcSerializerPolicy.java`
- `coherence-json/src/main/java/com/oracle/coherence/io/json/internal/JsonClassMetadataPolicy.java`
- `coherence-json/src/main/java/com/oracle/coherence/io/json/internal/SerializationGate.java`
- `coherence-rest/src/main/java/com/tangosol/coherence/rest/RestExpressionPolicy.java`
- `coherence-rest/src/main/java/com/tangosol/coherence/rest/RestQueryPolicy.java`
- `coherence-rest/src/main/java/com/tangosol/coherence/rest/providers/SecurityFilter.java`
- `coherence-rag-parent/coherence-rag/src/main/java/com/oracle/coherence/rag/api/RagSecurity.java`
- representative changed unit and functional tests for the same gates

## Context

The implementation is intended to decouple existing `coherence.mode` runtime
behavior from July compatibility-sensitive hardening. The new
`coherence.security.mode` property defaults to compatibility posture and
enables the same fail-closed hardening behavior only when set to `hardened`.

The review checked whether hardened mode still enforces the affected gates,
whether compatibility mode is no longer implicitly tied to `coherence.mode=prod`
or `coherence.mode=dev`, whether the new parser rejects unsupported aliases,
and whether former `isLegacy()` call sites were reclassified by behavior rather
than by mechanical replacement.

## Findings by severity

### P1: Explicit senior-metadata proof `required=true` now fails open unless global hardening is also enabled

`Grid.isSeniorMetadataProofEnforced()` now returns
`isSeniorMetadataProofRequired(msg) && CoherenceMode.isSecurityHardeningEnabled()`
at `coherence-core-components/src/main/java/com/tangosol/coherence/component/util/daemon/queueProcessor/service/Grid.java:5462`.
Both the sender-side proof-unavailable path at `Grid.java:5438` and the
receive-side verification path at `Grid.java:4775` use this predicate before
throwing.

That changes the explicit feature property
`coherence.security.peer.senior-metadata-proof.required=true` from a fail-closed
opt-in into a shadow-only setting unless the operator also sets
`coherence.security.mode=hardened`. The compatibility plan says explicit
feature-property opt-ins must preserve their semantics, including
`required=true`, `deny`, and `enforce` values independent of global hardening
unless a feature review deliberately changes that behavior. It also calls out
this exact peer row: `coherence.security.peer.senior-metadata-proof.required=true`
remains an explicit feature opt-in.

Security impact: an operator who explicitly requires senior-metadata proof can
still accept missing, unavailable, or invalid proof in compatibility security
mode. On sender-side failure the code reaches `onSeniorMetadataProofWouldReject`
and continues at `Grid.java:5443`; the equivalent receive-side invalid-proof
path does the same at `Grid.java:4780`. This is a fail-open peer-proof gate for
an explicitly requested security control.

The stale existing unit coverage catches the regression when run directly. This
review command failed with 12 tests run and 6 failures:

```bash
source ../bin/cfglocal.sh
mvn -Pmodules,-coherence -am -nsu \
  -Drevision=15.1.2-0-0-security-opt-in-SNAPSHOT \
  -Dcoherence.cluster=SeniorMetadataProofPolicyTest-review-20260611-codex \
  -pl test/unit/coherence-tests \
  -Dtest=SeniorMetadataProofPolicyTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Representative failed assertions:

- `SeniorMetadataProofPolicyTest.java:124`: expected an `IOException` for
  proof-required prod mode after `required=true`, but nothing was thrown.
- `SeniorMetadataProofPolicyTest.java:138`: expected rejection for incompatible
  recipients with proof required, but nothing was thrown.
- `SeniorMetadataProofPolicyTest.java:152`: expected rejection for disabled
  provider with proof required, but nothing was thrown.
- `SeniorMetadataProofPolicyTest.java:216`: expected rejection for incompatible
  directed recipient with proof required, but nothing was thrown.

Required fix: separate explicit proof-required enforcement from the global
hardening default. At minimum, `coherence.security.peer.senior-metadata-proof.required=true`
must remain fail-closed even when `coherence.security.mode` is unset or
`compatibility`. If hardened mode is intended to require senior-metadata proof
without the feature property, add that deliberately and test it separately;
do not make the explicit property depend on the global switch.

## What looks good

- The central `coherence.security.mode` parser accepts unset /
  `compatibility` and `hardened`, trims and compares case-insensitively, and
  rejects blank, unknown, shorthand, and boolean-style values.
- `coherence.mode` and hardening are no longer coupled in the reviewed central
  predicates. Representative gates for serialization limits, REST/JSON,
  gRPC diagnostics, TLS hostname verification, management publish URL
  validation, invocation/concurrent defaults, and RAG empty allowlist behavior
  now consult the security hardening predicate rather than `isProd()`.
- Hardened behavior remains present in the reviewed gates: hardened mode still
  rejects unsafe TLS hostname-verifier `allow`, weak controller algorithms,
  JSON class metadata, REST unsafe query/expression paths, gRPC rich
  diagnostics defaults, unauthenticated Basic-over-cleartext gRPC, and
  unverified federation first inbound connections.
- Compatibility-mode logging has mostly been relabeled away from
  `mode=legacy` toward `security-mode=compatibility`.
- `git diff --check` was clean.

## Test gaps and residual risk

- I did not run full module, functional, or Remote Queue verification. This
  was a source-review pass plus one focused unit repro.
- Peer senior-metadata tests were not updated as part of the implementation
  diff and currently fail under the focused command above. The receive-side
  peer proof test class should also be reviewed and updated because it uses the
  same `coherence.security.peer.senior-metadata-proof.required` property and
  the same enforcement predicate.
- A narrow stale-comment scan still found REST test comments that say
  `LEGACY mode`; those are not product behavior issues, but they should be
  cleaned when touching tests so the removed public mode does not survive in
  active test documentation.
- The broad suggested stale-reference regex matches the current
  `CoherenceModeHelper.securityHardened()` helper, which appears to be an
  intentional helper for the new string-valued security mode. I did not treat
  that helper name as stale.

## Required follow-up work

1. Fix senior-metadata proof enforcement so an explicit
   `coherence.security.peer.senior-metadata-proof.required=true` fails closed
   independently of global hardening.
2. Update `SeniorMetadataProofPolicyTest` for the new security-mode contract:
   compatibility should shadow when the feature is not explicitly required,
   explicit `required=true` should fail closed, and hardened behavior should be
   asserted deliberately.
3. Review and update `SeniorMetadataProofReceiveVerificationTest` for the same
   explicit-required and hardened/compatibility matrix.
4. Re-run the focused peer proof tests, then re-run `git diff --check` and the
   stale-reference scan.

## Out of scope

- No implementation files, tests, P4 state, commits, submits, backports,
  branches, remotes, or `main` state were modified during this review.
- I did not create a separate implementation prompt file; the self-contained
  handoff prompt below is the durable follow-up prompt for this blocking
  review.
- I did not assess customer-facing documentation or release notes beyond the
  plan and stale-reference scans named in the request.

## Verification evidence

Review commands:

```bash
git branch --show-current
git log -1 --format='%H %s'
git status --short
git diff --stat
git diff --check
rg -n "CoherenceMode\.(isProd|isDev|current)\(|com\.tangosol\.util\.CoherenceMode\.(isProd|isDev|current)\(" \
  coherence-core-components/src/main/java coherence-core/src/main/java \
  coherence-grpc/src/main/java coherence-json/src/main/java \
  coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|isLegacy\(|CoherenceMode\.LEGACY|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" \
  coherence-* test/unit test/functional \
  design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md \
  design/features/security-bugs/reviews/2026-06-11-security-mode-property-design-review.md \
  -g'*.java' -g'*.md'
rg -n "LEGACY mode|legacy mode|legacy Coherence|legacy compatibility|legacy behavior" \
  coherence-* test/unit test/functional -g'*.java' -g'*.md'
source ../bin/cfglocal.sh
mvn -Pmodules,-coherence -am -nsu \
  -Drevision=15.1.2-0-0-security-opt-in-SNAPSHOT \
  -Dcoherence.cluster=SeniorMetadataProofPolicyTest-review-20260611-codex \
  -pl test/unit/coherence-tests \
  -Dtest=SeniorMetadataProofPolicyTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Observed results:

- Branch was `codex/coherence-mode-security-opt-in`.
- Head was `88e1c8f93e6d664b994e8491edc2db3dc72fe34a P4 //dev/main baseline at change 120899`.
- `git diff --check` produced no output.
- The old boolean property / removed public mode scan had no product Java hits
  for `coherence.security.hardened`, `PROP_SECURITY_HARDENED`,
  `restoreSecurityHardened`, `isLegacy()`, or `CoherenceMode.LEGACY`; remaining
  hits were historical plan/review text. The broader `securityHardened` pattern
  matches the current test helper and was treated separately.
- The focused `SeniorMetadataProofPolicyTest` run failed: 12 tests, 6 failures,
  0 errors.

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

Task:
Fix the blocking security review finding in the senior-metadata proof path.

Finding to fix:
- `coherence.security.peer.senior-metadata-proof.required=true` currently fails
  open unless `coherence.security.mode=hardened` is also set.
- The problematic predicate is
  `coherence-core-components/src/main/java/com/tangosol/coherence/component/util/daemon/queueProcessor/service/Grid.java`
  where `isSeniorMetadataProofEnforced(Message)` returns
  `isSeniorMetadataProofRequired(msg) && CoherenceMode.isSecurityHardeningEnabled()`.
- This violates the compatibility plan's explicit feature-property rule:
  explicit `required=true`, `deny`, and `enforce` settings must preserve their
  fail-closed semantics independent of the global security mode unless a
  feature review deliberately changes them.

Do:
- Preserve patch compatibility for the default/unset case.
- Ensure explicit
  `coherence.security.peer.senior-metadata-proof.required=true` fails closed
  when proof is missing, unavailable, incompatible, malformed, or invalid.
- Keep `coherence.security.mode=hardened` behavior fail-closed for the peer
  proof paths according to the plan.
- Update `SeniorMetadataProofPolicyTest` and
  `SeniorMetadataProofReceiveVerificationTest` so they assert the new
  compatibility/hardened matrix and explicit `required=true` behavior.
- Clean stale active-test references to `LEGACY mode` when touching the peer
  tests.

Do not:
- Do not reintroduce `CoherenceMode.LEGACY`, `isLegacy()`, or
  `coherence.mode=legacy`.
- Do not make `coherence.security.hardened` an alias.
- Do not loosen hardened-mode peer proof enforcement.
- Do not broaden the change into unrelated gates or refactors.
- Do not stage, commit, submit P4, fetch, push, use remote syntax, or touch
  `main`.

Verification:
```bash
source ../bin/cfglocal.sh
REVISION=15.1.2-0-0-security-opt-in-SNAPSHOT
make coherence ARGS="-Drevision=${REVISION}"
mvn -Pmodules,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -Dcoherence.cluster=SeniorMetadataProof-review-fix-20260611-codex \
  -pl test/unit/coherence-tests \
  -Dtest=SeniorMetadataProofPolicyTest,SeniorMetadataProofReceiveVerificationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|isLegacy\(|CoherenceMode\.LEGACY|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" \
  coherence-* test/unit test/functional -g'*.java' -g'*.md'
```

Report the code/test changes, verification results, and any remaining stale
reference hits. If the fix changes the intended meaning of
`coherence.security.peer.senior-metadata-proof.required`, stop and ask for
architect/security-reviewer confirmation instead of guessing.
~~~
