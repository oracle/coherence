/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;
import com.tangosol.net.topic.NamedTopicListener;
import com.tangosol.util.SynchronousListener;

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
        Logger.info("Entered DestroyTopic.call() topic=" + f_sTopic);
        Session       session    = Coherence.getInstance().getSession();
        NamedTopic<?> topic      = session.getTopic(f_sTopic);
        Listener      listener   = new Listener();
        Logger.info("In DestroyTopic.call() ensured topic=" + f_sTopic);

        topic.addListener(listener);
        Logger.info("In DestroyTopic.call() added topic listener topic=" + f_sTopic);

        assertThat(topic.isActive(), is(true));
        Logger.info("In DestroyTopic.call() topic is active topic=" + f_sTopic);
        assertThat(topic.isDestroyed(), is(false));
        Logger.info("In DestroyTopic.call() topic is not destroyed topic=" + f_sTopic);
        Logger.info("In DestroyTopic.call() destroying topic topic=" + f_sTopic);
        session.destroy(topic);
        Logger.info("In DestroyTopic.call() destroyed topic topic=" + f_sTopic);
        assertThat(topic.isActive(), is(false));
        Logger.info("In DestroyTopic.call() topic is not active topic=" + f_sTopic);
        assertThat(topic.isDestroyed(), is(true));
        Logger.info("In DestroyTopic.call() topic is destroyed topic=" + f_sTopic);
        assertThat(listener.isDestroyed(), is(true));
        Logger.info("Exiting DestroyTopic.call() topic=" + f_sTopic);
        return true;
        }

    @SuppressWarnings("rawtypes")
    private static class Listener
            implements NamedTopicListener, SynchronousListener
        {
        @Override
        public void onEvent(NamedTopicEvent evt)
            {
            if (evt.getType() == NamedTopicEvent.Type.Destroyed)
                {
                f_fDestroyed.set(true);
                }
            }

        public boolean isDestroyed()
            {
            return f_fDestroyed.get();
            }

        // ----- data members ---------------------------------------------------

        private final AtomicBoolean f_fDestroyed = new AtomicBoolean(false);
        }

    // ----- data members ---------------------------------------------------

    private final String f_sTopic;
    }
