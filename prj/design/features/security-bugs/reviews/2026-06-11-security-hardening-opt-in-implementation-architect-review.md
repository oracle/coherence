<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in implementation architect review

- **Date:** 2026-06-11
- **Role:** Architect
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
  (`P4 //dev/main baseline at change 120899`)
- **Working tree reviewed:** uncommitted implementation of
  `coherence.security.mode` plus untracked compatibility plan/review docs
- **Plan reviewed:**
  `design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
- **Verdict:** not ready; blocking test/fixture conversion remains

## Prior reviews

- `design/features/security-bugs/reviews/2026-06-11-security-mode-property-design-review.md`

## Files in scope

- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
- `coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java`
- `coherence-testing-support/src/main/java/com/oracle/coherence/testing/util/CoherenceModeHelper.java`
- Current implementation diff touching hardening gates in Coherence Core,
  REST, JSON, gRPC, RAG, Concurrent, and focused functional tests
- Current test and functional fixtures that still configure `"legacy"` as a
  `coherence.mode` value
- Verification evidence recorded in
  `.ai/branches/codex/coherence-mode-security-opt-in/journal.md`

## Context

The compatibility plan requires restoring released `coherence.mode` behavior
and moving the July compatibility-sensitive hardening gates behind the new
string-valued `coherence.security.mode` property. Supported security-mode
values are unset / `compatibility` and `hardened`; blank, unknown, shorthand,
boolean-style values, and the old unshipped `coherence.security.hardened`
property are not aliases.

The implementation in the central resolver follows that shape: the public
`LEGACY` runtime-mode constant and `isLegacy()` facade are removed, runtime
mode accepts released values only, and the named compatibility-sensitive gates
delegate to `isSecurityHardeningEnabled()`. Source scans did not find remaining
Java references to `CoherenceMode.LEGACY`, `isLegacy()`,
`coherence.security.hardened`, or `PROP_SECURITY_HARDENED`.

However, a broader string-literal scan found many current unit and functional
tests still configuring `coherence.mode` to `"legacy"`. Since the updated
resolver now correctly rejects that value, those tests no longer model
compatibility posture and will fail or start members with invalid mode when
they run.

## Findings by severity

### P1: Current tests and functional fixtures still configure removed `coherence.mode=legacy`

The implementation removes the unreleased `legacy` runtime mode and tests that
`CoherenceMode.current()` rejects `"legacy"`, but a large set of existing
tests still use `"legacy"` as the active runtime mode. These are not only
method names or comments; the helpers write the literal into
`CoherenceMode.PROP_COHERENCE_MODE` or into member startup properties.

Representative examples:

- `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CacheNamedCacheInstallGateTest.java:113`
  and `:167` call `setMode("legacy", null)`, and that helper writes the value
  to `CoherenceMode.PROP_COHERENCE_MODE` at `:212`.
- `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/MapTriggerInstallGateTest.java:98`
  and `:159` call `setMode("legacy", null)`, with the same direct property
  write at `:201`.
- `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/TopicsSubscriberInstallGateTest.java:103`,
  `:148`, `:194`, and `:239` still call `setMode("legacy", null)`, with the
  direct property write at `:356`.
- `test/unit/coherence-core-tests/src/test/java/com/tangosol/internal/net/service/extend/proxy/InvocationServiceProxyTest.java:85`,
  `:200`, and `:306` call `setMode("legacy")`, which writes
  `coherence.mode` at `:399`.
- Peer proof tests still use `"legacy"` in compatibility-path rows:
  `test/unit/coherence-tests/src/test/java/com/tangosol/coherence/component/net/message/SeniorMetadataProofPolicyTest.java:81`
  and `:227`,
  `RequestMessageSubjectProofPolicyTest.java:173`, and
  `SeniorMetadataProofReceiveVerificationTest.java:269`, `:297`, `:318`,
  and `:414`.
- Functional fixtures still start members with invalid runtime mode:
  `test/functional/extend/src/main/java/extend/ExtendNamedCacheInstallModeMatrixIntegrationTest.java:127`,
  `:157`, and `:191`;
  `TriggerInstallModeMatrixIntegrationTest.java:122`, `:171`, `:180`,
  `:212`, and `:288`;
  `DynamicRemoteModeIntegrationTest.java:132`, `:156`, and `:162`;
  `test/functional/topics/src/main/java/topics/TopicsSubscriberInstallModeMatrixIntegrationTest.java:120`,
  `:161`, `:202`, `:234`, `:263`, and `:304`;
  and `test/functional/rest/src/main/java/rest/RestSecurityModeFunctionalTests.java:91`,
  `:95`, `:109`, `:124`, `:138`, and `:156`.
- `test/functional/tcmp/src/main/java/tcmp/LicenseModeCompatibilityTests.java:42`,
  `:48`, `:54`, and `:60` still define rolling-restart scenarios into and out
  of `"legacy"`, while the implementation deliberately removed the
  `matchLegacyLicenseMode` join-time compatibility path.

This violates the plan requirement that no unreleased `LEGACY` mode test
surface remains and leaves important compatibility coverage aimed at the old
model instead of the new contract. The compatibility cases should be rewritten
to use released runtime modes (`dev`, `prod`, or unset as appropriate) crossed
with `coherence.security.mode=compatibility`; hardened counterparts should use
the same released runtime modes crossed with
`coherence.security.mode=hardened`. Expected telemetry should stop expecting
`mode=legacy` and should assert the released runtime mode plus
`security-mode=compatibility` where the product emits it.

Until this is fixed, the implementation is not ready for full unit,
functional, RQ, or P4 submit validation.

## What looks good

- `CoherenceMode` now separates runtime mode from hardening posture and adds
  only the narrow internal boolean predicate required by the plan.
- The parser accepts `compatibility` / `hardened` case-insensitively after
  trimming and rejects blank, unknown, shorthand, and boolean-style security
  mode values.
- `coherence.security.hardened` and the old Java boolean property names do not
  remain in Java source or XML.
- Meaningful hardening gates inspected in the implementation diff use
  `isSecurityHardeningEnabled()` or a named predicate delegating to it rather
  than `coherence.mode=prod`.
- The removal of the cluster license-mode legacy adaptation matches the plan's
  requirement to restore released `coherence.mode` / license-mode behavior.
- The JDK 25 security-manager failure is an environment limitation:
  `/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java
  -Djava.security.manager -version` exits during VM initialization with
  "Enabling a Security Manager is not supported." The JDK 21 pass recorded in
  the branch journal is therefore the relevant product verification evidence.

## Test gaps and residual risk

- The focused verification recorded in the journal is valuable but does not
  cover the stale `"legacy"` tests listed above.
- The compatibility matrix needs a final pass after test conversion to ensure
  representative default compatibility, explicit `compatibility`, explicit
  `hardened`, invalid security mode values, and released `dev` / `prod`
  runtime modes are all exercised.
- Rolling-upgrade-sensitive local coverage should be updated from the removed
  legacy-mode join tests to released-mode patch compatibility coverage, or the
  remaining mixed-version coverage gap should be explicitly accepted before RQ.

## Required follow-up work

- Convert current unit and functional tests that configure
  `coherence.mode=legacy` to the new two-axis contract:
  released `coherence.mode` values plus `coherence.security.mode`.
- Rename stale test methods and comments that describe `LEGACY` mode when they
  are now testing security-mode compatibility.
- Update expected telemetry and log assertions away from `mode=legacy` to the
  actual released mode and, where emitted, `security-mode=compatibility`.
- Re-run targeted unit and functional tests covering the converted files, then
  repeat a stale string scan for `"legacy"` in current security-mode tests and
  fixtures.

## Out of scope

- No product source, tests, XML, P4 state, commits, or backports were modified
  as part of this review.
- Pure threat-model analysis is intentionally left to the separate security
  reviewer subagent.
- Historical design and review docs that record the old legacy-mode or
  `coherence.security.hardened` working design were not treated as product
  residue.

## Verification evidence

Review-only commands run:

```bash
git branch --show-current
git log -1 --format='%H %s'
git status --short
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" coherence-* test --glob '*.java' --glob '*.xml'
rg -n "\"legacy\"" test/unit test/functional coherence-* --glob '*.java' --glob '*.xml'
/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java -Djava.security.manager -version
```

Results:

- `git diff --check` produced no output.
- Removed Java/API/property scans were clean for the exact old property and
  API names in Java/XML.
- The broader `"legacy"` literal scan found the blocking stale test and
  functional fixture uses listed above.
- The JDK 25 security-manager command failed during VM initialization before
  product code could run, confirming the environment limitation classification.

No Maven tests were run for this architect review because source review already
found a blocking completeness issue.

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
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review.md`

