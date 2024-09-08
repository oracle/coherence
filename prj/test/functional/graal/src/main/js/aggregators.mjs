/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
export class AgeAdder {

  constructor() {
    this.sum = 0;
  }

  accumulate(entry) {
    this.sum += entry.value.age;
    return true;
  }

  combine(partialResult) {
    this.sum += partialResult;
    return true;
  }

  getPartialResult() {
    return this.sum;
  }

  finalizeResult() {
    return this.sum;
  }
}

export class MinMaxAgeFinder {

  constructor() {
    this.min = 909090;
    this.max =  -909090;
  }

  accumulate(entry) {
    if (entry.value.age < this.min) {
      this.min = entry.value.age;
    }
    if (entry.value.age > this.max) {
      this.max = entry.value.age;
    }

    return true;
  }

  combine(partialResult) {
    let p = JSON.parse(partialResult);

    if (p.min < this.min) {
      this.min = p.min;
    }
    if (p.max > this.max) {
      this.max = p.max;
    }
    return true;
  }

  getPartialResult() {
    return JSON.stringify({min: this.min, max: this.max});
  }

  finalizeResult() {
    return {min: this.min, max: this.max}
  }
}

export class DummyAggregator {

  constructor(argOne, argTwo) {
    this.min = argOne;
    this.max =  argTwo;
  }

  accumulate(entry) {
    return true;
  }

  combine(partialResult) {
    return true;
  }

  getPartialResult() {
    return JSON.stringify({min: this.min, max: this.max});
  }

  finalizeResult() {
    return {min: this.min, max: this.max}
  }
}