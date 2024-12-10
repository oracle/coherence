/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// node_modules/text-lower-case/dist.es2015/index.js
function lowerCase(str) {
  return str.toLowerCase();
}

// node_modules/upper-case/dist.es2015/index.js
function upperCase(str) {
  return str.toUpperCase();
}

// processors.mjs
var LowerCaseProcessor = class {
  constructor(propertyName) {
    this.propertyName = propertyName;
  }
  process(entry) {
    let value = entry.value;
    value[this.propertyName] = lowerCase(value[this.propertyName]);
    entry.setValue(value);
    return value[this.propertyName];
  }
};
var UpperCaseProcessor = class {
  constructor(propertyName) {
    this.propertyName = propertyName;
  }
  process(entry) {
    let value = entry.value;
    value[this.propertyName] = upperCase(value[this.propertyName]);
    entry.setValue(value);
    return value[this.propertyName];
  }
};
export {
  LowerCaseProcessor,
  UpperCaseProcessor
};
