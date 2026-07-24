<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in implementation security review round 4

- **Date:** 2026-06-11
- **Role:** Security Reviewer
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
  (`P4 //dev/main baseline at change 120899`)
- **Working tree reviewed:** current uncommitted implementation after round-3
  architect and security fixes, plus untracked compatibility plan and review
  documents
- **Review artifact:** `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round4.md`
- **Verdict:** accept with non-blocking follow-up; no product security-gate
  bypass or explicit fail-closed override regression found in this pass

## Prior reviews consulted

- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round3.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round3.md`

Earlier review context was also available through the branch journal and the
compatibility plan.

## Files in scope

The current local `git diff --name-status` implementation set was in scope.
The round-4 security pass focused on:

- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
- `coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java`
- `coherence-testing-support/src/main/java/com/oracle/coherence/testing/util/CoherenceModeHelper.java`
- `coherence-core-components/src/main/java/com/tangosol/coherence/component/util/daemon/queueProcessor/service/Grid.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/security/RemoteExecutionMode.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/security/RemoteInstallGate.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/security/TopicsPersistedPolicyDrift.java`
- `coherence-core/src/main/java/com/oracle/coherence/common/net/SSLSocketProvider.java`
- `coherence-core/src/main/java/com/tangosol/coherence/config/builder/SSLSocketProviderDependenciesBuilder.java`
- `coherence-core/src/main/java/com/tangosol/net/internal/NameServiceValuePolicy.java`
- `coherence-core/src/main/java/com/tangosol/internal/federation/service/ConnectRequestHandler.java`
- `coherence-core/src/main/java/com/tangosol/internal/net/management/HttpAuthDefaults.java`
- `coherence-core/src/main/java/com/tangosol/coherence/config/xml/preprocessor/ConcurrentProxyPreprocessor.java`
- `coherence-core-components/src/main/java/com/tangosol/coherence/component/net/extend/proxy/serviceProxy/InvocationServiceProxy.java`
- `coherence-core/src/main/java/com/tangosol/io/internal/SerializationTelemetry.java`
- REST, JSON, gRPC, RAG, Reporter, Management, TCMP, Extend, Concurrent,
  Topics, SSL, and resolver unit / functional tests touched by the working tree
- `docs/core/04_serialization_hardening.adoc`
- `test/functional/tcmp/src/main/java/tcmp/LicenseModeCompatibilityTests.java`

## Context

The compatibility plan requires separating runtime mode from compatibility-
sensitive July hardening. `coherence.mode` must retain released behavior, while
`coherence.security.mode=hardened` is the explicit opt-in for hardening gates.
Default and explicit compatibility security mode must preserve customer and
rolling-upgrade compatibility. Explicit feature-level safe settings, such as
`required=true`, `deny`, and `reject`, must continue to fail closed.

Prior security rounds found fail-open override regressions at senior metadata
proof, dynamic remote install, and topic replay. Round 3 found a TCMP test
contract issue that asserted mixed `dev` / `prod` license-mode joins even
though product membership still rejects mismatched license modes. The branch
journal records that those fixes were applied, and this pass rechecked the
source shape.

## Findings by severity

### P3: Active REST compatibility helpers and tests still use removed legacy-mode terminology

No runtime `coherence.mode=legacy` alias remains, and this is not a security
gate bypass. However, active REST code still names compatibility-mode behavior
as "legacy":

- `coherence-rest/src/main/java/com/tangosol/coherence/rest/RestExpressionPolicy.java:56`,
  `62`, `105`, `129`, and `149` call `isLegacyExpressionAllowed()`, and the
  helper is declared at line `181`.
- `coherence-rest/src/test/java/com/tangosol/coherence/rest/CacheResourceTest.java:364`,
  `488`, and `636` still use `Legacy` in test names while exercising
  `CoherenceModeHelper.securityCompatibility()`.
- `coherence-rest/src/test/java/com/tangosol/coherence/rest/RestExpressionPolicyTest.java:73`
  and
  `coherence-rest/src/test/java/com/tangosol/coherence/rest/query/QueryEngineTest.java:93`
  and `108` have the same test-name residue.

The emitted REST warnings already say compatibility mode and point to
`coherence.security.mode=hardened`, so this should not mislead operators. The
risk is reviewer / maintainer confusion after the removed public `LEGACY`
mode surface. Follow-up should rename the helper to compatibility terminology
and update the test names without changing behavior.

## What looks good

- The central resolver now exposes only `EVAL`, `DEV`, and `PROD` runtime
  modes, defaults unset `coherence.mode` to `DEV`, accepts released aliases
  including `development` and `production`, and rejects blank, unknown,
  `legacy`, and `legacy-compatibility` values.
- `coherence.security.mode` is centralized, defaults unset to compatibility,
  accepts only `compatibility` and `hardened`, trims and compares
  case-insensitively, and rejects blank, shorthand, unknown, and boolean-style
  values.
- The old unshipped `coherence.security.hardened=true` property is covered by
  a resolver test proving it is not an alias and does not enable hardening.
- The prior TCMP round-3 finding is fixed: `LicenseModeCompatibilityTests`
  now covers same-mode `dev -> dev` and `prod -> prod` rolling restarts with
  security mode unset, matching the existing license-mode membership contract.
- The prior explicit-override regressions remain fixed in source:
  `RemoteInstallGate.enforceDynamicInstall(...)` rejects explicit dynamic
  remote `deny` before compatibility shadow telemetry, topic replay `reject`
  is checked before replay dedup / warn-allow can allow the payload, and
  senior metadata proof `required=true` participates in the effective
  fail-closed requirement independently from global hardening.
- Reviewed hardening gates for TLS hostname verification, federation
  direct-connect endpoint validation, management JMX publish URL validation,
  HTTP auth defaults, concurrent and invocation proxy defaults, gRPC
  diagnostics/auth/serializer behavior, JSON metadata, REST expression/query
  handling, RAG empty allowlist, XML parser protections, Reporter and
  Management policy shadowing, and remote executable / install gates are keyed
  from `isSecurityHardeningEnabled()` or a named predicate that delegates to
  it, rather than from `coherence.mode=prod`.
- Explicit feature settings checked in this pass still win: TLS explicit
  `allow` is rejected in hardened mode, explicit gRPC diagnostics values win
  over `auto`, explicit HTTP auth values win over the mode default, and
  explicit proxy enable / disable properties win over the security-mode
  default.
- Customer-facing SER-01 documentation now describes the two-axis contract:
  runtime `coherence.mode` is not the hardening opt-in, and
  `coherence.security.mode=hardened` enables compatibility-sensitive hardening
  after migration.
- `git diff --check` produced no output.

## Required follow-up work

1. Non-blocking cleanup: rename `RestExpressionPolicy.isLegacyExpressionAllowed()`
   to compatibility terminology, for example
   `isCompatibilityExpressionAllowed()`, and update the stale REST test method
   names listed above. Do not change runtime behavior while doing this.
2. Keep the current explicit override regression coverage for senior metadata
   proof `required=true`, dynamic remote `deny`, and topic replay `reject`.
3. Before final handoff, re-run `git diff --check`, stale-reference scans, and
   the affected REST unit tests if the naming cleanup is applied.

## Out of scope

- No product source, tests, XML, docs, P4 state, commits, submits, shelves,
  branches, remotes, or `//dev/main` workspace state were modified during this
  review.
