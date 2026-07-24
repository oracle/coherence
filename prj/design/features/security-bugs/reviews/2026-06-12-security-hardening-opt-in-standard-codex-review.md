<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in standard Codex review

- **Date:** 2026-06-12
- **Role:** Standard Codex code review
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
  (`P4 //dev/main baseline at change 120899`)
- **Working tree reviewed:** current uncommitted local git working tree after
  architect/security review rounds through round 4 and the follow-up REST
  terminology cleanup. This review file was not present when the review began.
- **Verdict:** Accept; no blocking, major, or minor standard-review findings
  were found.

## Prior reviews

- `design/features/security-bugs/reviews/2026-06-11-security-mode-property-design-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round2.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round2.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round3.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round3.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round4.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round4.md`

## Files in scope

- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
- `coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java`
- `coherence-testing-support/src/main/java/com/oracle/coherence/testing/util/CoherenceModeHelper.java`
- Representative hardening gates in Coherence Core, REST, JSON, gRPC, RAG,
  Concurrent, Extend, TCMP, Reporter, Management, SSL, federation, and related
  unit / functional tests touched by the current working tree
- `docs/core/04_serialization_hardening.adoc`
- Round-4 REST cleanup files:
  `coherence-rest/src/main/java/com/tangosol/coherence/rest/RestExpressionPolicy.java`,
  `coherence-rest/src/test/java/com/tangosol/coherence/rest/CacheResourceTest.java`,
  `coherence-rest/src/test/java/com/tangosol/coherence/rest/RestExpressionPolicyTest.java`,
  and
  `coherence-rest/src/test/java/com/tangosol/coherence/rest/query/QueryEngineTest.java`
- `test/functional/tcmp/src/main/java/tcmp/LicenseModeCompatibilityTests.java`
- Untracked replacement test
  `test/unit/coherence-core-tests/src/test/java/com/tangosol/util/CompatibilityShadowExecutablePolicyTest.java`

## Context

This was a standard Codex code review of the local git working tree only. The
goal was to look for bugs, behavioral regressions, missing tests, accidental
API or compatibility changes, stale docs/tests, build/test risks, and
maintainability issues in the security-hardening opt-in compatibility
implementation.

The expected product contract is the two-axis compatibility model:
`coherence.mode` remains the released runtime/license mode, with unset
defaulting to `dev`, released aliases including `development` and `production`,
and no unreleased `LEGACY` / `isLegacy()` surface. The new
`coherence.security.mode` property controls the compatibility-sensitive July
hardening gates, with only unset / `compatibility` and `hardened` supported.
Blank, unknown, shorthand, and boolean-style security-mode values fail fast,
and the old unshipped `coherence.security.hardened` property is not an alias.

Architect round 4 accepted the implementation with no findings. Security round
4 accepted with one non-blocking REST terminology cleanup; the branch journal
records that cleanup as applied and verified before this review.

## Findings by severity

No findings.

I did not find a material bug, behavioral regression, missing focused test,
accidental API/compatibility issue, stale docs/tests issue, or build/test risk
that should block this implementation. The only active REST `legacy` wording
found in product code during this pass is the existing
`PassThroughRootResource` pass-through auto-publish deprecation warning, not
the removed `coherence.mode=legacy` surface or the round-4
`RestExpressionPolicy` helper/test residue.

## What looks good

- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
  now separates runtime mode from security hardening. `current()` resolves
  released runtime values, while `isSecurityHardeningEnabled()` resolves only
  `coherence.security.mode`.
- Central gate predicates such as `isAllowlistEnforced()`,
  `isDynamicRemoteDefaultDeny()`, `isRemoteExecutableEnforced()`,
  `isCoherenceRestAuthEnforced()`,
  `isCoherenceRestPassThroughAllowlistRequired()`, and
  `isXmlExternalEntityProtectionRequired()` delegate to the security-mode
  hardening predicate instead of treating `coherence.mode=prod` as hardening.
- `coherence-core/src/main/resources/tangosol-coherence.xml` restores the
  operational `license-mode` default to `dev`.
- The central resolver tests cover unset mode defaulting, released aliases,
  rejection of `legacy` / `legacy-compatibility`, rejection of blank and
  malformed runtime modes, accepted security modes, rejected boolean/shorthand
  security-mode values, memoization, and the non-alias behavior of
  `coherence.security.hardened`.
- Representative explicit fail-closed feature overrides remain intact in the
  reviewed code: dynamic remote `deny`, topic replay `reject`, senior metadata
  proof `required=true`, and TLS explicit `allow` rejection under hardened
  mode.
