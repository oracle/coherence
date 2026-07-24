<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security Hardening Opt-In Compatibility Plan

[Security bugs home](../README.md)

- Status: proposed for architect review
- Workstream: July security patch compatibility
- Scope: preserve no-config-change upgrade compatibility for customers running
  `coherence.mode=prod`, including rolling upgrade from a prior patch set on
  the same code line, while keeping the new security hardening available as an
  explicit opt-in
- Primary compatibility case: rolling upgrade from `15.1.1.0.3` to
  `15.1.1.0.4` with existing customer configuration unchanged
- Working switch name: `coherence.security.mode`

## Changelog

- 2026-06-13: WLS integration RQ `job.9.20260613165439.7991` completed with
  only stage2 and stage5 failing. Stage2's Extend identity-chain hardened test
  now explicitly enables the invocation-service proxy because hardened mode
  disables that proxy by default before the identity rejection path is reached.
  Stage5's ObjectInputFilter compatibility test now expects success with
  unset `coherence.security.mode`, while fail-closed coverage runs in an
  explicit `coherence.security.mode=hardened` test path.
- 2026-06-13: Main RQ `job.9.20260613151729.8329` exposed stale
  hardened-mode assumptions in stage1, stage2, and stage9 tests. Remote model
  management invocation validation, Extend default identity-chain rejection,
  and Management REST cluster-member update rejection are hardened-mode
  assertions and now opt in with `coherence.security.mode=hardened` before
  expecting fail-closed behavior.
- 2026-06-13: WLS 15.1.2.0 integration triage found three additional
  compatibility misses after the opt-in CL landed on main. Default Extend
  identity assertion must continue to accept a raw `javax.security.auth.Subject`
  token when `coherence.security.mode` is unset or `compatibility`, while
  `hardened` rejects that token. Coherence's default/JVM-wide
  `ObjectInputFilter` attachment to otherwise unfiltered `BufferInput` streams
  must also be a hardened-mode gate so existing WLS Java-security deployments
  do not fail cluster join on WLS serial-filter rules. Management REST
  cache-member updates to non-writable attributes preserve the old 200
  response with per-attribute failure details in compatibility mode, and fail
  closed with 401 in hardened mode.
- 2026-06-12: Stage 1 RQ triage found a concurrency race in the memoized
  `CoherenceMode` / `coherence.security.mode` resolver. A thread that lost
  the lazy `AtomicReference` initialization race could return a stale `null`
  when tests reset the memoized value concurrently, so the resolver now
  retries until it has a concrete resolved value.
- 2026-06-12: Stage 8 and Stage 9 RQ triage found additional failures with
  the same senior-metadata-proof root cause as Stage 2. Hardened gRPC,
  management, and metrics-auth fixtures hit broadcast senior metadata sends
  with unknown recipient sets and stopped their members before the tested HTTP
  or gRPC policy could complete. Focused local reruns of those failed fixtures
  passed after senior metadata proof was decoupled from global hardened mode.
  The separate Micrometer exact-metric comparison failure passed in a focused
  local rerun and does not change the security-mode plan.
- 2026-06-12: Refined the senior metadata proof decision after Stage 2 RQ
  exposed a hardened Extend server restart loop. `coherence.security.mode`
  no longer implicitly requires senior metadata proof because the production
  default provider is disabled and broadcast `SeniorMemberHeartbeat` messages
  can have no concrete recipient set. The explicit
  `coherence.security.peer.senior-metadata-proof.required=true` opt-in still
  fails closed in all security modes. Subject proof remains hardened-mode
  enforced where a route explicitly requires subject proof.
- 2026-06-12: Added the implementation-time test contract from the first
  Remote Queue failure triage. Rejection matrices that validate July
  hardening must now opt in with `coherence.security.mode=hardened`; existing
  `coherence.mode=prod` fixtures must not be treated as hardened unless they
  also set the security mode. Compatibility/default behavior should be covered
  by central resolver tests and representative gates rather than by duplicating
  every broad hardened rejection matrix.
- 2026-06-12: Refined the RemoteExecutor functional-test strategy after local
  reproduction of the RQ failure. `coherence.security.mode=hardened` can fail
  closed first on senior metadata proof before the RemoteExecutor install
  gate under test is reached. The RemoteExecutor functional matrix therefore
  validates the `prod` compatibility/shadow path and asserts the storage
  member's `SerializationTelemetry` `would_reject` counters through a Bedrock
  remote callable, while hardened remote-executable rejection remains covered
  by focused unit tests until a peer-proof-ready functional fixture is
  available.
- 2026-06-12: Preserved explicit hardened-mode functional smoke coverage in
  management and metrics. Those suites start a real member with
  `coherence.security.mode=hardened` and verify both default authenticated
  HTTP rejection and successful authenticated requests, proving that a hardened
  member can start and serve a meaningful operation even while broader
  install-gate matrices use compatibility/shadow coverage to avoid unrelated
  peer-proof preemption.
- 2026-06-12: Updated the REST functional-test contract after RQ triage. REST
  compatibility-mode expression tests should use raw URL expressions because
  alias resolution is part of the hardened REST expression policy. Processor
  compatibility tests should use fixture inputs that match the historical
  reflective method names, so fixture accessor failures are not mistaken for
  policy denials. REST auth fixtures also need short per-member cluster names
  to avoid Bedrock startup failures unrelated to the security-mode contract.
- 2026-06-11: Updated the plan with implementation-time compatibility
  findings from Perforce history. The target patch line's pre-hardening
  default `license-mode` / `coherence.mode` value is `dev`, so unset
  `coherence.mode` must default to `dev`. The existing startup path accepted
  the long aliases `development` and `production`, so those aliases remain
  accepted. The hardening-era `DEV_WARNING` is removed because it was not part
  of the pre-hardening `coherence.mode` behavior.
- 2026-06-11: Switched the proposed global hardening control from boolean
  `coherence.security.hardened` to string-valued `coherence.security.mode`.
  The new proposal defaults to unset / `compatibility` and enables the July
  hardening gates with `hardened`. This keeps the current patch behavior
  opt-in while leaving room for future named security profiles.

## Problem

The July security patch currently ties multiple compatibility-breaking
security defaults to `coherence.mode=prod`. That is not safe for patch-set
customers.

Many production customers already run with `coherence.mode=prod`. If
`15.1.1.0.4` changes the meaning of that existing setting so that new hardening
gates fail closed, a no-config-change patch application can break existing
applications. It can also break rolling upgrade because mixed `15.1.1.0.3` /
`15.1.1.0.4` clusters would have different defaults for behavior that
previously worked.

The compatibility requirement is:

- customers can apply the July patch without changing operational config;
- existing deployments using `coherence.mode=prod` continue to behave as they
  did before the patch unless they explicitly opt in to new hardening;
- rolling upgrade from the previous patch set on the same code line works with
  unchanged config;
