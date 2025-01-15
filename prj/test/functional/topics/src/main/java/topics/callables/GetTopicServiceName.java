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

public class GetTopicServiceName
        implements RemoteCallable<String>
    {
    public GetTopicServiceName(String sName)
        {
        f_sName = sName;
        }

    @Override
    public String call() throws Exception
        {
        Logger.info("Entered GetTopicServiceName callable. Ensuring topic " + f_sName);
        Coherence coherence = Coherence.getInstance();
        coherence.startAndWait();

        NamedTopic<?> topic        = coherence.getSession().getTopic(f_sName);
        String        sServiceName = topic.getService().getInfo().getServiceName();
        Logger.info("Leaving GetTopicServiceName callable. Ensured topic " + f_sName + " service " + sServiceName);
        return sServiceName;
        }

    private final String f_sName;
    }
