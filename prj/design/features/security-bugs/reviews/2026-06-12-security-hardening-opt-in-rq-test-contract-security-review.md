<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in RQ test-contract security review

- **Date:** 2026-06-12
- **Role:** Security Reviewer
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
  (`P4 //dev/main baseline at change 120899`)
- **Working tree reviewed:** current uncommitted local git working tree after
  the RQ stale prod-default test-contract updates for
  `job.9.20260611214457.28597`
- **P4 / RQ context:** P4 CL `120916` was shelved by the parent and enqueued
  without auto-submit as `job.9.20260612123142.29593`
- **Review artifact:** `design/features/security-bugs/reviews/2026-06-12-security-hardening-opt-in-rq-test-contract-security-review.md`
- **Verdict:** accept with non-blocking follow-up; no product security-gate
  bypass, accidental hardening default, or explicit fail-closed override
  regression was found in the scoped test-contract changes

## Prior reviews consulted

- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round4.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round4.md`
- `design/features/security-bugs/reviews/2026-06-12-security-hardening-opt-in-standard-codex-review.md`

## Files in scope

- `design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
- `test/unit/coherence-tests/src/test/java/com/tangosol/internal/util/security/DynamicLambdaModeTest.java`
- `test/unit/coherence-core-tests/src/test/java/com/tangosol/internal/util/security/RemoteExecutionModeConfigPropertyTest.java`
- `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheCascadeTelemetryTest.java`
- `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheCompositeCascadeTest.java`
- `test/unit/coherence-tests/src/test/java/com/tangosol/util/ExternalizableHelperTest.java`
- `coherence-rest/src/test/java/com/tangosol/coherence/rest/util/PartialObjectTest.java`
- `test/functional/concurrent/src/main/java/concurrent/executor/RemoteExecutorInstallModeMatrixIT.java`

## Context

The RQ failures in `job.9.20260611214457.28597` came from tests that still
treated `coherence.mode=prod` as implicit security hardening. The intended
contract is now two-axis behavior:

- `coherence.mode` remains runtime / license mode and must preserve released
  `dev` / `prod` compatibility.
- `coherence.security.mode=hardened` is the explicit opt-in for July
  compatibility-sensitive hardening gates.
- Unset or `compatibility` security mode must preserve customer and rolling
  upgrade compatibility.
- Explicit feature-level deny / enforce / safe settings must still fail closed
  where those settings own enforcement.

This review focused only on the latest test-contract updates, not the full
product implementation.

## Findings by severity

### P3: RemoteExecutor compatibility functional test does not prove the remote shadow event is emitted

`test/functional/concurrent/src/main/java/concurrent/executor/RemoteExecutorInstallModeMatrixIT.java:131`
and `:141` now submit unannotated RemoteExecutor payloads in
`coherence.mode=prod` with `coherence.security.mode=compatibility` and assert
that the customer path succeeds. That is the right compatibility behavior, and
it avoids the local repro where full hardened mode fails earlier on senior
metadata proof before this install gate is reached.

The remaining gap is that the functional assertions validate compatibility
success, but not the "shadow" side of "compatibility/shadow": the test would
also pass if the RemoteExecutor functional path stopped invoking
`RemoteExecutablePolicy` in compatibility mode and therefore stopped emitting
the expected `would_reject` telemetry or warning. The focused unit coverage
does still prove the gate behavior itself:
`test/unit/coherence-core-tests/src/test/java/com/tangosol/util/RemoteExecutablePolicyTest.java:114`
checks hardened rejection, `:128` checks compatibility `would_reject`, and
the concurrent unit suites exercise hardened nested RemoteExecutor callback
rejection in
`coherence-concurrent/src/test/java/com/oracle/coherence/concurrent/executor/ClusteredTaskManagerInstallGateTest.java:88`
and
`coherence-concurrent/src/test/java/com/oracle/coherence/concurrent/executor/ConcurrentTaskInstallGateTest.java:101`.