- the new security hardening remains available and documented for customers
  who can make the required configuration or application changes.

## Why This Change Is Needed

Patch-set customers expect a security patch to be installable with no
application or operational configuration changes. That expectation is especially
important for customers already running deployments with an explicit
`coherence.mode` value, because that setting is already part of their accepted
runtime posture. Most affected customers use `coherence.mode=prod`, but the
same patch-compatibility principle applies to customers who explicitly run
with `coherence.mode=dev`.

If the patch changes `coherence.mode=prod` from "production mode" into
"production mode plus new fail-closed security gates", the patch creates an
upgrade cliff. Likewise, if the patch changes `coherence.mode=dev` from
"development mode" into "development mode plus new fail-closed security
gates", explicit dev-mode deployments can break during a no-config-change
patch application.

The issue is not that the new gates are wrong. The issue is activating them
through an existing customer setting whose previous meaning did not include
those compatibility-breaking checks.

The change is therefore intended to preserve the patch contract:

- `coherence.mode=prod` keeps meaning "run in production mode";
- `coherence.mode=dev` keeps meaning "run in development mode";
- new compatibility-breaking hardening remains available;
- customers must explicitly opt in to the new fail-closed behavior after they
  validate the required configuration and application changes;
- a rolling upgrade can mix old and new patch-set members without the new
  members suddenly rejecting traffic or configuration that the old members
  accepted.

## Recommendation

Split production mode from compatibility-breaking security hardening.

Keep `coherence.mode=prod` as the operational/product mode. Add a separate
security mode switch for the July compatibility-breaking security gates:

```text
-Dcoherence.security.mode=hardened
```

The property defaults to `compatibility`.

## Security Mode Values

This plan uses a string-valued security mode instead of a boolean switch so the
contract can grow into additional named security profiles later without
changing the property name.

Initial supported values:

| Value | Meaning |
|---|---|
| unset / `compatibility` | Default. Preserve pre-July patch behavior so existing `coherence.mode=prod` and `coherence.mode=dev` deployments continue working without configuration changes. |
| `hardened` | Enable the July compatibility-sensitive security hardening gates. This is the explicit opt-in mode. |

Parser requirements:

- unset defaults to `compatibility`;
- explicit values are trimmed and compared case-insensitively;
- blank after trim fails fast;
- unknown values fail fast.

Because this is a new explicit property, silent fallback would make deployment
state harder to reason about. The earlier unshipped working property
`coherence.security.hardened` is not an alias for `coherence.security.mode`
unless a separate release-management decision explicitly adds that alias.

Rejected values:

| Rejected value | Reason |
|---|---|
| `legacy` | Reintroduces the same unreleased compatibility identity this plan removes from `coherence.mode`. No released customer depends on it, and the default `compatibility` value already captures the desired behavior. |
| `compat` | Unnecessary abbreviation. Accepting shorthand creates avoidable alias surface for a new patch-line property. |
| `true` / `false` | Boolean-style values are not aliases. The property is intentionally string-valued so future security profiles can be named without changing the property. |
| `on` / `off` | Boolean-style values are not aliases and can sound like security is globally enabled or disabled, instead of selecting between compatibility posture and hardened posture. |
| `enabled` / `disabled` | Boolean-style values are not aliases. They also make the compatibility posture sound like a broad security disablement rather than a preserved patch contract. |
| `yes` / `no` | Boolean-style values are not aliases. Accepting them would undermine the explicit two-value mode contract. |
| `prod` | Confuses the new security profile with the existing `coherence.mode=prod` runtime mode and risks recreating the coupling this plan separates. |
| `secure` | Implies the default compatibility posture is insecure. `hardened` is more precise because it identifies an additional fail-closed posture above the compatibility baseline. |
| `strict` | Reasonable as a possible future profile name, but not implemented or documented as reserved now because there is no concrete behavior beyond the current July hardening gates. |

## Resolved Decisions

This plan proceeds with the following decisions:

- Use `coherence.security.mode` as the working property name, with initial
  values `compatibility` and `hardened`. The name can still change if final
  review finds a better name before the patch ships, but implementation should
  proceed with this property for now.
- Do not keep the earlier unshipped `coherence.security.hardened` working name
  as an alias unless a human makes a separate release-management decision.
- Do not introduce `coherence.mode=legacy`. Remove the unreleased public
  `LEGACY` mode surface before the patch ships.
- Restore `coherence.mode` processing to the behavior it had before the
  security hardening work. The hardening switch must not change how
  `coherence.mode` is parsed, defaulted, logged, validated, serialized, or
  exposed to the cluster.
- On the verified target line, pre-hardening operational configuration
  defaulted unset `coherence.mode` / `license-mode` to `dev`; the July patch
  compatibility fix must restore that default. Perforce history also showed
  that the startup path accepted `development` and `production`, so those
  aliases remain supported. Parser aliases and warnings introduced only by the
  security-hardening work, including the prominent DEV warning, are removed.
- Do not add new hardening checks as part of this conversion. Change the
  existing July hardening checks to ask the new hardening predicate instead of
  deriving hardening from `coherence.mode`.
- Leave normal `dev` / `prod` operational behavior alone.
- If a mode check is unclear, leave it unchanged and call it out for review.
- Preserve existing explicit feature-property semantics. Explicit deny /
  enforce / safe settings always win. Explicit allow / compatibility settings
  keep their existing meaning unless review deliberately changes that specific
  feature.
- Do not recommend enabling `coherence.security.mode=hardened` during rolling
  upgrade by default. Patch and roll with the security mode unset or set to
  `compatibility`, validate, then enable `hardened` in a later planned rollout
  unless a specific gate is verified as mixed-version safe.
- Treat TLS hostname verification, weak signature / subject-binding
  hardening, serialization limits, peer subject proof routes that explicitly
  require proof, federation direct-connect endpoint validation, Management JMX
  publish URL validation, and gRPC diagnostics auto defaults as July
  compatibility-breaking hardening gates. These gates should use
  `isSecurityHardeningEnabled()` rather than deriving enforcement from
  `coherence.mode`. Senior metadata proof remains controlled by its explicit
  required property until production proof-provider wiring and broadcast
  recipient handling are available.
- Treat RAG model download allowlist empty-default enforcement as a
  module-scoped July hardening gate if RAG is included in the target patch
  line. If RAG is not in that patch line, leave the RAG row out of scope for
  this compatibility patch.
- For peer subject-proof routes and federation direct-connect endpoint checks,
  it is acceptable that a connection requires both sides to run with
  `coherence.security.mode=hardened` before hardened validation succeeds. A
  one-sided opt-in may fail closed. That is an explicit operational
  requirement for enabling hardening, not a reason to keep those checks tied to
  production mode. Senior metadata proof is separate: it remains controlled by
  `coherence.security.peer.senior-metadata-proof.required` until production
  proof-provider wiring and broadcast recipient handling are available.

