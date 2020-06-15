#
# Copyright (c) 2000, 2020, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# http://oss.oracle.com/licenses/upl.
#

FROM openjdk:11-jre-slim

ENV COHERENCE_HOME=/coherence \
    DEPENDENCY_MODULES=/coherence/thirdparty/lib

COPY start.sh                 /start.sh
COPY extend-cache-config.xml  $COHERENCE_HOME/conf/extend-cache-config.xml
COPY coherence/               $COHERENCE_HOME/

ENTRYPOINT ["/start.sh"]

