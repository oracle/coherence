/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
module.exports = {

  NameExtractor: class NameExtractor {
    extract(value) {
      return value.name;
    }
  },

  AgeExtractor: class AgeExtractor {
    extract(value) {
      return value.age;
    }
  }

}


