/*
 * Copyright (c) 2000, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
import {lowerCase} from "text-lower-case";
import {upperCase} from "upper-case";

export class LowerCaseProcessor {
  constructor(propertyName) {
    this.propertyName = propertyName;
  }

  process(entry) {
    let value = entry.value;
    value[this.propertyName] = lowerCase(value[this.propertyName])

    entry.setValue(value);
    return value[this.propertyName];

  }
}

export class UpperCaseProcessor {
  constructor(propertyName) {
    this.propertyName = propertyName;
  }

  process(entry) {
    let value = entry.value;
    value[this.propertyName] = upperCase(value[this.propertyName])

    entry.setValue(value);
    return value[this.propertyName];
  }
}