The recommended runtime-mode and security-mode matrix is:

| `coherence.mode` | `coherence.security.mode` | Runtime / product posture | July compatibility-breaking security posture |
|---|---|---|---|
| unset | unset | pre-security default runtime posture (`dev` on the verified target line) | compatibility posture |
| unset | `compatibility` | pre-security default runtime posture (`dev` on the verified target line) | compatibility posture |
| unset | `hardened` | pre-security default runtime posture (`dev` on the verified target line) plus explicit hardening | hardened posture |
| any value accepted before the security work, such as `dev` | unset | pre-security runtime posture for that value | compatibility posture |
| any value accepted before the security work, such as `dev` | `compatibility` | pre-security runtime posture for that value | compatibility posture |
| any value accepted before the security work, such as `dev` | `hardened` | pre-security runtime posture for that value plus explicit hardening | hardened posture |
| any value accepted before the security work, such as `prod` | unset | pre-security runtime posture for that value | compatibility posture |
| any value accepted before the security work, such as `prod` | `compatibility` | pre-security runtime posture for that value | compatibility posture |
| any value accepted before the security work, such as `prod` | `hardened` | pre-security runtime posture for that value plus explicit hardening | hardened posture |
| blank or invalid value | any | exact pre-security behavior for that value; do not use invalid input as a hardening control | controlled only by `coherence.security.mode` if the process continues |

The important compatibility rule is that no existing `coherence.mode` value,
including `dev`, implicitly enables the new July compatibility-breaking
hardening. `coherence.mode` continues to select the runtime posture. The new
property selects whether the July fail-closed hardening gates are active.

The implementation should not keep aliases, fallback behavior, warning
messages, or public API values that were introduced only by the security work.
If the old `15.1.1.0.3` mode path accepted or rejected a value, the patched
line should continue to accept or reject that value the same way. Any exact
alias list in this plan is illustrative until verified against the target
patch-line code.

This preserves patch compatibility because the new behavior is opt-in. It also
keeps the security work usable because customers can enable hardening with one
global property after they have updated configuration and validated the
application.

## Rejected Alternative: New Legacy Mode

Earlier unreleased work introduced `legacy` / `legacy-compatibility` as a
possible `coherence.mode` value and as the default compatibility posture. This
plan rejects that approach.

`legacy` has not shipped in any released Coherence version, so no customer can
already depend on `coherence.mode=legacy`. With
`coherence.security.mode=compatibility` as the default, `legacy` also no
longer adds useful compatibility behavior. It would introduce a second compatibility
knob at the same time this plan is trying to separate runtime mode from
security hardening.

The implementation should remove the unreleased public `legacy` /
`legacy-compatibility` mode contract before this patch ships. The public
contract should restore the pre-security `coherence.mode` / `license-mode`
behavior exactly and add only the separate new hardening switch.

## Rejected Alternative: Secured Production

We considered using the old secured production switch, either the
`secured-production` operational-config element or the related
`coherence.secured.production` system property. That alternative is rejected as
the primary design.

The existing `secured-production` operational-config element is documented as
deprecated since `14.1.2.0` and has no effect. Some tests still set
`coherence.secured.production`, but the schema-level contract says the element
is inert.

Reusing that deprecated switch as the primary hardening control has two risks:

- customers may still have old `secured-production` config present from prior
  releases and reasonably expect it to remain inert;
- restoring behavior to a deprecated no-effect property would itself be a
  compatibility change.

There is also a naming problem. "Secured production" sounds like a mode-level
replacement for `coherence.mode=prod`, while the desired behavior is narrower:
only the July compatibility-breaking hardening gates should move behind the
switch. The switch should not imply that production mode is otherwise
unsecured, and it should not alter unrelated production defaults.

The safer path is a new property with a direct name and a narrow contract:
`coherence.security.mode`.

For this patch, do not make `secured-production` or
`coherence.secured.production` an alias for the new hardening switch unless a
separate compatibility audit proves that old customer configurations cannot
accidentally enable July hardening. The primary documented switch should be
the new property.

## Contract

`coherence.mode` continues to mean runtime/product mode and must use the same
processing behavior it had before the security hardening work:

- pre-security defaults remain unchanged; on the verified target line, unset
  `coherence.mode` resolves to `dev`;
- accepted mode values and aliases remain unchanged; on the verified target
  line this includes the existing `development` and `production` aliases;
- blank and invalid values keep their pre-security validation behavior;
- existing mode logging, warnings, license-mode serialization, and cluster
  mode exchange remain unchanged.

`coherence.security.mode` controls only compatibility-breaking security
enforcement introduced by the July patch. It should not change unrelated prod
or dev operational behavior.

Unset or `compatibility`:

- preserve existing customer behavior, including under `coherence.mode=prod`
  and `coherence.mode=dev`;
- emit bounded shadow telemetry or `would_reject` warnings where such paths
  already exist;
- allow rolling upgrade from the prior patch set without requiring config
  changes.

`hardened`:

- enable the current July hardened enforcement posture;
- fail closed for gates whose new behavior requires customer configuration or
  application changes;
- remain compatible with existing explicit feature-level overrides.

Feature-level explicit properties continue to take precedence. The opt-in
conversion should preserve their existing meaning and should change only the
default behavior currently inferred from `coherence.mode`.

The default rule is:

| Feature-level setting | `coherence.security.mode=compatibility` | `coherence.security.mode=hardened` |
|---|---|---|
| unset | compatibility behavior | hardened behavior |
| explicit deny / enforce / safe value | deny / enforce / safe behavior | deny / enforce / safe behavior |
| explicit allow / compatibility value | preserve existing explicit-property behavior | preserve existing explicit-property behavior unless review deliberately changes that feature |

This table intentionally avoids inventing new override semantics while doing
the opt-in conversion. The implementation should document exact behavior for
each existing explicit feature property it touches.

TLS hostname verification has one deliberately stricter explicit override
because it should preserve the hardened behavior currently associated with
`coherence.mode=prod` when `coherence.security.mode=hardened`:

| TLS hostname-verifier setting | `coherence.security.mode=compatibility` | `coherence.security.mode=hardened` |
|---|---|---|
| `coherence.security.hostname.verification` unset, using the shipped XML fallback `allow` | compatibility behavior: allow hostname mismatch with bounded `would_reject` logging | use the default hostname verifier |
| explicit `coherence.security.hostname.verification=allow` | compatibility behavior: allow hostname mismatch with bounded `would_reject` logging | reject the unsafe explicit `allow` value |
| explicit default / non-`allow` action | use the default hostname verifier | use the default hostname verifier |
| custom `<hostname-verifier>` class | preserve the custom verifier | preserve the custom verifier |

## Implementation Shape

