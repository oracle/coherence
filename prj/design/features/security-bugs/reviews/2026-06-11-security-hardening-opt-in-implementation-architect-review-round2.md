<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in implementation architect review round 2

- **Date:** 2026-06-11
- **Role:** Architect
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
  (`P4 //dev/main baseline at change 120899`)
- **Working tree reviewed:** current uncommitted implementation after the
  first architect and security review fixes, plus untracked compatibility
  plan and review documents
- **Plan reviewed:**
  `design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
- **Verdict:** ready for broader validation; no blocking architect /
  plan-conformance findings found in this round

## Prior reviews

- `design/features/security-bugs/reviews/2026-06-11-security-mode-property-design-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review.md`

## Files in scope

- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
- `coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java`
- `coherence-testing-support/src/main/java/com/oracle/coherence/testing/util/CoherenceModeHelper.java`
- `coherence-core-components/src/main/java/com/tangosol/coherence/component/net/Member.java`
- `coherence-core-components/src/main/java/com/tangosol/coherence/component/util/daemon/queueProcessor/service/Grid.java`
- `coherence-core-components/src/main/java/com/tangosol/coherence/component/util/daemon/queueProcessor/service/grid/ClusterService.java`
- Representative hardening gates in Coherence Core, REST, JSON, gRPC, RAG,
  Concurrent, Extend, TCMP, and touched functional tests
- Current unit and functional tests that were converted from active
  `coherence.mode=legacy` usage to the two-axis runtime/security-mode contract
- Verification evidence recorded in
  `.ai/branches/codex/coherence-mode-security-opt-in/journal.md`

## Context

The compatibility plan requires preserving released `coherence.mode` behavior
and moving July compatibility-sensitive hardening gates behind the new
string-valued `coherence.security.mode` property. The supported security-mode
values are unset / `compatibility` and `hardened`; explicit values trim and
compare case-insensitively; blank, unknown, shorthand, and boolean-style
values fail fast. The earlier unshipped `coherence.security.hardened` property
must not remain as an alias.

The first architect review blocked on active tests and functional fixtures
still configuring removed `coherence.mode=legacy`. The security review blocked
on senior metadata proof making explicit
`coherence.security.peer.senior-metadata-proof.required=true` depend on the
global hardening mode. The current diff addresses both issues:

- active Java/XML scans no longer find `CoherenceMode.LEGACY`, `isLegacy()`,
  `mode("legacy")`, `restore("legacy")`, `coherence.mode=legacy`, or the old
  boolean property in product/test code;
- senior metadata proof now enforces when the explicit required property is
  set, and hardened mode also requires the proof;
- current tests and functional fixtures use released runtime modes plus
  `coherence.security.mode=compatibility` or `hardened`;
- patch-line license-mode compatibility coverage has been rolled back to
  released `dev` and `prod` modes.

## Findings by severity

No P1 or P2 architect findings were found in this round.

### P3: Some active test names and comments still describe compatibility posture as LEGACY mode

Runtime behavior no longer depends on `coherence.mode=legacy`, but several
active tests still use method names, helper names, or comments that describe
the compatibility posture as `LEGACY` mode. Examples:

- `coherence-rest/src/test/java/com/tangosol/coherence/rest/PassThroughRootResourceTest.java:31`
  still says the prompt requires `LEGACY auto-publish compatibility`.
- `coherence-rest/src/test/java/com/tangosol/coherence/rest/PassThroughRootResourceTest.java:87`
  and `:90` still describe the compatibility test as legacy mode.
- `coherence-rest/src/test/java/com/tangosol/coherence/rest/providers/SecurityFilterTest.java:183`,
  `:196`, `:208`, `:241`, and `:341` still use LEGACY wording for
  compatibility-mode behavior.
- `coherence-rest/src/test/java/com/tangosol/coherence/rest/providers/SecurityFilterTest.java:190`,
  `:202`, `:214`, `:247`, and `:348` keep `Legacy` in test/helper names even
  though the helpers now use `CoherenceModeHelper.securityCompatibility()`.
- `coherence-json/src/test/java/com/oracle/coherence/io/json/internal/MapConverterTest.java:36`
  and
  `coherence-json/src/test/java/com/oracle/coherence/io/json/internal/ClassConverterTest.java:32`
  still describe the active test matrix in legacy/dev/prod terms.
- `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/LegacyShadowExecutablePolicyTest.java:24`
  still names the compatibility shadow coverage as `LEGACY`.

This is not a blocking behavior issue because the scan for active
`coherence.mode=legacy` configuration is clean except for
`CoherenceModeTest.assertInvalidMode("legacy")` and unrelated legacy literals.
It is still worth cleaning before final handoff or documentation work, because
the plan explicitly removes the unreleased public legacy-mode identity and
prefers `security-mode=compatibility` language for active test surfaces.

## What looks good

- `coherence.mode` no longer exposes the unshipped `LEGACY` enum value,
  `isLegacy()` predicate, parser aliases, legacy banner, or license-mode
  rolling-upgrade adaptation.
- `coherence.security.mode` accepts unset / `compatibility` and `hardened`,
  trims and compares values case-insensitively, and rejects blank, unknown,
  shorthand, and boolean-style values in the central resolver.
- `coherence.security.hardened` and `PROP_SECURITY_HARDENED` are absent from
  product and test code; remaining hits are historical plan/review text.
- Representative compatibility-sensitive gates use
  `isSecurityHardeningEnabled()` or a named predicate delegating to it, rather
  than `coherence.mode=prod`, including serialization limits, REST, JSON,
  gRPC diagnostics, TLS hostname verification, management publish URL, dynamic
  remote executable install, peer proof, federation, Concurrent Extend, and
  RAG.
- Explicit feature overrides retain the planned behavior in the reviewed
  examples: explicit senior metadata proof `required=true` now fails closed
  independent of global security mode; TLS `allow` remains a compatibility
  setting and is rejected under hardened mode; explicit gRPC diagnostic values
  still win over `auto`; explicit dynamic remote deny/reject settings still
  deny or reject in compatibility mode.
- Current test and functional fixture conversions now model released runtime
  modes crossed with `coherence.security.mode=compatibility` or `hardened`,
  rather than the removed `legacy` runtime mode.
- `test/functional/tcmp/src/main/java/tcmp/LicenseModeCompatibilityTests.java`
  now covers rolling between released `dev` and `prod` license modes instead
  of rolling through the removed legacy mode.

## Required follow-up work

No blocking implementation follow-up is required by this architect review.

Before treating the branch as final, clean the non-blocking active-test
language above so method names, helper names, and comments describe
`security-mode=compatibility` or historical compatibility behavior rather than
LEGACY mode.

Consider adding one focused central-resolver assertion that setting only the
unshipped `coherence.security.hardened` property does not enable hardening.
The current product scan proves the old property is not read, and the existing
security-mode parser tests cover invalid boolean-style values on
`coherence.security.mode`; an explicit non-alias assertion would make that
release-management rule harder to regress.

Proceed with broader validation appropriate for submit readiness, including
Remote Queue planning, once the owner is ready. The parent already recorded
successful focused unit, functional compile, JDK 21 security functional, stale
scan, and `git diff --check` verification.

## Out of scope

- No product source, tests, XML, P4 state, commits, submits, backports, or RQ
  jobs were modified or launched during this review.
- Customer-facing release notes and documentation beyond the compatibility
  plan were not reviewed for final wording.
- Deep threat modeling remains covered by the separate security review; this
  pass focused on architecture and plan conformance after the review fixes.
- Historical design and review documents that intentionally preserve the old
  `legacy` or `coherence.security.hardened` working design were not treated as
  product residue.

## Verification evidence

Review-only commands run:

```bash
source ../bin/cfglocal.sh
git branch --show-current
git log -1 --format='%H %s'
git status --short
git diff --stat
git diff --name-only
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" \
  coherence-* test/unit test/functional \
  design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md \
  design/features/security-bugs/reviews/2026-06-11-security-mode-property-design-review.md \
  -g'*.java' -g'*.xml' -g'*.md'
