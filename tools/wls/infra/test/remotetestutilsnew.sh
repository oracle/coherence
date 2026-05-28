#!/bin/sh
# Copyright 2006 BEA Systems, Inc.
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required

stopvscan() {
#    ant -f ${DEV_ROOT:?}/wls/build.xml stopvscan >/dev/null
    true
}

startvscan() {
#    ant -f ${DEV_ROOT:?}/wls/build.xml startvscan >/dev/null
    true
}

# kill any active console remote desktop (terminal server) sessions, they
# cause problems with gui tests
kill_console_rdp() {
    # don't mess with sessions like ">console" but any session of the
    # form ">***" that isn't the console then reconnect it with the console
    # use columns 40 thru 48 of the qwinsta output because that is the
    # session id (there isn't always data in each column so $3 is unreliable)
    session="`qwinsta 2>/dev/null | awk '/console/{next}/^>/{print sub(40,48); next}'`"
    if [ -n "$session" ]; then
	infomsg "somebody is connected to the console of this machine using remote desktop"
	infomsg "which can break GUI tests, disconnecting that session."
	tscon "$session" /dest:console
    fi
}

ftp_list() {
    : ${1:? relative publish path}
    (
	cd ${TMPDIR:?}
        cd $RQSITE/"$1" && find * -type f
    )
}

ftp_xfer_file() {
    : ${1:? relative publish path} ${2:?file to transfer}
    tmptar=${TMPDIR:?}/file$PPID.tar
    (
	cd `dirname "$2"`
	tar -c -f "$tmptar" `basename "$2"`
    )
    ftp_xfer "$1" "$tmptar"
}

ftp_xfer() {
    : ${1:?relative publish path} ${2:?local tar file}
    cat "$2" | ( mkdir -p $RQSITE/"$1" ; cd $RQSITE/"$1" && tar xf - && chmod -R a+rx . )
    rm "$2"
}

publish() {
    : ${LOGDIR:?} ${1:?}
    thetar=${TMPDIR:?}/publish.tar
    case `uname` in
        CYGWIN_NT*)
           tar -c -f $thetar -T /dev/null
           ;;
        *)
           cat /dev/null > $thetar
           ;;
    esac
    # don't store the local dir for the file(s)
    for i in "$@"; do
	(
	    cd "`dirname $i`"
	    tar -r -f $thetar "`basename $i`"
	    saved_tar=$?
	    if [ $saved_tar -ne 0 ]; then
		infomsg "tar command return $saved_tar"
	    fi
	)
    done
    ftp_xfer $LOGDIR $thetar
}

publish_build() {
    : ${1:?} ${LOGDIR:?}
    (
    publish "$@" &
    debugmsg "done publish_build"
    ) 2>&1 | grep -v ': File name too long$'
}

publish_drt() {
    : ${1:?} ${DEV_ROOT:?} ${LOGDIR:?}
    (
    drttar=${TMPDIR:?}/drttar.tar
    cat /dev/null > $drttar
    debugmsg "publish_drt: tar'ing log/out/debug/xml/html/txt in the test directory"
    cd $DEV_ROOT/prj/test
    find * -type f -a -newer ${TMPDIR:?}/publish.touchfile -a \( -name "*.xml" -o -name "*.txt" -o -name "*.html" -o -name "*.log*" -o -name "*.out" -o -name "*.debug" -o -name junit-noframes.html -o -name screenshot.jpg -o -name "jrockit.*.mdmp" -o -name "jrockit.*.dump" -o -name "*.err" -o \
	 -name "*.dump" -o \
	 -name "*.dumpstream" \
	 \) | while read i; do tar -r -f $drttar "$i"; done
    # ftp in parallel with the next tar
    debugmsg "publish_drt: ftp'ing $drttar to filer"
    ftp_xfer $LOGDIR $drttar &
    wait
    debugmsg "done publish_drt"
    ) 2>&1 | grep -v ': File name too long$'
}

# this function is used to publish files under special dirs for specified drt.
publish_special() {
	(
	cd $1
	debugmsg "publish_special: tar'ing files/dirs in the spicified dir ($1)"
	drtsltar=${TMPDIR:?}/drtsltar.tar
	cat /dev/null > $drtsltar
	case $2 in
	    wsee_wlhome)
	        fsize=`du -k test/wsee/server/stage/myserverVerbose.out | awk '{print $1}'`
	        if [ $fsize -gt "160000" ]; then
	        # 150M=153600K, so make it a little more than 150M to let split has redundancy
	            head -c 150m test/wsee/server/stage/myserverVerbose.out > test/wsee/server/stage/tidy_myserverVerbose.out
	            rm -f test/wsee/server/stage/myserverVerbose.out
	        fi
	        find test/wsee -type f -a -newer ${TMPDIR:?}/publish.touchfile | while read i; do tar -r -f $drtsltar "$i"; done
	        ;;
	    suc_wsee_wlhome)
	        find test/wsee/server/reports -type f -a -newer ${TMPDIR:?}/publish.touchfile | while read i; do tar -r -f $drtsltar "$i"; done
	        find test/wsee/unit/reports -type f -a -newer ${TMPDIR:?}/publish.touchfile | while read i; do tar -r -f $drtsltar "$i"; done
	        ;;
	    wsmats)
	        find results/wls/unit -type f -a -newer ${TMPDIR:?}/publish.touchfile | while read i; do tar -r -f $drtsltar "$i"; done
	        ;;
	    *)
	        ;;
	esac
	if [ -s $drtsltar ]; then
	    debugmsg "publish_special: ftp'ing $drtsltar to filer"
	    ftp_xfer $LOGDIR/LOG_HOME $drtsltar &
    fi
	wait
	debugmsg "done publish_special"
	) 2>&1 | grep -v ': File name too long$'
}

