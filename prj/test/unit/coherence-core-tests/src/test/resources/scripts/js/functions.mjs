/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

export class Echo {
  apply(arg) {
    return arg
  }
}

export class Ping {
  call() {
    return "pong"
  }
}