Remove the unreleased public `LEGACY` mode surface if it exists in the target
code line. That includes the parser aliases `legacy` and
`legacy-compatibility`, public enum constants, documentation, tests, and
customer-facing warnings that present `legacy` as a supported
`coherence.mode` value.

Also remove customer-facing warnings introduced only by the security-hardening
work. In particular, the prominent DEV warning is not retained because
`coherence.mode=dev` and unset `coherence.mode` already existed before this
hardening work and must keep their prior logging behavior.

Do not replace the old `coherence.mode` / `license-mode` processing with a new
security-specific parser. Restore the pre-security behavior and, where a helper
is still useful, have it reflect the already-resolved runtime mode rather than
changing the parsing/defaulting contract. The new hardening resolver is
separate and reads only `coherence.security.mode`.

Implementation verification against Perforce history should be recorded for
the target patch line. For the current branch, that verification showed:

- the pre-hardening operational XML default for `license-mode` was `dev`;
- the security-hardening work changed that default to `prod`, and this
  compatibility patch restores it to `dev`;
- the startup mode resolver accepted `development` and `production` aliases;
- the hardening-era prominent DEV warning had no pre-hardening equivalent and
  is removed.

Add a central resolver in `com.tangosol.internal.util.CoherenceMode`:

```java
public static boolean isSecurityHardeningEnabled()
```

Resolve `coherence.security.mode` as a string-valued mode with default
`compatibility`. Accept only `compatibility` and `hardened`; reject blank or
unknown values with a clear startup error. Cache the resolved value
consistently with the existing `CoherenceMode` memoization pattern, and reset
it from `resetForTesting()`.

Keep `isSecurityHardeningEnabled()` as the narrow internal API unless a caller
needs the symbolic security mode. Do not introduce a public security-mode enum
or facade as part of this rename.

Change existing July compatibility-breaking hardening predicates to consult the
new hardening predicate instead of deriving enforcement directly from
`coherence.mode`. Do not add new hardening check sites as part of this
conversion. Leave normal `dev` / `prod` operational behavior alone. If a check
is unclear, leave it unchanged and call it out for review.

Functional coverage should prioritize no-config-change compatibility for
released `coherence.mode` values and explicit override semantics. Full
positive runtime coverage under `coherence.security.mode=hardened` can be
limited when an unrelated hardened gate fails closed before the feature under
test. In those cases, keep the hardened gate logic covered by focused unit
tests and use functional tests to validate that default `prod` / `dev` plus
unset security mode remains compatible, while
explicit safe values such as `deny` still reject.

The following examples are illustrative until implementation confirms the
final predicate set:

```java
public static boolean isAllowlistEnforced()
    {
    return isSecurityHardeningEnabled();
    }

public static boolean isDynamicRemoteDefaultDeny()
    {
    return isSecurityHardeningEnabled();
    }

public static boolean isRemoteExecutableEnforced()
    {
    return isSecurityHardeningEnabled();
    }

public static boolean isCoherenceRestAuthEnforced()
    {
    return isSecurityHardeningEnabled();
    }

public static boolean isCoherenceRestPassThroughAllowlistRequired()
    {
    return isSecurityHardeningEnabled();
    }

public static boolean isXmlExternalEntityProtectionRequired()
    {
    return isSecurityHardeningEnabled();
    }
```

The exact predicate list should be confirmed by a code audit, but the important
rule is that every compatibility-breaking security default uses a named
predicate. New code should not inspect `isProd()` directly to decide whether to
reject previously accepted customer behavior.

## Handling Former `isLegacy()` Call Sites

Because this plan removes the unreleased `LEGACY` mode surface, implementation
must not leave source code depending on `CoherenceMode.LEGACY`,
`CoherenceMode.isLegacy()`, parser aliases for `legacy`, or log messages that
claim the process is running in legacy mode.

Former `isLegacy()` call sites should be converted by behavior bucket rather
than by mechanical replacement:

| Bucket | Target decision |
|---|---|
| July compatibility-breaking hardening gate | Use `isSecurityHardeningEnabled()`. `SM=compatibility` takes the prior-patch compatibility path; `SM=hardened` takes the hardened path. |
| Normal `dev` / `prod` operational behavior | Keep tied to the released runtime mode, but remove any dependency on the unreleased `LEGACY` constant or parser value. |
| Explicit feature-property opt-in | Preserve the explicit property semantics. If `required=true`, `deny`, `enforce`, or another safe explicit value already means fail closed, keep that behavior independent of global hardening unless a feature review says otherwise. |
| Logging / telemetry | Remove `mode=legacy` labels. Use `security-mode=compatibility` or the actual released `coherence.mode` value. |

The currently resolved former-legacy buckets are:

| Area | Decision |
|---|---|
| Serialization limits | July hardening gate. `SM=compatibility` preserves released limit compatibility; `SM=hardened` applies finite defaults and rejects unlimited values where the July hardening requires that. |
| TLS hostname verification | July hardening gate. `SM=compatibility` preserves released hostname-verifier compatibility; `SM=hardened` rejects unsafe `allow` behavior except for the already-approved system-property default path and enforces the hardened verifier default. |
| Weak signature algorithm / subject binding | July hardening gate. `SM=compatibility` preserves compatibility with warnings or shadow telemetry; `SM=hardened` rejects weak algorithms and enforces verified signer binding. |
| Peer subject proof and senior metadata proof | Subject proof is a July hardening gate only for routes that explicitly require proof. Senior metadata proof remains controlled by `coherence.security.peer.senior-metadata-proof.required=true`; `SM=hardened` alone does not require senior metadata proof until production proof-provider wiring and broadcast recipient handling are available. |
| Federation direct-connect endpoint validation | July hardening gate. `SM=compatibility` preserves compatibility after existing participant validation. `SM=hardened` rejects unverified endpoints; both sides of a hardened direct-connect topology should be configured consistently. |

Every remaining direct `isLegacy()` use must either map to one of these buckets
or be called out as a separate implementation decision before the `LEGACY`
surface is removed.

## Initial Code Audit Targets

Start from the existing central mode resolver and its named security
predicates:

- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
- `coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java`

Then audit call sites that currently tie security behavior to mode:

- serialization allowlist enforcement;
- dynamic remote payload defaults;
- `RemoteExecutablePolicy` enforcement;
- REST authentication and pass-through allowlist defaults;
- XML external entity fail-closed behavior;
- management and Reporter hardening paths that currently distinguish
  the unreleased `LEGACY` mode from `DEV` / `PROD`;
- TLS hostname verification and weak signature algorithm hardening;
- Management JMX publish URL validation;
- gRPC diagnostics and error disclosure auto defaults;
- RAG model download allowlist if it is included in the July patch line.

### Mode-Check Audit Table

This table is the initial mode-check audit for the current branch. It must be
kept current as the implementation proceeds. Rows marked "decision required"
must be resolved before that gate is changed.

Legend:

