#!/bin/sh
# Copyright 2006 BEA Systems, Inc.

cd `dirname $0`

p4 sync //dev/main/tools/wls/infra/build/restart.sh

./at.sh ../build/restart.sh "$@"

# sleep for 5 minutes for the restart to kill us, this is so that if this
# script is run via the queue if won't dequeue the next entry before
# the restart happens
sleep 300
