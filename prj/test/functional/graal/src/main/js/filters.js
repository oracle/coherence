/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
export class AgeFilter {
  constructor(checkEven) {
    this.checkEven = checkEven;
  }

  evaluate(value) {
    return (this.checkEven) ? value.age % 2 === 0 : value.age % 2 === 1;
  }
}