Task:
Fix the blocking architect-review finding: current unit and functional tests
still configure removed `coherence.mode=legacy` even though the implementation
now correctly rejects that runtime mode.

Do:
- Keep the product implementation shape intact unless a specific test exposes
  a real product bug.
- Convert tests that currently use `"legacy"` as a runtime mode to the new
  two-axis contract: released `coherence.mode` values (`prod`, `dev`, or unset
  as appropriate) crossed with `coherence.security.mode=compatibility` for the
  compatibility path and `coherence.security.mode=hardened` for hardened
  counterparts.
- Update helper methods, member startup properties, expected telemetry, method
  names, and comments so they refer to compatibility posture rather than
  LEGACY mode.
- Preserve explicit feature-property semantics such as deny/enforce/allow
  overrides.
- Keep unrelated legacy concepts, such as LegacyXml helpers, out of scope.

Do not:
- Reintroduce `CoherenceMode.LEGACY`, `isLegacy()`, parser aliases for
  `legacy`, `coherence.security.hardened`, or any alias from
  `coherence.mode=legacy` to the new security mode.
- Fetch, push, submit P4, touch `main`, revert unrelated user/agent changes,
  or perform backports.
- Run security-manager functional tests on JDK 25.

Suggested verification:
```bash
source ../bin/cfglocal.sh
REVISION=15.1.2-0-0-security-opt-in-SNAPSHOT
rg -n "\"legacy\"" test/unit test/functional coherence-* --glob '*.java' --glob '*.xml'
mvn -Pmodules,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl test/unit/coherence-core-tests,test/unit/coherence-tests \
  -Dtest=CacheNamedCacheInstallGateTest,MapTriggerInstallGateTest,TopicsSubscriberInstallGateTest,RemoteExecutablePolicyTest,InvocationServiceProxyTest,SeniorMetadataProofPolicyTest,RequestMessageSubjectProofPolicyTest,SeniorMetadataProofReceiveVerificationTest,SerializationAllowlistTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -Pmodules,modular-tests,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl test/functional/extend,test/functional/topics,test/functional/rest,test/functional/tcmp \
  -Dit.test=ExtendNamedCacheInstallModeMatrixIntegrationTest,TriggerInstallModeMatrixIntegrationTest,DynamicRemoteModeIntegrationTest,TopicsSubscriberInstallModeMatrixIntegrationTest,TopicsSubscriberReplayIntegrationTest,RestSecurityModeFunctionalTests,LicenseModeCompatibilityTests \
  -Dcoherence.cluster=security-mode-compat-tests-20260611-codex \
  verify
git diff --check
```

Report the files changed, the remaining `"legacy"` scan hits with rationale
for any unrelated legacy concepts, and verification results. Do not stage or
commit unless explicitly asked.
~~~
