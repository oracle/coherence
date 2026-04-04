#!/bin/sh
# Copyright (c) 2006, 2026, Oracle and/or its affiliates.
#
# The ~static file remotetest.sh calls this more-specialized file.
#
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required
: ${SRC:?} ${DEV_ROOT:?} ${1:?test to run} ${2:?zip of changes} ${3:?jobid} ${4:?logdir}

# set these because $* get overwritten when we . devenv
REMOTE_TEST="$1"
REMOTE_ZIP="$2"
JOBID="$3"
shortid=`echo $JOBID | sed 's/-[0-9][0-9]*$//'`
# needed by remotetestutils
LOGDIR="$4"

# remove .stage# surffix from REMOTE_TEST, for .stage# jobs are sub jobs. Their sync/clean/build/test targets are all same with master job.
# .stage# surffix is used to identify jobs in rq pages.
set -x
echo $REMOTE_TEST |grep "\.stage" >/dev/null
if [ $? -eq 0 ]; then
    REMOTE_TEST=${REMOTE_TEST%.stage*}
fi
set +x

mkdir -p ${TMPDIR:?}/$JOBID

# Set this to non-null to collect the config in test.log quickly by
# not syncing, not cleaning, and exiting after parsing TESTDATA.  This
# MUST be null for normal operation of this script.
#run_debug=true
run_debug=

echo $0 | grep new.sh >/dev/null
if [ $? -eq 0 ]; then
    NEW=new
else
    NEW=
fi
. $DEV_ROOT/tools/wls/infra/test/remotetestutils$NEW.sh

p4 client -o | grep [^-]//dev/$SRC/\.\.\. >/dev/null
if [ $? -ne 0 ]; then
    fatalmsg "the branch //dev/$SRC/... is not in the client map"
fi

cd $DEV_ROOT
# save the JV setting, re-set it after apply_zip and sync/resolve.  If JV is a
# bad value we don't want it breaking the devenv -dl.
RBT_JV="$JV"
unset JV
if [ -f .rbt.envs ]; then
    . ./.rbt.envs
fi
. tools/wls/infra/infraenv.sh
. tools/wls/infra/build/rbt.sh

if [ -n "$NEW" ]; then
    infomsg "running the remotetest*new.sh scripts"
fi

if [ "$run_debug" != "" ]; then
    infomsg "THIS IS A DEBUG RUN - IT ONLY COLLECTS RQ CONFIG - NO BUILD WILL BE DONE"
fi

dir="`echo $LOGDIR | sed 's|^.*/remotetests/||' `"
infomsg "<a href=\"http://home.us.oracle.com/centralrq/coherence/$SRC/remotetests/$dir/\">Results directory</a> (<a href=\"http://home.us.oracle.com/internal/coherence/$SRC/zipresults.jsp?id=$JOBID&timestamp=`basename $dir`\">Results Zip</a>)"
infomsg "<a href=\"http://home.us.oracle.com/centralrq/coherence/$SRC/remotetests/zips/$shortid.zip\">P4 opened files</a> (<a href=\"http://home.us.oracle.com/centralrq/coherence/$SRC/remotetests/$dir/p4diff.log\">P4 Diffs</a>)"
unset dir

tools/wls/infra/build/killbt.sh -u >${TMPDIR:?}/$JOBID/killbt.log 2>&1
publish ${TMPDIR:?}/$JOBID/killbt.log
infomsg "ran killbt"

p4err() {
 fatalmsg "you can not use $REMOTE_P4USER to run remote tests, use your actual p4 user"
}

case $REMOTE_P4USER in
    bt)     p4err ;;
    build)  p4err ;;
    echu)   p4err ;;
    jrbt)   p4err ;;
    release)    p4err ;;
    root)   p4err ;;
    sjbuild)    p4err ;;
    wlesqa) p4err ;;
    *)
    p4 users | grep ^"$REMOTE_P4USER " > /dev/null
    if [ $? -ne 0 ]; then
        p4err
    fi
    ;;
esac

safe_revert

apply_zip "$REMOTE_ZIP"
if [ -f p4description.txt ]; then
    publish p4description.txt
    rm -f p4description.txt
fi

