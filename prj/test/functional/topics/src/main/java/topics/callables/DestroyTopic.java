/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DestroyTopic
        implements RemoteCallable<Boolean>
    {
    public DestroyTopic(String sTopic)
        {
        f_sTopic = sTopic;
        }

    @Override
    public Boolean call()
        {
        Session       session    = Coherence.getInstance().getSession();
        NamedTopic<?> topic      = session.getTopic(f_sTopic);
        AtomicBoolean fDestroyed = new AtomicBoolean(false);

        topic.addListener(evt ->
            {
            if (evt.getType() == NamedTopicEvent.Type.Destroyed)
                {
                fDestroyed.set(true);
                }
            });

        assertThat(topic.isActive(), is(true));
        assertThat(topic.isDestroyed(), is(false));
        session.destroy(topic);
        assertThat(topic.isActive(), is(false));
        assertThat(topic.isDestroyed(), is(true));
        // ToDo: Fails....
        assertThat(fDestroyed.get(), is(true));
        return true;
        }

    // ----- data members ---------------------------------------------------

    private final String f_sTopic;
    }
