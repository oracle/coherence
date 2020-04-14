/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

module.exports = {
  AgeFilter: class AgeFilter {
    constructor(checkEven) {
      this.checkEven = checkEven;
    }

    evaluate(value) {
      return (this.checkEven) ? value.age % 2 == 0 : value.age % 2 == 1;
    }
  }
}


