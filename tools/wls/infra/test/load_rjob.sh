#!/bin/bash
# Copyright 2006 BEA Systems, Inc.

: ${SRC:?branch name}

BASE=`basename $0`_coherence.${NEW_SRC/\//_}
DATAROOT=${RQSITE:?}/$SRC
COMMONROOT=${RQSITE:?}/common

tmpfile=/tmp/$BASE.tmp_$$
touchfile=/tmp/$BASE.touch
newtouchfile=/tmp/$BASE.touch.new

# create a touch file based on the hour, the algorithm is a
# such... once an hour reprocess all the files modified in that hour.
# I've noticed files getting created older than the touch file
# presumably because of date/time skews between all the machines.
# Hopefully reprocessing the one hour old files once an hour will
# correct most of these problems.
hourtouchfile=/tmp/$BASE.touch.hour-`date +%Y%m%d%H`
# grab the most recent hour touch file (if it exists)
lasthourtouchfile="`\ls -t /tmp/$BASE.touch.hour-* | head -1`"

if [ -f "$hourtouchfile" -o ! -f "$lasthourtouchfile" ]; then
    if [ -f "$touchfile" ]; then
	find_opt="-newer $touchfile"
    else
	find_opt=""
    fi
else
    find_opt="-newer $lasthourtouchfile"
fi

cd ${DATAROOT:?}

ROOT=$DEV_ROOT/tools/wls/infra/test

before="INFO: `date`: begin load"

# create the hour touch file if it does *not* exist already
[ -f "$hourtouchfile" ] || touch "$hourtouchfile"
touch "$newtouchfile"
(
    find queued status $find_opt -name "job.*" -a ! -name "job.*.owner" | while read i; do
	[ -f "$i" ] || continue
	tr -d '\r\000' < "$i" | awk $AWKOPTS -v id="$i" -v RQSITE="${RQSITE:?}" -v BRANCH=coherence/$SRC -f $ROOT/load_rjob.awk || (echo 1>&2 "exiting job loading on $i..." ; exit)
    done

    find status $find_opt -name "machine.*.*" | sed 's|.*\/machine\.[^\.]*\.||' | \
	sort -u | while read i; do
	# cat'ing machine.[0-9] prevents machine..NAME issues
	cat status/machine.[0-9]*.$i | tr -d '\r\000' | awk -v MACHINE=$i -v BRANCH=coherence/$SRC -f $ROOT/load_mstatus.awk || (echo 1>&2 "exiting machine loading on $i..." ; exit )
    done
    
    find queued -name "kill.job.*" | sed 's|.*\/kill\.||' | while read i; do
	[ "$i" != "" ] && echo "delete from rjob where branch = \"coherence/$SRC\" and id = \"$i\";"
    done

    find remotetests/requeued $find_opt -name "job.*" | sed 's|.*\/||' | while read i; do
	[ "$i" != "" ] && echo "insert ignore into requeued set id = \"$i\", branch=\"coherence/$SRC\";"
    done

    find available -type f | sed 's|[^/]*/||' | while read i; do
	tr -d '\r\000' < available/$i | awk -F= -v MACHINE=$i -f $ROOT/load_machine.awk
    done
    
    cd $COMMONROOT

    find available -type f | sed 's|[^/]*/||' | while read i; do
	tr -d '\r\000' < available/$i | grep "^SRC=coherence/$SRC$" >/dev/null
        # sometimes the file goes away between the time the find finds the file and when this
        # loop can process it so do a last check to make sure it exists
        if [ -s available/$i ]; then
            tr -d '\r\000' < available/$i | grep "^SRC=coherence/$SRC$" >/dev/null
            if [ $? -eq 0 ]; then
                tr -d '\r\000' < available/$i | awk -F= -v MACHINE=$i -f $ROOT/load_machine.awk
            fi
        fi
    done

) > $tmpfile

if [ $? -eq 0 -a -s $tmpfile ]; then
    loaded=`grep ^replace < $tmpfile | wc -l`
    deleted=`grep ^delete < $tmpfile | wc -l`
    # This assume there is a ~/.my.cnf, that looks something like this
    # ---- cut here ----
    # [client]
    # host = tamarac.us.oracle.com
    # user = bt
    # password = xxxxx
    # database = infra_data
    # ---- cut here ----
    # remove stuff before #'s and add the real password
    mysql --compress < $tmpfile
    if [ $? -eq 0 ]; then
	mv -f "$newtouchfile" "$touchfile"
	# remove old hour based touch files
	\ls -t /tmp/$BASE.touch.hour-* | tail -n +2 | xargs -r rm
    fi
    if [ $loaded -gt 0 ]; then
	echo "$before"
	echo INFO: "`date`": loaded $loaded record"(s)"
    fi
    if [ $deleted -gt 0 ]; then
	echo INFO: "`date`": deleted $deleted record"(s)"
    fi
fi

rm -f "$tmpfile"

# archive all job.* and machine.* files older than a week, this keeps the
# find commands above fast.  When the directories on the "night" filer have
# more than 20,000 files or so in them the "find" commands really slow down

mkdir -p archive.status
find status -type f -mtime +7 | xargs -i mv '{}' archive.status
