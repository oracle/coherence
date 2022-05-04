/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package lambda;

/**
 * RemoteFunctionTests with POF serialization and and misconfiguration of static lambdas enabled in server and
 * dynamic lambdas enabled in client.
 * Test validates dynamic lambda sent by client is rejected by server.
 *
 * @author jf  2020.06.02
 */
public class RemoteFunctionPofDynamicLambdaDisabledOnServerTests
    extends AbstractRemoteFunctionTests
    {
    public RemoteFunctionPofDynamicLambdaDisabledOnServerTests()
        {
        super(/*fPof*/true, /*fServerDisableDynamicLambdas*/true, /*fClientDisableDynamicLambdas*/false);
        }
    }