if [ -f customizedprops.txt ]; then
    RQ_SYSPROP="`cat customizedprops.txt |sed 's/\\r//' |grep "^SYS_PROP" |sed 's/^SYS_PROP *//'`"
    RQ_ANTPROP="`cat customizedprops.txt |sed 's/\\r//' |grep "^ANT_PROP" |sed 's/^ANT_PROP *//'`"
    if [ -n "$RQ_ANTPROP" ]; then
        if [ "$REMOTE_TEST" != "remote.customized" ]; then
            fatalmsg "-Drq.antprop must be with remote.customized"
        fi
    fi
    rm -f customizedprops.txt
fi

SUBMIT="`echo $SUBMIT | tr A-Z a-z`"
if [ "$SUBMIT" = "true" -o "$SUBMIT" = "yes" -o "$SUBMIT" = "requeue" ]; then
    if [ "`p4 diff | wc -l`" -eq 0 -a \
     "`p4 opened | egrep ' - (add|delete|branch) ' | wc -l`" -eq 0 ]; then
    fatalmsg "-Dsubmit=$SUBMIT but there are no changes to submit"
    fi
fi

BAREMETAL="`echo $BAREMETAL | tr A-Z a-z`"
if [ "$BAREMETAL" = "true" ]; then
    infomsg "BAREMETAL value set to $BAREMETAL"
else
    BAREMETAL=false
fi

echo "$REMOTE_TEST" | grep wls > /dev/null
if [ $? -eq 0 ]; then
    # remote.wls shouldn't submit directly
    COH_SUBMIT=$SUBMIT
    SUBMIT=false
fi

# ------------------------------------------------------------------------------
#                                 syncto
# ------------------------------------------------------------------------------

# since we overwrite the original value of SYNCTO, we need to save the original value for spin jobs
SYNCTO_SAVE="$SYNCTO"

cd tools/wls/infra/test
if [ -z "$SYNCTO" ]; then
    perl ./remotetestprops$NEW.pl syncto $REMOTE_TEST ./remotetestprops$NEW.env
    if [ $? -ne 0 ]; then
        fatalmsg "non-zero status was returned by command:  remotetestprops$NEW.pl syncto.  See Results directory/test.log for more info"
    else
        set `perl ./remotetestprops$NEW.pl syncto $REMOTE_TEST ./remotetestprops$NEW.env`
        eval ctr="$1"
        echo "ctr=$ctr"
    fi

    SYNCTO=`p4 counter $ctr`
    # first assume the label has a Revison: setting
    if [ "$SYNCTO" -eq 0 ]; then
    SYNCTO=`p4 label -o $ctr | awk -F@ '/^Revision:/ {print $2}'`
    fi
    # if syncto is still 0 then do it the older slower way
    if [ "$SYNCTO" -eq 0 ]; then
    SYNCTO=`p4 changes -m1 //...@$ctr | awk '{print $2}'`
    fi
    echo SYNCTO=$SYNCTO

    set `perl ./remotetestprops$NEW.pl infomsg $REMOTE_TEST ./remotetestprops$NEW.env`
    if [ $? -ne 0 ]; then
        fatalmsg "non-zero status was returned by command:  remotetestprops$NEW.pl infomsg.  See Results directory/test.log for more info"
    else
        eval infomsg="$*"
        echo infomsg="$infomsg"
    fi

    infomsg "$infomsg"
else
    # lower the case and remove any leading # or @
    if [ "`echo $SYNCTO | sed 's/^[#@]//' | tr A-Z a-z`" = "head" ]; then
    SYNCTO=`p4 counter change`
    infomsg "-Dsyncto=head supplied, #head is change $SYNCTO"
    fi
    infomsg "clean label ignored, -Dsyncto=$SYNCTO, using that change number"
fi
cd $DEV_ROOT
set +x

if [ "$SYNCTO" -eq 0 ]; then
    fatalmsg_noexit "The change number is zero, unknown clean counter or label for this test."
    fatalmsg_noexit "There is probably no CruiseControl machine for this test on $SRC."
    fatalmsg        "Manually pick a change number and rerun the remote test with -Dsyncto="
fi