publish_modules() {
    : ${LOGDIR:?} ${BEA_HOME:?}
    (
    cd $BEA_HOME
    modulestar=${TMPDIR:?}/modulestar.tar
    cat /dev/null > $modulestar
    debugmsg "publish_modules: tar'ing log/out in the BEA_HOME/results ($BEA_HOME/results)"
    find results -type f -a -newer ${TMPDIR:?}/publish.touchfile -a \( -name "*.xml*" -o -name "*.html" -o -name "*.css" \) | while read i; do tar -r -f $modulestar "$i"; done
    debugmsg "publish_modules: tar'ing wlw files under WORKSHOP_HOME"
    if [ -s $modulestar ]; then
	debugmsg "publish_modules: ftp'ing $modulestar to filer"
	ftp_xfer $LOGDIR/BEA_HOME/$1 $modulestar
    fi
    wait
    debugmsg "done publish_modules"
    ) 2>&1 | grep -v ': File name too long$'
}

publish_touchfile() {
    # touch a file that will later be used by publish_cit to pick up -newer .out/.log files
    tmpdir=${TMPDIR:?}
    [ -d "$tmpdir" ] || mkdir "$tmpdir"
    touch "$tmpdir/publish.touchfile"
    unset tmpdir
}

publish_cit() {
    : ${DEV_ROOT:?} ${LOGDIR:?} ${1:?test run target} ${2:?test run dir}
    infomsg "publish $2 $1 result files"
    case "$1" in
	deploy.checkintest|wls.remotedeploycheckintest)	cd $DEV_ROOT/wls/tools ;;
	*)						cd $DEV_ROOT/wls/tools/weblogic/qa/tests ;;
    esac

    echo $2 | grep qa/tests/cts > /dev/null
    if [ $? -eq 0 ]; then
	cd $DEV_ROOT/wls/tools/weblogic/qa/tests/cts5/xml
    fi

    cittar=${TMPDIR:?}/cittar.tar
    cat /dev/null > $cittar
    debugmsg "publish_cit: tar'ing list of logs"
    for i in *.log *.out *.debug *.trc test.xml core jrockit.*.mdmp jrockit.*.dump ctsgtlf.xml config.xml *.err; do
	[ -f "$i" ] || continue
	tar -r -f $cittar "$i"
    done

    case "$1" in
	*webapplib.run*)			  cd $WL_HOME/tools/qa/webapplib ;;
	*webapp.checkintest*)			  cd $WL_HOME/tools/qa/webapp ;;
	*applib.run*)				  cd $WL_HOME/tools/qa/applib ;;
        *)                      		  cd $DEV_ROOT/wls/tools/weblogic/qa/tests/ ;;
    esac
    echo $2 | grep qa/tests/cts > /dev/null
    if [ $? -eq 0 ]; then
        cd $DEV_ROOT/wls/cts/cts5/weblogic/config/mydomain
    fi
    # look in both qa/tests and WLS_TEST_RESULTS for files created by the run
    # done use full paths in the find because we don't want the full paths to
    # be reproduced on the test results web server
    for i in . $WLS_TEST_RESULTS; do
        pwd | grep "qa/tests" > /dev/null
        if [ $? -eq 0 -a "$i" = "." ]; then
            echo "files under qa/tests are build/test generated under $DEV_ROOT/wls/tools/weblogic/qa/tests
others are from $WLS_TEST_RESULTS"
            cd ../..
            searchpath="qa/tests/*/"
        else
	    cd $i
            searchpath="*/"
        fi
	debugmsg "publish_cit: tar'ing stuff newer than touchfile in `pwd`"
	find $searchpath \
	    -newer ${TMPDIR:?}/publish.touchfile -a \( \
                -name "weblogic-domain-backup*.zip" -o \
                -name "*.log" -o \
	        -name "*.out" -o \
	        -name "*.debug" -o \
	        -name "*.html" -o \
	        -name "*.trc" -o \
	        -name "*.xml" -o \
	        -name core -o \
	        -name "jrockit.*.mdmp" -o \
	        -name "jrockit.*.dump" -o \
	        -name "*.dif" -o \
                -name "*.err" \
	    \) | while read i; do tar -r -f $cittar "$i"; done
    done
    ftp_xfer $LOGDIR/"$1" $cittar
    debugmsg "done publish_cit"
}

# note about test.status, this is a local file separate from test.log
# that keeps the run status of the test.  I can't use test.log because
# test_status is called from remotetestrun.sh and test.log is stdout.
# Cygwin sometimes hangs when the process is trying to grep its own
# stdout.  I could use the job id file on $RQSITE but I'd have to get
# it on the net and it includes status from previous runs that and
# reruns when the job was returned to the central queue.

msgurl() {
    : ${1:?}
    if [ "$2" != "" -a -f "${TMPDIR:?}/$JOBID/$2" ]; then
	dir="`echo ${LOGDIR:?} | sed 's|^.*/remotetests/||' `"
	echo "$1 (<a href=\"http://home.us.oracle.com/centralrq/coherence/$SRC/remotetests/$dir/$2\">$2</a>)"
    else
	echo "$1"
    fi
}

infomsg() {
    : ${JOBID:?}
    msg=`msgurl "$1" "$2"`
    echo "INFO: `date`: $msg" 1>&2
    (
	set +e
	rqstatus -j $JOBID "INFO: $msg"
	# in case this is the first status
	mkdir -p ${TMPDIR:?}/$JOBID
	echo "INFO: $msg" >> ${TMPDIR:?}/$JOBID/test.status
        # tag this job with the user, to do performance throttling so this
	# user can not hog the RQ
	rqstatus -j $JOBID -u $REMOTE_P4USER
    )
}

debugmsg() {
    return
    [ "$REMOTE_P4USER" != "herrlich" ] && return
    infomsg "DEBUG: $1"
}