- The round-4 REST cleanup changed
  `RestExpressionPolicy.isLegacyExpressionAllowed()` to
  `isCompatibilityExpressionAllowed()` and updated the listed stale REST test
  names without changing the intended runtime behavior.
- Customer documentation now describes the two-axis `coherence.mode` /
  `coherence.security.mode` model and no longer says that
  `coherence.mode=prod` alone enables the July hardening gates.

## Required follow-up work

No product, test, or documentation follow-up is required by this standard
Codex review.

Residual risk remains because this pass intentionally did not run Maven,
functional tests, security-manager tests, Remote Queue, P4 commands, or shelf
validation. It relied on source/diff review, static scans, and the verification
evidence already recorded in the branch journal, including focused unit /
functional checks, JDK 21 security functional verification, REST cleanup tests,
`git diff --check`, stale-reference scans, and local `enqueue -z -c 120916`.

## Out of scope

- No product source, tests, XML, XSD, customer documentation, P4 state,
  commits, submits, shelves, branches, remotes, staging area, or Remote Queue
  jobs were modified or queried during this review.
- This pass did not inspect P4 shelf CL `120916`, did not interact with any
  Remote Queue job, and did not run long Maven suites.
- Deep architecture and threat-model judgment remains covered by the prior
  architect and security review series. This pass focused on standard
  code-review risk in the local working tree.

## Verification evidence

Review-only commands run:

```bash
git branch --show-current
git log -1 --format='%H %s'
git status --short
git diff --stat
sed -n '1,260p' AGENTS.md
sed -n '1,260p' .ai/dev-environment.md
sed -n '1,700p' .ai/branches/codex/coherence-mode-security-opt-in/journal.md
sed -n '1,760p' design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md
sed -n '1,320p' design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round4.md
sed -n '1,320p' design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round4.md
git diff --check
git diff --name-status
git diff -- coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java coherence-testing-support/src/main/java/com/oracle/coherence/testing/util/CoherenceModeHelper.java
git diff -- coherence-rest/src/main/java/com/tangosol/coherence/rest/RestExpressionPolicy.java coherence-rest/src/test/java/com/tangosol/coherence/rest/CacheResourceTest.java coherence-rest/src/test/java/com/tangosol/coherence/rest/RestExpressionPolicyTest.java coherence-rest/src/test/java/com/tangosol/coherence/rest/query/QueryEngineTest.java
git diff -- coherence-core-components/src/main/java/com/tangosol/coherence/component/net/Member.java coherence-core-components/src/main/java/com/tangosol/coherence/component/util/daemon/queueProcessor/service/ClusterService.java coherence-core-components/src/main/java/com/tangosol/coherence/component/util/daemon/queueProcessor/service/Grid.java coherence-core/src/main/resources/tangosol-coherence.xml test/functional/tcmp/src/main/java/tcmp/LicenseModeCompatibilityTests.java
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)|LegacyShadow|isLegacyShadowReason|isLegacyExpressionAllowed" coherence-* test/unit test/functional docs -g'*.java' -g'*.xml' -g'*.properties' -g'*.adoc' -g'*.md'
rg -n "CoherenceMode\.(isProd|isDev|current)\(|com\.tangosol\.util\.CoherenceMode\.(isProd|isDev|current)\(" coherence-core-components/src/main/java coherence-core/src/main/java coherence-grpc/src/main/java coherence-json/src/main/java coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java -g'*.java'
rg -n "isSecurityHardeningEnabled\(|isAllowlistEnforced\(|isDynamicRemoteDefaultDeny\(|isRemoteExecutableEnforced\(|isCoherenceRestAuthEnforced\(|isCoherenceRestPassThroughAllowlistRequired\(|isXmlExternalEntityProtectionRequired\(" coherence-core-components/src/main/java coherence-core/src/main/java coherence-grpc/src/main/java coherence-json/src/main/java coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java -g'*.java'
rg -n "coherence\.mode=prod.*hard|hard.*coherence\.mode=prod|coherence\.mode.*LEGACY|LEGACY.*coherence\.mode|coherence\.mode=legacy|legacy-compatibility|coherence\.security\.hardened" docs coherence-distribution coherence-core/src/main/resources coherence-rest/src/main/resources coherence-grpc/src/main/resources coherence-json/src/main/resources -g'*.md' -g'*.adoc' -g'*.xml' -g'*.properties' -g'*.txt'
rg -n "coherence\.security\.mode.*(true|false|on|off|enabled|disabled|yes|no)|coherence\.security\.mode=(true|false|on|off|enabled|disabled|yes|no)|security\.mode=.*compat\b|security\.mode=.*prod\b|security\.mode=.*secure\b|security\.mode=.*strict\b" docs coherence-* test/unit test/functional -g'*.java' -g'*.xml' -g'*.properties' -g'*.adoc' -g'*.md'
rg -n "Legacy|legacy" coherence-rest/src/main/java/com/tangosol/coherence/rest coherence-rest/src/test/java/com/tangosol/coherence/rest -g'*.java'
```

