/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
const Converter = require('case_converter')

module.exports = {

  LowerCaseProcessor: function (propertyName) {
    return {
      process: function (entry) {
        let value = entry.value;
        Converter.toLower(value, propertyName)

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
      Converter.toUpper(value, this.propertyName)

      entry.setValue(value);
      return value[this.propertyName];
    }
  }

}
