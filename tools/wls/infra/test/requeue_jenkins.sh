#!/bin/sh
# Copyright 2016 BEA Systems, Inc.
#
# check for jobs where the jenkins build FAILED or was ABORTED and re-queue as necessary. works similar to load.sh
#

: ${DEV_ROOT:?} ${SRC:?}

. $DEV_ROOT/tools/wls/infra/infraenv.sh

: ${RQSITE:?}

(
    flock -n 9 || exit 1
    . $DEV_ROOT/tools/wls/infra/build/rbt.sh
    cd $DEV_ROOT/tools/wls/infra/test
    tmpfile=$(mktemp)
    sleep 120 & # <---  this is the minimum time between checks, it will be however long it takes to run the code below or the time of this sleep whatever is longer                                    
    find $RQSITE/$SRC/status -maxdepth 1 -name 'job.9.2*' ! -name 'job.*.owner' -type f -mtime -1 | while read i; do
        JOBID=$(basename "$i")
	unset QUEUE_ID # so that it does not get reused from a previous iteration
	# only look for the build number if "moving to jenkins" has been in the file for at least one minute... "jenkins_queuedid" only looks for it for 15 seconds
	if [ -n "$(find $i -maxdepth 0 -mmin +1)" ]; then
            tail -1 "$i" | grep ': moving to jenkins' | grep queueid= > $tmpfile
            if [ $? -eq 0 ]; then
		QUEUE_ID=$(awk < $tmpfile -F= '{print $NF}')
		# protect vars used in the function from overwriting the ones here (Ex. "i")
		(
                    jenkins_queueid "$JOBID" "$QUEUE_ID"
		)
	    fi
	fi
	# this will either pick up build numbers that were just inserted above or ones that have been in there for several minutes
        tail -1 "$i" | grep ' jenkins: ' | grep /job/ > $tmpfile
        if [ $? -eq 0 ]; then
            URL=$(awk '{print $NF}' < $tmpfile)
            curl --fail --noproxy '*' --fail --silent --show-error "${URL:?}api/json?pretty=true" | grep '"result"' | grep -e ': "FAILURE"' -e ': "ABORTED"'
            if [ $? -eq 0 ]; then
                rqstatus -j $JOBID "$URL failed or aborted requeuing"
                mv $i $RQSITE/$SRC/queued/
            fi
        fi
	# if the file is more than 15 minutes old then check to see if moving to jenkins is *still* the last line, if so requeue
	if [ -n "$(find $i -maxdepth 0 -mmin +15)" ]; then
            tail -1 "$i" | grep ': moving to jenkins' > /dev/null
            if [ $? -eq 0 ]; then
                rqstatus -j $JOBID "lost queueid $QUEUE_ID, requeuing"
                mv $i $RQSITE/$SRC/queued/
	    fi
	fi
    done >> requeue_jenkins_`date +%Y-%m-%d`.log 2>&1
    rm -f $tmpfile

    # wait for sleep to complete, if the find took as long as the sleep immediately find again                                                                                                          
    wait
) 9> $RQSITE/$SRC/requeue_jenkins.lock


