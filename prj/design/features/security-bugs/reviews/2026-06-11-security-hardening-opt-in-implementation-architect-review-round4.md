<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in implementation architect review round 4

- **Date:** 2026-06-11
- **Role:** Architect
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
  (`P4 //dev/main baseline at change 120899`)
- **Working tree reviewed:** current uncommitted local git working tree after
  the round-3 architect and security findings were addressed, plus untracked
  compatibility plan and prior review documents. This review file was not
  present when the review began.
- **Plan reviewed:**
  `design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
- **Verdict:** Accept

## Prior reviews

- `design/features/security-bugs/reviews/2026-06-11-security-mode-property-design-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round2.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round2.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round3.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round3.md`

## Files in scope

- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
- `coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java`
- `coherence-testing-support/src/main/java/com/oracle/coherence/testing/util/CoherenceModeHelper.java`
- `coherence-core/src/main/resources/tangosol-coherence.xml`
- Current implementation diff touching hardening gates in Coherence Core,
  REST, JSON, gRPC, RAG, Concurrent, Extend, TCMP, Reporter, Management, SSL,
  federation, and related unit / functional tests
- `docs/core/04_serialization_hardening.adoc`
- `test/functional/tcmp/src/main/java/tcmp/LicenseModeCompatibilityTests.java`
- Untracked replacement test
  `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CompatibilityShadowExecutablePolicyTest.java`
- Verification and RQ readiness notes recorded in
  `.ai/branches/codex/coherence-mode-security-opt-in/journal.md`

## Context

The compatibility plan requires restoring released `coherence.mode` behavior
and moving compatibility-sensitive July security hardening behind
`coherence.security.mode=compatibility|hardened`. The required runtime-mode
contract is: unset `coherence.mode` defaults to `dev`; released values
including `eval`, `dev`, `development`, `prod`, and `production` are accepted;
blank, invalid, `legacy`, and `legacy-compatibility` are rejected; and the
unshipped `LEGACY` / `isLegacy()` surface is removed.

Round 3 found four follow-ups:

1. SER-01 customer documentation still described the removed single-axis
   `LEGACY` / `coherence.mode=prod` hardening contract.
2. Reporter and Management helpers still used `isLegacyShadowReason(...)`
   names even though behavior had moved to compatibility security mode.
3. The central resolver tests lacked an explicit assertion that the old
   unshipped `coherence.security.hardened` property is not an alias.
4. `LicenseModeCompatibilityTests` asserted unsupported mixed `dev` / `prod`
   rolling cluster membership.

This pass reviewed the local git working tree only. It did not fetch, push,
submit, shelve, inspect P4 state, modify P4 `//dev/main`, or interact with the
reported Remote Queue job. The branch journal says P4 shelf CL `120916` and
non-auto-submit RQ job `job.9.20260611214457.28597` exist after local
`enqueue -z` passed; this review treats those as external readiness notes and
does not independently validate them.

## Findings by severity

No blocking, major, or minor architect findings were found in this round.

The round-3 architect and security findings appear addressed:

- `docs/core/04_serialization_hardening.adoc:23` now describes the two
  independent axes, and `docs/core/04_serialization_hardening.adoc:104`
  documents restored runtime-mode values, unset default `dev`, rejection of
  blank / invalid / legacy mode values, and `coherence.security.mode` as the
  hardening opt-in.
- `docs/core/04_serialization_hardening.adoc:688` now lists
  `coherence.mode` as runtime/license mode and `coherence.security.mode` as
  the SER-01 hardening posture; the only old-property references are the
  intentional non-alias statements.
- `coherence-core/src/main/java/com/tangosol/coherence/reporter/ReporterSecurity.java:728`
  and
  `coherence-core/src/main/java/com/tangosol/net/management/ManagementInvocationPolicy.java:477`
  now call `isCompatibilityShadowReason(...)`; the helper declarations at
  `ReporterSecurity.java:759` and `ManagementInvocationPolicy.java:510`
  use compatibility-mode terminology.
- `test/unit/coherence-tests/src/test/java/com/tangosol/internal/util/CoherenceModeTest.java:207`
  asserts that setting only `coherence.security.hardened=true` does not enable
  hardening and is not an alias for the new property.
- `test/functional/tcmp/src/main/java/tcmp/LicenseModeCompatibilityTests.java:39`
  now tests same-mode `dev -> dev` and `prod -> prod` rolling compatibility
  with unset security mode instead of unsupported mixed `dev` / `prod`
  membership.