This is not a product security bypass and should not block the RQ rerun. Before
final closure, either add a remote-member log / telemetry assertion for the
functional RemoteExecutor shadow path, or explicitly document / rename the
functional test as compatibility-success coverage and leave the shadow event
proof to focused unit coverage until a peer-proof-ready hardened functional
fixture exists.

## Review question answers

1. The scoped test changes correctly distinguish runtime mode from security
   hardening. Tests that prove hardening now set
   `coherence.security.mode=hardened`; tests that prove default compatibility
   leave security mode unset or set it to `compatibility`.
2. Hardened negative coverage was not accidentally removed without replacement.
   The broad unit rejection matrices now opt in to hardened mode, and the
   RemoteExecutor functional hardened rejection was replaced with unit-level
   hardened gate coverage because senior metadata proof preempts the functional
   path in full hardened mode.
3. The RemoteExecutor functional-test decision is sound for RQ and for the
   opt-in plan, with the P3 test-strength caveat above.
4. Explicit feature-level deny semantics remain tested where the property owns
   enforcement. Dynamic remote `deny` is covered in focused dynamic remote and
   install-gate tests; the RemoteExecutor remote-executable policy gate is not
   controlled by that dynamic remote property, and the plan now documents that
   it must not be treated as a substitute hardened rejection switch.
5. No product security regression, compatibility regression, or plan
   contradiction was found in the scoped changes.

## What looks good

- `DynamicLambdaModeTest` now has a representative prod compatibility default
  success assertion and a separate hardened default rejection assertion.
- `RemoteExecutionModeConfigPropertyTest` preserves blank-as-unset behavior and
  verifies that prod compatibility defaults allow while hardened defaults deny.
- `CacheCascadeTelemetryTest` and `CacheCompositeCascadeTest` explicitly set
  hardened mode before asserting hardened cascade behavior.
- `ExternalizableHelperTest` now makes the XML serialization allowlist check a
  hardened-mode test instead of relying on `coherence.mode=prod`.
- `PartialObjectTest` opts generated partial allowlist checks into hardened
  mode for both `dev` and `prod`, which matches the two-axis model.
- `RemoteExecutorInstallModeMatrixIT` explicitly pins both client and storage
  member to `coherence.security.mode=compatibility`, clears the unrelated
  dynamic remote property, and restores prior system-property state.
- The plan's RQ triage notes correctly document why stale prod-default failures
  are test-contract fixes, and why RemoteExecutor functional coverage should
  not use dynamic remote `deny` as a substitute for the remote-executable
  hardening gate.

## Required follow-up work

1. Non-blocking test-strength follow-up: either assert RemoteExecutor
   compatibility `would_reject` telemetry / warning from the storage member, or
   record that the current RemoteExecutor functional test is
   compatibility-success coverage only while shadow event proof remains covered
   by focused unit tests.
2. Keep the current explicit-deny regression coverage for dynamic remote
   `deny`, topic replay `reject`, and senior metadata proof `required=true`.

## Out of scope

- No implementation, test, product, XML, docs, P4 shelf, RQ job, staging area,
  commit, branch, or submit changes were made during this review, except
  creating this durable review file.
- The review did not inspect the P4 shelf contents directly and did not query
  the RQ job page.
- The review did not rerun Maven, functional, security-manager, or RQ tests.
  It relied on the parent-provided verification evidence plus local
  source/diff review and `git diff --check`.

## Verification evidence

Parent verification reported in the review request:

```bash
git diff --check
mvn -Pmodules,-coherence -am -nsu -Drevision=15.1.2-0-0-security-opt-in-SNAPSHOT -Dsurefire.failIfNoSpecifiedTests=false -pl test/unit/coherence-tests,test/unit/coherence-core-tests,coherence-rest -Dtest=DynamicLambdaModeTest,ExternalizableHelperTest,RemoteExecutionModeConfigPropertyTest,CacheCascadeTelemetryTest,CacheCompositeCascadeTest,PartialObjectTest test
mvn -Pmodules,modular-tests,-coherence -nsu -Drevision=15.1.2-0-0-security-opt-in-SNAPSHOT -Dcoherence.cluster=RemoteExecutorInstallModeMatrixIT-codex-20260612-rqrerun -Dfailsafe.failIfNoSpecifiedTests=false -Dsurefire.failIfNoSpecifiedTests=false -pl test/functional/concurrent -Dit.test=RemoteExecutorInstallModeMatrixIT verify
```

