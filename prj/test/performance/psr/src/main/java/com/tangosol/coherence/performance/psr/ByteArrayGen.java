/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance.psr;

import java.util.Arrays;
import java.util.Random; 


public class ByteArrayGen {
  private static Random wheel = new Random();
    public ByteArrayGen() {
        super();
    }

    public static void main(String[] args) {
        RandomRange rr = new RandomRange();
      byte [] baa = new byte[10];
        for(int i=0; i<10; i++) {System.out.println(rr.getRandomSize(10000000)); 
              Arrays.fill(baa, (byte)rr.getRandomSize(10000));//
              System.out.println(" after "+Arrays.toString(baa));
            }
        ByteArrayGen byteArrayGen = new ByteArrayGen();
        int size = 1500;
      byte [] ba = new byte[size];
            wheel.nextBytes(ba);
      System.out.println(ba.length);
   //   System.out.println("\n "+Arrays.toString(ba));
      byte b[]= Arrays.copyOf(ba,size ); 
      System.out.println(" befor "+Arrays.toString(b));
      byteArrayGen.replace(b, 1);
      System.out.println(" after "+Arrays.toString(b));
      System.out.println(" length = "+b.length);

    }
    
    
  public byte [] replace ( byte[] sourceArray, int percentag_size){
  int length = sourceArray.length*percentag_size/100;
        System.out.println(" \n length of ("+percentag_size+")% = "+ length);
    byte [] tempArray = new byte[length];
    wheel.nextBytes(tempArray);
    System.arraycopy(tempArray, 0, sourceArray, 0, length);
                 return sourceArray;
  }
}