- No Maven, functional, security-manager, or Remote Queue tests were run in
  this pass. This was a source/diff review plus static scans, relying on the
  focused verification evidence recorded in the branch journal.
- The P4 shelf CL `120916` and non-auto-submit RQ job
  `job.9.20260611214457.28597` were treated as context only. The review
  focused on the local git working tree.
- Broad unrelated `LegacyXml...`, metrics legacy-name, transaction legacy, and
  protocol-version legacy references were not treated as removed
  `coherence.mode=legacy` residue.

## Verification evidence

Review commands run:

```bash
source ../bin/cfglocal.sh
git branch --show-current
git log -1 --format='%H %s'
git status --short
git diff --name-status
git diff --stat
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" coherence-* test/unit test/functional docs --glob '*.java' --glob '*.xml' --glob '*.properties' --glob '*.adoc' --glob '*.md'
rg -n "LEGACY mode|legacy mode|legacy compatibility|LegacyShadow|isLegacyShadowReason|mode=legacy" coherence-* test/unit test/functional docs --glob '*.java' --glob '*.xml' --glob '*.properties' --glob '*.adoc' --glob '*.md'
rg -n "CoherenceMode\.(isProd|isDev|current)\(|com\.tangosol\.util\.CoherenceMode\.(isProd|isDev|current)\(" coherence-core-components/src/main/java coherence-core/src/main/java coherence-grpc/src/main/java coherence-json/src/main/java coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java --glob '*.java'
rg -n "isSecurityHardeningEnabled\(|isAllowlistEnforced\(|isDynamicRemoteDefaultDeny\(|isRemoteExecutableEnforced\(|isCoherenceRestAuthEnforced\(|isCoherenceRestPassThroughAllowlistRequired\(|isXmlExternalEntityProtectionRequired\(" coherence-core-components/src/main/java coherence-core/src/main/java coherence-grpc/src/main/java coherence-json/src/main/java coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java --glob '*.java'
rg -n "coherence\.mode=prod.*hard|hard.*coherence\.mode=prod|coherence\.mode.*LEGACY|LEGACY.*coherence\.mode|coherence\.mode=legacy|legacy-compatibility|security\.hardened" docs coherence-core/src/main/resources coherence-rest/src/main/resources coherence-grpc/src/main/resources coherence-json/src/main/resources --glob '*.adoc' --glob '*.md' --glob '*.xml' --glob '*.properties' --glob '*.txt'
rg -n "Legacy|legacy" coherence-rest/src/main/java/com/tangosol/coherence/rest coherence-rest/src/test/java/com/tangosol/coherence/rest --glob '*.java'
```