if [ "$REMOTE_TEST" = "remote.multiple" -a -z "$RTARGETS" ]; then
    fatalmsg_noexit "you are required to give at least one target when running remote.multiple"
    fatalmsg        "Please re-enqueue using -Drtargets= with one or more comma separated remote targets"
fi

# ------------------------------------------------------------------------------
#                                 sync spec
# ------------------------------------------------------------------------------
syncspec="...@$SYNCTO"

if [ "$run_debug" = "" ]; then
    sync_files "$syncspec"

    p4 sync //dev/$SRC/tools/wls/infra/test/remotetestprops%%1
    if [ -n "$SYNCTO_SAVE" -a "$REMOTETESTENVSYNCTO" = "true" ]; then
        infomsg "-Dsyncto is specified and -DremotetestenvSyncto=true, sync //dev/$SRC/tools/wls/infra/test/remotetestprops$NEW.env to @$SYNCTO"
        p4 sync //dev/$SRC/tools/wls/infra/test/remotetestprops$NEW.env@$SYNCTO
    fi
    resolve_files

    p4 changes -m1 //dev/$SRC/...#have
else
    infomsg "debug mode:  not syncing"
fi

# ------------------------------------------------------------------------------
#                                 misc
# ------------------------------------------------------------------------------
# provide a little visual space between the devenv output and the
# INFO: messages
echo ""
echo ""

# re-set JV now that devenv changes have been applied, sync'ed and
# resolved on this p4 client.  So that devenv and *.dat changes can be
# tested in the RQ.
if [ -n "$RBT_JV" ]; then
    JV="$RBT_JV"
    export JV
else
    # JV="" isn't the same as unset for devenv.pl, in order to get the
    # devenv default java version you need to unset JV.  So there is going to be
    # *no* way in the remote tests to set JV="", you either have to put
    # JV=jrockit or JV=server and JV="" or no JV setting is going to be unset JV
    unset JV
fi
unset RBT_JV

cd $DEV_ROOT
local_repo=`dirname $DEV_ROOT | sed 's/coherence.*/repository/g'`
if [ "$OS" = "Windows_NT" ]; then
   mvn_prompt="mvn.cmd"
else
   mvn_prompt="mvn"
fi

MVN_CMD_WITHOUT_DEBUG="$mvn_prompt -V -U -Dmaven.repo.local=$local_repo -f $DEV_ROOT/prj/pom.xml -e -B -Dproject.official=true"
MVN_CMD="$MVN_CMD_WITHOUT_DEBUG --debug"

SECURITY=`echo $SECURITY | tr A-Z a-z`
if [ "$SECURITY" = "true" ]; then
  MVN_TEST_CMD="${MVN_CMD} -Dtest.security.enabled=true "
else
  MVN_TEST_CMD=$MVN_CMD
fi

check_markdown_copyright() {
    opened_list=${TMPDIR:?}/$JOBID/p4opened-markdown.txt
    markdown_list=${TMPDIR:?}/$JOBID/markdown-files.txt
    NEW_SRC="`echo $SRC | sed -e 's|\/|\\\/|g'`"

    p4 opened //dev/${SRC}/... > "$opened_list" 2>/dev/null || true
    sed -e '/ - delete /d' -e '/ - move\/delete /d' -e "s/\/\/dev\/$NEW_SRC\/\(.*\)#.*/\1/g" "$opened_list" | grep '\.md$' | sort -u > "$markdown_list"

    while IFS= read -r markdown_file; do
        [ -n "$markdown_file" ] || continue
        "$DEV_ROOT/prj/etc/copyright-check-enqueue.sh" "$markdown_file" || return $?
    done < "$markdown_list"
}

# provide a little visual space between the devenv output and the
# INFO: messages
echo ""
echo ""

if [ "$REMOTE_TEST" = "" ]; then
    fatalmsg "unknown test $REMOTE_TEST (error 3)"
fi
set +x

# ------------------------------------------------------------------------------
#                                 testdata
# ------------------------------------------------------------------------------

# report which perl(s) we have
#
cd tools/wls/infra/test

