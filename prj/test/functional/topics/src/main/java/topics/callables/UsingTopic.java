/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.util.function.Remote;

public class UsingTopic<R>
        implements RemoteCallable<R>
    {
    public UsingTopic(String sTopic, Remote.Function<NamedTopic<?>, R> callable)
        {
        f_sTopic   = sTopic;
        f_callable = callable;
        }

    @Override
    public R call()
        {
        Session       session = Coherence.getInstance().getSession();
        NamedTopic<?> topic   = session.getTopic(f_sTopic);
        return f_callable.apply(topic);
        }

    // ----- data members ---------------------------------------------------

    private final String f_sTopic;

    private final Remote.Function<NamedTopic<?>, R> f_callable;
    }
