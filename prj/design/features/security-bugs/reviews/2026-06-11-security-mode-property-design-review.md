<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Security mode property design review

- **Date:** 2026-06-11
- **Branch:** `codex/coherence-mode-security-opt-in`
- **Commit reviewed:** `88e1c8f93e6d664b994e8491edc2db3dc72fe34a`
- **Working tree reviewed:** uncommitted compatibility-plan update plus
  unstaged earlier boolean-property implementation context
- **Plan reviewed:**
  `design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
- **Verdict:** accept the move from boolean `coherence.security.hardened` to
  string-valued `coherence.security.mode`; no blocking design issue found

## Prior reviews

None for this plan decision.

## Files in scope

- `design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md`
- `.ai/branches/codex/coherence-mode-security-opt-in/journal.md`
- `coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java`
- `coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java`

## Context

The compatibility plan originally used the boolean property
`coherence.security.hardened`. The plan now proposes
`coherence.security.mode`, defaulting to unset / `compatibility` and accepting
`hardened` as the explicit opt-in for the July compatibility-sensitive
fail-closed gates.

The current code in this branch still contains the earlier boolean
implementation, including `PROP_SECURITY_HARDENED` and
`Config.getBoolean("coherence.security.hardened", false)`. This review is
limited to the property design and plan/documentation implications; it does
not approve or implement the source rename.

## Findings by severity

### P2: Rejected values should explicitly reject boolean-style aliases and the old property as non-aliases

The rejected-values table covers `off` / `false`, but it does not explicitly
cover the most likely migration mistakes from the already-started boolean
implementation: `true`, `on`, `enabled`, `yes`, and continued use of
`coherence.security.hardened`. Because the old boolean name exists only in
uncommitted branch work, the product should not preserve it as an alias.
However, the plan should say that directly so implementation, tests, release
notes, and backport instructions do not accidentally keep a hidden compatibility
surface.

Required change: expand the rejected-values / migration section to state that
`coherence.security.mode` accepts only `compatibility` and `hardened`; boolean
literal aliases such as `true` / `false`, `on` / `off`,
`enabled` / `disabled`, and `yes` / `no` fail fast; and
`coherence.security.hardened` is not a supported alias unless a later explicit
release-management decision changes that.

### P3: The plan still has boolean shorthand that can leak into customer docs

The plan correctly defines a string mode, but several sections still describe
the state as hardening `on` / `off`, `enabled` / `disabled`, or a switch. This
is acceptable for the internal predicate name `isSecurityHardeningEnabled()`,
but it is less clear in customer-facing tables and test/documentation
requirements. In particular, the recommended matrix uses `off` / `on`, the
test plan says `leaves hardening disabled` / `enables hardening`, and the docs
section says docs need to show hardening `disabled or enabled`.

Required change: keep the Java predicate boolean if that is the least invasive
internal API, but rewrite customer-facing plan language to prefer
`security-mode=compatibility` and `security-mode=hardened`, or
`compatibility posture` and `hardened posture`. Avoid using `on`, `off`,
`enabled`, and `disabled` as value-like terms in matrices, docs, and release
notes.

### P3: The rename needs an explicit hygiene checklist because code already started on the boolean property

The branch journal and current diff show that source and tests were already
converted to `coherence.security.hardened`. Switching the plan now is still
safe because the boolean property has not shipped, but it introduces a normal
rename risk: stale constants, helper names, test data, documentation text,
release-note examples, and operator prompts can survive the mechanical rename.

Required change: add an implementation acceptance item requiring a final
repository scan for `coherence.security.hardened`, `PROP_SECURITY_HARDENED`,
boolean test values for the security mode, and release/documentation examples.
The expected result should be no product, test, plan, or doc reference to the
boolean property except historical changelog/review text.

## Assessment

`coherence.security.mode` is a better long-term property name than
`coherence.security.hardened`. It separates runtime mode from security profile,
does not overload `coherence.mode=prod`, and leaves space for later named
profiles without another public property rename.

The initial values are clear enough for a patch line. `compatibility` is the
right default word because it describes the patch contract without suggesting
that security is disabled wholesale. `hardened` is a good first opt-in value
because it describes an additional fail-closed posture above the compatibility
baseline without claiming that other modes are insecure.

Blank and unknown values should fail fast. This is a new explicit property, so
silent fallback would hide deployment drift and make rolling-upgrade state
harder to diagnose. The plan should keep treating unset differently from blank:
unset means default compatibility posture; blank means invalid explicit input.

The rejected-values list has the right direction but should be broadened as
noted above. The current reasoning for `legacy`, `off` / `false`, `prod`,
`secure`, and `strict` is sound. `strict` can remain reserved for a future
profile, but it should not be accepted until it has concrete behavior distinct
from `hardened`.

The migration and backport risk from changing the plan after code started is
manageable. Since `coherence.security.hardened` is unshipped branch work, it
should be removed rather than aliased. The main risk is not customer
compatibility; it is internal residue in tests, docs, release notes, and
handoff prompts.

## Open questions

- Should the property parser accept case-insensitive values and trim outer
  whitespace, or should it require exact lower-case values after trimming?
  Recommendation: trim and compare case-insensitively, but treat empty-after-
  trim as invalid.
- Should `strict` be documented as reserved, or simply rejected as unknown for
  now? Recommendation: do not document it to customers yet; keep it only in the
  design rationale until a future profile exists.
- Should implementation expose a public enum for security mode, or only the
  boolean predicate? Recommendation: keep only the predicate for this patch
  unless a real caller needs the symbolic mode.

## Required follow-up work

- Update the plan before the implementation rename proceeds to add the
  boolean-alias / old-property non-alias rule.
- Replace value-like boolean terminology in customer-facing plan, test-plan,
  and documentation sections with the two named mode values.
- During implementation rename, remove `coherence.security.hardened` rather
  than aliasing it, update tests to exercise string mode parsing, and run a
  final repository scan for stale boolean-property references.

## Verification evidence

Review-only commands:

```bash
git branch --show-current
git log -1 --format='%H %s'
git status --short
rg -n "coherence\.security\.hardened|boolean|true|false|H=|hardening is off|hardening is on|disabled|enabled|enable|disable|off|on|switch" \
  design/features/security-bugs/plans/security-hardening-opt-in-compatibility.md