## What looks good

- The central resolver at
  `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java:161`
  restores released runtime-mode parsing and defaulting while keeping
  security-mode parsing separate at `CoherenceMode.java:184`.
- `coherence-core/src/main/resources/tangosol-coherence.xml` restores the
  operational `license-mode` default from `prod` to `dev`.
- The public REST-facing facade no longer exposes `current()`, `isDev()`, or
  `isProd()` hardening helpers; it exposes only security-hardening predicates
  needed by REST.
- The testing helper now uses `securityCompatibility()`, `securityHardened()`,
  `securityMode(String)`, and `restoreSecurityMode(String)` names instead of
  the removed boolean hardening helper surface.
- Reviewed compatibility-sensitive gates use
  `isSecurityHardeningEnabled()` or named predicates that delegate to it,
  rather than treating `coherence.mode=prod` as a hardening gate.
- Remaining direct `CoherenceMode.current()` uses reviewed in this pass are
  runtime-mode telemetry, compatibility log context, or operational default
  resolution. They are not new fail-closed hardening gates keyed solely from
  `prod`.
- Explicit safe feature settings still fail closed at the checked high-risk
  boundaries: dynamic remote `deny`, topic replay `reject`, senior metadata
  proof `required=true`, and TLS explicit `allow` under hardened mode.
- Static scans found no active Java/XML/property/docs references to the
  removed `CoherenceMode.LEGACY`, `isLegacy()`, active
  `coherence.mode=legacy`, `mode("legacy")`, `restore("legacy")`,
  `LegacyShadow`, or `isLegacyShadowReason` surfaces. The remaining
  `coherence.security.hardened` references are the intentional non-alias
  documentation and central resolver test constant.
- `git diff --check` produced no output.

## Required follow-up work

No product, test, or documentation fixes are required by this architect review.

Before treating the shelf as ready for final submit, continue the normal
validation flow already in progress:

1. Monitor the reported non-auto-submit RQ job and investigate any stage
   failure from the logs.
2. Keep the local review and RQ status distinct from project closure; do not
   mark P4/RQ/backport state closed until a human supplies the submitted P4 CL.
3. If additional product changes are made after this review, rerun the focused
   resolver/docs/stale-reference checks and request another review pass.

Residual risk: this review did not rerun Maven, functional, security-manager,
or Remote Queue verification. It relied on source/diff review, static scans,
and the verification evidence recorded in the branch journal, including the
post-round-3 `CoherenceModeTest`, `LicenseModeCompatibilityTests`, build,
`git diff --check`, stale-reference scans, and local `enqueue -z` result.

## Out of scope

- No product source, tests, XML, customer documentation, P4 state, commits,
  submits, shelves, backports, branches, remotes, or RQ jobs were modified
  during this review.
- This pass did not inspect P4 shelf CL `120916`, did not validate RQ job
  `job.9.20260611214457.28597`, and did not touch the P4 `//dev/main`
  workspace.
- Deep threat-model validation remains covered by the security review series.
  This pass focused on architecture, compatibility, docs, test contract, and
  plan conformance.

## Verification evidence

Review-only commands run:

```bash
source ../bin/cfglocal.sh
git branch --show-current
git log -1 --format='%H %s'
git status --short
git diff --stat
git diff --name-status
git diff --check
git ls-files --others --exclude-standard
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" coherence-* test/unit test/functional docs --glob '*.java' --glob '*.xml' --glob '*.properties' --glob '*.adoc' --glob '*.md'
rg -n "LEGACY mode|legacy mode|legacy compatibility|LegacyShadow|isLegacyShadowReason|mode=legacy" coherence-* test/unit test/functional docs --glob '*.java' --glob '*.xml' --glob '*.properties' --glob '*.adoc' --glob '*.md'
rg -n "CoherenceMode\.(isProd|isDev|current)\(|com\.tangosol\.util\.CoherenceMode\.(isProd|isDev|current)\(" coherence-core-components/src/main/java coherence-core/src/main/java coherence-grpc/src/main/java coherence-json/src/main/java coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java --glob '*.java'
rg -n "isSecurityHardeningEnabled\(|isAllowlistEnforced\(|isDynamicRemoteDefaultDeny\(|isRemoteExecutableEnforced\(|isCoherenceRestAuthEnforced\(|isCoherenceRestPassThroughAllowlistRequired\(|isXmlExternalEntityProtectionRequired\(" coherence-core-components/src/main/java coherence-core/src/main/java coherence-grpc/src/main/java coherence-json/src/main/java coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java --glob '*.java'
rg -n "coherence\.mode=prod.*hardening|hardening.*coherence\.mode=prod|coherence\.mode.*legacy|LEGACY.*coherence\.mode|coherence\.mode=legacy|legacy-compatibility|coherence\.security\.hardened" docs coherence-distribution coherence-core/src/main/resources coherence-rest/src/main/resources coherence-grpc/src/main/resources coherence-json/src/main/resources --glob '*.md' --glob '*.adoc' --glob '*.xml' --glob '*.properties' --glob '*.txt'
rg -n "coherence\.security\.mode.*(true|false|on|off|enabled|disabled|yes|no)|coherence\.security\.mode=(true|false|on|off|enabled|disabled|yes|no)|security\.mode=.*compat\b|security\.mode=.*prod\b|security\.mode=.*secure\b|security\.mode=.*strict\b" docs coherence-* test/unit test/functional --glob '*.java' --glob '*.xml' --glob '*.properties' --glob '*.adoc' --glob '*.md'
```