set -x
if [ "$PARALLEL" = "true" ]; then
    if [ -z "$STAGE_NAME" ]; then
        STAGEDATA=`perl ./remotetestprops$NEW.pl stages $REMOTE_TEST remotetestprops$NEW.env`
        saved_status=$?
        if [ $saved_status -ne 0 ]; then
            fatalmsg "non-zero status was returned by command:  remotetestprops$NEW.pl stages.  See Results directory/test.log for more info"
        fi
    else
        STAGEDATA=`echo $STAGE_NAME|sed 's/,/ /g'`
    fi

    unset PARALLEL_STAGES
    echo $STAGEDATA|grep ' ' >/dev/null
    # if there are more than 1 stage, run tests parallel
    if [ $? -eq 0 ]; then
        unset STAGE_NAME
        # save the parallel tests for later after the build and then enqueue them
        for i in $STAGEDATA; do
            PARALLEL_STAGES="$PARALLEL_STAGES $i"
        done
    fi
    # if there are not any tests to run parallel then just forget resetting REMOTE_TEST and set PARALLEL to be false
    if [ -z "$PARALLEL_STAGES" ]; then
        PARALLEL=false
    fi
fi

set +x

TESTDATA=`perl ./remotetestprops$NEW.pl tdata $REMOTE_TEST ./remotetestprops$NEW.env`
saved_status=$?
if [ $saved_status -ne 0 ]; then
    fatalmsg "non-zero status was returned by command:  remotetestprops$NEW.pl tdata.  See Results directory/test.log for more info"
fi
# ------------------------------------------------------------------------------
#                                 p4clean
# ------------------------------------------------------------------------------

infomsg "doing p4clean..."
timebomb 3600 &
timebomb_pid=$!
(
 rm -f /tmp/not_in_p4.sh.*
 cd $DEV_ROOT
 rbt_p4clean
) > ${TMPDIR:?}/$JOBID/p4clean.log 2>&1
saved_status=$?
# in case the timebomb went off give it a little time to complete before
# trying to killing it (it has a sleep 3 in it)
sleep 5
(
    echo ""
    echo "INFO: `date`: killing timebomb pid"
    echo ""
    kill -9 $timebomb_pid
    wait
) >> ${TMPDIR:?}/$JOBID/p4clean.log 2>&1
publish ${TMPDIR:?}/$JOBID/p4clean.log
for i in /tmp/not_in_p4.sh.*; do
    [ -f "$i" ] && publish "$i"
    rm -f "$i"
done
if [ $saved_status -eq 0 ]; then
    ! timebomb_went_off
    saved_status=$?
fi
if [ $saved_status -eq 0 ]; then
    infomsg "p4clean was successful"
else
    fatalmsg "p4clean failed" p4clean.log
fi

# ------------------------------------------------------------------------------
#                             setup - cfglocal.sh
# ------------------------------------------------------------------------------
cd $DEV_ROOT
unset DEV_ROOT JAVA_HOME
echo "Calling cfglocal.sh..."
. ./bin/cfglocal.sh
echo "[ After cfglocal.sh ] JAVA_HOME : $JAVA_HOME"
cd $DEV_ROOT/prj

if [ "$run_debug" != "" ]; then
    echo '----------------------------------------------------------------------'
    echo set start
    echo '----------------------------------------------------------------------'
    set
    echo '----------------------------------------------------------------------'
    echo set end
    echo '----------------------------------------------------------------------'
fi

if [ "$TESTDATA" = "" ]; then
    fatalmsg "no build or test targets for $REMOTE_TEST, missing setup line (error 4)"
fi

if [ "$run_debug" != "" ]; then
    infomsg  "debug mode: quiting right after collecting RQ config i.e. TESTDATA etc."
    exit 0
fi

