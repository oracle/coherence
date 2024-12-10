/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package reporter;


import java.util.Date;


/**
 * TODO: Fill in class description.
 *
 * @author everettwilliams May 14, 2008
 */
public interface DeltaDataMBean
    {
    public String getString();
    public int getInt();
    public long getLong();
    public Date getDate();
    public String[] getStringArray();
    public int[] getIntArray();
    public long[] getLongArray();
    public double getDouble();
    public double[] getDoubleArray();
    public boolean getBool();
    }