git diff -- coherence-core/src/main/java/com/tangosol/internal/util/CoherenceMode.java \
  coherence-core/src/main/java/com/tangosol/util/CoherenceMode.java
```

No build or tests were run because this was a plan/property-design review.

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

Task:
Update the compatibility plan and then implement the property rename from the
unshipped boolean `coherence.security.hardened` to string-valued
`coherence.security.mode`.

Design constraints:
- Do not keep `coherence.security.hardened` as an alias unless a human gives a
  new explicit release-management decision.
- `coherence.security.mode` accepts only unset / `compatibility` and
  `hardened`.
- Blank or unknown explicit values fail fast.
- Boolean-style values such as `true`, `false`, `on`, `off`, `enabled`,
  `disabled`, `yes`, and `no` are not aliases.
- Preserve the existing plan's compatibility contract for `coherence.mode`:
  no existing released `coherence.mode` value implicitly enables July
  compatibility-breaking hardening.
- Keep the Java predicate `isSecurityHardeningEnabled()` only if it remains the
  narrowest internal API; do not expose a broader public security-mode API
  unless implementation actually needs it.

Plan updates required before source rename:
- Add the boolean-alias and old-property non-alias rule.
- Replace customer-facing `on` / `off`, `enabled` / `disabled` value-like
  language with `security-mode=compatibility` and
  `security-mode=hardened`.
- Add an acceptance item requiring a final stale-reference scan for
  `coherence.security.hardened`, `PROP_SECURITY_HARDENED`, and boolean-style
  security-mode examples.

Verification after implementation:
```bash
source ../bin/cfglocal.sh >/dev/null
REVISION=15.1.2-0-0-security-opt-in-SNAPSHOT
make coherence ARGS="-Drevision=${REVISION}"
mvn -Pmodules,-coherence -am -nsu \
  -Drevision=${REVISION} \
  -pl test/unit/coherence-tests \
  -Dtest=CoherenceModeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
rg -n "coherence\.security\.hardened|PROP_SECURITY_HARDENED" .
git diff --check
```

Report the plan edits, source/test rename summary, verification results, and
any remaining stale-reference hits. Do not stage, commit, submit P4, enqueue
Remote Queue, or rename unrelated security properties unless explicitly asked.
~~~
