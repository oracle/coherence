<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in latest test coverage security review

- **Date:** 2026-06-12
- **Role:** Security Reviewer
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
  (`P4 //dev/main baseline at change 120899`)
- **Working tree reviewed:** current uncommitted local git working tree after
  the management, metrics, REST, Extend, and gRPC functional-test cleanup that
  followed RQ `job.9.20260612123142.29593`
- **Review artifact:** `design/features/security-bugs/reviews/2026-06-12-security-hardening-opt-in-latest-test-coverage-security-review.md`
- **Verdict:** accept; no P0-P3 findings were found in the scoped latest test
  changes

## Prior reviews consulted

- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round4.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round4.md`
- `design/features/security-bugs/reviews/2026-06-12-security-hardening-opt-in-rq-test-contract-security-review.md`
- `design/features/security-bugs/reviews/2026-06-12-security-hardening-opt-in-standard-codex-review.md`

## Files in scope

- `design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
- `test/functional/management/src/main/java/management/ManagementHttpAuthIT.java`
- `test/functional/metrics/src/main/java/metrics/MetricsAuthIT.java`
- `test/functional/rest/src/main/java/rest/RestSecurityModeFunctionalTests.java`
- `test/functional/rest/src/main/java/rest/RestEnforcementIntegrationTest.java`
- `test/functional/extend/src/main/java/extend/ExtendNamedCacheInstallModeMatrixIntegrationTest.java`
- `test/functional/extend/src/main/java/extend/TriggerInstallModeMatrixIntegrationTest.java`
- `test/functional/extend/src/main/resources/META-INF/coherence/security-config.xml`
- `test/functional/extend/src/main/resources/extend/test-pof-config.xml`
- `test/functional/grpc-client-tck/src/main/java/grpc/client/GrpcV1EnforcementIntegrationTest.java`
- `test/functional/grpc-client-tck/src/main/java/grpc/client/GrpcV0EnforcementIntegrationTest.java`
- `test/functional/grpc-client-tck/src/main/java/grpc/client/ServerHelper.java`
- Supporting unit coverage checked for removed Extend trigger hardening
  assertions:
  `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/MapTriggerInstallGateTest.java`,
  `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheNamedCacheInstallGateTest.java`,
  `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheCompositeCascadeTest.java`, and
  `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/RemoteExecutablePolicyTest.java`

## Context

The opt-in plan separates released runtime mode from the new July security
hardening posture:

- `coherence.mode` remains the released runtime / license-mode control.
- unset or `coherence.security.mode=compatibility` preserves patch-line
  compatibility and rolling-upgrade behavior.
- `coherence.security.mode=hardened` explicitly enables the July
  compatibility-sensitive fail-closed gates.
- Functional tests that cannot reach a specific install gate in fully hardened
  clusters, because senior metadata or peer proof fails closed earlier, may
  validate compatibility success plus `would_reject` shadow telemetry instead.
- At least one functional suite must still start a real hardened member and
  prove meaningful use.

This review focused only on the latest test changes. It did not re-review the
full production implementation.

## Findings by severity

No P0, P1, P2, or P3 findings.

## Review question answers

1. Yes. The latest management and metrics tests prove that a real
   `coherence.security.mode=hardened` member can start and perform meaningful
   work. `ManagementHttpAuthIT` starts hardened members and verifies default
   unauthenticated GET / POST rejection plus a successful authenticated basic
   request. `MetricsAuthIT` starts a hardened member and verifies default
   unauthenticated scrape rejection plus successful authenticated scrape.
2. Yes. REST and Extend functional tests now intentionally exercise
   compatibility / shadow behavior where that is the correct patch-line
   contract, and they assert `would_reject` telemetry for representative
   unsafe payloads. gRPC enforcement tests explicitly set
   `coherence.security.mode=hardened` and `ServerHelper` resets
   `CoherenceModeHelper` after applying mode properties, so those TCK tests run
   with the requested hardened posture instead of stale cached mode state.
3. No meaningful security coverage was removed without replacement. The
   removed Extend trigger removal-path checks were not valid hardened coverage
   because they used the dynamic-remote property as a substitute security-mode
   gate. Hardened trigger install rejection remains covered by
   `MapTriggerInstallGateTest`, and composite trigger / extractor rejection is
   covered by related unit gate tests. The Extend functional suite retains
   compatibility success plus `would_reject` telemetry.