- `SM=compatibility`: `coherence.security.mode` is unset or set to
  `compatibility`.
- `SM=hardened`: `coherence.security.mode=hardened`.
- Compatibility behavior means behavior compatible with the previous released
  patch set for the same code line and the same customer configuration.

| Area / call site | Current mode dependency | Prior-patch compatibility baseline | Classification | Target behavior | Explicit override behavior | Tests required | Owner |
|---|---|---|---|---|---|---|---|
| Mode parser and public mode surface: `com.tangosol.internal.util.CoherenceMode`, `com.tangosol.util.CoherenceMode`, `CoherenceModeTest` | Adds unreleased `LEGACY`, parses `legacy` / `legacy-compatibility`, defaults unset/blank/invalid to `LEGACY`, exposes `isLegacy()` | Released customers know only the pre-security `coherence.mode` / `license-mode` contract; no released customer can depend on `legacy` | Restore pre-security mode processing and remove unreleased public mode | Remove public `LEGACY` mode, parser aliases, public facade, legacy banner, and legacy tests. Add `isSecurityHardeningEnabled()`. Restore the exact pre-security behavior for defaults, accepted values, invalid values, logging/warnings, license-mode serialization, and cluster mode exchange. | `coherence.security.mode` is independent of `coherence.mode`. `secured-production` remains inert. | Resolver tests comparing unset/blank/invalid and accepted mode values to pre-security behavior, plus security mode unset/`compatibility`/`hardened`, memoization reset, and no security-introduced `legacy` mode contract | Implementation agent; architect confirms exact pre-security target-line behavior |
| Central gate predicates: `isAllowlistEnforced()`, `isDynamicRemoteDefaultDeny()`, `isRemoteExecutableEnforced()`, `isCoherenceRestAuthEnforced()`, `isCoherenceRestPassThroughAllowlistRequired()`, `isXmlExternalEntityProtectionRequired()` | Current branch maps several gates to `!isLegacy()` or `isProd()` | Previous patch set did not fail closed solely because customer had `coherence.mode=dev` or `prod` | Move only after gate is confirmed July compatibility-breaking | For confirmed gates, `SM=compatibility` means compatibility behavior in all released modes; `SM=hardened` means hardened behavior in all released modes. Do not use `coherence.mode` alone to reject. | Per-feature override table controls feature-specific properties. Explicit deny/enforce wins. Explicit allow requires per-feature approval. | Predicate matrix tests for unset, `dev`, `prod` crossed with SM=compatibility/SM=hardened | Implementation agent; architect approves final predicate list |
| Serialization allowlist: `SerializationAllowlist.isProdMode()` | Delegates to `CoherenceMode.isAllowlistEnforced()` | Existing customers should not need generated allowlist config merely because they patch while running `dev` or `prod` | July compatibility-breaking gate | `SM=compatibility`: compatibility allow/shadow. `SM=hardened`: enforce allowlist. | Existing allowlist config remains honored. No blanket explicit allow unless documented by the serialization config contract. | Unit tests for compatibility posture under `dev`/`prod` with SM=compatibility and enforced behavior with SM=hardened; telemetry/would-reject check where available | Implementation agent |
| Dynamic remote payload default: `RemoteExecutionMode`, `LambdaBytecodeGate`, `RemoteInstallGate` | Unset `coherence.remote.dynamic.unauthenticated` defaults from `CoherenceMode.isDynamicRemoteDefaultDeny()`; install gate uses `isLegacy()` for shadow | Previous behavior allowed dynamic paths unless explicitly denied; July hardening denies by default in prod | July compatibility-breaking gate | `SM=compatibility`: unset defaults to allow with bounded would-reject telemetry where applicable. `SM=hardened`: unset defaults to deny. | `coherence.remote.dynamic.unauthenticated=deny` always denies. `allow` preserves its existing explicit-property meaning and allows unless review deliberately changes this feature. Invalid/blank uses default for security mode state. | Unit tests for unset/allow/deny/invalid under SM=compatibility/SM=hardened; lambda and remote-install representative tests | Implementation agent |
| Remote executable policy: `DefaultRemoteExecutablePolicy`, `RemoteInstallGate.enforceReplay`, executable telemetry | `isRemoteExecutableEnforced()` plus direct `isLegacy()` shadow/replay checks | Previously accepted customer executable payloads must not fail solely after patch | July compatibility-breaking gate | `SM=compatibility`: do not throw for missing `@Remote.Executable`; record would-reject telemetry. `SM=hardened`: reject missing executable classification. | Security config executable entries still allow. No broad property-level allow unless explicitly added and documented. | Unit tests for would-reject vs rejected, replay/dedup path, telemetry labels without `mode=legacy` | Implementation agent |
| Cache processor executable wrappers: `CacheProcessors` function-based processors | `isLegacy()` returns anonymous/lambda historical processors; non-legacy returns named classified processors | Existing API return/serialization behavior should not change unless `coherence.security.mode=hardened` | July compatibility-breaking behavior-selection gate | `SM=compatibility`: use historical processor path. `SM=hardened`: use classified wrapper processors needed for executable policy enforcement. | No known property override. | Unit tests for returned processor class/serialization path with SM=compatibility/SM=hardened; existing cache processor tests | Implementation agent |
| Topics persisted policy drift: `TopicsPersistedPolicyDrift`, `RemoteInstallGate.enforceReplay` | Default is `reject` in `prod`, `warn-allow` otherwise | Existing persisted topic payloads should not become reject-by-default merely because `prod` is set | July compatibility-breaking gate if included in patch scope | `SM=compatibility`: unset defaults to `warn-allow`. `SM=hardened`: unset defaults to `reject`. | `coherence.topics.persisted.policy-drift=reject` always rejects. `warn-allow` preserves its existing explicit-property meaning and allows unless review deliberately changes this feature. Invalid uses security-mode-based default. | Unit tests for unset/reject/warn-allow/invalid under SM=compatibility/SM=hardened; persisted replay representative test | Implementation agent if in July scope |
| XML external entity protection: `SaxParser`, `isXmlExternalEntityProtectionRequired()` | Fail-closed parser protection requirements use the mode predicate | Existing XML parser fallback behavior should not fail solely due to `dev` or `prod` after patch | July compatibility-breaking gate | `SM=compatibility`: released compatibility behavior. `SM=hardened`: fail closed when required protections cannot be applied. | No known property override beyond parser configuration. | Existing `SaxParserTest` matrix updated for SM=compatibility/SM=hardened and `dev`/`prod` | Implementation agent |
| REST authentication and pass-through defaults: public facade `com.tangosol.util.CoherenceMode`, REST call sites | REST gates use `isCoherenceRestAuthEnforced()` and `isCoherenceRestPassThroughAllowlistRequired()` | Existing REST deployments should not require new auth/pass-through allowlist config solely after patch | July compatibility-breaking gate | `SM=compatibility`: compatibility behavior for explicit `dev` and `prod`. `SM=hardened`: enforce auth/pass-through allowlist behavior. | Existing REST explicit config wins. Any explicit compatibility escape must be named in feature override table. | REST unit/functional matrix with `dev`, `prod`, SM=compatibility/SM=hardened; pass-through and auth engaged cases | Implementation agent |
| JSON class metadata hardening: `SerializationGate`, `JsonClassMetadataPolicy`, `MapConverter` | Direct `isLegacy()` chooses historical class metadata resolution/fallback versus configured alias enforcement | Existing JSON/REST payloads using historical Genson metadata should not fail solely after patch unless `coherence.security.mode=hardened` | July compatibility-breaking gate if REST-01 JSON scope is in July patch | `SM=compatibility`: historical metadata compatibility and fallback. `SM=hardened`: configured alias / denied metadata enforcement. | Configured aliases always allow approved metadata. No blanket arbitrary class allow under SM=hardened unless explicitly documented. | JSON metadata unit tests and REST projection/alias functional tests under SM=compatibility/SM=hardened | Implementation agent; architect confirms July scope |
| Serialization limits: `SerializationLimitPolicy` | Direct `isLegacy()` allows unlimited defaults/properties and shadows recommended thresholds; non-legacy requires finite limits | Existing POF payloads/config using no explicit limits or unlimited values must not break solely after patch | July compatibility-breaking gate | `SM=compatibility`: released compatibility limits behavior. `SM=hardened`: finite defaults and reject unlimited values where the July hardening requires that. | Explicit finite limit properties always honored. Unlimited under SM=hardened should reject. Unlimited under SM=compatibility follows released compatibility. | Serializer limit unit tests for unset/finite/unlimited under SM=compatibility/SM=hardened | Implementation agent |
| Management TCMP and Reporter hardening: `ManagementInvocationPolicy`, `ReporterSecurity` | Direct `isLegacy()` converts selected rejections to shadow allow | Existing management/Reporter supported behavior should remain patch-compatible unless `coherence.security.mode=hardened` | July compatibility-breaking gate if MGMT-01 slices are in July patch | `SM=compatibility`: allow/shadow supported compatibility paths. `SM=hardened`: reject unsafe management/Reporter operations. | Existing allowlists/source policies still allow. Explicit compatibility properties must be listed separately if any. | Management and Reporter focused functional tests under `prod` SM=compatibility/SM=hardened | Architect confirms exact MGMT gates; implementation agent |
| Management and metrics HTTP auth defaults: `HttpAuthDefaults` | Mode default is `none` for `LEGACY`, `basic` otherwise | Existing HTTP management/metrics deployments may rely on unauthenticated defaults | July compatibility-breaking gate if MGMT HTTP auth defaults are in patch scope | If included: `SM=compatibility`: released auth default. `SM=hardened`: hardened auth default. Remove `LEGACY` from resolution. | `coherence.management.http.auth` and `coherence.metrics.http.auth` explicit values win. | HttpAuthDefaults unit tests for explicit/owned XML/property and SM=compatibility/SM=hardened | Architect decides scope; implementation agent |
| Management JMX publish URL policy: `NameServiceValuePolicy` | RMI stub URL fallback throws in `PROD`, warns in `LEGACY`, fine in dev | Existing management publish behavior should not change solely after patch unless `coherence.security.mode=hardened` | July compatibility-breaking gate | `SM=compatibility`: compatibility fallback with bounded logging. `SM=hardened`: reject unsafe RMI stub URL. | No known feature property. | Connector publish trust tests under SM=compatibility/SM=hardened | Implementation agent |
| TLS hostname verification: `SSLSocketProvider`, `SSLSocketProviderDependenciesBuilder`, `SSLHostnameVerifierPreprocessor` | Direct `isLegacy()` allows null/allow hostname verifier compatibility; non-legacy rejects or uses default verifier | Existing TLS configs with `allow` or unset verifier may break if hardened by mode | July compatibility-breaking gate | `SM=compatibility`: released hostname verification behavior. `SM=hardened`: reject explicit unsafe `allow`, treat shipped XML fallback `allow` as default verifier, and enforce default verifier for unset/null verifier paths. | Explicit `coherence.security.hostname.verification=allow` allows only when SM=compatibility and rejects when SM=hardened. Explicit default/non-`allow` uses default verifier. Custom hostname-verifier classes are preserved in both states. | SSL hostname verification unit/functional tests with SM=compatibility/SM=hardened, unset fallback, explicit `allow`, explicit default/non-`allow`, null verifier, and custom verifier cases | Implementation agent |
| Weak signature algorithm / subject binding: `DefaultController` | Direct `isLegacy()` permits weak defaults and multi-principal fallback; non-legacy rejects/constrains | Existing security configs may use weak algorithms or multi-principal subjects | July compatibility-breaking gate | `SM=compatibility`: compatibility behavior with warnings. `SM=hardened`: reject weak algorithms and enforce verified signer binding. | Explicit strong algorithm always allowed. Weak explicit values under SM=hardened reject. | DefaultController subprocess tests under SM=compatibility/SM=hardened for algorithm and principal binding | Implementation agent |
| gRPC diagnostics and error disclosure: `GrpcDiagnosticsPolicy` | `auto` channelz enabled unless `prod`; `auto` error disclosure safe in `prod` | Diagnostics defaults should not become less diagnostic solely because an existing deployment sets `coherence.mode=prod` | July compatibility-breaking diagnostics hardening gate | `SM=compatibility`: `auto` uses rich diagnostics compatibility defaults. `SM=hardened`: `auto` uses hardened defaults: safe error disclosure and hardened Channelz policy. | `coherence.grpc.channelz=enabled/disabled` and `coherence.grpc.error-disclosure=safe/diagnostic` explicit values win. Invalid still throws. | GrpcDiagnosticsPolicy unit tests for auto/explicit values crossed with SM=compatibility/SM=hardened | Implementation agent |
| InvocationService proxy default: `DefaultInvocationServiceProxyDependencies`, `InvocationServiceProxy` | Default enabled is `!isProd()`; warning suppressed in `LEGACY` | Existing Extend invocation-service users in prod can break if disabled by patch | July compatibility-breaking gate if CACHE-01 Slice A is in July patch | `SM=compatibility`: default enabled for compatibility. `SM=hardened`: default disabled unless explicitly enabled. | `coherence.invocation.enabled=true/false` explicit property wins. Blank/unset uses the security mode default. | Dependency unit tests and Extend invocation proxy functional tests under SM=compatibility/SM=hardened | Implementation agent if in July scope |
| Concurrent Extend proxy default: `ConcurrentProxyPreprocessor` | Shipped marker defaults to enabled except `prod`; warning suppressed in `LEGACY` | Existing Concurrent Extend access in prod can break if disabled by patch | July compatibility-breaking gate if CACHE-01 concurrent slice is in July patch | `SM=compatibility`: default enabled. `SM=hardened`: default disabled unless explicitly enabled. | `coherence.concurrent.extend.enabled=true/false` explicit property wins. Blank/unset uses the security mode default. | Preprocessor tests and functional concurrent proxy tests under SM=compatibility/SM=hardened | Implementation agent if in July scope |
| Peer subject proof and senior metadata proof: `Grid` | Subject proof enforced in non-legacy; senior metadata proof enforces when explicit required property and `prod` | Peer proof paths may affect mixed-version clusters and protocol compatibility | Subject proof is a hardened-mode enforcement gate only for routes that explicitly require subject proof. Senior metadata proof remains an explicit feature opt-in until production proof-provider wiring and broadcast recipient handling are available. | `SM=compatibility`: compatibility allow/shadow behavior for mixed-version upgrade. `SM=hardened`: enforce subject proof where the targeted route requires proof. `SM=hardened` alone does not implicitly require senior metadata proof because the production default senior-metadata provider is disabled and senior heartbeats may be broadcast without a concrete recipient set. | `coherence.security.peer.senior-metadata-proof.required=true` remains an explicit feature opt-in and fails closed independent of `coherence.security.mode`. Both sides of a senior-metadata-proof deployment need equivalent proof support. | Peer proof unit tests for SM=compatibility, SM=hardened, explicit senior-metadata proof required, disabled-provider failure, and explicit-property fail closed. Functional hardened-mode tests may use other gates until a peer-proof-ready functional fixture exists. | Implementation agent; peer owner validates protocol cases |
| Federation direct-connect endpoint validation: `ConnectRequestHandler` | Unverified endpoint allows in `LEGACY`, rejects in `PROD`, likely debug-allows in dev | Federation direct-connect validation may affect mixed clusters and customer topologies | July compatibility-breaking gate; mixed-version sensitive | `SM=compatibility`: compatibility allow after participant validation. `SM=hardened`: reject unverified endpoints. A one-sided hardening opt-in may fail closed if the remote side is not configured for hardened validation. | Direct-connect configuration of expected endpoints always allows matching endpoints. Hardened direct-connect topologies should enable and configure hardening consistently on both sides. | Federation connect validation tests under SM=compatibility/SM=hardened, configured endpoint cases, and one-sided SM=hardened failure | Implementation agent; federation owner validates topology cases |
| RAG model download allowlist: `RagSecurity` | Empty allowlist rejects only in `prod`; dev warns and allows | RAG may not be on all patch lines; behavior affects model download compatibility | Module-scoped July hardening gate if RAG is in the target patch line; otherwise out of scope | If RAG is included: `SM=compatibility`: warn/allow empty allowlist. `SM=hardened`: reject empty allowlist. If RAG is not in the patch line, do not change it for this compatibility patch. | `coherence.rag.security.huggingface.allowed-models` explicit allowlist always controls. | RAG security unit tests under SM=compatibility/SM=hardened if module present | Implementation agent if RAG is in scope; otherwise out of scope |
| Telemetry labels using `CoherenceMode.current()` or `mode=legacy`: `SerializationTelemetry`, `ClassIdentityAllowlist`, `InvocationServiceProxy`, logs in mode-gated paths | Emits runtime mode names and `mode=legacy` shadow messages | Logs should remain actionable and not claim customers selected an unreleased mode | Logging cleanup | Remove `legacy` labels. Prefer `security-mode=compatibility` and actual `coherence.mode` where useful. | Not applicable. | Unit assertions for key log/telemetry strings where existing tests check them | Implementation agent |

