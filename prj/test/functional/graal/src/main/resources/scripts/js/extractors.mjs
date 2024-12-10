/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// extractors.mjs
var NameExtractor = class {
  extract(value) {
    return value.name;
  }
};
var AgeExtractor = class {
  extract(value) {
    return value.age;
  }
};
export {
  AgeExtractor,
  NameExtractor
};