Observed results:

- Branch was `codex/coherence-mode-security-opt-in`.
- Head was
  `88e1c8f93e6d664b994e8491edc2db3dc72fe34a P4 //dev/main baseline at change 120899`.
- `git status --short` showed the expected broad local uncommitted
  implementation diff, untracked branch/design/review documents, and untracked
  replacement `CompatibilityShadowExecutablePolicyTest.java`.
- `git diff --stat` reported 111 files changed, with 2018 insertions and 1489
  deletions.
- `git diff --check` produced no output.
- The exact removed API/property scan found only intentional non-alias
  references:
  `docs/core/04_serialization_hardening.adoc:123`,
  `docs/core/04_serialization_hardening.adoc:710`, and
  `test/unit/coherence-tests/src/test/java/com/tangosol/internal/util/CoherenceModeTest.java:317`.
- The product/source hardening predicate scan did not reveal a renewed
  hardening gate keyed directly to `coherence.mode=prod` in the reviewed
  source trees.
- The round-4 REST cleanup scan no longer found
  `isLegacyExpressionAllowed`, `LegacyShadow`, or `isLegacyShadowReason`.
  The only REST `legacy` match was the pre-existing pass-through
  auto-publish deprecation warning in
  `coherence-rest/src/main/java/com/tangosol/coherence/rest/PassThroughRootResource.java:59`.

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
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round4.md`
- `design/features/security-bugs/reviews/2026-06-12-security-hardening-opt-in-standard-codex-review.md`

Task:
Continue final validation or handoff for the security-hardening opt-in
compatibility implementation. Architect round 4 accepted with no findings,
security round 4 accepted with a REST terminology cleanup, that cleanup has
been applied, and the standard Codex review on 2026-06-12 found no required
product/test/docs follow-up.

Do:
- Keep the two-axis contract intact: `coherence.mode` is runtime/license mode;
  `coherence.security.mode` is the only global hardening opt-in.
- Preserve supported security mode values: unset / `compatibility` and
  `hardened`.
- Preserve fail-fast behavior for blank, unknown, shorthand, and boolean-style
  `coherence.security.mode` values.
- Preserve explicit fail-closed feature semantics such as dynamic remote
  `deny`, topic replay `reject`, senior metadata proof `required=true`, and
  TLS explicit `allow` rejection under hardened mode.
- If any local implementation files change after this review, rerun focused
  resolver/docs/stale-reference checks and relevant focused tests.

Do not:
- Do not fetch, push, submit, shelve, enqueue Remote Queue, touch P4, stage,
  commit, or revert unrelated user/agent changes unless the human explicitly
  asks.
- Do not reintroduce `CoherenceMode.LEGACY`, `isLegacy()`,
  `coherence.mode=legacy`, `legacy-compatibility`, boolean-style security-mode
  aliases, or `coherence.security.hardened` as an alias.
- Do not recouple July hardening gates to `coherence.mode=prod`.
- Do not treat the existing pass-through-resource "legacy" deprecation warning
  as evidence that the removed `coherence.mode=legacy` surface remains.

Suggested verification if product/test/docs files change again:
```bash
source ../bin/cfglocal.sh
REVISION=15.1.2-0-0-security-opt-in-SNAPSHOT
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)|LegacyShadow|isLegacyShadowReason|isLegacyExpressionAllowed" \
  coherence-* test/unit test/functional docs \
  -g'*.java' -g'*.xml' -g'*.properties' -g'*.adoc' -g'*.md'
rg -n "coherence\.mode=prod.*hard|hard.*coherence\.mode=prod|coherence\.mode.*LEGACY|LEGACY.*coherence\.mode" \
  docs coherence-core/src/main/resources \
  -g'*.adoc' -g'*.md' -g'*.xml' -g'*.properties'
mvn -Pmodules,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl test/unit/coherence-tests \
  -Dtest=CoherenceModeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -Pmodules,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl coherence-rest \
  -Dtest=CacheResourceTest,RestExpressionPolicyTest,QueryEngineTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Report branch/head/status, whether the working tree still matches the accepted
review state, and any test/RQ/P4 status only if explicitly requested.
~~~