warnmsg() {
    : ${JOBID:?}
    msg=`msgurl "$1" "$2"`
    echo "WARNING: `date`: $msg" 1>&2
    (
	set +e
	rqstatus -j $JOBID "WARNING: $msg"
	# in case this is the first status
	mkdir -p ${TMPDIR:?}/$JOBID
	echo "WARNING: $msg" >> ${TMPDIR:?}/$JOBID/test.status
    )
}

fatalmsg() {
    fatalmsg_noexit "$@"
    safe_revert
    # clear the username associated with this job
    rqstatus -j $JOBID -u
    exit 1
}

fatalmsg_noexit() {
    : ${JOBID:?}
    msg=`msgurl "$1" "$2"`
    echo "FATAL: `date`: $msg" 1>&2
    (
	set +e
	rqstatus -j $JOBID "FATAL: $msg"
	# in case this is the first status
	mkdir -p ${TMPDIR:?}/$JOBID
	echo "FATAL: $msg" >> ${TMPDIR:?}/$JOBID/test.status
    )
}

delete_p4adds() {
    BT=${DEV_ROOT:?}/tools/wls/infra/build

    # Do this odd way of testing for a newer file because -nt does not work
    # in "sh" on Solaris.  "[ a -nt b ]" doesn't work and "test a -nt b"
    # doesn't work because test is a "sh" builtin so I have to give it a full
    # path.
    if [ "$OS" = "SunOS" -a -x /usr/bin/test ]; then
	TEST=/usr/bin/test
    elif [ "$OS" = "Windows_NT" ]; then
	# this one is because I have also seen this occasionally with MKS where
	# it picks up infra/test/test.sh instead of the builtin and this fixes
	# it.  I guess maybe we should just rename test.sh sometime.
	TEST=test.exe
    else
	TEST=test
    fi

    if $TEST $BT/xargs_rm.java -nt $BT/xargs_rm.class ; then
	rm -f $BT/xargs_rm.class
	which ${JAVA_HOME}/bin/javac
	${JAVA_HOME}/bin/javac --version
	${JAVA_HOME}/bin/javac $BT/xargs_rm.java
    fi

    # if in cygwin then filter the filenames to Cygwin mixed mode so that
    # xargs_rm.java can delete them.  Originally this was using Cygwin
    # "xargs rm" which could handle pure Cygwin filenames but java cannot.
    if [ "`uname -o 2>&1`" = "Cygwin" ] ; then
	CYGPATH_FILTER="cygpath -m -f-"
    else
	CYGPATH_FILTER="cat"
    fi
    # make sure add'ed and branched files are deleted.  This function assumes
    # the output of p4 revert -n (from safe_revert) or p4 opened (from
    # the p4opened.files zip.opened.files .zip file) is piped into it.
    awk '-F#' '/#none - was (add|branch|move\/add), |#[0-9]* - (add|branch|move\/add) /{print $1}' |\
	while read i; do
	    # use p4 where to get the client file syntax (i.e. run it through
	    # the p4 client mapping)
	    # the complicated awk is the grab the right string from p4 where,
	    # especially in case the name has spaces in it. If NF (number of
	    # fields) is 3 then grab $3, if NF = 6 then grab $5 and $6, if
	    # NF = 9 then grab $7, $8 and $9, etc
	    p4 where "$i" | awk '{
a = "";
for (i = ((NF / 3) * 2) + 1; i <= NF; i++) { a = a " " $i };
sub(/^ /, "", a);
gsub(/\\/, "/", a);
print a;
}'
    done | $CYGPATH_FILTER | ${JAVA_HOME}/bin/java -cp $BT xargs_rm "$ENCODING"
    # use xargs_rm like p4clean does because it handles really long filenames
}

safe_revert() {
    # revert all open files, they should be deleted by p4clean but this is
    # extra robust
    p4 revert -n //dev/${SRC}/... | delete_p4adds
    p4 -r4 -vnet.maxwait=300 revert //dev/${SRC}/... >/dev/null
}

test_status() {
    : ${JOBID:-${RBT_QUEUED_ID:?}}
    status=${TMPDIR:?}/${JOBID:-$RBT_QUEUED_ID}/test.status

    pass_count=`awk '/^INFO: test .* passed/' $status | wc -l`
    start_count=`awk '/^INFO: test .* starting/' $status | wc -l`
    # all the tests can pass yet there be fatal errors if submit=true, but
    # the submit fails
    fatal_count=`awk '/^FATAL: /' $status | wc -l`

    if [ $pass_count -eq $start_count -a $fatal_count -eq 0 ]; then
	echo success
    elif [ $pass_count -eq 0 ]; then
	echo failed
    else
	echo partial success
    fi
}

