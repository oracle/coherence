/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import com.oracle.coherence.common.util.SafeClock;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by falcom on 3/4/15.
 */
public class TimeoutTests
    {
    @Test
    public void testTimeout()
         {
         Object o = new Object();
         long ldtStart;
         long cMillis;

         synchronized (o)
             {
             ldtStart = SafeClock.INSTANCE.getSafeTimeMillis();
             try(Timeout t = Timeout.after(10))
                 {
                 Blocking.wait(o);
                 }
             catch (InterruptedException vex) {}

             cMillis = SafeClock.INSTANCE.getSafeTimeMillis() - ldtStart;
             assertTrue(Long.toString(cMillis),cMillis > 5 && cMillis < 100);

             ldtStart = SafeClock.INSTANCE.getSafeTimeMillis();
             try(Timeout t = Timeout.after(100))
                 {
                 Blocking.wait(o);
                 }
             catch (InterruptedException vex) {}

             cMillis = SafeClock.INSTANCE.getSafeTimeMillis() - ldtStart;
             assertTrue(Long.toString(cMillis),cMillis > 50 && cMillis < 200);

             ldtStart = SafeClock.INSTANCE.getSafeTimeMillis();
             try(Timeout t = Timeout.after(1000))
                 {
                 Blocking.wait(o);
                 }
             catch (InterruptedException vex) {}

             cMillis = SafeClock.INSTANCE.getSafeTimeMillis() - ldtStart;
             assertTrue(Long.toString(cMillis),cMillis > 900 && cMillis < 1100);

             long ldtOuterStart = SafeClock.INSTANCE.getSafeTimeMillis();
             try(Timeout to = Timeout.after(2000))
                 {
                 ldtStart = SafeClock.INSTANCE.getSafeTimeMillis();
                 try(Timeout t = Timeout.after(10))
                     {
                     Blocking.wait(o, 100);
                     }
                 catch (InterruptedException vex) {}

                 cMillis = SafeClock.INSTANCE.getSafeTimeMillis() - ldtStart;
                 assertTrue(Long.toString(cMillis),cMillis > 5 && cMillis < 50);

                 ldtStart = SafeClock.INSTANCE.getSafeTimeMillis();
                 try(Timeout t = Timeout.after(100))
                     {
                     Blocking.wait(o, 1000);
                     }
                 catch (InterruptedException vex) {}

                 cMillis = SafeClock.INSTANCE.getSafeTimeMillis() - ldtStart;
                 assertTrue(Long.toString(cMillis),cMillis > 50 && cMillis < 200);

                 ldtStart = SafeClock.INSTANCE.getSafeTimeMillis();
                 try(Timeout t = Timeout.after(1000))
                     {
                     Blocking.wait(o, 10000);
                     }
                 catch (InterruptedException vex) {}

                 cMillis = SafeClock.INSTANCE.getSafeTimeMillis() - ldtStart;
                 assertTrue(Long.toString(cMillis),cMillis > 900 && cMillis < 1100);

                 try(Timeout t = Timeout.after(5000)) // try to extend timeout
                     {
                     Blocking.wait(o); // remainder of the 2s timeout
                     }
                 catch (InterruptedException vex) {}
                 }
             catch (InterruptedException vex) {}

             cMillis = SafeClock.INSTANCE.getSafeTimeMillis() - ldtOuterStart;
             assertTrue(Long.toString(cMillis),cMillis > 1900 && cMillis < 2100);
             }
         }
    }