rg -n "\"legacy\"" test/unit test/functional coherence-* --glob '*.java' --glob '*.xml'
rg -n "security-mode=compatibility|mode=legacy|LEGACY mode|legacy mode|legacy compatibility|LEGACY" \
  coherence-* test/unit test/functional --glob '*.java' --glob '*.xml'
rg -n "com\.tangosol\.util\.CoherenceMode|CoherenceMode\.isSecurityHardeningEnabled\(|CoherenceMode\.isCoherenceRestAuthEnforced\(|CoherenceMode\.isCoherenceRestPassThroughAllowlistRequired\(" \
  coherence-rest coherence-core test/unit test/functional coherence-* --glob '*.java'
```

Observed results:

- Branch was `codex/coherence-mode-security-opt-in`.
- Head was
  `88e1c8f93e6d664b994e8491edc2db3dc72fe34a P4 //dev/main baseline at change 120899`.
- `git diff --check` produced no output.
- The exact removed API/property scan found no product or test hits for
  `coherence.security.hardened`, `PROP_SECURITY_HARDENED`,
  `restoreSecurityHardened`, `CoherenceMode.LEGACY`, `isLegacy()`,
  `coherence.mode=legacy`, `mode("legacy")`, or `restore("legacy")`.
  Remaining old-property hits were historical design/review text.