apply_opened_files() {
    infomsg "syncing the p4 opened files to the right revision..."
    # sync to the exact revision of each file except add's and branches
    # make sure add's and branch's aren't in p4 client by sync'ing to #none
    awk < p4opened.files '-F - ' '
/#[0-9]* - (add|branch|move\/add) /   {split($0, a, "#"); print a[1] "#none";next}
/\.\.\. - file\(s\) not opened on this client/{next}
{print $1;next}' > p4edit.files
    # do not p4 -x sync unless the file is non-empty
    if [ -s p4edit.files ]; then
	p4 -x p4edit.files sync
    fi
    rm p4edit.files
    infomsg "deleting deleted p4 opened files..."
    # p4 delete the deletes
    awk < p4opened.files '-F#' '/#[0-9]* - (delete|move\/delete) /{print $1}' | p4 -x- delete
    infomsg "editing edit/integ p4 opened files..."
    # p4 edit the integs and edits
    awk < p4opened.files '-F#' '/#[0-9]* - (edit|integrate) /{print $1}' | p4 -x- edit
    # make sure and remove all the files that will be add'ed
    delete_p4adds < p4opened.files
    # create a list of files to unjar.  I do this because the
    # p4opened.files might have been filtered (to just repository/...)
    # above to just be a subset of the files in the zip/jar
    # note the & sed that quotes files to xargs that contain spaces
    # note the & sed that quotes files to xargs that contain '
    # that is really confusing '/" syntax, it means...
    # sed -e "blabla'" appended with 'blabla"&"blabla' so that I can get
    # a sed string with both ' and " in it
    sed -e '/#[0-9]* - (delete|move\/delete) /d' \
	-e '/^change=none, no open files/d' \
	-e '/^... - file(s) not /d' \
	-e "s|^//dev/$SRC/||" \
	-e 's/#.*//' \
	-e 's/^.* .*$/"&"/' \
	-e "s/^.*'"'.*$/"&"/' \
	< p4opened.files > files.to.unjar
    echo p4changes.txt >> files.to.unjar
    if [ -s files.to.unjar ]; then
        if [ -z "$ENCODING" ]; then
	    jar -xf "$1" @files.to.unjar
        else
            ant ant.unzip -Dfile2unzip=files.to.unjar -Dzippedfile="$1" -Dencoding="$ENCODING"
        fi
	# make sure all the files have "sane" timestamps.  If the zip
	# was created on machine in a different timezone then the
	# dates of the restored files can be in the future or the
	# past.  Make all the files the current time and date.
	cat files.to.unjar | while read i; do
	    [ -f "$i" ] && touch "$i"
	done
    fi
    rm -f files.to.unjar
    infomsg "adding add/branch/moved p4 opened files..."
    # p4 add the add's and branch's
    # do this after the files have been unjarred
    #awk < p4opened.files '-F#' '/#[0-9]* - (add|branch) /{print $1}' | p4 -x- add
    awk < p4opened.files '-F#' '/#[0-9]* - (add|move\/add) /{print $1}' | p4 -x- add
    if [ -s p4branched.files ]; then
        perl $DEV_ROOT/tools/wls/infra/test/p4branched.pl -p4branched p4branched.files
    else
        if [ ! -f p4branched.files ]; then
            awk < p4opened.files '-F#' '/#[0-9]* - (branch) /{print $1}' | p4 -x- add
        fi
    fi
}

apply_zip() {
    set +e
    # don't call this env var tmpfile because apply_zip_internal also
    # uses "tmpfile"
    applytmpfile=${TMPDIR:?}/apply_zip.tmp_$$
    export applytmpfile
    descfile=${TMPDIR:?}/apply_zip.desc_$$
    export descfile
    (
	set -e
	apply_zip_internal "$@"
	# a few env vars need to be "output" from apply_zip_internal even
	# though I'm using () to capture the error status
	(
	    echo "P4CHANGES=\"$P4CHANGES\" ; export P4CHANGES"
	    # use a file because DESC might have shell expansion characters
	    # that are hard to quote
	    echo "$DESC" > $descfile
	) > $applytmpfile
    )
    if [ $? -ne 0 ]; then
	fatalmsg "problem applying your zipped open p4 files to remote machine p4 client (see test.log for the precise error)" test.log
    fi
    . $applytmpfile
    DESC="`cat $descfile |sed 's/\\r//'`"
    export DESC
    rm -f $applytmpfile $descfile

    # run dos2unix on all files that p4 considers "text"... (*text*) as the file
    # type, this should be in apply_zip
    uname | grep -e Linux -e SunOS >/dev/null && which dos2unix >/dev/null
    if [ $? -eq 0 ]; then
	p4 opened //dev/$SRC/... | awk -F# '/\(.*text.*\)/{print $1}' |\
	    p4 -x- where | awk '{
a = "";
for (i = ((NF / 3) * 2) + 1; i <= NF; i++) { a = a " " $i };
sub(/^ /, "", a);
gsub(/\\/, "/", a);
print a;
}' | while read i; do
	    echo "begin $i"
	    thedir=`dirname "$i"`
	    thefile=`basename "$i"`
  	    # I do not know why but if you feed the full path to the file on
	    # the dos2unix command line it will frequently core dump but I have
	    # yet to see it core dump on the simple filename
	    ( cd "$thedir" && dos2unix -q -o "$thefile" )
	    echo "end $i"
	done
    fi
}

