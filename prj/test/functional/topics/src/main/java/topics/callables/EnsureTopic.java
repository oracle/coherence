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
import com.tangosol.net.topic.NamedTopic;

public class EnsureTopic
        implements RemoteCallable<Boolean>
    {
    public EnsureTopic(String sName)
        {
        this(sName, null);
        }

    public EnsureTopic(String sName, String sGroup)
        {
        f_sName  = sName;
        f_sGroup = sGroup;
        }

    @Override
    public Boolean call() throws Exception
        {
        Logger.info("In EnsureTopic callable. Ensuring topic " + f_sName);
        Coherence coherence = Coherence.getInstance();
        coherence.startAndWait();

        NamedTopic<?> topic    = coherence.getSession().getTopic(f_sName);
        if (f_sGroup != null && !f_sGroup.isEmpty())
            {
            Logger.info("In EnsureTopic callable. Ensuring subscriber group " + f_sGroup + " for topic " + f_sName);
            topic.ensureSubscriberGroup(f_sGroup);
            }
        Logger.info("In EnsureTopic callable. Ensured topic " + f_sName);
        return true;
        }

    private final String f_sName;

    private final String f_sGroup;
    }