# kick off sub jobs before build
if [ "$PARALLEL" = "true" ]; then
    count=1
    jobfile=$DEV_ROOT/tools/wls/infra/test/queued/$RBT_QUEUED_ID
    if [ -s $jobfile ]; then
        for i in $PARALLEL_STAGES; do
            # copy the current RQ job id saving the characteristics and replacing argument 2 (the test name) with all the tests
            # then use the regular rbt.sh function to "return" the new local job to the central RQ.  Use printf to add leading
            # zero so the rdequeuer will alphabeticaly sort them properly and roughly dequeue them in "order".
            NEWJOB=${RBT_QUEUED_ID}-`printf "%02d" $count`
            # sometimes the queued directory does not exist
            mkdir -p $DEV_ROOT/tools/wls/infra/test/queued
            sed < $jobfile \
                -e '1,2s/\r$//g' -e '2s/ PARALLEL=[^ ]*/ /g' -e '2s/ SYNCTO=[^ ]*/ /g' -e '2s/ STAGE_NAME=[^ ]*/ /g' |\
                awk > $DEV_ROOT/tools/wls/infra/test/queued/$NEWJOB \
                    -v test=${REMOTE_TEST}.$i -v stage_name=$i -v syncto=$SYNCTO \
                    'FNR == 1 { print } FNR == 2 {if ($1 == "sh") {$3=test} else {$2=test}; print $0 " PARALLEL=false SYNCTO=" syncto " STAGE_NAME=" stage_name }'
            rquemove -r $NEWJOB
            infomsg "$NEWJOB created to run $i"
            count=`expr $count + 1`
        done
    else
        warnmsg "Cannot find master job file $jobfile to modify for parallel jobs, internal error"
    fi
    unset PARALLEL_STAGES
fi