apply_zip_internal() {
    : ${1:?zip file}
    # make sure we are in the branch root (it might not exist for
    # read-only depot tmp clients)
    root=`p4 client -o | awk '/^Root:/{print $NF}'`
    mkdir -p $root/dev/$SRC
    cd $root/dev/$SRC
    wrkdir="$root/dev/$SRC"
    if [ -z "$INTEGFROM" -a "$REVERT" != "true" ]; then
	# retrieve the opened files contained in the zip
        if [ -z "$ENCODING" ]; then
	    jar -xf "$1" p4opened.files p4branched.files customizedprops.txt
        else
            ant ant.unzip -Dzippedfile="$1" -Dunzipfilelist="p4opened.files p4branched.files customizedprops.txt" -Dunzipdest="$wrkdir" -Dencoding="$ENCODING"
        fi
	set +e
	grep /wls/infra/test/remotetest p4opened.files >/dev/null
	if [ $? -eq 0 ]; then
	    fatalmsg_noexit "remotetest*.sh scripts found in .zip file!"
	    fatalmsg        "you can not use the remote queue to test changes to the remote queue scripts!"
	fi
	# filter out any repository files from the opened files so that they do not get "applied" unless this is the auto-submit monkey
	if [ "${USER:-${USERNAME:-${LOGNAME}}}" != "p4as" ]; then
	    egrep -v "//dev/$SRC/repository/.*/" < p4opened.files > tmp.file
	    mv -f tmp.file p4opened.files
	fi
	set -e
	apply_opened_files "$1"
    else
        if [ -z "$ENCODING" ]; then
	    jar -xf "$1" p4changes.txt p4opened.files p4branched.files customizedprops.txt
        else
            ant ant.unzip -Dzippedfile="$1" -Dunzipfilelist="p4changes.txt p4opened.files p4branched.files customizedprops.txt" -Dunzipdest="$wrkdir" -Dencoding="$ENCODING"
        fi
    fi
    P4CHANGES="`head -1 p4changes.txt | tr A-Z a-z | tr -d '\r'`"
    if [ "$P4CHANGES" = "none" ]; then
	P4CHANGES=
    fi
    export P4CHANGES
    tail -n +3 p4changes.txt > p4description.txt
    DESC="`cat p4description.txt |sed 's/\\r//'`"
    export DESC
    # if the file is just an empty line then delete it
    if [ "`wc -c < p4description.txt`" -le 1 ]; then
	rm p4description.txt
    fi
    # add pending change number descriptions, if -Dchange= was used
    if [ -n "$P4CHANGES" ]; then
	if [ -z "$INTEGFROM" -a "$REVERT" != "true" ]; then
	    infomsg "using pending change(s) $P4CHANGES"
	else
	    infomsg "using submitted change(s) $P4CHANGES"
	fi
	touch p4description.txt
	for i in `echo $P4CHANGES | sed 's/,/ /g'`; do
	    p4 change -o $i |\
		sed -e '1,/^Description:$/d' -e '/Files:$/,$d' |\
		sed -e '$d' -e 's/^.//' >> p4description.txt
	done
    fi
    if [ -n "$INTEGFROM" -o "$REVERT" = "true" ]; then
	if [ "$REVERT" = "true" ]; then
	    if [ "$P4CHANGES" = "" ]; then
		fatalmsg "there is no change to revert!"
	    fi
	    set +e
	    echo "$P4CHANGES" | grep , >/dev/null
	    if [ $? -eq 0 ]; then
		fatalmsg "you can not revert multiple change numbers (this is to be implemented)"
	    fi
	    set -e
	    infomsg "reverting $P4CHANGES (not using open files in zip file)"
	    p4 print -q //depot/dev/tools/sh/revert.sh  > ${TMPDIR:?}/revert.sh
	    p4 print -q //depot/dev/tools/sh/revert.awk > ${TMPDIR:?}/revert.awk
	    sh ${TMPDIR:?}/revert.sh $P4CHANGES
	elif [ -n "$INTEGFROM" ]; then
	    if [ "$P4CHANGES" = "" ]; then
		fatalmsg "there is no change to integ!"
	    fi
	    set +e
	    echo "$P4CHANGES" | grep , >/dev/null
	    if [ $? -eq 0 ]; then
		fatalmsg "you can not auto-integ multiple change numbers (this is to be implemented)"
	    fi
	    set -e
	    infomsg "doing integ $P4CHANGES $INTEGFROM -> $SRC (not using open files in zip file)"
	    # turn off set -e because grep failure will cause the script to exit
	    # turn on set -e right before we do each p4 integ
	    set +e
	    tmpfile=/tmp/branch_$SRC.tmp_$$
	    p4 branch -o "$INTEGFROM" > "$tmpfile"
	    grep "//dev/$SRC/" >/dev/null < "$tmpfile"
	    if [ $? -eq 0 ]; then
		frombranch=`sed -n '/^View:/,$p' < "$tmpfile" | sed -n -e '/-\/\/depot/d' -e 's|.*//depot.* //dev/\([^/]*\)/.*|\1|p' | uniq`
		infomsg "using p4 integ -ihtrb $INTEGFROM to do integ"
		set -e
		p4 integ -ihtrb "$INTEGFROM" -s "//dev/${frombranch:?}/...@=$P4CHANGES"
	    else
		p4 branch -o "$SRC" > "$tmpfile"
		grep "//dev/$INTEGFROM/" >/dev/null < "$tmpfile"
		if [ $? -eq 0 ]; then
		    frombranch=`sed -n '/^View:/,$p' < "$tmpfile" | sed -n -e '/-\/\/depot/d' -e 's|.*//dev/\([^/]*\)/.* //depot.*|\1|p' | uniq`
		    infomsg "using p4 integ -ihtb $SRC to do integ"
		    set -e
		    p4 integ -ihtb "$SRC"    -s "//dev/${frombranch:?}/...@=$P4CHANGES"
		else
		    infomsg "using p4 integ -iht //dev/$INTEGFROM/... //dev/$SRC/..."
		    set -e
		    p4 integ -iht "//dev/$INTEGFROM/...@=$P4CHANGES" "//dev/$SRC/..."
		fi
	    fi
	    rm -f "$tmpfile"
	else
	    fatalmsg "internal error, should never get here"
	fi
	set +e
	# revert all repository files opened by the integ or revert, we never
	# want to use them and we also do not want them to cause any resolve
	# failures.  repository files will always be built from changes made
	# to the modules directory or, if this is running in the auto-submit
	# monkey the files will have been already built on the RQ machine
	# and provided to this script in the .zip file.  Note that this can
	# cause a problem integing/reverting changes that are *just* to the
	# repository directory.  That should not normally happen and those
	# changes will have to be nag ignored or something.  This script does
	# not attempt to handle those change numbers.
	p4 opened //dev/$SRC/repository/%%1/... > repos.files
	if [ `wc -l < repos.files` -gt 0 ]; then
	    p4 revert -n //dev/$SRC/repository/%%1/... | delete_p4adds
	    p4 revert //dev/$SRC/repository/%%1/...
	fi
	rm -f repos.files
	# if this is in the auto-submit monkey then get repository files from
	# the zip file created on the RQ machine that successfully ran the
	# modules build and tests (if any).
	if [ "${USER:-${USERNAME:-${LOGNAME}}}" = "p4as" ]; then
	    egrep    "//dev/$SRC/repository/.*/" < p4opened.files > zip.files
	    if [ `wc -l < zip.files` -gt 0 ]; then
		infomsg "applying /repository/... files built during the remote test to the auto-submit..."
		mv -f zip.files p4opened.files
		apply_opened_files "$1"
	    fi
	    rm -f zip.files
	fi
    fi
    if [ "`p4 opened //dev/${SRC}/... | wc -l`" -eq 0 ]; then
	warnmsg "there are no open files to test, assuming you are using -Dchange="
    fi
    set +e
    if [ -z "$INTEGFROM" -a "$REVERT" != "true" ]; then
	#egrep '#[0-9]* - (integrate|branch) ' < p4opened.files > /dev/null
	egrep '#[0-9]* - (integrate) ' < p4opened.files > /dev/null
	if [ $? -eq 0 ]; then
	    SUBMIT="`echo $SUBMIT | tr A-Z a-z`"
	    if [ "$SUBMIT" = "true" -o "$SUBMIT" = "yes" -o "$SUBMIT" = "requeue" ]; then
		fatalmsg_noexit "you can not auto-submit files that you integed on your p4 client"
		fatalmsg_noexit "because the remote test can not properly reproduce the integ/resolve"
		fatalmsg_noexit "commands you did.  If the remote queue submitted the files as edits"
		fatalmsg_noexit "it will confuse nags and bulk integs.  To do a true integ in the"
		fatalmsg_noexit "remote queue use ant -Dintegfrom=branch -Dchange=change -Dsubmit=true"
		fatalmsg_noexit "*OR* do not use ant -Dsubmit=true in the remote queue and submit the"
		fatalmsg        "changes on your own p4 client."
	    fi
	fi
    fi
    rm -f p4opened.files p4changes.txt p4branched.files
}