Do not blindly replace every `CoherenceMode.isProd()` use. Some uses are
ordinary production-mode operational behavior and should remain tied to
`coherence.mode=prod`.

## Logging And Telemetry

The current prerelease code has shadow paths that log or report `mode=legacy`
/ `would_reject`. With this plan, customers may be in `coherence.mode=prod` or
`coherence.mode=dev` while `coherence.security.mode` is unset or
`compatibility`. Messages should avoid saying that those customers are running
legacy mode.

Prefer language such as:

```text
security-mode=compatibility
result=would_reject
```

The goal is to make the logs actionable without incorrectly implying that a
customer has selected an unreleased legacy runtime mode.

## Rolling Upgrade

The new switch must be local and default to compatibility behavior. No cluster
protocol negotiation should be required for the compatibility fix.

For a rolling upgrade from `15.1.1.0.3` to `15.1.1.0.4`:

1. Existing members run with their current behavior.
2. New `15.1.1.0.4` members join with `coherence.security.mode=compatibility`
   by default.
3. Compatibility-breaking gates remain in compatibility posture on the
   upgraded members unless the operator explicitly selects hardened mode.
4. After the full cluster is upgraded and the application/configuration has
   been validated, the operator can roll the cluster again with
   `-Dcoherence.security.mode=hardened`.

