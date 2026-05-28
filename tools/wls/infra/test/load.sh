#!/bin/sh
# Copyright 2016 BEA Systems, Inc.
#
# Load the mysql rjob database, this is run from rbt.sh in the -g central RQ script
#

: ${DEV_ROOT:?} ${SRC:?}

. $DEV_ROOT/tools/wls/infra/infraenv.sh

: ${RQSITE:?}

(
    flock -n 9 || exit 1
    cd $DEV_ROOT/tools/wls/infra/test
    sleep 60 &
    # create a new log each day
    ./load_rjob.sh >> load_rjob_`date +%Y-%m-%d`.log 2>&1
    # delete rjob log files older than a week
    find . -name 'load_rjob_*.log' -mtime +7 -delete
    # wait for sleep to complete, if load_rjob took as long as the sleep immediately load again
    wait
) 9> $RQSITE/$SRC/load.lock
