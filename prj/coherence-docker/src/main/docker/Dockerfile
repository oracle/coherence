#
# Copyright (c) 2000, 2020, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# http://oss.oracle.com/licenses/upl.
#

FROM oracle/graalvm-ce:19.0.0

ENV PATH=$PATH:/usr/java/default/bin \
    CONFIG_JVM_ARGS="-Djava.security.egd=file:/dev/./urandom" \
    COHERENCE_HOME=/u01/coherence \
    DEPENDENCY_MODULES=/u01/coherence/thirdparty/lib

COPY start.sh                           /start.sh
COPY extend-cache-config.xml            $COHERENCE_HOME/conf/extend-cache-config.xml
COPY logging.properties                 $COHERENCE_HOME/conf/logging.properties
COPY coherence/                         $COHERENCE_HOME/
COPY coherence/thirdparty/lib/          $DEPENDENCY_MODULES/

RUN mkdir -p /logs && \
    adduser oracle && \
    chown oracle -R /u01 && \
    chgrp -R 0 /u01 && \
    chmod -R g=u /u01 && \
    chown oracle -R /logs && \
    chgrp -R 0 /logs && \
    chmod -R g=u /logs

USER oracle

ENTRYPOINT ["/start.sh"]

