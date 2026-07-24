<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in implementation architect review round 3

- **Date:** 2026-06-11
- **Role:** Architect
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
  (`P4 //dev/main baseline at change 120899`)
- **Working tree reviewed:** current uncommitted implementation after the
  first architect review, first security review, architect round-2 cleanup, and
  security round-2 explicit-override fixes, plus untracked compatibility plan
  and review documents
- **Plan reviewed:**
  `design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
- **Verdict:** not ready; customer-facing hardening documentation still
  publishes the removed `LEGACY` / `coherence.mode=prod` hardening contract

## Prior reviews

- `design/features/security-bugs/reviews/2026-06-11-security-mode-property-design-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round2.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round2.md`

## Files in scope

- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
- `coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java`
- `coherence-testing-support/src/main/java/com/oracle/coherence/testing/util/CoherenceModeHelper.java`
- `coherence-core/src/main/resources/tangosol-coherence.xml`
- Current implementation diff touching hardening gates in Coherence Core,
  REST, JSON, gRPC, RAG, Concurrent, Extend, TCMP, Reporter, Management, SSL,
  and related unit / functional tests
- Untracked replacement test
  `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CompatibilityShadowExecutablePolicyTest.java`
- Customer-facing documentation scanned under `docs/` and relevant resource
  trees, especially `docs/core/04_serialization_hardening.adoc`
- Verification evidence recorded in
  `.ai/branches/codex/coherence-mode-security-opt-in/journal.md`

## Context

The compatibility plan requires restoring released `coherence.mode` behavior
and separating July compatibility-sensitive hardening behind
`coherence.security.mode=compatibility|hardened`. The restored runtime-mode
contract is: unset defaults to `dev`; released values including `eval`, `dev`,
`development`, `prod`, and `production` are accepted; blank, invalid, and
unreleased `legacy` / `legacy-compatibility` values are rejected; and the
hardening-era prominent DEV warning is removed.

The current product implementation mostly follows that contract. The central
resolver exposes only `EVAL`, `DEV`, and `PROD`; it defaults unset
`coherence.mode` to `DEV`; the operational XML `license-mode` default is back
to `dev`; and compatibility-sensitive gates reviewed in this pass use
`isSecurityHardeningEnabled()` or a named predicate that delegates to it.

The round-2 security findings for explicit fail-closed feature properties also
appear addressed at the reviewed boundaries: explicit
`coherence.remote.dynamic.unauthenticated=deny` is checked before compatibility
shadowing in dynamic install, and explicit
`coherence.topics.persisted.policy-drift=reject` rejects before replay
shadow/dedup can allow the payload.

The remaining blocking issue is documentation. The customer-facing SER-01
hardening document still describes the old unshipped design where
`coherence.mode` has a `LEGACY` default and `coherence.mode=prod` enables the
hardening posture.

## Findings by severity

### P1: Customer-facing serialization hardening docs still describe removed `LEGACY` mode and `coherence.mode=prod` hardening

The implementation restores `coherence.mode` as a released runtime mode and
moves compatibility-breaking hardening behind `coherence.security.mode`, but
`docs/core/04_serialization_hardening.adoc` still publishes the obsolete
single-axis mode contract:

- Lines 23-26 say Coherence starts in `LEGACY` mode when `coherence.mode` is
  unset and instruct customers to set `-Dcoherence.mode=prod` for the
  hardening-on posture.
- Lines 54-75 document `LEGACY`, `DEV`, and `PROD` as the three hardening
  postures, with unset / blank / invalid defaulting to `LEGACY`.
- Lines 82-93 tell customers to start from post-upgrade `LEGACY`, watch
  `LEGACY` shadow telemetry, and test migration with `-Dcoherence.mode=prod`.
- Lines 104-121 say `coherence.mode` accepts `legacy` and
  `legacy-compatibility`, that unset / blank / invalid defaults to `LEGACY`,
  and that `PROD` is the hardening-on target.
- Lines 123-145 document the removed hardening-era DEV warning and the removed
  `LEGACY_WARNING` banner.
- Lines 710-716 list `coherence.mode` as
  `legacy|legacy-compatibility|dev|development|prod|production`, defaulting to
  `LEGACY`, and describe `DEV` / `LEGACY` warnings.
- Lines 837-856, 953-959, and 1015-1022 still explain dynamic remote and
  telemetry behavior in terms of `coherence.mode=prod` and `LEGACY` shadow
  tuples.

This directly violates the plan's documentation requirement at
`design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md:640`,
which says customer-facing docs must stop saying that `coherence.mode=prod`
alone enables the July compatibility-breaking hardening. It also leaves a
public `legacy` runtime-mode contract in docs after the code correctly rejects
that value.

Required fix: rewrite this document to the two-axis contract:

- `coherence.mode` is runtime/license mode; unset defaults to `dev`; accepted
  values are the restored released values, including `eval`, `dev`,
  `development`, `prod`, and `production`; blank, invalid, `legacy`, and
  `legacy-compatibility` are rejected.
- `coherence.security.mode` defaults to `compatibility` and accepts only
  `compatibility` and `hardened`; `hardened` enables the July hardening gates.
- Migration guidance should say patch / roll with unchanged config and
  compatibility security mode, then enable `coherence.security.mode=hardened`
  in a later validation window.
- Runtime property tables, error references, telemetry text, and dynamic
  remote examples should describe compatibility vs hardened security mode,
  while preserving explicit feature-property guidance such as
  `coherence.remote.dynamic.unauthenticated=allow|deny`.

### P3: Active product helper names still call compatibility shadow reasons "Legacy"

The earlier active-test wording cleanup is mostly complete, but two active
product helpers still carry the removed legacy-mode identity:

- `coherence-core/src/main/java/com/tangosol/coherence/reporter/ReporterSecurity.java:728`
  calls `isLegacyShadowReason(...)`, and the helper is declared at line 759.
- `coherence-core/src/main/java/com/tangosol/net/management/ManagementInvocationPolicy.java:477`
  calls `isLegacyShadowReason(...)`, and the helper is declared at line 510.

The behavior is now correctly keyed from
`!CoherenceMode.isSecurityHardeningEnabled()` and the emitted log text says
`security-mode=compatibility`, so this is not a behavior blocker. It is still
active product-code terminology that conflicts with the plan's removal of the
unreleased legacy mode surface and can mislead future maintainers auditing
compatibility-mode logic.

Required fix: rename these helpers to compatibility-mode language, for
example `isCompatibilityShadowReason(...)` or
`isCompatibilityShadowEligibleReason(...)`, without changing the reason sets
or enforcement behavior.

### P3: Central resolver tests still lack the explicit old-property non-alias assertion from the plan

The product/test source scan found no active references to
`coherence.security.hardened` or `PROP_SECURITY_HARDENED`, which is good
implementation evidence. However, the plan's central resolver test list
explicitly requires verifying that the earlier unshipped
`coherence.security.hardened` working property is not an alias
(`design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md:589`).

`CoherenceModeTest` now covers the new values, trimming/case-insensitivity,
blank / unknown values, and boolean-style aliases at
`test/unit/coherence-tests/src/test/java/com/tangosol/internal/util/CoherenceModeTest.java:179`,
but it does not set only `coherence.security.hardened=true` and assert that
hardening remains disabled when `coherence.security.mode` is unset.

This is lower risk than an active source hit because the implementation does
not read the old property today. Adding the assertion would close the
test-plan gap and prevent the old boolean property from reappearing as an
accidental alias.

## What looks good

- `CoherenceMode` now exposes only `EVAL`, `DEV`, and `PROD` runtime modes,
  defaults unset `coherence.mode` to `DEV`, accepts `development` and
  `production`, rejects invalid values including `legacy`, and removes the
  hardening-era warning logger and warning constants.
- `coherence.security.mode` parsing is centralized, defaults unset to
  compatibility posture, trims and compares values case-insensitively, accepts
  only `compatibility` and `hardened`, and rejects blank / unknown values.
- The operational XML `license-mode` default is restored to `dev`.
- Representative compatibility-sensitive gates now use
  `isSecurityHardeningEnabled()` or a named predicate delegating to it rather
  than `coherence.mode=prod`.
- The prior round-2 explicit-override bugs are fixed in the reviewed
  `RemoteInstallGate` paths: explicit dynamic remote `deny` and explicit topic
  replay drift `reject` now fail closed in compatibility security mode.
- Active Java/XML/property scans found no product or test references to
  `coherence.security.hardened`, `PROP_SECURITY_HARDENED`,
  `restoreSecurityHardened`, `CoherenceMode.LEGACY`, `isLegacy()`, active
  `coherence.mode=legacy`, `mode("legacy")`, or `restore("legacy")`.
- Current tests and functional fixtures have largely moved from active
  `coherence.mode=legacy` configuration to released runtime modes crossed with
  `coherence.security.mode=compatibility` or `hardened`.

## Required follow-up work

1. Update `docs/core/04_serialization_hardening.adoc` so customer-facing docs
   match the two-axis runtime-mode / security-mode contract.
2. Rename the active product helper methods `isLegacyShadowReason(...)` in
   Reporter and Management TCMP policy to compatibility-mode terminology.
3. Add a focused `CoherenceModeTest` assertion that setting only the old
   unshipped `coherence.security.hardened` property does not enable hardening
   and is not treated as an alias.
4. Re-run `git diff --check`, stale-reference scans, and the affected focused
   tests after the cleanup.

## Out of scope

- No product source, tests, XML, docs, P4 state, commits, submits, backports,
  branches, remotes, or RQ jobs were modified during this review.
- This pass did not rerun Maven unit, functional, security-manager, or Remote
  Queue verification. It relied on source/diff review, scans, and the focused
  verification evidence recorded in the branch journal.
- Deep threat-model validation remains covered by the security reviews. This
  pass focused on architecture, compatibility, docs, and plan conformance.

## Verification evidence

Review-only commands run:

```bash
source ../bin/cfglocal.sh
git branch --show-current
git log -1 --format='%H %s'
git status --short
git diff --stat
git diff --check
git ls-files --others --exclude-standard
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" coherence-* test/unit test/functional --glob '*.java' --glob '*.xml' --glob '*.properties'
rg -n '"legacy"|LEGACY mode|legacy mode|legacy compatibility|legacy behavior|LegacyShadow' coherence-* test/unit test/functional --glob '*.java' --glob '*.xml' --glob '*.properties'
rg -n "coherence\.mode=prod|coherence\.mode|security\.mode|security\.hardened|legacy mode|LEGACY mode" docs coherence-distribution coherence-core/src/main/resources coherence-rest/src/main/resources coherence-grpc/src/main/resources coherence-json/src/main/resources --glob '*.md' --glob '*.adoc' --glob '*.xml' --glob '*.properties' --glob '*.txt'
rg -n "CoherenceMode\.isProd\(|CoherenceMode\.isDev\(|CoherenceMode\.current\(\)|com\.tangosol\.util\.CoherenceMode\.(isProd|isDev|current)\(" coherence-core-components/src/main/java coherence-core/src/main/java coherence-grpc/src/main/java coherence-json/src/main/java coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java --glob '*.java'
rg -n "isSecurityHardeningEnabled\(|isAllowlistEnforced\(|isDynamicRemoteDefaultDeny\(|isRemoteExecutableEnforced\(|isCoherenceRestAuthEnforced\(|isCoherenceRestPassThroughAllowlistRequired\(|isXmlExternalEntityProtectionRequired\(" coherence-core-components/src/main/java coherence-core/src/main/java coherence-grpc/src/main/java coherence-json/src/main/java coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java --glob '*.java'
```

Observed results:

- Branch was `codex/coherence-mode-security-opt-in`.
- Head was
  `88e1c8f93e6d664b994e8491edc2db3dc72fe34a P4 //dev/main baseline at change 120899`.