UNKNOWN=false
for i in $TESTDATA; do
    # if the test is special keywork unknown then all tests run after this
    # point are not known to be clean at this change number.  When they fail
    # issue an informational message to that effect so that people know that
    # it might be "ok" that they are failing.
    if [ "$i" = "UNKNOWN" -o "$i" = "unknown" ]; then
        if [ "$UNKNOWN" != "true" ]; then
            UNKNOWN=true
            infomsg "Tests run after this point are not known to be clean at this change number."
            infomsg "This is usually because this test does not have a CruiseControl machine"
            infomsg "or because this test's CruiseControl machine does not syncronize with"
            infomsg "the platform clean label."
        fi
        continue
    fi
    build_target=`echo $i | awk -F: '{print $1}' | sed 's/,/ /g'`
    test_dir=`echo $i | awk -F: '{print $2}'`
    test_target="`echo $i | awk -F: '{print $3}' | sed 's/,/ /g'`"
    WIN_DIR="`echo $test_dir | sed 's|/|\\\|g'`"
    # we do this just to use in the cmd call for remoteDRT_ctscheckintest
    export WIN_DIR
    UNIX_DIR="`echo $test_dir | sed 's|\\\|/|g'`"
    # used to name the log files
    test_name="`echo $test_dir $test_target | sed -e 's|/|_|g' -e 's|\\\|_|g' -e 's| |_|g' -e 's|\.|_|g' -e 's/^_*//'`"

    # run examples in master job of remote.full, so no need to run build in master job
    if [ "$REMOTE_TEST" != "remote.full" -o "$PARALLEL" != "true" ]; then
        cd $DEV_ROOT/prj
        infomsg "build starting..."
        publish_touchfile
        if [ "$build_target" = "build_installer" ]; then
           timebomb 7200 &
           timebomb_pid=$!
           (set -x ; env | sort ; ls -l $JAVA_HOME ; $MVN_CMD validate -P copyright && check_markdown_copyright && $MVN_CMD -DskipTests=true -Pcoherence,-modules clean install && $MVN_CMD clean install -P-coherence,modules,-javadoc -nsu -Dproject.installer -DskipTests ) > ${TMPDIR:?}/$JOBID/build.log 2>&1
        elif [ -n "$STAGE_NAME" ]; then
          timebomb 600 &
          timebomb_pid=$!
          (set -x ; env | sort ; ls -l $JAVA_HOME ; $MVN_CMD validate -P copyright && check_markdown_copyright ) > ${TMPDIR:?}/$JOBID/build.log 2>&1
        else
           timebomb 2400 &
           timebomb_pid=$!
           (set -x ; env | sort ; ls -l $JAVA_HOME ; $MVN_CMD validate -P copyright && check_markdown_copyright && $MVN_CMD -Pcoherence,-modules clean install -DskipTests && $MVN_CMD -DskipTests -P-coherence,modules,-javadoc -nsu clean install) > ${TMPDIR:?}/$JOBID/build.log 2>&1
        fi
        saved_status=$?

        # in case the timebomb went off give it a little time to complete before
        # trying to killing it (it has a sleep 3 in it)
        sleep 5
        (
                echo ""
                echo "INFO: `date`: killing timebomb pid"
                echo ""
                kill -9 $timebomb_pid
                echo ""
                echo "INFO: `date`: killing any remaining processes"
                echo ""
                $DEV_ROOT/tools/wls/infra/build/killbt.sh -u
                wait
        ) >> ${TMPDIR:?}/$JOBID/build_killbt.log 2>&1
        publish ${TMPDIR:?}/$JOBID/build_killbt.log

        # do this *after* killing the timebomb because sometimes the time it takes to do publish_build is long enough to cause the timebomb to trigger... in other words, the build is successful but it displays as timebomb'ed because the publish is really slow (we are publishing too much!!!)
        publish_build ${TMPDIR:?}/$JOBID/build.log

        # when the timebomb kills stuff sometimes ant doesn't return failure
        if [ $saved_status -eq 0 ]; then
            ! timebomb_went_off
            saved_status=$?
        fi
        if [ $saved_status -eq 0 ]; then
            infomsg "build successful"
            if [ -f $DEV_ROOT/dist/coherence_generic.jar ]; then
                publish_build $DEV_ROOT/dist/coherence_generic.jar
            fi
        else
            fatalmsg_noexit "build failed, ant returned non-zero status ($saved_status)" build.log
            # this is an error, don't exit immediately, break out of the build/test loop so that it can hit the "spin" logic
            break
        fi
    fi

    if [ "$test_target" != "" -a "$test_target" != "none" ];then
        # kill remote desktop sessions since they messup gui tests
        test_timebomb=${METATIME:-36000}
        timebomb $test_timebomb &
        timebomb_pid=$!
        if [ -n "$STAGE_NAME" ]; then
            infomsg "test $UNIX_DIR $FUNCTEST $STAGE_NAME clean $test_target starting..."
            (set -x ; cd $DEV_ROOT && $MVN_TEST_CMD clean install -Pcoherence,-modules,${STAGE_NAME} && $MVN_TEST_CMD clean $test_target -P-coherence,modules,${STAGE_NAME} -nsu) > ${TMPDIR:?}/$JOBID/$test_name.log 2>&1
            saved_status=$?
        else
            if [ "$REMOTE_TEST" = "remote.full" -a "$PARALLEL" = "true" ]; then
                infomsg "test examples_maven starting..."
            else
                infomsg "test $UNIX_DIR $FUNCTEST $test_target starting..."
            fi
            echo $REMOTE_TEST | grep "remote.function" >/dev/null
            if [ $? = 0 ]; then
                test_name=${FUNCTEST}_functional
                (set -x ; cd $DEV_ROOT/prj && $MVN_TEST_CMD -DskipTests clean install -Pcoherence,-modules && $MVN_TEST_CMD -DskipTests -P-coherence,modules -nsu clean install && $MVN_TEST_CMD -P-coherence,modules -nsu -pl $test_dir/$FUNCTEST clean $test_target) > ${TMPDIR:?}/$JOBID/$test_name.log 2>&1
            elif [ "$REMOTE_TEST" = "remote.distribution" ]; then
                test_name=${FUNCTEST}_distribution
                (set -x ; cd $DEV_ROOT/prj && $MVN_TEST_CMD clean install -Pcoherence,-modules && $MVN_TEST_CMD -amd -P-coherence,modules -nsu -pl $test_dir/$FUNCTEST $test_target) > ${TMPDIR:?}/$JOBID/$test_name.log 2>&1
            elif [ "$REMOTE_TEST" = "remote.compatibility" ]; then
                test_name=${FUNCTEST}_compatibility
                (set -x ; cd $DEV_ROOT/prj && $MVN_TEST_CMD clean install -Pcoherence,-modules && $MVN_TEST_CMD -P-coherence,modules -nsu -pl $test_dir/$FUNCTEST $test_target ) > ${TMPDIR:?}/$JOBID/$test_name.log 2>&1
            # main job of remote.full
            elif [ "$REMOTE_TEST" = "remote.full" ]; then
                test_name=examples_maven
                (set -x ; ls -l $JAVA_HOME ; env | sort ; cd $DEV_ROOT/prj && $MVN_CMD_WITHOUT_DEBUG -DskipTests=true -Pcoherence,-modules clean install && $MVN_CMD_WITHOUT_DEBUG -DskipTests=true -P-coherence,modules clean install -nsu && $MVN_CMD_WITHOUT_DEBUG -Pexamples clean install -nsu) > ${TMPDIR:?}/$JOBID/${test_name}.log 2>&1
                saved_status=$?
                if [ $saved_status -eq 0 ]; then
                  infomsg "test ${test_name} passed"
                  publish ${TMPDIR:?}/$JOBID/${test_name}.log
                  test_name=examples_gradle
                  infomsg "test $test_name starting..."
                  (set -x ; ls -l $JAVA_HOME ; export GRADLE_USER_HOME=$local_repo ; env | sort ; cd $DEV_ROOT/prj/examples && ./gradlew -Dmaven.repo.local=$local_repo -Dhttp.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttps.proxyPort=80 "-Dhttp.nonProxyHosts=localhost|*.oracleads.com|*.us.oracle.com|*.uk.oracle.com|*.ca.oracle.com|*.oraclecorp.com|*.oracleportal.com|*.oraclevcn.com" clean build --info) > ${TMPDIR:?}/$JOBID/${test_name}.log 2>&1
                  saved_status=$?
                else
                  warnmsg "test ${test_name} failed" $test_name.log
                fi
            else
                (set -x ; cd $DEV_ROOT && $MVN_TEST_CMD $test_target) > ${TMPDIR:?}/$JOBID/$test_name.log 2>&1
            fi
        fi
        if [ -z "$saved_status" ]; then
          saved_status=$?
        fi

        debugmsg "test run complete, starting process cleanup and log copies"

        # in case the timebomb went off give it a little time to complete before
        # trying to killing it (it has a sleep 3 in it)
        sleep 5
        (
           echo ""
           echo "INFO: `date`: killing timebomb pid"
           echo ""
           kill -9 $timebomb_pid
           echo ""
           echo "INFO: killing any remaining processes"
           echo ""
           $DEV_ROOT/tools/wls/infra/build/killbt.sh -u
        ) >> ${TMPDIR:?}/$JOBID/${test_name}_killbt.log 2>&1
        publish ${TMPDIR:?}/$JOBID/$test_name.log
        publish ${TMPDIR:?}/$JOBID/${test_name}_killbt.log
        # when the timebomb kills stuff sometimes ant doesn't return failure
        if [ $saved_status -eq 0 ]; then
           ! timebomb_went_off
           saved_status=$?
        fi
        if [ $saved_status -ne 0 -o "$PUBLISH" = "true" ]; then
            publish_drt $UNIX_DIR
        fi

        if [ $saved_status -eq 0 ]; then
            if [ "$REMOTE_TEST" = "remote.full" -a "$PARALLEL" = "true" ]; then
                infomsg "test ${test_name} passed"
            else
                infomsg "test $UNIX_DIR $FUNCTEST $STAGE_NAME $test_target passed"
            fi
        else
            if [ "$UNKNOWN" = "true" ]; then
                infomsg "This test is not known to be clean at this change number."
            fi
            if [ "$REMOTE_TEST" = "remote.full" -a "$PARALLEL" = "true" ]; then
                warnmsg "test ${test_name} failed" $test_name.log
            else
                warnmsg "test $UNIX_DIR $FUNCTEST $STAGE_NAME $test_target failed" $test_name.log
            fi
        fi
    else
        infomsg "no tests specified"
    fi

