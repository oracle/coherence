/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

module.exports = {
  EntryEcho: class EntryEcho {
    process(entry) {
      return entry.getValue()
    }
  },

  ValuePresentFilter: class ValuePresentFilter {
    evaluate(value) {
      return value != null
    }
  },

  IdentityExtractor: class IdentityExtractor {
    extract(value) {
      return value
    }
  },

  CountAggregator: class CountAggregator {
    constructor() {
      this.count = 0
    }

    accumulate(entry) {
      this.count++
      return true
    }

    combine(partialResult) {
      this.count += partialResult
      return true
    }

    getPartialResult() {
      return this.count
    }

    finalizeResult() {
      return this.count
    }
  },

  RuntimeProbe: class RuntimeProbe {
    process(entry) {
      return Java.type("java.lang.Runtime").getRuntime().availableProcessors()
    }
  }
}