4. No P0-P3 coverage gaps were found before the next RQ or reviewer cycle.

## What looks good

- `ManagementHttpAuthIT` and `MetricsAuthIT` provide the required real
  hardened-mode functional smoke coverage instead of relying only on unit
  tests or compatibility matrices.
- The HTTP auth tests also preserve explicit `auth=none` behavior under
  hardened mode with warning assertions, which matches the plan's rule that
  explicit feature properties keep their existing semantics.
- REST compatibility tests use raw URL expressions in compatibility mode and
  assert `would_reject` telemetry for representative REST executable-policy
  shadows.
- Extend named-cache and trigger matrices no longer treat `coherence.mode=prod`
  or `coherence.mode=dev` as implicit hardening. They prove install success in
  compatibility and verify shadow telemetry.
- Explicit dynamic remote `deny` remains tested as fail-closed behavior where
  that property actually owns enforcement.
- gRPC V0 and V1 enforcement tests explicitly opt in to
  `coherence.security.mode=hardened`, preserving hardened functional coverage
  for that protocol path.
- Stale references to the removed `ObservableRemovalTrigger` fixture and its
  POF type were removed from the Extend resources.

## Required follow-up work

None from this review.

## Residual risk and test gaps

- This review did not run Maven, functional tests, RQ, P4 commands, or
  subagents. It relied on bounded diff/source review plus the parent-reported
  successful focused checks.
- Broad REST and Extend install-gate functional matrices intentionally do not
  run as fully hardened clusters because unrelated peer/senior proof gates can
  fail closed before the specific gate under test. Hardened enforcement for
  those gates remains primarily unit-level coverage until a peer-proof-ready
  functional fixture exists.
- The next full RQ run remains the right place to catch cross-module or
  packaging issues outside this scoped review.

## Out of scope

- No production source, test source, XML resource, P4 shelf, RQ job, staging
  area, commit, branch, or submit changes were made during this review, except
  creating this durable review file.
- The review did not query RQ job pages or inspect P4 shelf contents.
- The stalled prior reviewer output was not available and was not used.

## Verification evidence

Parent-reported focused checks:

- management / metrics hardened smoke passed
- Extend focused tests passed
- REST focused tests passed
- gRPC Netty focused hardened test passed through the `grpc-client-netty`
  `stage8` runner
- `git diff --check` passed

Review-only commands run in this pass:

```bash
git branch --show-current
git status --short
git log -1 --format='%H %s'
git diff -- test/functional/management/src/main/java/management/ManagementHttpAuthIT.java test/functional/metrics/src/main/java/metrics/MetricsAuthIT.java
git diff -- test/functional/rest/src/main/java/rest/RestSecurityModeFunctionalTests.java test/functional/rest/src/main/java/rest/RestEnforcementIntegrationTest.java
git diff -- test/functional/extend/src/main/java/extend/ExtendNamedCacheInstallModeMatrixIntegrationTest.java test/functional/extend/src/main/java/extend/TriggerInstallModeMatrixIntegrationTest.java test/functional/extend/src/main/resources/META-INF/coherence/security-config.xml test/functional/extend/src/main/resources/extend/test-pof-config.xml
git diff -- test/functional/grpc-client-tck/src/main/java/grpc/client/GrpcV1EnforcementIntegrationTest.java test/functional/grpc-client-tck/src/main/java/grpc/client/GrpcV0EnforcementIntegrationTest.java test/functional/grpc-client-tck/src/main/java/grpc/client/ServerHelper.java
rg -n "security.mode|SECURITY_MODE_HARDENED|SECURITY_MODE_COMPATIBILITY|shouldReject|should.*Hardened|would_reject|Shadows|Compatibility|ExplicitDeny|deny|required|hardened" test/functional/management/src/main/java/management/ManagementHttpAuthIT.java test/functional/metrics/src/main/java/metrics/MetricsAuthIT.java test/functional/rest/src/main/java/rest/RestSecurityModeFunctionalTests.java test/functional/rest/src/main/java/rest/RestEnforcementIntegrationTest.java test/functional/extend/src/main/java/extend/ExtendNamedCacheInstallModeMatrixIntegrationTest.java test/functional/extend/src/main/java/extend/TriggerInstallModeMatrixIntegrationTest.java test/functional/grpc-client-tck/src/main/java/grpc/client/GrpcV1EnforcementIntegrationTest.java test/functional/grpc-client-tck/src/main/java/grpc/client/GrpcV0EnforcementIntegrationTest.java test/functional/grpc-client-tck/src/main/java/grpc/client/ServerHelper.java
rg -n "MapTrigger|Trigger|map-trigger|PlainTrigger|Generated\\$\\$LambdaTrigger|Synthetic\\$\\$LambdaShapedTrigger|SECURITY_MODE_HARDENED|securityHardened|would_reject|rejected" test/unit/coherence-core-tests/src/test/java/com/tangosol/util test/unit/coherence-tests/src/test/java/com/tangosol/util coherence-core/src/main/java/com/tangosol/util coherence-core/src/main/java/com/tangosol/internal/util/security
rg -n "coherence.security.mode=hardened|SECURITY_MODE_HARDENED|security.mode.*hardened|securityHardened|PROP_SECURITY_MODE" test/functional/management/src/main/java/management/ManagementHttpAuthIT.java test/functional/metrics/src/main/java/metrics/MetricsAuthIT.java test/functional/grpc-client-tck/src/main/java/grpc/client/GrpcV0EnforcementIntegrationTest.java test/functional/grpc-client-tck/src/main/java/grpc/client/GrpcV1EnforcementIntegrationTest.java test/functional/rest/src/main/java/rest test/functional/extend/src/main/java/extend
rg -n "ObservableRemovalTrigger|Observable\\$\\$LambdaRemovalTrigger|3034" test/functional/extend/src/main/resources test/functional/extend/src/main/java/extend/TriggerInstallModeMatrixIntegrationTest.java
rg -n "ProdDefault|DevDefault" test/functional/management/src/main/java/management/ManagementHttpAuthIT.java test/functional/metrics/src/main/java/metrics/MetricsAuthIT.java
git diff --check
```

Observed results:

- Branch was `codex/coherence-mode-security-opt-in`.
- Head was
  `88e1c8f93e6d664b994e8491edc2db3dc72fe34a P4 //dev/main baseline at change 120899`.
- `git status --short` showed the expected broad uncommitted implementation
  tree plus untracked design/review files.
- `git diff --check` produced no output.
- The stale `ObservableRemovalTrigger` / POF type scan produced no output.
- The stale `ProdDefault` / `DevDefault` management and metrics scan produced
  no output.

## Codex handoff prompt

~~~markdown
You are continuing the Coherence security hardening opt-in worktree at
`/Users/phfry/dev/perforce/projects/coherence-mode-security-opt-in/prj` on
branch `codex/coherence-mode-security-opt-in`.

Read:

- `.ai/dev-environment.md`
- `.ai/branches/codex/coherence-mode-security-opt-in/journal.md`
- `design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
- `design/features/security-bugs/reviews/2026-06-12-security-hardening-opt-in-latest-test-coverage-security-review.md`

The latest scoped security review accepted the management, metrics, REST,
Extend, and gRPC test changes with no P0-P3 findings. Do not undo the
functional test split: management/metrics and gRPC preserve explicit hardened
functional coverage; REST/Extend broad matrices intentionally prove
compatibility success plus `would_reject` telemetry where hardened mode can be
preempted by unrelated peer/senior proof gates.

Before any RQ or further reviewer cycle, discuss the plan with the user. Do
not enqueue RQ or start subagents without explicit user approval.

Useful lightweight verification before handoff or RQ:

```bash
git diff --check
rg -n "ObservableRemovalTrigger|Observable\\$\\$LambdaRemovalTrigger|3034" test/functional/extend/src/main/resources test/functional/extend/src/main/java/extend/TriggerInstallModeMatrixIntegrationTest.java
rg -n "ProdDefault|DevDefault" test/functional/management/src/main/java/management/ManagementHttpAuthIT.java test/functional/metrics/src/main/java/metrics/MetricsAuthIT.java
```
~~~