done


set +e

# ------------------------------------------------------------------------------
#                                 submit
# ------------------------------------------------------------------------------

cd $DEV_ROOT

SUBMIT="`echo $SUBMIT | tr A-Z a-z`"

syncfile=`mktemp syncfile.XXXXXXXX`

# if this is not being run parallel then all the related jobs have completed
RELATED_JOBS="completed"
rjobsync_status=0

if [ "$RBT_QUEUED_ID" != "$shortid" -o "$PARALLEL" = "true" ]; then
    echo "RBT_QUEUED_ID=$RBT_QUEUED_ID,shortid=$shortid"
    echo "Job sync..."
    tries=0
    while [ $tries -lt 5 -a ! -s $syncfile ]; do
        ( rjobsync $RBT_QUEUED_ID x ) > $syncfile
        rjobsync_status=$?
        if [ ! -s $syncfile ]; then
            sleep 60
        fi
        tries=`expr $tries + 1`
    done
    if [ $rjobsync_status -eq 0 -a -s $syncfile ]; then
        cat $syncfile
        echo "checking job sync..."
        # queued failed success running unknown
        grep -e ' queued$' -e ' failed$' -e ' running$' -e ' unknown$' < $syncfile
        if [ $? -eq 0 ]; then
            echo "Some job is still active, do not send email."
            RELATED_JOBS="active"
        else
            echo "All (?) jobs completed, send email."
            RELATED_JOBS="completed"
        fi
    else
        rjobsync_status=1;
        echo "rjobsync exited abnormally. The return code is $rjobsync_status"
    fi