Observed results:

- Branch was `codex/coherence-mode-security-opt-in`.
- Head was
  `88e1c8f93e6d664b994e8491edc2db3dc72fe34a P4 //dev/main baseline at change 120899`.
- `git status --short` showed the broad expected uncommitted implementation
  diff plus untracked branch/design/review docs and the replacement
  `CompatibilityShadowExecutablePolicyTest.java`.
- `git diff --check` produced no output.
- The exact removed API/property scan found only:
  - `docs/core/04_serialization_hardening.adoc:123`
  - `docs/core/04_serialization_hardening.adoc:710`
  - `test/unit/coherence-tests/src/test/java/com/tangosol/internal/util/CoherenceModeTest.java:317`
- The broader legacy wording scan found no active matches for the removed
  legacy-mode surface in the scanned source/test/docs scopes.
- Product mode scans did not reveal a renewed hardening gate keyed directly to
  `coherence.mode=prod` in the reviewed source trees.

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
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round4.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round3.md`

Task:
Continue final validation and handoff for the security-hardening opt-in
compatibility work. Round-4 architect review accepted the local git working
tree with no required product/test/docs fixes.

Do:
- Keep reviewing the local feature working tree only unless the human
  explicitly changes scope.
- Monitor or summarize the reported non-auto-submit Remote Queue job if asked,
  but do not treat RQ success as a project closure signal by itself.
- If new product/test/docs changes are made after round 4, rerun the focused
  resolver/docs/stale-reference checks and request another architect/security
  review as appropriate.
- Preserve the two-axis contract: `coherence.mode` is runtime/license mode;
  `coherence.security.mode` is the only global hardening opt-in, with unset /
  `compatibility` and `hardened` as the supported values.
- Preserve explicit fail-closed feature semantics such as dynamic remote
  `deny`, topic replay `reject`, senior metadata proof `required=true`, and
  TLS explicit `allow` rejection under hardened mode.

Do not:
- Do not fetch, push, submit, shelve, stage, commit, or touch the P4
  `//dev/main` workspace unless the human explicitly asks.
- Do not implement product, test, or documentation fixes unless a new review
  finding or human instruction requires them.
- Do not reintroduce `CoherenceMode.LEGACY`, `isLegacy()`,
  `coherence.mode=legacy`, `legacy-compatibility`, boolean-style
  `coherence.security.mode` aliases, or the old
  `coherence.security.hardened` property as an alias.
- Do not re-couple July hardening gates to `coherence.mode=prod`.

Suggested verification if the working tree changes again:
```bash
source ../bin/cfglocal.sh
REVISION=15.1.2-0-0-security-opt-in-SNAPSHOT
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)|LegacyShadow|isLegacyShadowReason" \
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
mvn -Pmodules,modular-tests,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl test/functional/tcmp \
  -Dit.test=LicenseModeCompatibilityTests \
  -Dcoherence.cluster=license-mode-compat-$(date +%Y%m%d%H%M%S)-codex \
  verify
```

Report the current branch/head/status, any RQ status if requested, and whether
the working tree still matches the round-4 accepted state. If an RQ stage
fails, investigate that stage's logs and recommend the narrowest rerun; do not
rerun or auto-submit without explicit human instruction.
~~~
