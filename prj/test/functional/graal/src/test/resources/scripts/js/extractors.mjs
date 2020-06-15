/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
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