fi

STATUS="`test_status`"
if [ "$RELATED_JOBS" = "completed" -a $rjobsync_status -eq 0 ]; then
    if [ "$SUBMIT" = "true" -o "$SUBMIT" = "yes" -o "$SUBMIT" = "requeue" ]; then
        if [ "$STATUS" = "success" ]; then
            auto_submit
        else
            warnmsg "Submit flag is on, but the tests did not pass"
        fi
    fi
fi

if [ "$STATUS" != "success" ]; then
    warnmsg "Test(s) failed, see the <a href=\"http://home.us.oracle.com/internal/coherence/$SRC/remote.html#failureguide\">Test Failure Guide</a> for more information."
fi

if [ "$RELATED_JOBS" = "completed" -a $rjobsync_status -eq 0 ]; then
    if [ "$RBT_QUEUED_ID" != "$shortid" ]; then
        REAL_REMOTE_TEST=`awk 'FNR == 2 {print $2;exit}' $RQSITE/$SRC/status/$shortid`
    else
        if [ "$PARALLEL" = "true" ]; then
            REAL_REMOTE_TEST=$old_REMOTE_TEST
        else
            REAL_REMOTE_TEST=$REMOTE_TEST
        fi
    fi
    echo "$REAL_REMOTE_TEST" | grep wls > /dev/null
    if [ $? -eq 0 ]; then
       # test coherence bits in WLS depot
       infomsg "Submit WLS job with coherence bits"
       COH_BRANCH=$SRC
       COH_JOBID=$RBT_QUEUED_ID
       COH_SYNCTO=$SYNCTO
       COH_FILECOUNT=`p4 opened //dev/${SRC}/... |wc -l`
       export REMOTE_P4USER COH_JOBID COH_SUBMIT COH_FILECOUNT COH_SYNCTO COH_BRANCH
       ant -f tools/wls/infra/build.xml wls.remotetest > ${TMPDIR:?}/$JOBID/wls_remotetest.log 2>&1
       timebomb 300 &
       timebomb_pid=$!
       saved_status=$?
       # in case the timebomb went off give it a little time to complete before
       # trying to killing it (it has a sleep 3 in it)
       sleep 5
       (
           echo ""
           echo "INFO: `date`: killing timebomb pid"
           echo ""
           kill -9 $timebomb_pid
           echo ""
           echo "INFO: `date`: killing any remaining processes"
           echo ""
           $DEV_ROOT/tools/wls/infra/build/killbt.sh -u
       ) >> ${TMPDIR:?}/$JOBID/wls_remotetest_killbt.log 2>&1

       publish $TMPDIR/$JOBID/wls_remotetest.log
       publish $TMPDIR/$JOBID/wls_remotetest_killbt.log

       if [ $saved_status -eq 0 ]; then
           wls_joblink="`grep http < $TMPDIR/$JOBID/wls_remotetest.log | sed 's/.* //'`"
           infomsg "WLS job is submitted successfully, job link: $wls_joblink"
       else
           fatalmsg_noexit "Submit WLS job failed"
       fi
    fi
fi

if [ -f .rbt.envs ]; then
    . ./.rbt.envs
fi

# clear the username associated with this job
rqstatus -j $JOBID -u

set +e