Documentation should recommend selecting `coherence.security.mode=hardened`
after the rolling upgrade is complete unless a specific gate is proven
mixed-version safe.

## Test Plan

Add focused unit tests for the central resolver:

- default security mode is `compatibility` with `coherence.mode` unset;
- default security mode is `compatibility` with `coherence.mode=prod`;
- default security mode is `compatibility` with `coherence.mode=dev`;
- `coherence.security.mode=compatibility` selects compatibility posture;
- `coherence.security.mode=hardened` selects hardened posture;
- `coherence.security.mode` parsing trims and compares case-insensitively;
- blank after trim and unknown `coherence.security.mode` values fail fast;
- boolean-style values `true`, `false`, `on`, `off`, `enabled`, `disabled`,
  `yes`, and `no` fail fast and are not aliases;
- the earlier unshipped `coherence.security.hardened` working property is not
  an alias;
- memoization and `resetForTesting()` behavior are correct;
- public facade methods return the same values as the internal resolver where
  applicable.

Update mode predicate tests so `coherence.mode=prod` no longer implies every
compatibility-breaking gate is enforced.

Add focused tests for representative gates:

- a remote executable path records `would_reject` but does not throw when
  `coherence.security.mode` is unset or `compatibility`;
- the same path throws when `coherence.security.mode=hardened`;
- explicit feature-level deny settings still deny when the security mode is
  unset or `compatibility`;
- REST pass-through compatibility remains allowed in prod mode when hardening
  is in compatibility posture and is denied in hardened posture;
- XML parser fail-closed behavior follows the new hardening switch for the
  compatibility-sensitive path.

For functional coverage, add or update at least one prod-mode compatibility
test that starts with:

```text
-Dcoherence.mode=prod
```

and does not set `coherence.security.mode`. That test should demonstrate a
previously supported customer path continues to work.

Add at least one equivalent dev-mode compatibility assertion for a
representative gate:

```text
-Dcoherence.mode=dev
```