sync_files() {
    : ${1:?syncspec}

    p4sync_output=${TMPDIR:?}/p4sync_output.tmp_$$
    filtered_output=${TMPDIR:?}/filtered_output.tmp_$$

    infomsg "sync to $1"
    set +e
    p4 sync $1 > $p4sync_output
    p4sync_status=$?
    egrep -v ' - (refreshing|updating|deleted as|added as) ' < $p4sync_output > $filtered_output
    cat $filtered_output
    grep ' - is opened at a later revision ' < $filtered_output > /dev/null
    if [ $? -eq 0 ]; then
	warnmsg "There are newer p4 changes in your p4 opened files, they might not build/test at $1"
    fi
    rm -f $p4sync_output $filtered_output

# if the sync failed it may have synced most everything except some scripts
# being run from wls/infra.  So... try it again taking the output of sync -n
# filter out wls/infra and feed it to p4 -x- sync.  It is fails this type
# then let the script fail.
    # FIXME: herrlich@bea.com Oct 25, 2005
    # this is *not* the return status of the p4 sync, it should be
    if [ $p4sync_status -ne 0 ]; then
	set -e
	p4 sync -n $1 > $p4sync_output
	# do this *not* in a pipe because if the p4 sync -n fails the error
	# status won't be propagated thru the pipe and cause the script to
	# exit based on the set -e
	awk '-F - ' '/\/wls\/infra\//{next}{print $1}' < $p4sync_output | p4 -x- sync
	rm -f $p4sync_output
	echo $?
    fi
}

resolve_files() {
    set -e
    : ${SRC:?}

    infomsg "p4 resolving files..."
    # do a plain resolve -as first
    p4 resolve -as //dev/$SRC/...
    # this will take care of simple binary merges (I'm not 100% sure if this
    # will ever do anything but it won't hurt).
    p4 resolve -t -as //dev/$SRC/...
    # merge anything brought in by the sync
    p4 resolve -am //dev/$SRC/...
    # merge anything brought in by the sync ignoring whitespace
    # (-dw is slower so doing -am first handles the easy resolves first)
    p4 resolve -dw -am //dev/$SRC/...

    set +e

    # publish this file whether or not it resolved
    # create a file listing all the diffs in the p4 client
    # only do this if LOGDIR is defined, if it is not then this is probably in the remotetestsubmit*.sh auto-submit daemon
    if [ -n "$LOGDIR" ]; then
	(
	    p4 opened | egrep '#[0-9]* - (add|branch|delete) '
	    # < /dev/null is an attempt to stop a diff hang I am seeing doing large translation adoptions - alan.herrlich@oracle.com Aug 23, 2019
	    # -vnet.maxwait=300 is another attempt to work around large translation adoptions hangs - alan.herrlich@oracle.com Apri 30, 2020
	    p4 -vnet.maxwait=300 diff -dl < /dev/null
	) > ${TMPDIR:?}/$JOBID/p4diff.log
	publish ${TMPDIR:?}/$JOBID/p4diff.log
    fi

    if [ `p4 resolve -n //dev/$SRC/... | wc -l` -ne 0 ]; then
	p4 resolve -n //dev/$SRC/...
	safe_revert
	fatalmsg "there were unresolved p4 sync/resolve changes"
    else
	infomsg "p4 resolve was successful"
    fi
}

timebomb() {
    mkdir -p ${TMPDIR:?}
    rm -f ${TMPDIR:?}/timebomb.expired
    sleep_time=${1:-${METATIME:-21600}}
    sleep $sleep_time || return
    # in case the end of the build/test killbt'd the sleep but the sleep
    # didn't return failure then give the killbt a little time to complete
    # (and kill this timebomb) before trying to killing it (it has
    # a sleep 3 in it)
    sleep 5
    # before we kill the java processes, lets get some thread dumps for debugging
    # currently only works with jrockit
    echo $JAVA_VENDOR | grep BEA > /dev/null
    if [  $? -eq 0 ]; then
	i=1
	while [ $i -le 6 ]; do
	    # sleep 30 seconds between each print_threads, but not before
	    # the first one or after the last
	    if [ $i -ne 1 ]; then
		sleep 30
	    fi
	    # this prints out all java processes running
	    jrcmd -p
	    # now get thread dumps
	    jrcmd -p | while read pid junk; do
		if [ "$pid" != "" ]; then
		    jrcmd $pid print_threads jvmmonitors=true nativestack=true
		fi
	    done
	    i=`expr $i + 1`
	done
    fi
    echo $JAVA_VENDOR | grep -i oracle > /dev/null
    if [  $? -eq 0 ]; then
        i=1
        while [ $i -le 6 ]; do
            echo "Starting thread dumps, iteration $i"
            # sleep 30 seconds between each print_threads, but not before
            # the first one or after the last
            if [ $i -ne 1 ]; then
                sleep 30
            fi
            # this prints out all java processes running
            jcmd -l
            # now get thread dumps
            jcmd -l | while read pid junk ; do
                if [ "$pid" != "" ]; then
                    echo "working on pid=$pid"
                    kill -3 $pid
                    jcmd $pid Thread.print  -l
                fi
            done
            i=`expr $i + 1`
        done
    fi


    warnmsg "timebomb after $sleep_time seconds, killing java processes"
    infomsg "for detailed information see the build.log and test.log"
    # do this after the warnmsg, while this timebomb is being killbt'ed
    # I've seen it get to the touch before being killed, but I haven't seen
    # it get through the warnmsg before being killed.
    touch ${TMPDIR:?}/timebomb.expired
    $DEV_ROOT/tools/wls/infra/build/killbt.sh java javaw jview /jre/ /java/
}