- `git diff --check` produced no output.
- The exact removed API/property scan was clean in product and test Java/XML/
  property scopes.
- The broader active-code legacy wording scan found the two
  `isLegacyShadowReason(...)` helper names listed above, plus unrelated legacy
  concepts and the expected `CoherenceModeTest.assertInvalidMode("legacy")`.
- The docs scan found the blocking stale SER-01 documentation listed above.
- Product mode scans did not reveal a renewed obvious hardening gate keyed
  directly to `coherence.mode=prod` in the reviewed source trees.

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
- `design/features/security-bugs/reviews/2026-06-11-security-mode-property-design-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round2.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round2.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round3.md`

Task:
Fix the round-3 architect-review findings without changing the approved
implementation shape.

Do:
- Update `docs/core/04_serialization_hardening.adoc` to the two-axis contract:
  `coherence.mode` is runtime/license mode, unset defaults to `dev`, accepted
  values include `eval`, `dev`, `development`, `prod`, and `production`, and
  blank / invalid / `legacy` / `legacy-compatibility` are rejected.
- Document `coherence.security.mode` as the only global hardening opt-in, with
  unset / `compatibility` preserving patch compatibility and `hardened`
  enabling the July compatibility-sensitive hardening gates.
- Remove customer-facing documentation that says `LEGACY` is a runtime mode,
  unset / blank / invalid `coherence.mode` defaults to `LEGACY`, the removed
  DEV or LEGACY warning banners are emitted, or `coherence.mode=prod` alone
  enables the July hardening gates.
