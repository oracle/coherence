/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import java.util.concurrent.TimeUnit;

public class SleepingApplication
{
    /**
     * Entry Point of the Application.
     *
     * @param arguments the arguments
     *
     * @throws InterruptedException  when the sleeping was interrupted
     */
    public static void main(String[] arguments) throws InterruptedException
    {
        System.out.printf("%s started\n", SleepingApplication.class.getName());

        System.out.printf("Using java.home: %s\n", System.getProperty("java.home"));

        // determine the number of seconds to sleep
        int secondsToSleep = 30;

        if (arguments.length == 1)
        {
            try
            {
                secondsToSleep = Integer.parseInt(arguments[0]);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Argument [" + arguments[0]
                                   + "] is not a number representing seconds, defaulting to 5 seconds");
                secondsToSleep = 5;
            }
        }

        System.out.println("Now sleeping for " + secondsToSleep + " seconds");

        Thread.sleep(TimeUnit.SECONDS.toMillis(secondsToSleep));

        System.out.println("Finished sleeping... now terminating");
    }
}
