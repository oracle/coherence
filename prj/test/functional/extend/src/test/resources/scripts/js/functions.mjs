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

  RuntimeProbe: class RuntimeProbe {
    process(entry) {
      return Java.type("java.lang.Runtime").getRuntime().availableProcessors()
    }
  }
}