- Update runtime property tables, migration guidance, error references, and
  telemetry text to describe compatibility vs hardened security mode while
  preserving explicit feature-property guidance.
- Rename the active product helpers named `isLegacyShadowReason(...)` in
  `ReporterSecurity` and `ManagementInvocationPolicy` to compatibility-mode
  terminology, without changing behavior.
- Add a focused central resolver test proving that setting only the old
  unshipped `coherence.security.hardened=true` property does not enable
  hardening and is not an alias for `coherence.security.mode=hardened`.

Do not:
- Reintroduce `CoherenceMode.LEGACY`, `isLegacy()`, `coherence.mode=legacy`,
  `legacy-compatibility`, or `coherence.security.hardened` as active product
  aliases.
- Re-couple hardening gates to `coherence.mode=prod`.
- Change product hardening behavior except for the helper rename and resolver
  test described above.
- Stage, commit, submit P4, enqueue Remote Queue, fetch, push, touch `main`,
  or revert unrelated user/agent changes unless explicitly asked.

Suggested verification:
```bash
source ../bin/cfglocal.sh
REVISION=15.1.2-0-0-security-opt-in-SNAPSHOT
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" \
  coherence-* test/unit test/functional docs \
  -g'*.java' -g'*.xml' -g'*.properties' -g'*.adoc' -g'*.md'
rg -n "LEGACY mode|legacy mode|legacy compatibility|LegacyShadow|isLegacyShadowReason" \
  coherence-* test/unit test/functional docs \
  -g'*.java' -g'*.xml' -g'*.properties' -g'*.adoc' -g'*.md'
rg -n "coherence\.mode=prod.*hardening|hardening.*coherence\.mode=prod|coherence\.mode.*legacy|LEGACY.*coherence\.mode" \
  docs coherence-core/src/main/resources \
  -g'*.adoc' -g'*.md' -g'*.xml' -g'*.properties'
mvn -Pmodules,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl test/unit/coherence-tests \
  -Dtest=CoherenceModeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Report the documentation updates, helper/test cleanup, scan results, and test
results. If you find additional docs that intentionally need old historical
legacy wording, explain why they are unrelated to the removed
`coherence.mode=legacy` contract.
~~~