Reported results:

- `git diff --check`: pass
- combined focused unit / REST command: pass
- focused concurrent functional command: pass
- P4 CL `120916` shelved
- RQ job `job.9.20260612123142.29593` enqueued without auto-submit

Review-only commands run in this pass:

```bash
git branch --show-current
git log -1 --format='%H %s'
git status --short
git diff -- design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md test/unit/coherence-tests/src/test/java/com/tangosol/internal/util/security/DynamicLambdaModeTest.java test/unit/coherence-core-tests/src/test/java/com/tangosol/internal/util/security/RemoteExecutionModeConfigPropertyTest.java test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheCascadeTelemetryTest.java test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheCompositeCascadeTest.java test/unit/coherence-tests/src/test/java/com/tangosol/util/ExternalizableHelperTest.java coherence-rest/src/test/java/com/tangosol/coherence/rest/util/PartialObjectTest.java test/functional/concurrent/src/main/java/concurrent/executor/RemoteExecutorInstallModeMatrixIT.java
rg -n "security.mode|securityHardened|SECURITY_MODE_HARDENED|compatibility|RemoteExecutorInstallModeMatrix|DynamicLambda|blankValue|GeneratedPartial|FmtXml|Cascade|shouldShadow|shouldReject" design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md test/unit/coherence-tests/src/test/java/com/tangosol/internal/util/security/DynamicLambdaModeTest.java test/unit/coherence-core-tests/src/test/java/com/tangosol/internal/util/security/RemoteExecutionModeConfigPropertyTest.java test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheCascadeTelemetryTest.java test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheCompositeCascadeTest.java test/unit/coherence-tests/src/test/java/com/tangosol/util/ExternalizableHelperTest.java coherence-rest/src/test/java/com/tangosol/coherence/rest/util/PartialObjectTest.java test/functional/concurrent/src/main/java/concurrent/executor/RemoteExecutorInstallModeMatrixIT.java
nl -ba test/functional/concurrent/src/main/java/concurrent/executor/RemoteExecutorInstallModeMatrixIT.java
nl -ba test/unit/coherence-core-tests/src/test/java/com/tangosol/util/RemoteExecutablePolicyTest.java
nl -ba coherence-core/src/main/java/com/tangosol/internal/util/security/RemoteExecutionMode.java
nl -ba test/unit/coherence-tests/src/test/java/com/tangosol/internal/util/security/DynamicLambdaModeTest.java
nl -ba test/unit/coherence-core-tests/src/test/java/com/tangosol/internal/util/security/RemoteExecutionModeConfigPropertyTest.java
nl -ba test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheCascadeTelemetryTest.java
nl -ba test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheCompositeCascadeTest.java
nl -ba test/unit/coherence-tests/src/test/java/com/tangosol/util/ExternalizableHelperTest.java
nl -ba coherence-rest/src/test/java/com/tangosol/coherence/rest/util/PartialObjectTest.java
rg -n "RemoteExecutablePolicy|enforce\\(|remote-executable-policy|CONCURRENT|CONCURRENT_TASK|PlainCallable|PlainExecutorPredicate" coherence-concurrent coherence-core test/unit/coherence-core-tests/src/test/java/com/tangosol/util/RemoteExecutablePolicyTest.java test/functional/concurrent/src/main/java/concurrent/executor/RemoteExecutorInstallModeMatrixIT.java
git diff --check
```

Observed results:

- Branch was `codex/coherence-mode-security-opt-in`.
- Head was
  `88e1c8f93e6d664b994e8491edc2db3dc72fe34a P4 //dev/main baseline at change 120899`.
