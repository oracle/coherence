/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance.psr;

import java.util.Random;

public class RandomRange
    {

    private static Random wheel = new Random();
    private int r_start = 1;
    private int r_end = 10;
    static final String ACL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static final String ZipL = "0123456789";

    public RandomRange()
        {
        }

    public RandomRange(int rStart, int rEnd)
        {
        r_start = rStart;
        r_end = rEnd;
        }

    public int getRandomSize()
        {
        /* generate random integers in the specified range. nextDouble returns values between [0,1] inclusive */
        int randomSize = r_start + (int) ((r_end - r_start) * wheel.nextDouble());

        return (randomSize);
        }

    public int getRandomSize(int range)
        {

        int randomSize = (int) (range * wheel.nextDouble());

        return (randomSize);
        }

    String randomPercntString(String org, int p)
        {
        int len = org.length();
        StringBuilder sb = new StringBuilder(len);
        int perc = len * p / 100;
        for (int i = 0; i < perc; i++)
            {
            sb.append(ACL.charAt(wheel.nextInt(ACL.length())));
            }
        sb.append(org.substring(perc));
        return sb.toString();
        }

    String random_Loc_PercntString(String org, int p)
        {
        int len = org.length();
        int perc = len * p / 100;
        StringBuilder sb = new StringBuilder(len);
        //  print("Length to replace: "+perc);
        for (int i = 0; i < perc; i++)
            {
            sb.append(ACL.charAt(wheel.nextInt(ACL.length())));
            }
        int randomLocation = getRandomSize(org.length() - perc);
        //   print("Random Location..." + randomLocation);    print(" replace: '" + org.subSequence(randomLocation, randomLocation + perc) + "' with: '" + sb.subSequence(0, perc) + "'");
        return org.replace(org.subSequence(randomLocation, randomLocation + perc), sb.subSequence(0, perc));
        }

    String randomString(int size)
        {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++)
            {
            sb.append(ACL.charAt(wheel.nextInt(ACL.length())));
            }
        //  print(sb.toString());
        return sb.toString();
        }


    String randomZip(int len, int p)
        {
        StringBuilder sb = new StringBuilder(len);
        p = len * p / 100;
        for (int i = 0; i < p; i++)
            {
            sb.append(ZipL.charAt(wheel.nextInt(ZipL.length())));
            }
        char ca[] = new char[len - p];
        sb.append(String.copyValueOf(ca));
        return sb.toString();
        }
    }
