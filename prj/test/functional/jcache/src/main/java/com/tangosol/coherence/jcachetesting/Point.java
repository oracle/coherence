/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.Serializable;

public class Point implements Comparable<Point>, PortableObject, Serializable {

    // ----- constructors -----------------------------------------------

    /**
     * Portable Object requires this
     */
    public Point()
    {
        this.x = 0;
        this.y = 0;
    }

    /**
     * Constructs a point
     *
     * @param x x
     * @param y y
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Method description
     *
     * @return string
     */
    public String toString() {
        return "Point[x=" + x + ",y=" + y + "]";
    }

    /**
     * Method description
     *
     * @param point a point
     * @return 0 iff if this is same as point, otherwise return negative or postitive comparative value
     */
    @Override
    public int compareTo(Point point) {
        if ((this.x == point.x) && (this.y == point.y)) {
            return 0;
        } else if (this.x != point.x) {
            return this.x - point.x;
        } else {
            return this.y - point.y;
        }
    }

    @Override
    public boolean equals(Object point)
    {
        return point instanceof Point ? compareTo((Point)point) == 0 : false;

    }

    // ----- PortableObject interface -----------------------------------

    @Override
    public void readExternal(PofReader pofReader) throws IOException {
        this.x = pofReader.readInt(0);
        this.y = pofReader.readInt(1);
    }

    @Override
    public void writeExternal(PofWriter pofWriter) throws IOException {
        pofWriter.writeInt(0, x);
        pofWriter.writeInt(1, y);
    }


    // ----- data members -----------------------------------------------
    public int x;
    public int y;
}