- The broad `"legacy"` Java/XML scan found only
  `CoherenceModeTest.assertInvalidMode("legacy")` plus unrelated legacy
  concepts such as `LegacyXmlPartitionedServiceHelper` and serialization
  format comments.
- The broader active-test language scan found the P3 stale LEGACY wording
  listed above.

No Maven or functional tests were run in this review because the parent
already ran the focused verification after the fixes and this pass was limited
to plan-conformance review.

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

Task:
Prepare the security hardening opt-in branch for final validation without
changing the approved implementation shape.

Do:
- Keep `coherence.mode` restored to released runtime/license-mode behavior.
- Keep `coherence.security.mode` as the only global hardening property, with
  supported values unset / `compatibility` and `hardened`.
- Do not make `coherence.security.hardened` an alias.
- Preserve explicit feature override behavior, especially senior metadata proof
  `required=true` and TLS hostname verification.
- Clean non-blocking stale active-test wording that still describes
  compatibility posture as LEGACY mode, renaming methods/helpers/comments to
  compatibility-mode language.
- Optionally add a focused resolver assertion that setting only
  `coherence.security.hardened=true` does not enable hardening.
- Re-run the focused scans and any tests affected by the wording/test cleanup.

Do not:
- Reintroduce `CoherenceMode.LEGACY`, `isLegacy()`, parser aliases for
  `legacy`, `coherence.mode=legacy`, or a boolean hardening property.
- Change product hardening gates unless a new failing test proves a product
  bug.
- Fetch, push, submit P4, enqueue Remote Queue, stage, commit, touch `main`,
  or revert unrelated user/agent changes unless explicitly asked.
- Run security-manager functional tests on JDK 25.

Suggested verification:
```bash
source ../bin/cfglocal.sh
REVISION=15.1.2-0-0-security-opt-in-SNAPSHOT
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" \
  coherence-* test/unit test/functional -g'*.java' -g'*.xml'
rg -n "\"legacy\"" test/unit test/functional coherence-* --glob '*.java' --glob '*.xml'
mvn -Pmodules,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl test/unit/coherence-tests,test/unit/coherence-core-tests \
  -Dtest=CoherenceModeTest,PassThroughRootResourceTest,SecurityFilterTest,JsonClassMetadataPolicyTest,MapConverterTest,SerializationTelemetryTest,RemoteExecutablePolicyTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Report the files changed, scan results, tests run, and any remaining LEGACY
hits with rationale. If you only perform review/validation and make no source
or test edits, say that directly.
~~~
