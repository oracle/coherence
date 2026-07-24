<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security hardening opt-in implementation security review round 3

- **Date:** 2026-06-11
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
  (`P4 //dev/main baseline at change 120899`)
- **Working tree reviewed:** current uncommitted implementation after the
  round-1 and round-2 architect/security review fixes, plus untracked
  compatibility plan and review documents
- **Review artifact:** `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round3.md`
- **Verdict:** not ready for final validation until the misleading TCMP
  license-mode rolling test is corrected; no new product fail-open security
  gate regression was found in this pass

## Prior reviews consulted

- `design/features/security-bugs/reviews/2026-06-11-security-mode-property-design-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-architect-review-round2.md`
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round2.md`

## Files in scope

The full current `git diff --name-status` implementation set was in scope.
The round-3 security review focused on the gates named in the compatibility
plan and in the user's review request:

- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
- `coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java`
- `coherence-testing-support/src/main/java/com/oracle/coherence/testing/util/CoherenceModeHelper.java`
- `coherence-core-components/src/main/java/com/tangosol/coherence/component/util/daemon/queueProcessor/service/Grid.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/security/RemoteExecutionMode.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/security/RemoteInstallGate.java`
- `coherence-core/src/main/java/com/tangosol/internal/util/security/TopicsPersistedPolicyDrift.java`
- TLS hostname verification, weak signature / subject binding, serialization
  limits, REST/query/expression/pass-through, JSON metadata, management/JMX,
  Reporter, gRPC diagnostics/auth/serializer, RAG allowlist, XML parser,
  invocation proxy, concurrent proxy, federation direct-connect, and
  representative unit/functional tests for those gates
- `test/functional/tcmp/src/main/java/tcmp/LicenseModeCompatibilityTests.java`
  because it is a touched compatibility test for the removed legacy
  license-mode path

## Context

The accepted plan keeps `coherence.mode` as a released runtime/license mode and
moves July compatibility-sensitive fail-closed behavior behind
`coherence.security.mode=hardened`. Default and explicit
`coherence.security.mode=compatibility` must preserve rolling-upgrade/customer
compatibility, but explicit feature properties such as `required=true`,
`deny`, and `reject` must still fail closed.

Round 1 found that explicit senior-metadata proof `required=true` was made
dependent on global hardening. Round 2 found the same fail-open shape at the
dynamic remote install and persisted topic replay boundaries. This pass
rechecked those fixes and scanned the other named hardening gates for the same
pattern.

## Findings by severity

### P2: `LicenseModeCompatibilityTests` asserts a dev/prod rolling join that the product still rejects

`test/functional/tcmp/src/main/java/tcmp/LicenseModeCompatibilityTests.java`
now describes itself as testing "mixed existing dev/prod license mode
clusters" and actively rolls `dev -> prod` and `prod -> dev` at lines 32 and
40-48. The rolling helper starts the replacement member with the second mode
and then expects a two-member cluster at lines 70-72.

The product code does not permit that join. `ClusterService.isLicenseModeCompatible(...)`
returns true only when the joining and existing numeric license modes are
equal at
`coherence-core-components/src/main/java/com/tangosol/coherence/component/util/daemon/queueProcessor/service/grid/ClusterService.java:5803`.
The membership path reads the joining and local modes at lines 10459-10460 and
rejects with `REJECT_LICENSE_TYPE` when `isLicenseModeCompatible(...)` is
false at lines 10508-10511. The `Member` property documentation still says
different modes must not be mixed in one cluster at
`coherence-core-components/src/main/java/com/tangosol/coherence/component/net/Member.java:2598`.

This is not a new product fail-open bug, but it is a blocking test-contract
problem for this branch. The compatibility plan removes the unreleased
`legacy` license-mode adaptation and restores released `coherence.mode`
behavior; it does not establish a new dev/prod license-mode mixing contract.
As written, this functional test will either fail during broader validation or
mislead reviewers into thinking the implementation intentionally supports a
mixed dev/prod rolling cluster.

Required fix: replace this test with released-mode patch compatibility that
matches the product contract, such as same-mode `dev -> dev` and `prod -> prod`
rolling restarts with `coherence.security.mode` unset / compatibility. If the
desired product behavior really is to allow `dev` and `prod` members to mix
during rolling restart, stop and get architect/security sign-off because that
would be a product behavior change beyond removing the unshipped legacy mode
surface.

## What looks good

- The prior senior-metadata proof finding is fixed: effective senior metadata
  proof requirement now includes explicit
  `coherence.security.peer.senior-metadata-proof.required=true` and hardened
  mode, and enforcement no longer adds a second global-hardening predicate.
- The round-2 explicit override findings are fixed in source:
  `RemoteInstallGate.enforceDynamicInstall(...)` checks
  `RemoteExecutionMode.isDynamicRemoteAllowed()` before compatibility shadow
  telemetry, and `RemoteInstallGate.enforceReplay(...)` rejects
  `TopicsPersistedPolicyDrift.VALUE_REJECT` before replay dedup or
  compatibility shadowing can allow the payload.
- The central `coherence.security.mode` parser keeps the intended two-value
  contract. Blank, unknown, shorthand, boolean-style values, and the old
  unshipped `coherence.security.hardened` property are not accepted as aliases.
- Reviewed gates for TLS hostname verification, weak signature / subject
  binding, serialization limits, REST/query/expression/pass-through, JSON
  metadata, management/Reporter, gRPC diagnostics/auth/serializer, RAG empty
  allowlist, XML parser protections, invocation/concurrent proxy defaults, and
  federation direct-connect validation now use the security-mode boundary for
  compatibility-sensitive defaults instead of deriving enforcement from
  `coherence.mode=prod`.
- Reviewed explicit safe settings still win in the checked examples: senior
  metadata proof `required=true`, dynamic remote `deny`, persisted topic
  replay `reject`, TLS explicit `allow` rejection under hardened mode, and
  explicit gRPC diagnostic policies.
- Active Java/XML/property scans did not find product or test references to
  the removed `CoherenceMode.LEGACY`, `isLegacy()`,
  `coherence.mode=legacy`, `coherence.security.hardened`, or
  `PROP_SECURITY_HARDENED` symbols. The remaining exact `"legacy"` hits are
  the expected invalid-mode assertion and unrelated historical/format names.
- `git diff --check` produced no output.

## Required follow-up work

1. Fix `LicenseModeCompatibilityTests` so it no longer asserts unsupported
   mixed `dev` / `prod` cluster membership.
2. Re-run the focused TCMP functional test or an equivalent compile/test check
   after the test is corrected.
3. Keep the existing explicit override regression coverage for senior metadata
   proof, dynamic remote install, and topic replay; those are the highest-risk
   fail-open regressions found in prior rounds.
4. Before final handoff, run the usual `git diff --check` and stale-reference
   scans again. A focused resolver assertion that
   `coherence.security.hardened=true` alone does not enable hardening remains
   useful but non-blocking.

## Out of scope

- No product source, tests, XML, P4 state, commits, submits, backports,
  branches, remotes, or `main` state were modified during this review.
- No Maven, functional, security-manager, or Remote Queue tests were run in
  this pass; this was a source/diff review plus static scans.
- Customer-facing release notes and documentation beyond the compatibility
  plan were not reviewed.
- Historical design/review documents that intentionally preserve old
  `legacy` or `coherence.security.hardened` working-design text were not
  treated as active product residue.

## Verification evidence

Review commands run:

```bash
source ../bin/cfglocal.sh
git branch --show-current
git log -1 --format='%H %s'
git status --short --branch
git diff --stat
git diff --name-status
git diff --check
rg -n "CoherenceMode\.(isProd|isDev|current)\(" \
  coherence-core-components/src/main/java coherence-core/src/main/java \
  coherence-grpc/src/main/java coherence-json/src/main/java \
  coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java \
  --glob '*.java'
