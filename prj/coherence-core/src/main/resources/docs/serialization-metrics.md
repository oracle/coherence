<!--
  Copyright (c) 2000, 2026, Oracle and/or its affiliates.

  Licensed under the Universal Permissive License v 1.0 as shown at
  https://oss.oracle.com/licenses/upl.
  -->

# Serialization Metrics

### Serialization gates

Coherence records serialization gate decisions so operators can see which
transport or subsystem reached a shared serialization check. The counters cover
class-filter decisions, ExternalizableHelper FMT-code rejection, POF type-id
resolution, and lambda bytecode/class-name checks. They are exposed through
lazy per-tuple `type=SerializationGates` management MBeans and the standard
metrics bridge. Each distinct metric, route, result, reason, and metric-specific
integer tag is published as one MBean with a single `Count` attribute. All
counters also include a `mode` tag whose value is `prod` or `dev`, matching the
resolved `coherence.mode` for the member.

The `route` tag values are:

`CLUSTER`, `EXTEND_PROXY`, `EXTEND_CLIENT`, `GRPC`, `REST`,
`MANAGEMENT_REST`, `JMX`, `SESSION`, `FEDERATION`, `PERSISTENCE`,
`CACHE_STORE`, `TOPICS`, `TOOLING`, `UNCLASSIFIED`.

`UNCLASSIFIED` means a serialization gate ran without a known route scope. Treat
that as a bug signal and investigate the call path that reached the gate.

#### `coh.serialization.filter_check`

| Tag | Values | Description |
| --- | --- | --- |
| `result` | `allowed`, `rejected` | Class-filter decision result. The current F.1 filter gates record rejections. |
| `reason` | `serialization-allowlist-rejected`, `class-rejected`, `array-rejected`, `json-class-rejected`, `class-identity-package-not-allowed` | Concrete filter rejection reason. |
| `route` | All `SerializationRole` values | Route active when the filter decision was recorded. |
| `principal` | Logged WARN field only | Best-effort current subject identity; intentionally not an MBean tag. |
| `denied-class` | Logged WARN field only | Denied class name in structured rejection logs; not exposed as an MBean attribute in F.1. |

#### `coh.serialization.fmt_check`

| Tag | Values | Description |
| --- | --- | --- |
| `result` | `allowed`, `rejected` | FMT-code decision result. The current F.1 FMT gate records rejections. |
| `reason` | `invalid-format` | ExternalizableHelper format code was not recognized. |
| `route` | All `SerializationRole` values | Route active when the FMT decision was recorded. |
| `fmt` | Integer format code | The rejected format code. |

#### `coh.serialization.pof_check`

| Tag | Values | Description |
| --- | --- | --- |
| `result` | `allowed`, `rejected` | POF type-id decisions recorded by the current F.1 gates. |
| `reason` | `registered-type`, `unknown-type`, `safe-serializable`, `safe-portable`, `invalid-safe-portable-type` | Concrete POF type decision reason. |
| `route` | All `SerializationRole` values | Route active when the POF decision was recorded. |
| `type_id` | Integer POF type id | The POF type id being resolved. |

#### `coh.serialization.lambda_bytecode_check`

| Tag | Values | Description |
| --- | --- | --- |
| `result` | `allowed`, `rejected` | Lambda bytecode or class-name gate decision. |
| `reason` | `none`, `bytecode-references-gadget`, `class-name-on-denylist`, `invalid-bytecode` | Concrete lambda gate decision reason. Empty bytecode is reported as `invalid-bytecode`. |
| `route` | All `SerializationRole` values | Route active when the lambda gate decision was recorded. |
| `denied-class` | Logged WARN field only | Denied class or bytecode reference in structured rejection logs; not exposed as an MBean attribute in F.1. |

## Dashboard Example

Reject rate by route over five minutes can be charted from the metrics bridge
with a PromQL-style expression such as:

```text
sum by (route, mode) (
  rate(coh_serialization_filter_check_count{result="rejected"}[5m])
  + rate(coh_serialization_fmt_check_count{result="rejected"}[5m])
  + rate(coh_serialization_pof_check_count{result="rejected"}[5m])
  + rate(coh_serialization_lambda_bytecode_check_count{result="rejected"}[5m])
)
```

The corresponding JMX source is a family of global Coherence management MBeans
with this ObjectName shape:

```text
Coherence:type=SerializationGates,metric=<metric>,route=<role>,mode=<dev|prod>,result=<allowed|rejected>,reason=<reason>[,fmt=<int>|type_id=<int>][,nodeId=<N>]
```

Each MBean exposes a single numeric `Count` attribute. The metrics bridge uses
the ObjectName properties as labels, excluding standard management keys such as
`type`. To bound label cardinality, Coherence registers at most 1024
serialization-gate tuples per member; additional distinct tuples increment the
`route=UNCLASSIFIED,mode=<dev|prod>,reason=tag-ceiling-reached` overflow
counter.