- `git status --short` showed the expected broad uncommitted implementation
  tree plus untracked design/review files.
- `git diff --check` produced no output.
- The scoped source review found no test that still treats `coherence.mode=prod`
  alone as hardening in the updated RQ failure set.

## Follow-up resolution

The P3 RemoteExecutor functional shadow-telemetry finding was addressed on
2026-06-12 by adding a storage-member remote callable assertion to
`RemoteExecutorInstallModeMatrixIT`. The compatibility functional test now
resets `SerializationTelemetry` on the storage member before each test and
queries the server-side snapshot after unannotated RemoteExecutor payloads
succeed in `coherence.mode=prod` with `coherence.security.mode=compatibility`.

The test asserts `coh.executable.policy_check{result=would_reject,...}` for
both `PlainCallable` and `PlainExecutorPredicate`, so the functional matrix now
proves compatibility success and the shadow policy event. Hardened rejection
remains covered by focused unit tests because full
`coherence.security.mode=hardened` functional coverage can still fail closed
first on senior metadata proof before reaching the RemoteExecutor install
gate.

Focused verification passed:

```bash
source ../bin/cfglocal.sh
mvn -Pmodules,modular-tests,-coherence -nsu \
  -Drevision=15.1.2-0-0-security-opt-in-SNAPSHOT \
  -Dcoherence.cluster=RemoteExecutorInstallModeMatrixIT-codex-20260612-shadowtelemetry \
  -Dfailsafe.failIfNoSpecifiedTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl test/functional/concurrent \
  -Dit.test=RemoteExecutorInstallModeMatrixIT verify
```

Result: `RemoteExecutorInstallModeMatrixIT` passed with 4 tests, 0 failures,
0 errors, and 0 skipped. The review verdict remains accepted, and the
non-blocking P3 follow-up is now closed.

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
- `design/features/security-bugs/reviews/2026-06-12-security-hardening-opt-in-rq-test-contract-security-review.md`

Task:
Handle the non-blocking P3 test-strength follow-up from the RQ test-contract
security review, if the human wants it addressed before final closure.

Do:
- Keep the two-axis contract intact: `coherence.mode` is runtime / license
  mode; `coherence.security.mode=hardened` is the only global hardening opt-in.
- Do not make `coherence.mode=prod` imply hardening.
- Do not use `coherence.remote.dynamic.unauthenticated=deny` as a substitute
  for RemoteExecutor remote-executable-policy hardening.
- Either add a reliable RemoteExecutor functional assertion that the storage
  member emits the compatibility `would_reject` shadow event for unannotated
  RemoteExecutor payloads, or rename / document the current functional test as
  compatibility-success coverage while leaving shadow event proof to focused
  unit tests.

Do not:
- Re-enable hardened mode in `RemoteExecutorInstallModeMatrixIT` unless the
  fixture is also updated to satisfy senior metadata proof first.
- Remove the existing focused hardened unit coverage in
  `RemoteExecutablePolicyTest`, `ClusteredTaskManagerInstallGateTest`, or
  `ConcurrentTaskInstallGateTest`.
- Change product behavior as part of this test-strength follow-up.

Verification:
- Run `git diff --check`.
- If only documentation / naming is updated, run the relevant focused unit or
  no-test justification.
- If `RemoteExecutorInstallModeMatrixIT` changes, run:

```bash
source ../bin/cfglocal.sh
mvn -Pmodules,modular-tests,-coherence -nsu \
  -Drevision=15.1.2-0-0-security-opt-in-SNAPSHOT \
  -Dcoherence.cluster=RemoteExecutorInstallModeMatrixIT-codex-<unique-suffix> \
  -Dfailsafe.failIfNoSpecifiedTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl test/functional/concurrent \
  -Dit.test=RemoteExecutorInstallModeMatrixIT verify
```

Document any resulting decision in
`design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
and update `.ai/branches/codex/coherence-mode-security-opt-in/journal.md`.
~~~