rg -n "isSecurityHardeningEnabled\(|isAllowlistEnforced\(|isDynamicRemoteDefaultDeny\(|isRemoteExecutableEnforced\(|isCoherenceRestAuthEnforced\(|isCoherenceRestPassThroughAllowlistRequired\(|isXmlExternalEntityProtectionRequired\(" \
  coherence-core-components/src/main/java coherence-core/src/main/java \
  coherence-grpc/src/main/java coherence-json/src/main/java \
  coherence-rest/src/main/java coherence-rag-parent/coherence-rag/src/main/java \
  --glob '*.java'
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|CoherenceMode\.LEGACY|isLegacy\(|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" \
  coherence-* test/unit test/functional --glob '*.java' --glob '*.xml' --glob '*.properties'
rg -n "\"legacy\"|LEGACY mode|legacy mode|legacy compatibility|mode=legacy" \
  coherence-* test/unit test/functional --glob '*.java' --glob '*.xml' --glob '*.properties'
```

Observed results:

- Branch was `codex/coherence-mode-security-opt-in`.
- Head was `88e1c8f93e6d664b994e8491edc2db3dc72fe34a P4 //dev/main baseline at change 120899`.
- `git diff --check` produced no output.
- The exact removed API/property scan had no active Java/XML/property hits for
  the removed legacy runtime-mode surface or the old boolean hardening
  property.
- The broad `"legacy"` Java/XML/property scan found
  `CoherenceModeTest.assertInvalidMode("legacy")` plus unrelated legacy
  concepts such as `LegacyXmlPartitionedServiceHelper` and serialization
  format comments.

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
- `design/features/security-bugs/reviews/2026-06-11-security-hardening-opt-in-implementation-security-review-round3.md`

Task:
Fix the round-3 security review test-contract finding without changing the
approved product security-gate behavior.

Finding to fix:
- `test/functional/tcmp/src/main/java/tcmp/LicenseModeCompatibilityTests.java`
  now asserts rolling `dev -> prod` and `prod -> dev` cluster membership.
- `ClusterService.isLicenseModeCompatible(...)` still permits only equal
  license modes, and the membership path rejects mismatched modes with
  `REJECT_LICENSE_TYPE`.
- The compatibility plan removes the unshipped legacy license-mode adaptation
  and restores released `coherence.mode` behavior; it does not create a new
  mixed dev/prod cluster contract.

Do:
- Replace the misleading mixed dev/prod rolling assertions with released-mode
  patch compatibility coverage that matches the product contract, for example
  same-mode `dev -> dev` and `prod -> prod` rolling restarts with
  `coherence.security.mode` unset / compatibility.
- Keep `coherence.mode` restored to released runtime/license-mode behavior.
- Keep `coherence.security.mode` as the only global hardening property, with
  supported values unset / `compatibility` and `hardened`.
- Preserve the existing explicit override fixes for senior metadata proof
  `required=true`, dynamic remote `deny`, and topic replay `reject`.
- Re-run the focused TCMP test or at least the relevant functional compile
  target, then run `git diff --check` and stale-reference scans.

Do not:
- Do not reintroduce `CoherenceMode.LEGACY`, `isLegacy()`, parser aliases for
  `legacy`, `coherence.mode=legacy`, or `coherence.security.hardened`.
- Do not make dev/prod license modes join-compatible unless an architect and
  security reviewer explicitly approve that product behavior change.
- Do not change unrelated REST, TLS, gRPC, JSON, RAG, management, reporting,
  peer-proof, install-gate, or XML parser behavior.
- Do not fetch, push, submit P4, enqueue Remote Queue, stage, commit, touch
  `main`, or revert unrelated user/agent changes unless explicitly asked.

Suggested verification:
```bash
source ../bin/cfglocal.sh
REVISION=15.1.2-0-0-security-opt-in-SNAPSHOT
mvn -Pmodules,modular-tests,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl test/functional/tcmp \
  -Dit.test=LicenseModeCompatibilityTests \
  -Dcoherence.cluster=license-mode-compat-20260611-codex \
  verify
git diff --check
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED|restoreSecurityHardened|isLegacy\(|CoherenceMode\.LEGACY|coherence\.mode=legacy|mode\(\"legacy\"\)|restore\(\"legacy\"\)" \
  coherence-* test/unit test/functional -g'*.java' -g'*.xml' -g'*.properties'
rg -n "\"legacy\"|LEGACY mode|legacy mode|legacy compatibility|mode=legacy" \
  coherence-* test/unit test/functional -g'*.java' -g'*.xml' -g'*.properties'
```

Report the test change, verification results, and any remaining stale-reference
hits. If fixing the test appears to require changing product license-mode
join behavior, stop and ask for architect/security-review confirmation.
~~~