and no `coherence.security.mode` property. This verifies that explicit
dev-mode deployments are not broken by the patch either.

Add the hardened counterpart with:

```text
-Dcoherence.mode=prod -Dcoherence.security.mode=hardened
```

and assert the expected rejection or required configuration behavior.

Implementation-time RQ triage refined the test contract:

- broad rejection matrices that prove fail-closed July hardening behavior must
  explicitly set `coherence.security.mode=hardened`;
- existing `coherence.mode=prod` or `coherence.mode=dev` test fixtures must
  remain compatibility tests unless they also set the security mode;
- compatibility/default assertions should be concentrated in the central
  resolver and representative gates, not duplicated across every cascade or
  matrix suite whose purpose is hardened rejection coverage;
- explicit feature-level deny settings still need compatibility-mode coverage
  where they are intended to reject independently of the global security mode;
- functional suites that need to prove a specific install gate may use a
  feature-level explicit deny setting when that setting owns enforcement and
  full hardened mode can fail closed first on another unrelated gate;
- when a feature-level property only controls a different default, such as the
  dynamic remote payload setting versus the RemoteExecutor
  remote-executable-policy gate, do not treat it as a substitute hardened
  rejection switch. Keep the functional test on compatibility/shadow behavior,
  assert server-side `would_reject` telemetry where a stable member-state hook
  exists, and rely on focused unit tests for hardened rejection until the
  functional fixture can satisfy the earlier hardened gates. The RemoteExecutor
  matrix uses a storage-member remote callable to inspect
  `SerializationTelemetry` rather than relying on server log scraping;
- `coherence.security.mode=hardened` should not be used as a proxy for senior
  metadata proof enforcement. Senior metadata proof is controlled by the
  explicit `coherence.security.peer.senior-metadata-proof.required=true`
  opt-in until production proof-provider wiring and broadcast-recipient
  handling exist. This prevents functional tests from failing closed on
  cluster heartbeat proof before reaching the gate under test;
- REST expression-policy compatibility tests should assert historical raw URL
  expression behavior. Alias-backed projection, sort, processor, and metadata
  resolution belongs to the hardened policy path and should not be used as the
  compatibility proof;
- telemetry assertions may continue to include `mode=prod` or `mode=dev` when
  that label intentionally reports runtime mode, but hardening posture should
  be identified separately as `security-mode=compatibility` or
  `security-mode=hardened`;
- tests that directly validate management method-dispatch hardening or default
  Extend identity-chain rejection should run under
  `coherence.security.mode=hardened`; compatibility-mode tests should assert the
  historical allow behavior instead of expecting those hardening rejections;
- Management REST cluster-member updates to non-writable attributes should
  mirror cache-member update coverage: compatibility mode preserves the
  historical successful response with per-attribute failure details, while
  hardened mode rejects the request;
- RQ failures caused by stale prod-default hardening assumptions are test
  contract fixes, not product behavior changes.

## Documentation

Update customer-facing documentation to stop saying that
`coherence.mode=prod` alone enables the July compatibility-breaking hardening.

The new documentation should say:

- `coherence.mode=prod` is production mode;
- `coherence.mode=dev` is development mode;
- `coherence.security.mode=hardened` enables the July hardening gates;
- the security mode defaults to `compatibility` for patch-set compatibility,
  regardless of `coherence.mode`;
- customers should first patch and complete rolling upgrade with unchanged
  config, then enable hardening in a planned validation window;
- explicit per-feature security properties remain available for more granular
  control.

Existing docs that present a binary `dev` / `prod` hardening table need a
third dimension: any runtime mode can run with `security-mode=compatibility`
or `security-mode=hardened`.

## Architect Review Questions

1. Raise any objection to `coherence.security.mode` as the working property
   name, with initial values `compatibility` and `hardened`, before
   implementation begins.
2. Confirm that `coherence.secured.production` and `secured-production` remain
   inert for this patch and are not aliases.
3. Confirm the implementation plan restores the exact pre-security
   `coherence.mode` / `license-mode` behavior from the target code line,
   including defaults, accepted values, invalid-value handling, logging,
   serialization, and cluster mode exchange.
4. Review the mode-check audit table for completeness, including the resolved
   TLS, serialization-limit, Management JMX publish URL, gRPC diagnostics,
   peer-proof, federation, and RAG hardening gate classifications.
5. Confirm that existing explicit feature-property semantics are preserved
   unless a specific feature is deliberately changed.
6. What is the long-term default plan after the July patch line: keep the
   default security mode as `compatibility` indefinitely, or change it in a
   future major/minor release after release-note lead time?

## Proposed Acceptance Criteria

- A customer running `coherence.mode=prod` can patch from `15.1.1.0.3` to
  `15.1.1.0.4` without config changes and without the July compatibility-
  breaking hardening gates rejecting previously accepted behavior.
- A customer running `coherence.mode=dev` receives the same no-config-change
  patch compatibility for July compatibility-breaking hardening gates.
- The same customer can opt in to the July hardened behavior with one system
  property.
- Explicit feature-level security properties continue to work and override the
  global default where they already did.
- Logs and telemetry distinguish compatibility-mode shadow results from the
  selected runtime mode.
- Tests cover default compatibility-mode prod and dev behavior, explicit hardened-mode
  rejection, and at least one explicit feature-level override.
- Final implementation review includes a stale-reference scan for
  `coherence.security.hardened`, `PROP_SECURITY_HARDENED`, boolean-style
  `coherence.security.mode` values, and customer-facing `on` / `off` or
  `enabled` / `disabled` examples. Any remaining old-property hits must be
  limited to changelog or review-history text.

## Architect Handoff Prompt

~~~markdown
Please review
`design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
for the July security patch compatibility issue.

The core proposal is to keep `coherence.mode=prod` and `coherence.mode=dev`
backward compatible as runtime modes, and move compatibility-breaking July
hardening defaults behind a new default-compatibility property, proposed as
`coherence.security.mode`.

Please focus on:

- whether the resolved decisions are coherent enough to proceed to code;
- whether the new property name, default, and initial value set
  (`compatibility`, `hardened`) are acceptable;
- whether the deprecated `coherence.secured.production` /
  `secured-production` switch should remain inert for this patch as proposed;
- whether the plan clearly restores the exact pre-security `coherence.mode` /
  `license-mode` behavior, rather than preserving parser behavior introduced
  by the security work;
- whether the mode-check audit table correctly classifies
  `CoherenceMode.isProd()` / `isLegacy()` security call sites and clearly marks
  scope-conditional rows;
- whether the mode and hardening matrix correctly preserves patch
  compatibility for explicit `dev` and `prod` deployments;
- whether the feature-level override precedence table is sufficient;
- whether the rolling-upgrade contract is sufficient for
  `15.1.1.0.3` to `15.1.1.0.4`.

Do not implement changes during the review. Return findings, required design
changes, and a recommended implementation scope.
~~~
