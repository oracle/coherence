/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.remote;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.coherence.component.net.extend.RemoteNamedTopic;
import com.tangosol.coherence.component.util.SafeNamedTopic;
import com.tangosol.internal.net.SessionNamedTopic;
import com.tangosol.internal.net.topic.NamedTopicView;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.util.function.Remote;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EnsureTopic
        implements RemoteCallable<Boolean>
    {
    public EnsureTopic(String sTopic)
        {
        this(sTopic, null);
        }

    public EnsureTopic(String sTopic, Remote.Function<NamedTopic<?>, Boolean> callable)
        {
        f_sTopic   = sTopic;
        f_callable = callable;
        }

    @Override
    public Boolean call()
        {
        Session       session = Coherence.getInstance().getSession();
        NamedTopic<?> topic   = unwrap(session.getTopic(f_sTopic));
        assertThat(topic.getName(), is(f_sTopic));

        //assertThat(topic, is(instanceOf(NamedTopicView.class)));

        return f_callable == null || f_callable.apply(topic);
        }

    // ----- helper methods -------------------------------------------------

    public static NamedTopic<?> unwrap(NamedTopic<?> topic)
        {
        if (topic instanceof SessionNamedTopic<?>)
            {
            return unwrap(((SessionNamedTopic<?>) topic).getInternalNamedTopic());
            }
        if (topic instanceof SafeNamedTopic<?>)
            {
            return unwrap(((SafeNamedTopic<?>) topic).getNamedTopic());
            }
        return topic;
        }

    // ----- data members ---------------------------------------------------

    private final String f_sTopic;

    private final Remote.Function<NamedTopic<?>, Boolean> f_callable;
    }
