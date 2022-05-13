/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
const util = require("sub1/sub2/util")

module.exports = {

  LowerCaseProcessor: function (propertyName) {
    return {
      process: function (entry) {
        let value           = entry.value;
        value[propertyName] = value[propertyName].toLowerCase();

        entry.setValue(value);
        return value[propertyName];
      }
    }
  },

  UpperCaseProcessor: class MyProcessor {
    constructor(propertyName) {
      this.propertyName = propertyName;
    }

    process(entry) {
      let value = entry.value;
      value[propertyName] = value[propertyName].toUpperCase();

      entry.setValue(value);
      return value[propertyName];
    }
  },

  ES6UpperCaseProcessor: class ES6EntryProcessor {
    constructor(propertyName) {
      this.propertyName = propertyName;
    }

    process(entry) {
      let value = entry.value;
      value[propertyName] = value[propertyName].toUpperCase();

      entry.setValue(value);
      return value[propertyName];
    }
  },

  CaseConverter: class SimpleCaseConverter {

    static toUpper(str) {
      return str.toUpperCase();
    }

    static toLower(str) {
      return str.toLowerCase();
    }
  },

  Util: require('sub1/sub2/util'),

  Config: require('sub1/sub2/config.json')

}