Observed results:

- Branch was `codex/coherence-mode-security-opt-in`.
- Head was
  `88e1c8f93e6d664b994e8491edc2db3dc72fe34a P4 //dev/main baseline at change 120899`.
- `git diff --check` produced no output.
- The exact removed API/property scan found only intentional
  `coherence.security.hardened` non-alias documentation and the
  `CoherenceModeTest` old-property constant.
- The removed legacy-mode scan found no active `CoherenceMode.LEGACY`,
  `isLegacy()`, `coherence.mode=legacy`, `mode("legacy")`, or
  `restore("legacy")` references.
- Documentation scans did not find customer-facing guidance that
  `coherence.mode=prod` alone enables the July hardening gates.
- The remaining relevant active REST `Legacy` hits are the P3 naming residue
  listed above.

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
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round3.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round3.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round4.md`

Task:
Apply the non-blocking REST compatibility terminology cleanup from the round-4
security review without changing product behavior.

Do:
- Rename `RestExpressionPolicy.isLegacyExpressionAllowed()` to compatibility
  terminology, for example `isCompatibilityExpressionAllowed()`.
- Update the stale REST test method names that currently use `Legacy` while
  exercising `CoherenceModeHelper.securityCompatibility()`:
  `CacheResourceTest`, `RestExpressionPolicyTest`, and `QueryEngineTest`.
- Keep emitted product warnings in compatibility-mode language and keep the
  existing behavior unchanged.
- Run focused REST unit tests that cover the renamed code, plus the standard
  scans below.

Do not:
- Do not implement product security-gate behavior changes.
- Do not reintroduce `CoherenceMode.LEGACY`, `isLegacy()`,
  `coherence.mode=legacy`, `legacy-compatibility` aliases, or
  `coherence.security.hardened`.
- Do not recouple hardening gates to `coherence.mode=prod`.
- Do not fetch, push, submit, shelve, stage, commit, enqueue Remote Queue,
  touch P4 `//dev/main`, or revert unrelated user/agent changes unless a human
  explicitly asks.

Suggested verification:
```bash
source ../bin/cfglocal.sh
REVISION=15.1.2-0-0-security-opt-in-SNAPSHOT
mvn -Pmodules,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl coherence-rest \
  -Dtest=CacheResourceTest,RestExpressionPolicyTest,QueryEngineTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" \
  coherence-* test/unit test/functional docs -g'*.java' -g'*.xml' -g'*.properties' -g'*.adoc' -g'*.md'
rg -n "LEGACY mode|legacy mode|legacy compatibility|LegacyShadow|isLegacyShadowReason|mode=legacy|isLegacyExpressionAllowed" \
  coherence-* test/unit test/functional docs -g'*.java' -g'*.xml' -g'*.properties' -g'*.adoc' -g'*.md'
```

Report the rename, focused REST test results, scan results, and any remaining
intentional `LegacyXml...` / unrelated legacy references separately from the
removed `coherence.mode=legacy` surface.
~~~
