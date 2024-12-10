/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.docker;

import com.oracle.bedrock.runtime.ApplicationConsole;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;

import java.util.Objects;

/**
 * A Testcontainers log consumer to write logs to a Bedrock {@link ApplicationConsole}.
 */
public class ConsoleLogConsumer
        extends BaseConsumer<ConsoleLogConsumer>
    {
    public ConsoleLogConsumer(ApplicationConsole console)
        {
        m_console = Objects.requireNonNull(console);
        }

    @Override
    public void accept(OutputFrame outputFrame)
        {
        switch (outputFrame.getType())
            {
            case STDOUT:
                m_console.getOutputWriter().print(outputFrame.getUtf8String());
                break;
            case STDERR:
                m_console.getErrorWriter().print(outputFrame.getUtf8String());
                break;
            }
        }

    private final ApplicationConsole m_console;
    }