timebomb_went_off() {
    test -f ${TMPDIR:?}/timebomb.expired
}

BadMachine() {
    : ${RBT_QUEUED_ID:?}

    . infra/infraenv.sh
    . infra/build/rbt.sh
    JOBID="$RBT_QUEUED_ID"
    export JOBID
    # this must be done *before* rquemove because infomsg/rqstatus -j removes the job if centrally queued
    infomsg "[${1:-unknown problem}]"
    infomsg "this machine has a fatal error, returning job to central queue"
    rquemove -r "$RBT_QUEUED_ID"
    TO="yan.yu.geng@oracle.com"
    SUBJECT="FATAL: ${1:-unknown problem} on $HOST in $SRC!"
    (
	echo "To: $TO"
	echo "Subject: $SUBJECT"
	echo ""
	echo "Sleeping 1 hour so someone can look at it."
	echo ""
	echo "For info about what to do see: http://home.us.oracle.com/internal/$SRC/remote_internal.html"
	echo ""
	echo "Job id: http://home.us.oracle.com/internal/$SRC/job.jsp?id=$RBT_QUEUED_ID"
	set -x
	ps -ef
	ps -Wef
	set +x
    ) 2>&1 | ${MAILPROG}"$SUBJECT" $TO
    if [ -f /tmp/stop_rdequeuer ]; then
	sleep 300
    else
	sleep 3600
	rqstatus busy
	$DEV_ROOT/tools/wls/infra/test/at_rdequeuer.sh -r
	# give the at job enough time to killbt this job
	sleep 3600
	# if the old rq daemon is not killed by someone or new rq daemon,
	# then it should exit with 100 to avoid writing log to job file on cq.
    fi
    exit 100
}

AddToP4Client() {
    : ${1:?} ${P4CLIENT:?}
    if [ "`p4 info | grep '^Client unknown'`" != "" ]; then
        infomsg "ERROR: p4 client ${P4CLIENT} does not exist, not updating"
        return
    fi
    # FIXME: jun.li@bea.com Feb 18, 2008
    # delete the \r to make it work in bash3.1.17(6) in cygwin
    echo "$1" | tr -d '\r' | grep "^//depot/.*/...$" >/dev/null
    if [ $? -ne 0 ]; then
        infomsg "ERROR: $1 is not of the form //depot/*/..."
        return
    fi
    clientspec=`echo "$1" | sed "s|^//depot/|//$P4CLIENT/|"`
    p4 client -o |\
	grep -v "$1 " | sed "\$i\\
\	$1 $clientspec\\
" | p4 client -i
}

RemoveFromP4Client() {
    : ${1:?} ${P4CLIENT:?}
    if [ "`p4 info | grep '^Client unknown'`" != "" ]; then
        infomsg "ERROR: p4 client ${P4CLIENT} does not exist, not updating"
        return
    fi
    # FIXME: jun.li@bea.com Feb 15, 2008
    # delete the \r to make it work in bash3.1.17(6) in cygwin
    echo "$1" | tr -d '\r' | grep "^//depot/.*/...$" >/dev/null
    if [ $? -ne 0 ]; then
        infomsg "ERROR: $1 is not of the form //depot/*/..."
        return
    fi
    p4 client -o | grep -v "$1 " | p4 client -i | grep -v 'Client .* not changed\.'
}

GetSyncDirs() {
    : ${1:?paths file} ${DEV_ROOT:?} ${SRC:?} ${SYNCTO:?}
    set +e
    if [ ! -s "$DEV_ROOT/env/bin/$1" ]; then
	fatalmsg "could not find paths file ($1) to calculate sync directories"
    fi
    set -e
    # FIXME: alan.herrlich@oracle.com Oct 16, 2009
    # this will add //depot even if it already has it
    echo `sed < "$DEV_ROOT/env/bin/$1" -e 's|^|//dev/'$SRC'/|' -e 's|$|@'$SYNCTO'|'`
}

auto_submit() {
    set +e
    (
	set -e
	auto_submit_internal
    )
    if [ $? -ne 0 ]; then
	fatalmsg "problem auto-submiting your changes (see test.log for the precise error)" test.log
    fi
}

