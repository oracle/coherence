/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import java.io.Serializable;

public class NonPofPoint implements Serializable, Comparable<NonPofPoint>  {

        // ----- constructors -----------------------------------------------

        /**
         * Constructs a point
         *
         * @param x x
         * @param y y
         */
        public NonPofPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Method description
         *
         * @return string
         */
        public String toString() {
            return "NonPofPoint[x=" + x + ",y=" + y + "]";
        }

        /**
         * Method description
         *
         * @param point a point
         * @return 0 iff if this is same as point, otherwise return negative or postitive comparative value
         */
        @Override
        public int compareTo(NonPofPoint point) {
            if ((this.x == point.x) && (this.y == point.y)) {
                return 0;
            } else if (this.x != point.x) {
                return this.x - point.x;
            } else {
                return this.y - point.y;
            }
        }

        // ----- data members -----------------------------------------------
        final public int x;
        final public int y;

}