/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package lambda;

/**
 * RemoteFunctionTests to ensure dynamic lambda serialization functions with Pof
 *
 * @author jf  2020.06.02
 */
public class RemoteFunctionPofDynamicLambdaTests
    extends AbstractRemoteFunctionTests
    {
    public RemoteFunctionPofDynamicLambdaTests()
        {
        super(/*fPof*/true, /*fServerDisableDynamicLambdas*/false, /*fClientDisableDynamicLambdas*/false);
        }
    }