auto_submit_internal() {
    : ${REMOTE_P4USER:?} ${JOBID:?} ${RQSITE:?}
    set +e
    SUBMIT="`echo $SUBMIT | tr A-Z a-z`"

    infomsg "Preparing to submit tested changes..."

    # use the REMOTE_TEST target from the master RQ job in parallel jobs rather than the REMOTE_TEST of *this* RQ job
    shortid=`echo $JOBID | sed 's/-[0-9][0-9]*$//'`
    if [ "$JOBID" != "$shortid" ]; then
        REAL_REMOTE_TEST=`awk 'FNR == 2 {print $2;exit}' $RQSITE/$SRC/status/$shortid`
    else
        REAL_REMOTE_TEST=$REMOTE_TEST
    fi

    # make sure the .zip doesn't exist from a previous run of this job
    rm -f $RQSITE/$SRC/remotetests/autosubmit/$shortid.zip
    logfile=$RQSITE/$SRC/remotetests/autosubmit/$shortid.log
    rm -f $RQSITE/$SRC/remotetests/autosubmit/$shortid "$logfile"
    # * is a flag = empty
    echo "${SYNCTO:?}" "${REAL_REMOTE_TEST:?}" "${INTEGFROM:-*}" "${REVERT:-*}" "${APPROVED:-*}" > $RQSITE/$SRC/remotetests/autosubmit/$shortid
    count=0
    sleep_seconds=1
    max_seconds=1800
    filecount=`p4 opened //dev/$SRC/... |wc -l`
    if [ "$filecount" -gt "$max_seconds" ]; then
        max_seconds=$filecount
    fi
    while [ "$count" -lt "$max_seconds" -a ! -f "$logfile" ]; do
        count=`expr $count + $sleep_seconds`
        sleep $sleep_seconds
    done
    if [ -f $logfile ]; then
        cat < $logfile
        grep '^FATAL: ' < $logfile > /dev/null
        if [ $? -eq 0 ]; then
	    fatalmsg "auto-submit failed, see test.log for more info" test.log
        fi
        if [ `grep ^Change < $logfile | wc -l` -gt 0 ]; then
            infomsg "`grep ^Change < $logfile`"
            infomsg "Note: You will have to revert these files in your p4 client."
            infomsg "Note: Remember to manually delete files you p4 added."
            infomsg "Note: This remote test can not effect your locally opened files."
        fi
    else
        fatalmsg_noexit "timeout waiting for auto-submit to happen, waited $count seconds"
        fatalmsg        "the auto-submit will probably not happen"
    fi
    rm -f $RQSITE/$SRC/remotetests/autosubmit/$shortid $RQSITE/$SRC/remotetests/autosubmit/$shortid.*
}

auto_submit_real() {
    set +e
    (
	set -e
	auto_submit_real_internal
    )
    if [ $? -ne 0 ]; then
	fatalmsg "problem auto-submiting your changes (see test.log for the precise error)" test.log
    fi
}

auto_submit_real_internal() {
    : ${SRC:?} ${JOBID:?} ${SYNCTO:?}
    set +e
    infomsg "Preparing to submit tested changes..."
    set -e
    # only allow the same sync/resolves done previously and
    # successfully built and tested, if there have been *more*
    # checkins between the clean label and #head that require
    # sync/resolve then fail

    # sync open files to the same change number as the build/test change
    p4 opened //dev/${SRC}/...| awk '-F#' "{print \$1 \"@$SYNCTO\"}" | p4 -x- sync
    resolve_files

    # resolve_files turns this off
    set -e

    # sync open files to the #head
    p4 opened //dev/${SRC}/... | awk '-F#' "{print \$1 \"#head\"}" | p4 -x- sync
    if [ `p4 resolve -n //dev/$SRC/... | wc -l` -ne 0 ]; then
	p4 resolve -n //dev/$SRC/...
	safe_revert
	fatalmsg "There are additional sync/resolves that need to be done that are not in the clean label.  You will have to wait for them to be in the label or submit the files yourself."
    fi

    if [ `p4 opened //dev/${SRC}/... | wc -l` -le 0 ]; then
	fatalmsg "There are not any files to submit."
    fi
    # create the new change number
    (
	echo "Change: new"
	echo ""
	echo "Description:"
	# start with the passed in -Ddescription
	if [ "$DESC" != "" ]; then
	    # if DESC has new lines then make sure each line has a tab
	    echo "	$DESC" | sed 's/^[^	]/	&/' |sed 's/\\r//'
	fi
	# now append all the Description: from the passed in
	# change numbers that were zipped up
	for i in `echo $P4CHANGES | sed 's/,/ /g'`; do
	    p4 change -o $i |\
		sed -e '1,/^Description:$/d' -e '/Files:$/,$d' -e '/^Jobs:$/,$d' |\
		sed -e '$d' -e 's/\\r//'
	done
	if [ -z "$INTEGFROM" ]; then
	    echo "	(auto-submit $P4CHANGES after successfully running remote $REMOTE_TEST)"
	else
	    echo "	(auto-submit integ $P4CHANGES $INTEGFROM -> $SRC after successfully running remote $REMOTE_TEST)"
	fi
	echo "	Job ID: $JOBID"
    ) | p4 change -i > t.dat
    new_change=`sed < t.dat -n '/Change [0-9]* created/s/.*Change \([0-9]*\) created.*/\1/p'`
    rm -f t.dat
    # export so that the submit ( p4 revert -c $new_change ) error code
    # below can use "$new_change"
    export new_change
    set +e
    if [ -z "$new_change" ]; then
	fatalmsg "problem creating new change number"
    fi
    set -e
    p4 reopen -c $new_change //dev/${SRC}/...
    # true = disable the submit
    # false = enable the submit
    set +e
    if false; then
	p4 revert //dev/${SRC}/...
	p4 change -d $new_change
    else
	# revert empty files, but only if not doing an integ.  Even
	# empty submit changes can be important to resolve nags.
	if [ -z "$INTEGFROM" ]; then
	    set -e
	    infomsg "reverting empty files (if any)"
	    p4 revert -a //dev/${SRC}/...
	fi

	p4 submit -c $new_change > ${TMPDIR:?}/$JOBID/p4submit.log 2>&1 && \
	    infomsg "changes successfully submitted" || (
	    fatalmsg_noexit "changes failed to submit, see test.log for the exact p4 errors" test.log
	    p4 change -o $new_change
	    p4 revert -c $new_change //dev/${SRC}/...
	    p4 change -d $new_change
	)

	cat ${TMPDIR:?}/$JOBID/p4submit.log
    fi
}
