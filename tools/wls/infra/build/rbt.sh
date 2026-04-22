#!/bin/sh
# Copyright 2006 BEA Systems, Inc.
#
# rsh/rexec type client for use by the "bt" account.  This assumes it is
# being run under the "bt" account and should be rexec'ing into boxes using the
# "bt" account.  The script automatically figures out if the target machines
# are UNIX or Windows and also cd's to the root directory of the local p4
# client and current branch to execute the given command
# (i.e. /weblogic/dev/src).
#
# Usage:
# . ./rbt.sh (which defines the function "rbt" which can be used as
# shown below)
#
# rbt [-v] [-p] <hostlist> <command> [<args]...]
# -v   means debug/verbose output, must be first switch
# -p   do all hosts in parallel
# <hostlist> can be comma separated, quoted with spaces, or a mixture of both.
#
# Examples:
# rbt host cmd args
# rbt host1,host2,host3 cmd args
# rbt "host1 host2 host3" cmd args
# rbt "host1,host2 host3" cmd args
# rbt unixhost,winhost cmd args
#
# rbt stinson,qa117,hepcat,qa83 infra/build/killbt.sh
# rbt stinson,qa117,hepcat,qa83 ps -fu bt
# rbt stinson,qa117 p4 sync -f infra/...
# rbt stinson,qa117 p4 sync ...@999999
# rbt stinson,qa117 p4 changes -m1 ...
# rbt -p stinson,qa117,hepcat,qa83 infra/test/testrel_all.sh
#
# Notes: It does a 'p4 client -o' to figure out the client root and OS and
# caches that information in ./infra/.cache_MACHINE.  It checks to see if
# rbt.java is more recent than rbt.class, if not it javac's rbt.java.  Depends
# on GPL'd implementation of rexec in Java (I didn't have to touch a line of
# that code to get this working, checked in the GPL'd code to 3rdparty)
#
# FIXME:
#    - create an ant task with the same functionality
#    - allow easy turn on/off of debugging (client root and cmd echoing)
#
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required

if [ -f .rbt.envs ]; then
    . ./.rbt.envs
    #export PATH="$PATH:$DEV_ROOT/tools/wls/infra"
fi

if [ -f ../../.rbt.envs ]; then
    . ../../.rbt.envs
    #export PATH="$PATH:$DEV_ROOT/tools/wls/infra"
fi
#
get_generics() {
    : ${DEV_ROOT:?} ${SRC:?}

    rc=$DEV_ROOT/tools/wls/infra/build/statuspage/btstat${NEW_SRC}rc

    if [ "$#" -ne 0 ]; then
	echo FATAL: unknown option "$@" 1>&2
	return 1
    fi

    sed 's/@//' < $rc |\
	egrep '^section|^test.*=' |\
	while read rcline; do
	    set ${rcline:-""}
	    type=$1
	    cfg=$2
	    envs=$3
	    case $type in
	      section)
		sectionName="`echo \"${rcline}\" | sed 's/section  *//'`"
		section=`echo ${sectionName} | tr ' ' '_'`
		;;
	      test)
		cfg="`basename $cfg .cfg`"
		if [ -f $DEV_ROOT/tools/wls/infra/build/config/$cfg.cfg ]; then
		  echo $cfg $envs $rc $section
		else
		  echo "WARNING: renqueue_all_generics $cfg does not exist for $envs" 1>&2
		fi
		;;
	    esac
    done
}

# maybe add a -v (verify switch to be called each time by btstatus.sh and send email on error?)
renqueue_all_generics() {
    get_generics | while read cfg envs rc section; do
	# FIXME: herrlich@bea.com June 8, 2004
	# we need to remove this testrel.sh argument and just make qa vs
	# alternative something configured in the .cfg file
	shortrc=`basename "$rc"`
	case $cfg in
	    *-multiclient*|*-s2s*|*-iiop*|*-eos*|*-applet*|*-stress*|*-coverage*)
		renqueue "$envs" infra/test/testrel.sh sync alternative \
		    CONFIG=$cfg.cfg RC_FILE=$shortrc RC_SECTION=$section ">infra/test/testrel_$cfg.cfg.out 2>&1" ;;
	    *)
		renqueue "$envs" infra/test/testrel.sh sync qa \
		    CONFIG=$cfg.cfg RC_FILE=$shortrc RC_SECTION=$section ">infra/test/testrel_$cfg.cfg.out 2>&1" ;;
	esac
    done
}

#
# get list of @ machines from btstat*rc, do *not* include this machine
# using get_machines -a return *all* machines except this machine
# using get_machines -j   returns all             jenkins agents
# using get_machines -jio returns all idle online jenkins agents
# using get_machines -l return the load test machines from
# the btstatsrc_loadrc file (it ignores @)
# using get_machines -b return the load test machines from
# the btstatsrc_basicloadrc file (it ignores @)
# using get_machines -w return the weekly test machines from
# the btstatsrc_weeklyrc file (it ignores @)
#
get_machines() {
    : ${DEV_ROOT:?} ${SRC:?}
    rc=$DEV_ROOT/tools/wls/infra/build/statuspage/btstat${NEW_SRC}rc

    if [ "$#" -eq 1 -a "$1" = "-a" ]; then
	filter=""
    elif [ "$#" -eq 1 -a "$1" = "-l" ]; then
	filter="/load/"
	rc=$DEV_ROOT/tools/wls/infra/build/statuspage/btstat${NEW_SRC}_loadrc
    elif [ "$#" -eq 0 ]; then
	filter="/@/"
    elif [ "$#" -eq 1 -a "$1" = "-b" ]; then
	filter="/load/"
	rc=$DEV_ROOT/tools/wls/infra/build/statuspage/btstat${NEW_SRC}_basicloadrc
    elif [ "$#" -eq 0 ]; then
	filter="/@/"
    elif [ "$#" -eq 1 -a "$1" = "-w" ]; then
        filter="/weekly/"
        rc=$DEV_ROOT/tools/wls/infra/build/statuspage/btstat${NEW_SRC}_weeklyrc
    elif [ "$#" -eq 1 -a "$1" = "-j" ]; then
	curl --noproxy '*' --fail --silent --show-error "${JENKINS_URL}computer/api/json" | rbt_python $DEV_ROOT/tools/wls/infra/build/get_machines.py
	return
    elif [ '(' "$#" -eq 1 -o "$#" -eq 2 ')' -a "$1" = "-jio" ]; then
	curl --noproxy '*' --fail --silent     "${JENKINS_URL}computer/$2${2:+/}api/json" | rbt_python $DEV_ROOT/tools/wls/infra/build/get_machines.py -jio
	return
    elif [ "$#" -eq 0 ]; then
        filter="/@/"
    else
	echo FATAL: unknown option "$@" 1>&2
	return 1
    fi

    # just return empty if there is no rc file
    if [ -f "$rc" ]; then
	this_host="`uname_n`"
	(
	    # grab the machines being filtered
	    egrep -v '^[ 	]*(rb|wget|section|header|branch|html|rbtmachines|dynamicexclude|nightlyexclude|nightlyonly|#|$)' < $rc |\
		awk "$filter{print \$3}"
	    # grab the HOST= machines
	    sed -n '/^[	 ]*test.*@HOST=/s/.*@HOST=\([^, ]*\).*/\1/p' < $rc
	    # grab the rbtmachines and nightlyexclude machines
	    egrep '^[ 	]*(rbtmachines|nightlyexclude|nightlyonly)' < $rc |\
		awk '{for (i=2; i <= NF; i++) print $i}'
	) |	tr -d '\r' | egrep -v "=|@$this_host$|^$this_host$|^$" |\
	    sed 's/^@//' |\
	    tr A-Z a-z |\
	    sort -u
    fi
}

which python 2>&1 >/dev/null
if [ $? -eq 0 ]; then
    rbt_python() {
	python "$@"
    }
else
    which python3 2>&1 >/dev/null
    if [ $? -eq 0 ]; then
	rbt_python() {
	    LC_ALL="en_US.UTF-8" python3 "$@"
	}
    fi
fi

#
# return uname -n with domain stripped off, currently MacOS returns the fully qualified hostname
#
uname_n() {
    uname -n | tr A-Z a-z | sed -e 's/\.us\.oracle\.com//' -e 's/\.idc\.oracle\.com//' -e 's/\.beasys\.com//' -e 's/\.bea\.com//'
}

#
# return date -u without being effected by LANG
#
# most of the time this will be the same as date -u but not always if LANG is set to something odd
#
date_u() {
    date -u +'%a %b %d %H:%M:%S %Z %Y'
}

#
# kill -0 PID that works on windows by using tasklist
#
kill_0() {
    : ${1:?pid}
    if [ -z "$OS" ]; then
	. $DEV_ROOT/tools/wls/infra/infraenv.sh
    fi
    if [ "$OS" = "Windows_NT" ]; then
	test `tasklist /nh /fi "PID eq $1" | grep -v -e "^INFO:" -e "^$" | wc -l` -ge 1
    else
	# if the pid exists (kill -0 status=0) then check to make sure it isn't a zombie process in the
	# /proc/PID/status file and don't count it as a process that exists if it is a zombie
	kill -0 "$1" 2>/dev/null
	if [ $? -eq 0 ]; then
	    if [ -f /proc/$1/status ]; then
		grep "^State:" /proc/$1/status | grep -i zombie >/dev/null
		if [ $? -eq 0 ]; then
		    false # exists and is a zombie
		else
		    true  # exists and is not a zombie
		fi
	    else
		true # exists but no proc file, assume not a zombie
	    fi
	else
	    false # doesn't exist
	fi
    fi
}

#
# helper to test whether or not we are using git or not
#
using_git() {
    [ -s $DEV_ROOT/.git/config -o -s $DEV_ROOT/../.git/config ]
}

#
# helper to test whether or not we are using p4 or not
#
using_p4() {
    ! using_git
}

#
# helper to test whether or not we are running (a job) in a jenkins slave
#
using_jenkins() {
    [ -n "$JENKINS_URL" -a -n "$BUILD_URL" ]
}

#
# helper to test whether or not we are running in a docker container
#
using_docker() {
    # if there is no /proc filesystem then we aren't using docker
    if [ -f /proc/1/cmdline ]; then
	# if pid 1 command line has systemd or init then we are not using docker
	grep -e systemd -e init </proc/1/cmdline >/dev/null
	if [ $? -eq 0 ]; then
	    false
	else
	    true
	fi
    else
	false
    fi
}

#
# translate between the cpu model number and benchmark from https://www.cpubenchmark.net/high_end_cpus.html
get_cpu_benchmark() {
    name=`echo $1 | sed -e 's/^ *//g' -e 's/  */ /g' -e 's/(R)//g' -e 's/ CPU / /g' -e 's/ 0 / /'`
    case "$name" in
	'Intel Core Processor (Skylake)')	echo 26000 ;; #Julie: unable to find this model in the benchmark link, so set it freely
	'Intel Xeon E5-2699 v3 @ 2.30GHz')	echo 25192 ;;
	'Intel Xeon E5-2697 v3 @ 2.60GHz')	echo 22556 ;;
	'Intel Xeon E5-2695 v3 @ 2.30GHz')	echo 21123 ;;
	'Intel Xeon E5-2690 v3 @ 2.60GHz')	echo 19849 ;;
	'Intel Xeon E5-2680 v3 @ 2.50GHz')	echo 19623 ;;
	'Intel Xeon E5-2670 v3 @ 2.30GHz')	echo 17849 ;;
	'Intel Xeon E5-2697 v2 @ 2.70GHz')	echo 17516 ;;
	'Intel Xeon E5-2690 v2 @ 3.00GHz')	echo 17304 ;;
	'Intel Xeon E5-2695 v2 @ 2.40GHz')	echo 17231 ;;
	'Intel Xeon E5-2687W v2 @ 3.40GHz')	echo 16671 ;;
	'Intel Xeon E5-2667 v2 @ 3.30GHz')	echo 16403 ;;
	'Intel Xeon E5-2680 v2 @ 2.80GHz')	echo 16340 ;;
	'Intel Xeon E5-2660 v3 @ 2.60GHz')	echo 16272 ;;
	'Intel Xeon E5-1680 v2 @ 3.00GHz')	echo 16224 ;;
	'Intel Core i7-5960X @ 3.00GHz')	echo 16138 ;;
	'Intel Xeon E5-2650 v3 @ 2.30GHz')	echo 15768 ;;
	'Intel Xeon E5-1660 v3 @ 3.00GHz')	echo 15532 ;;
	'Intel Xeon E5-2687W v3 @ 3.10GHz')	echo 14894 ;;
	'Intel Xeon E5-2670 v2 @ 2.50GHz')	echo 14892 ;;
	'Intel Xeon E5-2640 v3 @ 2.60GHz')	echo 14550 ;;
	'Intel Xeon E5-2687W @ 3.10GHz')	echo 14525 ;;
	'Intel Xeon E5-2690 @ 2.90GHz')		echo 14354 ;;
	'Intel Xeon E5-1650 v3 @ 3.50GHz')	echo 14074 ;;
	'Intel Core i7-4960X @ 3.60GHz')	echo 13997 ;;
	'Intel Xeon E5-2689 @ 2.60GHz')		echo 13897 ;;
	'Intel Xeon E5-2658 v2 @ 2.40GHz')	echo 13875 ;;
	'Intel Xeon E5-2660 v2 @ 2.20GHz')	echo 13659 ;;
	'Intel Core i7-5930K @ 3.50GHz')	echo 13472 ;;
	'Intel Xeon E5-1660 v2 @ 3.70GHz')	echo 13450 ;;
	'Intel Xeon E5-2650 v2 @ 2.60GHz')	echo 13276 ;;
	'Intel Xeon E5-2680 @ 2.70GHz')		echo 13230 ;;
	'Intel Xeon E5-2630 v3 @ 2.40GHz')	echo 13218 ;;
	'Intel Core i7-4930K @ 3.40GHz')	echo 13122 ;;
	'Intel Core i7-5820K @ 3.30GHz')	echo 12947 ;;
	'Intel Xeon E5-2670 @ 2.60GHz')		echo 12867 ;;
	'Intel Core i7-3970X @ 3.50GHz')	echo 12786 ;;
	'Intel Core i7-3960X @ 3.30GHz')	echo 12722 ;;
	'Intel Xeon E5-1660 @ 3.30GHz')		echo 12675 ;;
	'Intel Xeon E5-1650 v2 @ 3.50GHz')	echo 12495 ;;
	'Intel Xeon E5-2665 @ 2.40GHz')		echo 12333 ;;
	'Intel Core i7-3930K @ 3.20GHz')	echo 12135 ;;
	'Intel Xeon E5-2660 @ 2.20GHz')		echo 11961 ;;
	'Intel Xeon E5-4650 @ 2.70GHz')		echo 11960 ;;
	'Intel Xeon E5-2643 v2 @ 3.50GHz')	echo 11735 ;;
	'Intel Xeon E5-1650 @ 3.20GHz')		echo 11646 ;;
	'Intel Core i7-4790K @ 4.00GHz')	echo 11298 ;;
	'Intel Xeon E5-2470 @ 2.30GHz')		echo 11149 ;;
	'Intel Xeon E5-2630 v2 @ 2.60GHz')	echo 10615 ;;
	'Intel Xeon E5-1630 v3 @ 3.70GHz')	echo 10589 ;;
	'Intel Xeon E3-1276 v3 @ 3.60GHz')	echo 10570 ;;
	'Intel Xeon E5-2637 v3 @ 3.50GHz')	echo 10519 ;;
	'Intel Xeon E5-2667 @ 2.90GHz')		echo 10482 ;;
	'Intel Xeon E5-2650 @ 2.00GHz')		echo 10443 ;;
	'Intel Xeon E3-1280 v3 @ 3.60GHz')	echo 10420 ;;
	'Intel Xeon E3-1271 v3 @ 3.60GHz')	echo 10390 ;;
	'AMD FX-9590 Eight-Core')		echo 10296 ;;
	'Intel Core i7-4770K @ 3.50GHz')	echo 10264 ;;
	'Intel Xeon E3-1246 v3 @ 3.50GHz')	echo 10184 ;;
	'Intel Core i7-4790 @ 3.60GHz')		echo 10168 ;;
	'Intel Core i7-4770R @ 3.20GHz')	echo 10112 ;;
	'Intel Core i7-4790S @ 3.20GHz')	echo 10018 ;;
	'Intel Xeon E3-1270 v3 @ 3.50GHz')	echo 10014 ;;
	'Intel Core i7-4771 @ 3.50GHz')		echo 9988 ;;
	'Intel Xeon E3-1290 V2 @ 3.70GHz')	echo 9930 ;;
	'Intel Core i7-4960HQ @ 2.60GHz')	echo 9920 ;;
	'Intel Core i7-4770 @ 3.40GHz')		echo 9911 ;;
	'Intel Core i7-4820K @ 3.70GHz')	echo 9873 ;;
	'Intel Core i7-4940MX @ 3.10GHz')	echo 9811 ;;
	'Intel Xeon E3-1241 v3 @ 3.50GHz')	echo 9796 ;;
	'Intel Core i7-4910MQ @ 2.90GHz')	echo 9796 ;;
	'Intel Xeon E3-1280 V2 @ 3.60GHz')	echo 9783 ;;
	'Intel Xeon W3690 @ 3.47GHz')		echo 9763 ;;
	'Intel Xeon E5-1620 v3 @ 3.50GHz')	echo 9739 ;;
	'Intel Xeon E3-1275 v3 @ 3.50GHz')	echo 9721 ;;
	'Intel Xeon E3-1240 v3 @ 3.40GHz')	echo 9718 ;;
	'Intel Xeon E5-2640 @ 2.50GHz')		echo 9708 ;;
	'Intel Xeon E3-1286 v3 @ 3.70GHz')	echo 9698 ;;
	'Intel Xeon E5-2658 @ 2.10GHz')		echo 9667 ;;
	'Intel Xeon E3-1270 V2 @ 3.50GHz')	echo 9636 ;;
	'Intel Xeon E3-1231 v3 @ 3.40GHz')	echo 9635 ;;
	'Intel Core i7-3770K @ 3.50GHz')	echo 9634 ;;
	'Intel Core i7-4980HQ @ 2.80GHz')	echo 9617 ;;
	'AMD FX-9370 Eight-Core')		echo 9615 ;;
	'Intel Xeon E3-1245 v3 @ 3.40GHz')	echo 9589 ;;
	'Intel Core i7-4930MX @ 3.00GHz')	echo 9562 ;;
	'Intel Xeon E5-1620 v2 @ 3.70GHz')	echo 9492 ;;
	'Intel Core i7-4770HQ @ 2.20GHz')	echo 9433 ;;
	'Intel Core i7-4790T @ 2.70GHz')	echo 9427 ;;
	'Intel Xeon E5-2440 v2 @ 1.90GHz')	echo 9425 ;;
	'Intel Core i7-4770S @ 3.10GHz')	echo 9422 ;;
	'Intel Xeon E3-1230 v3 @ 3.30GHz')	echo 9406 ;;
	'Intel Core i7-3770 @ 3.40GHz')		echo 9390 ;;
	'Intel Core i7 X 990 @ 3.47GHz')	echo 9380 ;;
	'Intel Core i7-4870HQ @ 2.50GHz')	echo 9326 ;;
	'Intel Xeon E3-1240 V2 @ 3.40GHz')	echo 9297 ;;
	'Intel Xeon X5690 @ 3.47GHz')		echo 9285 ;;
	'Intel Xeon X5680 @ 3.33GHz')		echo 9275 ;;
	'Intel Xeon W3680 @ 3.33GHz')		echo 9268 ;;
	'Intel Core i7-4860HQ @ 2.40GHz')	echo 9259 ;;
	'Intel Xeon E5-2637 v2 @ 3.50GHz')	echo 9240 ;;
	'Intel Xeon E3-1275 V2 @ 3.50GHz')	echo 9237 ;;
	'Intel Core i7-3920XM @ 2.90GHz')	echo 9202 ;;
	'Intel Core i7-3940XM @ 3.00GHz')	echo 9186 ;;
	'Intel Core i7-4900MQ @ 2.80GHz')	echo 9176 ;;
	'Intel Core i7-4850HQ @ 2.30GHz')	echo 9175 ;;
	'AMD FX-8370 Eight-Core')		echo 9159 ;;
	'Intel Xeon E5-1620 @ 3.60GHz')		echo 9117 ;;
	'AMD Opteron 6282 SE')			echo 9116 ;;
	'Intel Xeon E3-1245 V2 @ 3.40GHz')	echo 9055 ;;
	'Intel Core i7-4760HQ @ 2.10GHz')	echo 9044 ;;
	'Intel Core i7-3820 @ 3.60GHz')		echo 9028 ;;
	'AMD FX-8350 Eight-Core')		echo 9020 ;;
	'Intel Core i7-3840QM @ 2.80GHz')	echo 9004 ;;
	'Intel Core i7-3770S @ 3.10GHz')	echo 8941 ;;
	'Intel Core i7 X 980 @ 3.33GHz')	echo 8925 ;;
	'Intel Core i7-2700K @ 3.50GHz')	echo 8924 ;;
	'Intel Xeon E5-2630 @ 2.30GHz')		echo 8902 ;;
	'Intel Xeon E3-1265L v3 @ 2.50GHz')	echo 8859 ;;
	'Intel Xeon E3-1230 V2 @ 3.30GHz')	echo 8839 ;;
	'Intel Core i7-4770T @ 2.50GHz')	echo 8832 ;;
	'Intel Core i7-4810MQ @ 2.80GHz')	echo 8826 ;;
	'Intel Core i7 980 @ 3.33GHz')		echo 8819 ;;
	'Intel Xeon E3-1290 @ 3.60GHz')		echo 8704 ;;
	'Intel Xeon W3670 @ 3.20GHz')		echo 8689 ;;
	'Intel Xeon E5-2620 v2 @ 2.10GHz')	echo 8664 ;;
	'Intel Xeon E5-2420 v2 @ 2.20GHz')	echo 8597 ;;
	'Intel Core i7-4800MQ @ 2.70GHz')	echo 8584 ;;
	'Intel Core i7-2600K @ 3.40GHz')	echo 8584 ;;
	'Intel Xeon X5675 @ 3.07GHz')		echo 8567 ;;
	'Intel Core i7-3820QM @ 2.70GHz')	echo 8559 ;;
	'Intel Core i7 970 @ 3.20GHz')		echo 8551 ;;
	'Intel Xeon E3-1280 @ 3.50GHz')		echo 8502 ;;
	'Intel Xeon E5-2643 @ 3.30GHz')		echo 8469 ;;
	'Intel Core i7-3740QM @ 2.70GHz')	echo 8447 ;;
	'Intel Xeon E3-1270 @ 3.40GHz')		echo 8366 ;;
	'Intel Core i7-3720QM @ 2.60GHz')	echo 8333 ;;
	'Intel Xeon X5670 @ 2.93GHz')		echo 8283 ;;
	'Intel Core i7-3770T @ 2.50GHz')	echo 8282 ;;
	'Intel Core i7-2600 @ 3.40GHz')		echo 8281 ;;
	'Intel Core i7-4750HQ @ 2.00GHz')	echo 8224 ;;
	'Intel Xeon E3-1265L V2 @ 2.50GHz')	echo 8187 ;;
	'Intel Xeon E3-1240 @ 3.30GHz')		echo 8129 ;;
	'Intel Xeon X5660 @ 2.80GHz')		echo 8123 ;;
	'AMD FX-8320 Eight-Core')		echo 8081 ;;
	'Intel Xeon E3-1245 @ 3.30GHz')		echo 8058 ;;
	'AMD FX-8370E Eight-Core')		echo 8046 ;;
	'Intel Core i7-4710MQ @ 2.50GHz')	echo 8026 ;;
	'Intel Xeon E3-1230 @ 3.20GHz')		echo 8023 ;;
	'Intel Xeon E5-2620 @ 2.00GHz')		echo 7989 ;;
	'Intel Core i7-4710HQ @ 2.50GHz')	echo 7938 ;;
	'Intel Core i7-4700HQ @ 2.40GHz')	echo 7936 ;;
	'AMD FX-8310 Eight-Core')		echo 7921 ;;
	'Intel Xeon E3-1275 @ 3.40GHz')		echo 7919 ;;
	'Intel Core i7-4700MQ @ 2.40GHz')	echo 7861 ;;
	'Intel Core i5-4690K @ 3.50GHz')	echo 7756 ;;
	'Intel Core i7-4702HQ @ 2.20GHz')	echo 7752 ;;
	'AMD FX-8150 Eight-Core')		echo 7696 ;;
	'Intel Core i5-4670K @ 3.40GHz')	echo 7695 ;;
	'Intel Core i7-3630QM @ 2.40GHz')	echo 7691 ;;
	'Intel Xeon X5650 @ 2.67GHz')		echo 7653 ;;
	'Intel Core i5-4690 @ 3.50GHz')		echo 7646 ;;
	'Intel Core i7-4712HQ @ 2.30GHz')	echo 7642 ;;
	'Intel Core i7-4785T @ 2.20GHz')	echo 7541 ;;
	'Intel Core i7-3610QM @ 2.30GHz')	echo 7487 ;;
	'Intel Core i5-4670 @ 3.40GHz')		echo 7484 ;;
	'AMD FX-8320E Eight-Core')		echo 7474 ;;
	'Intel Core i5-4690S @ 3.20GHz')	echo 7454 ;;
	'Intel Xeon E3-1235 @ 3.20GHz')		echo 7453 ;;
	'Intel Core i7-4700EQ @ 2.40GHz')	echo 7408 ;;
	'Intel Core i7-2960XM @ 2.70GHz')	echo 7396 ;;
	'Intel Xeon E5-2420 @ 1.90GHz')		echo 7371 ;;
	'Intel Core i7-3615QM @ 2.30GHz')	echo 7343 ;;
	'Intel Xeon X5687 @ 3.60GHz')		echo 7333 ;;
	'Intel Xeon E3-1226 v3 @ 3.30GHz')	echo 7324 ;;
	'Intel Xeon E5-1410 @ 2.80GHz')		echo 7312 ;;
	'Intel Core i7-2860QM @ 2.50GHz')	echo 7290 ;;
	'Intel Core i7-4702MQ @ 2.20GHz')	echo 7265 ;;
	'Intel Core i5-4590 @ 3.30GHz')		echo 7244 ;;
	'Intel Core i7-4765T @ 2.00GHz')	echo 7231 ;;
	'Intel Xeon E5649 @ 2.53GHz')		echo 7192 ;;
	'Intel Core i7-4712MQ @ 2.30GHz')	echo 7181 ;;
	'Intel Core i5-3570K @ 3.40GHz')	echo 7163 ;;
	'Intel Core i7-2600S @ 2.80GHz')	echo 7131 ;;
	'AMD Opteron 6234')			echo 7093 ;;
	'Intel Xeon X5677 @ 3.47GHz')		echo 7077 ;;
	'Intel Core i5-4570 @ 3.20GHz')		echo 7074 ;;
	'Intel Core i7-2920XM @ 2.50GHz')	echo 7074 ;;
	'AMD FX-6350 Six-Core')			echo 7010 ;;
	'Intel Xeon E3-1220 v3 @ 3.10GHz')	echo 7010 ;;
	'Intel Core i5-3570 @ 3.40GHz')		echo 7000 ;;
	'Intel Core i5-4590S @ 3.00GHz')	echo 6998 ;;
	'Intel Core i7-3632QM @ 2.20GHz')	echo 6982 ;;
	'Intel Xeon E3-1225 v3 @ 3.20GHz')	echo 6951 ;;
	'Intel Xeon E5-2430 v2 @ 2.50GHz')	echo 6884 ;;
	'Intel Core i5-2550K @ 3.40GHz')	echo 6860 ;;
	'Intel Xeon E3-1225 V2 @ 3.20GHz')	echo 6846 ;;
	'Intel Core i7-3612QM @ 2.10GHz')	echo 6834 ;;
	'Intel Core i5-3550 @ 3.30GHz')		echo 6820 ;;
	'Intel Core i7-2760QM @ 2.40GHz')	echo 6807 ;;
	'Intel Core i5-4570S @ 2.90GHz')	echo 6798 ;;
	'Intel Core i7-2840QM @ 2.40GHz')	echo 6766 ;;
	'Intel Core i5-3570S @ 3.10GHz')	echo 6750 ;;
	'Intel Core i5-4460 @ 3.20GHz')		echo 6747 ;;
	'Intel Xeon E5645 @ 2.40GHz')		echo 6735 ;;
	'Intel Xeon W3580 @ 3.33GHz')		echo 6676 ;;
	'Intel Core i7-3612QE @ 2.10GHz')	echo 6665 ;;
	'Intel Core i7-2820QM @ 2.30GHz')	echo 6658 ;;
	'Intel Core i5-3550S @ 3.00GHz')	echo 6642 ;;
	'Intel Core i7-3635QM @ 2.40GHz')	echo 6594 ;;
	'AMD FX-8120 Eight-Core')		echo 6589 ;;
	'Intel Core i5-3470 @ 3.20GHz')		echo 6572 ;;
	'AMD Opteron 3380')			echo 6565 ;;
	'Intel Xeon E3-1260L @ 2.40GHz')	echo 6534 ;;
	'Intel Xeon E3-1220 V2 @ 3.10GHz')	echo 6517 ;;
	'Intel Core i5-2500K @ 3.30GHz')	echo 6488 ;;
	'Intel Core i5-4440 @ 3.10GHz')		echo 6485 ;;
	'Intel Core i5-3450 @ 3.10GHz')		echo 6456 ;;
	'Intel Core i5-4670S @ 3.10GHz')	echo 6436 ;;
	'Intel Xeon W3570 @ 3.20GHz')		echo 6431 ;;
	'Intel Core i5-4670T @ 2.30GHz')	echo 6419 ;;
	'Intel Core i5-4460S @ 2.90GHz')	echo 6415 ;;
	'AMD Opteron 6272')			echo 6384 ;;
	'Intel Xeon W5590 @ 3.33GHz')		echo 6375 ;;
	'AMD FX-6300 Six-Core')			echo 6354 ;;
	'Intel Core i5-4570R @ 2.70GHz')	echo 6349 ;;
	'Intel Core i5-4430 @ 3.00GHz')		echo 6302 ;;
	'Intel Core i7 975 @ 3.33GHz')		echo 6286 ;;
	'Intel Core i7-2720QM @ 2.20GHz')	echo 6238 ;;
	'Intel Core i5-2500 @ 3.30GHz')		echo 6221 ;;
	'Intel Core i7-3610QE @ 2.30GHz')	echo 6198 ;;
	'Intel Core i5-3475S @ 2.90GHz')	echo 6152 ;;
	'Intel Xeon E3-1220 @ 3.10GHz')		echo 6149 ;;
	'Intel Core i5-3470S @ 2.90GHz')	echo 6148 ;;
	'Intel Core i5-3340 @ 3.10GHz')		echo 6147 ;;
	'Intel Core i5-3350P @ 3.10GHz')	echo 6130 ;;
	'AMD FX-6200 Six-Core')			echo 6112 ;;
	'AMD FX-8100 Eight-Core')		echo 6075 ;;
	'Intel Xeon L5638 @ 2.00GHz')		echo 6057 ;;
	'Intel Core i5-4440S @ 2.80GHz')	echo 6034 ;;
	'Intel Core i7-2670QM @ 2.20GHz')	echo 6009 ;;
	'Intel Core i5-3450S @ 2.80GHz')	echo 5990 ;;
	'Intel Xeon E3-1225 @ 3.10GHz')		echo 5955 ;;
	'Intel Core i7 960 @ 3.20GHz')		echo 5943 ;;
	'Intel Core i5-3330 @ 3.00GHz')		echo 5902 ;;
	'AMD Phenom II X6 1100T')		echo 5892 ;;
	'Intel Core i5-4430S @ 2.70GHz')	echo 5880 ;;
	'Intel Core i7-2635QM @ 2.00GHz')	echo 5874 ;;
	'Intel Core i7 965 @ 3.20GHz')		echo 5849 ;;
	'Intel Core i5-3570T @ 2.30GHz')	echo 5836 ;;
	'Intel Core i5-2400 @ 3.10GHz')		echo 5814 ;;
	'Intel Xeon E5-1607 v2 @ 3.00GHz')	echo 5803 ;;
	'AMD FX-6120 Six-Core')			echo 5776 ;;
	'Intel Core i5-3335S @ 2.70GHz')	echo 5775 ;;
	'AMD Athlon X4 860K Quad Core')		echo 5761 ;;
	'Intel Xeon E5-1607 @ 3.00GHz')		echo 5754 ;;
	'Intel Core i5-3340S @ 2.80GHz')	echo 5736 ;;
	'AMD A10-7800 APU')			echo 5722 ;;
	'Intel Core i5-2380P @ 3.10GHz')	echo 5717 ;;
	'Intel Core i5-4590T @ 2.00GHz')	echo 5714 ;;
	'AMD Phenom II X6 1090T')		echo 5704 ;;
	'Intel Core i7 880 @ 3.07GHz')		echo 5688 ;;
	'AMD A10 PRO-7800B APU')		echo 5686 ;;
	'AMD A10-7850K APU')			echo 5667 ;;
	'Intel Xeon E5640 @ 2.67GHz')		echo 5662 ;;
	'Intel Core i5-2320 @ 3.00GHz')		echo 5656 ;;
	'Intel Core i5-3330S @ 2.70GHz')	echo 5647 ;;
	'Intel Core i7 950 @ 3.07GHz')		echo 5645 ;;
	'Intel Xeon X5570 @ 2.93GHz')		echo 5630 ;;
	'Intel Core i3-4370 @ 3.80GHz')		echo 5614 ;;
	'Intel Core i5-2450P @ 3.20GHz')	echo 5595 ;;
	'Intel Core i7-2630QM @ 2.00GHz')	echo 5582 ;;
	'Intel Core i3-4360 @ 3.70GHz')		echo 5581 ;;
	'Intel Core i7-2675QM @ 2.20GHz')	echo 5580 ;;
	'Intel Xeon X3470 @ 2.93GHz')		echo 5571 ;;
	'Intel Xeon W3540 @ 2.93GHz')		echo 5506 ;;
	'Intel Xeon X5560 @ 2.80GHz')		echo 5496 ;;
	'Intel Core i7 870 @ 2.93GHz')		echo 5487 ;;
	'Intel Xeon E5-1603 @ 2.80GHz')		echo 5452 ;;
	'Intel Core i7 940 @ 2.93GHz')		echo 5444 ;;
	'Intel Core i7 K 875 @ 2.93GHz')	echo 5443 ;;
	'Intel Core i5-2310 @ 2.90GHz')		echo 5429 ;;
	'AMD FX-6100 Six-Core')			echo 5427 ;;
	'AMD Phenom II X6 1075T')		echo 5423 ;;
	'Intel Xeon X5550 @ 2.67GHz')		echo 5409 ;;
	'Intel Core i7-2710QE @ 2.10GHz')	echo 5398 ;;
	'AMD A10-7700K APU')			echo 5355 ;;
	'Intel Core i3-4340 @ 3.60GHz')		echo 5300 ;;
	'Intel Core i5-2300 @ 2.80GHz')		echo 5279 ;;
	'AMD FX-4350 Quad-Core')		echo 5265 ;;
	'AMD A8-7600 APU')			echo 5251 ;;
	'Intel Xeon L5639 @ 2.13GHz')		echo 5248 ;;
	'Intel Core i7 930 @ 2.80GHz')		echo 5218 ;;
	'Intel Core i7-2715QE @ 2.10GHz')	echo 5199 ;;
	'AMD Phenom II X6 1065T')		echo 5183 ;;
	'AMD Opteron 3280')			echo 5181 ;;
	'Intel Core i5-2500S @ 2.70GHz')	echo 5176 ;;
	'Intel Core i3-4350 @ 3.60GHz')		echo 5170 ;;
	'Intel Xeon X3450 @ 2.67GHz')		echo 5154 ;;
	'Intel Core i3-4330 @ 3.50GHz')		echo 5094 ;;
	'Intel Xeon X5470 @ 3.33GHz')		echo 5093 ;;
	'Intel Xeon E5630 @ 2.53GHz')		echo 5089 ;;
	'Intel Core i7 860 @ 2.80GHz')		echo 5087 ;;
	'Intel Xeon W3520 @ 2.67GHz')		echo 5070 ;;
	'Intel Core i7-4610M @ 3.00GHz')	echo 5068 ;;
	'AMD Phenom II X6 1055T')		echo 5061 ;;
	'Intel Xeon X3460 @ 2.80GHz')		echo 5059 ;;
	'Intel Core i7 S 870 @ 2.67GHz')	echo 5052 ;;
	'Intel Core i3-4160 @ 3.60GHz')		echo 5051 ;;
	'Intel Core i7 920 @ 2.67GHz')		echo 5006 ;;
	'Intel Core i7-4600M @ 2.90GHz')	echo 4999 ;;
	'Intel Core i3-4150 @ 3.50GHz')		echo 4992 ;;
	'Intel Core i5-4210H @ 2.90GHz')	echo 4953 ;;
	'Intel Core i5-4460T @ 1.90GHz')	echo 4938 ;;
	'Intel Xeon E5620 @ 2.40GHz')		echo 4932 ;;
	'AMD Phenom II X6 1045T')		echo 4922 ;;
	'Intel Core i3-4360T @ 3.20GHz')	echo 4905 ;;
	'Intel Core i5-4570T @ 2.90GHz')	echo 4874 ;;
	'Intel Core i5-2400S @ 2.50GHz')	echo 4868 ;;
	'Intel Core i7 S 860 @ 2.53GHz')	echo 4856 ;;
	'Intel Xeon E5540 @ 2.53GHz')		echo 4830 ;;
	'Intel Core2 Extreme X9750 @ 3.16GHz')	echo 4822 ;;
	'Intel Xeon W3565 @ 3.20GHz')		echo 4817 ;;
	'Intel Core i3-4130 @ 3.40GHz')		echo 4803 ;;
	'AMD FX-4170 Quad-Core')		echo 4799 ;;
	'AMD A10-6790K APU')			echo 4786 ;;
	'AMD A10-5800B APU')			echo 4782 ;;
	'AMD Phenom II X6 1035T')		echo 4769 ;;
	'Intel Core i5-2405S @ 2.50GHz')	echo 4765 ;;
	'Intel Xeon W5580 @ 3.20GHz')		echo 4761 ;;
	'Intel Core i7-3540M @ 3.00GHz')	echo 4758 ;;
	'Intel Xeon X5482 @ 3.20GHz')		echo 4757 ;;
	'AMD A10-6800K APU')			echo 4749 ;;
	'Intel Xeon X3380 @ 3.16GHz')		echo 4737 ;;
	'Intel Core2 Extreme X9770 @ 3.20GHz')	echo 4733 ;;
	'Intel Core i5-4570TE @ 2.70GHz')	echo 4727 ;;
	'Intel Xeon E5530 @ 2.40GHz')		echo 4719 ;;
	'Intel Xeon E5-2407 v2 @ 2.40GHz')	echo 4718 ;;
	'AMD FX-4300 Quad-Core')		echo 4716 ;;
	'AMD A10-6700 APU')			echo 4694 ;;
	'Intel Core i5-2500T @ 2.30GHz')	echo 4682 ;;
	'Intel Core i5-4330M @ 2.80GHz')	echo 4673 ;;
	'AMD Phenom II X4 980')			echo 4667 ;;
	'AMD A8-6500B APU')			echo 4659 ;;
	'Intel Xeon W3550 @ 3.07GHz')		echo 4650 ;;
	'AMD A10-5800K APU')			echo 4648 ;;
	'AMD FX-670K Quad-Core')		echo 4639 ;;
	'AMD FX-4150 Quad-Core')		echo 4628 ;;
	'AMD FX-7600P APU with AMD Radeon R7 Graphics')echo 4627 ;;
	'Intel Xeon E5-2609 @ 2.40GHz')		echo 4618 ;;
	'AMD Phenom II X4 975')			echo 4606 ;;
	'Intel Core i5-4288U @ 2.60GHz')	echo 4593 ;;
	'Intel Xeon X3370 @ 3.00GHz')		echo 4592 ;;
	'Intel Core i5-4310M @ 2.70GHz')	echo 4584 ;;
	'AMD Athlon X4 760K Quad Core')		echo 4575 ;;
	'Intel Core i5-3470T @ 2.90GHz')	echo 4574 ;;
	'Intel Core i3-4160T @ 3.10GHz')	echo 4572 ;;
	'AMD A8-6500 APU')			echo 4569 ;;
	'AMD A8-6600K APU')			echo 4562 ;;
	'Intel Core i7-3520M @ 2.90GHz')	echo 4554 ;;
	'Intel Core2 Extreme X9775 @ 3.20GHz')	echo 4536 ;;
	'Intel Core i5-4300M @ 2.60GHz')	echo 4515 ;;
	'Intel Xeon E5520 @ 2.27GHz')		echo 4505 ;;
	'Intel Core i3-4330T @ 3.00GHz')	echo 4498 ;;
	'AMD Phenom II X4 970')			echo 4461 ;;
	'AMD FX-4200 Quad-Core')		echo 4454 ;;
	'Intel Core i3-3250 @ 3.50GHz')		echo 4450 ;;
	'Intel Xeon X5460 @ 3.16GHz')		echo 4439 ;;
	'Intel Xeon X3440 @ 2.53GHz')		echo 4429 ;;
	'Intel Core i5-4278U @ 2.60GHz')	echo 4426 ;;
	'Intel Xeon X5492 @ 3.40GHz')		echo 4426 ;;
	'Intel Core i5-4200H @ 2.80GHz')	echo 4424 ;;
	'Intel Core i3-4150T @ 3.00GHz')	echo 4417 ;;
	'Intel Core i3-3225 @ 3.30GHz')		echo 4390 ;;
	'Intel Core2 Extreme X9650 @ 3.00GHz')	echo 4372 ;;
	'Intel Xeon L5520 @ 2.27GHz')		echo 4356 ;;
	'Intel Core i7-3687U @ 2.10GHz')	echo 4349 ;;
	'AMD A8-5600K APU')			echo 4346 ;;
	'Intel Core i3-3245 @ 3.40GHz')		echo 4338 ;;
	'Intel Core i3-2140 @ 3.50GHz')		echo 4325 ;;
	'Intel Core i3-3240 @ 3.40GHz')		echo 4308 ;;
	'Intel Xeon E5472 @ 3.00GHz')		echo 4306 ;;
	'AMD Athlon X4 750K Quad Core')		echo 4306 ;;
	'Intel Core i5-3360M @ 2.80GHz')	echo 4303 ;;
	'Intel Core i7-4558U @ 2.80GHz')	echo 4302 ;;
	'Intel Xeon W3530 @ 2.80GHz')		echo 4280 ;;
	'Intel Core i5-4210M @ 2.60GHz')	echo 4273 ;;
	'Intel Xeon X5698 @ 4.40GHz')		echo 4272 ;;
	'Intel Xeon X3363 @ 2.83GHz')		echo 4271 ;;
	'AMD Phenom II X4 965')			echo 4270 ;;
	'Intel Xeon E5450 @ 3.00GHz')		echo 4265 ;;
	'Intel Core i7-4600U @ 2.10GHz')	echo 4257 ;;
	'Intel Core2 Quad Q9650 @ 3.00GHz')	echo 4247 ;;
	'Intel Core X 920 @ 2.00GHz')		echo 4237 ;;
	'Intel Xeon E5462 @ 2.80GHz')		echo 4229 ;;
	'Intel Core i3-3220 @ 3.30GHz')		echo 4220 ;;
	'Intel Core i5-3380M @ 2.90GHz')	echo 4211 ;;
	'AMD A10-5700 APU')			echo 4203 ;;
	'Intel Core i3-4130T @ 2.90GHz')	echo 4195 ;;
	'Intel Core i7-4650U @ 1.70GHz')	echo 4192 ;;
	'Intel Xeon X5450 @ 3.00GHz')		echo 4190 ;;
	'AMD FX-4130 Quad-Core')		echo 4188 ;;
	'AMD Phenom II X4 B60')			echo 4176 ;;
	'Intel Core i7 X 940 @ 2.13GHz')	echo 4160 ;;
	'Intel Core i5-4258U @ 2.40GHz')	echo 4157 ;;
	'AMD Phenom II X4 B99')			echo 4154 ;;
	'Intel Core i5-3340M @ 2.70GHz')	echo 4153 ;;
	'Intel Xeon L5630 @ 2.13GHz')		echo 4144 ;;
	'Intel Core i5-4200M @ 2.50GHz')	echo 4134 ;;
	'Intel Xeon L5430 @ 2.66GHz')		echo 4133 ;;
	'Intel Xeon X3360 @ 2.83GHz')		echo 4122 ;;
	'Intel Core i7-3555LE @ 2.50GHz')	echo 4080 ;;
	'Intel Core2 Quad Q9550 @ 2.83GHz')	echo 4067 ;;
	'Intel Core i3-2125 @ 3.30GHz')		echo 4052 ;;
	'AMD FX-4100 Quad-Core')		echo 4041 ;;
	'Intel Core i3-2130 @ 3.40GHz')		echo 4040 ;;
	'AMD A8-5500 APU')			echo 4029 ;;
	'Intel Core i5-3320M @ 2.60GHz')	echo 4024 ;;
	'AMD Phenom II X4 955')			echo 4014 ;;
	'AMD Opteron 3350 HE')			echo 4009 ;;
	'Intel Pentium G3258 @ 3.20GHz')	echo 4006 ;;
	'Intel Core i7-4510U @ 2.00GHz')	echo 3995 ;;
	'Intel Core i5-2390T @ 2.70GHz')	echo 3983 ;;
	'Intel Xeon L3426 @ 1.87GHz')		echo 3982 ;;
	'Intel Core i7-4550U @ 1.50GHz')	echo 3980 ;;
	'Intel Core i7-2640M @ 2.80GHz')	echo 3972 ;;
	'Intel Core i7-3667U @ 2.00GHz')	echo 3965 ;;
	'AMD Phenom II X4 B97')			echo 3957 ;;
	'Intel Core i5-3230M @ 2.60GHz')	echo 3956 ;;
	'Intel Core2 Quad Q9705 @ 3.16GHz')	echo 3947 ;;
	'AMD Phenom Ultra X4 24500')		echo 3945 ;;
	'Intel Xeon X5472 @ 3.00GHz')		echo 3943 ;;
	'AMD Phenom II X4 973')			echo 3940 ;;
	'Intel Xeon E5440 @ 2.83GHz')		echo 3940 ;;
	'Intel Atom C2750 @ 2.40GHz')		echo 3929 ;;
	'AMD Athlon X4 740 Quad Core')		echo 3928 ;;
	'Intel Core i5 760 @ 2.80GHz')		echo 3924 ;;
	'Intel Xeon X3350 @ 2.66GHz')		echo 3911 ;;
	'Intel Xeon E5430 @ 2.66GHz')		echo 3907 ;;
	'Intel Core i7-3537U @ 2.00GHz')	echo 3889 ;;
	'Intel Core i3-3210 @ 3.20GHz')		echo 3879 ;;
	'Intel Core i3-2120 @ 3.30GHz')		echo 3870 ;;
	'AMD Phenom II X4 960T')		echo 3864 ;;
	'Intel Core i7-2620M @ 2.70GHz')	echo 3863 ;;
	'Intel Core i7-4500U @ 1.80GHz')	echo 3845 ;;
	'AMD Phenom II X4 B95')			echo 3827 ;;
	'AMD A8-5500B APU')			echo 3820 ;;
	'Intel Core2 Quad Q9450 @ 2.66GHz')	echo 3811 ;;
	'AMD Phenom II X4 20')			echo 3805 ;;
	'Intel Core i5-3210M @ 2.50GHz')	echo 3802 ;;
	'AMD A10-6700T APU')			echo 3799 ;;
	'Intel Core i5-4310U @ 2.00GHz')	echo 3792 ;;
	'AMD Phenom II X4 B50')			echo 3791 ;;
	'Intel Pentium G3450 @ 3.40GHz')	echo 3780 ;;
	'Intel Core i5-4300U @ 1.90GHz')	echo 3771 ;;
	'Intel Core i3-2105 @ 3.10GHz')		echo 3758 ;;
	'AMD Phenom II X4 945')			echo 3755 ;;
	'Intel Core i5-2560M @ 2.70GHz')	echo 3752 ;;
	'Intel Core i5 750 @ 2.67GHz')		echo 3745 ;;
	'Intel Core i3-4100M @ 2.50GHz')	echo 3742 ;;
	'Intel Core2 Extreme Q6850 @ 3.00GHz')	echo 3734 ;;
	'Intel Xeon L5506 @ 2.13GHz')		echo 3715 ;;
	'Intel Core i3-3220T @ 2.80GHz')	echo 3710 ;;
	'Intel Core i7-4610Y @ 1.70GHz')	echo 3703 ;;
	'Intel Core i5-2540M @ 2.60GHz')	echo 3697 ;;
	'Intel Core i7-3517U @ 1.90GHz')	echo 3694 ;;
	'AMD FX-7500 APU')			echo 3682 ;;
	'Intel Core2 Quad Q9500 @ 2.83GHz')	echo 3679 ;;
	'Intel Core i3-3240T @ 2.90GHz')	echo 3673 ;;
	'Intel Xeon X3330 @ 2.66GHz')		echo 3671 ;;
	'Intel Core i7 920XM @ 2.00GHz')	echo 3666 ;;
	'AMD A8-3870K APU')			echo 3664 ;;
	'AMD Phenom II X4 940')			echo 3663 ;;
	'AMD Athlon II X4 651 Quad-Core')	echo 3652 ;;
	'Intel Core2 Extreme Q6800 @ 2.93GHz')	echo 3632 ;;
	'Intel Core2 Quad Q9505 @ 2.83GHz')	echo 3631 ;;
    esac
}

#
# return the amount of free megabytes of disk space for the p4 client, empty if unknown
#

get_disk() {
    : ${DEV_ROOT:?}
    tmpfile=`mktemp get_disk.tmpXXXXXX`
    # use -P because this gets the standard unix style output from MKS and Mac OS X
    # use -k because not all df's understand -m
    # use echo `` to flatten multi-line output (when the device name is long) to one line, after removing header Filesystem line
    echo `df -Pk ${DEV_ROOT} | grep -vi Filesystem` > $tmpfile
    disk=`awk '{print $(NF - 2)}' < $tmpfile`
    rm -f $tmpfile
    disk=`expr $disk / 1024`
    echo $disk
}

#
# return the speed of the cpu(s) or empty if unknown
#
# ex. for a 1.4 GHz box it might return
# 1390
# ex. for a dual 3 GHz box it might return
# 3054,3054
#
# for Windows the numbers aren't even numbers for what you'd expect.  And
# the numbers returned aren't comparable between OS's.
#
get_cpu() {
    mhz=""
    case `uname` in
	Windows_NT|CYGWIN_NT*)
	    # don't put in /tmp because regedit might have problems with
	    # cygwin /tmp path
	    # divide old 32 bit machine cpu Hz by two so that the rq.pl speed algorithms picks the right machines
	    case `uname -m` in
		i686)
		    tmp_uname_s="`uname -s`"
		    echo $tmp_uname_s | grep -i 'WOW64' > /dev/null
		    # Cygwin on 64 bit and 32 bit windows reports i686 of "uname -m",
		    # but 64 bit cygwin has WOW64 in "uanem -s" results.
		    if [ $? -eq 0 ]; then
			divisor=1
		    else
			divisor=2
		    fi
		    unset tmp_uname_s
		    ;; # Cygwin
		586)  divisor=2 ;; # MKS
		8664) divisor=1 ;; # MKS
		*)    divisor=1 ;;
	    esac
	    tmpfile=get_cpu.$$
	    regedit /e "$tmpfile" 'HKEY_LOCAL_MACHINE\HARDWARE\DESCRIPTION\System\CentralProcessor'
	    awk < "$tmpfile" -F= '/^"ProcessorNameString"=/{print $2}' | sed 's/"//g' | strings > get_cpu2.$$
	    while read i; do
		new=`get_cpu_benchmark "$i"`
		mhz="$mhz${mhz:+,}$new"
	    done < get_cpu2.$$
	    rm -f get_cpu2.$$
	    # this puts lots of null's in the output
	    # if this cpu is not recognized in the get_cpu_benchmark table then use the Mhz,
	    # but the table should probably be updated
	    if [ -z "$mhz" ]; then
		for i in `awk < "$tmpfile" -F: '/^"~MHz"=/{print toupper($2)}'`;do
		    # use bc to convert from hex to decimal
		    mhz="$mhz${mhz:+,}"`perl -e "print int(hex('$i') / $divisor)"`
		done
		unset divisor
	    fi
	    rm -f "$tmpfile"
	    ;;
	Linux)
	    awk -F: '/^model name/ { print $2 }' < /proc/cpuinfo > get_cpu.$$
	    while read i; do
		new=`get_cpu_benchmark "$i"`
		mhz="$mhz${mhz:+,}$new"
	    done < get_cpu.$$
	    rm -f get_cpu.$$
	    # if this cpu is not recognized in the get_cpu_benchmark table then use the Mhz,
	    # but the table should probably be updated
	    if [ -z "$mhz" ]; then
		for i in `awk < /proc/cpuinfo '/cpu MHz/{sub(/\..*$/,"");print $NF}'`;do
		    mhz="$mhz${mhz:+,}$i"
		done
	    fi
	    ;;
	SunOS)
	    for i in `/usr/sbin/psrinfo -v | awk '/processor operates at/{print $6}'`;do
		mhz="$mhz${mhz:+,}$i"
	    done
	    ;;
	AIX)
	    for i in `/usr/sbin/prtconf | awk '/Processor Clock Speed/{print $4}'`;do
		mhz="$mhz${mhz:+,}$i"
	    done
	    ;;
	HP-UX) ;;
	Darwin)
	    tmpfile=`mktemp get_cpu.XXXXXX`
	    system_profiler SPHardwareDataType >$tmpfile
	    speed=`awk '/Processor Speed: /{print $3}' < $tmpfile`
	    units=`awk '/Processor Speed: /{print $4}' < $tmpfile`
	    if [ "$units" = "GHz" ]; then
		speed=`expr $speed '*' 1000`
	    fi
	    cores=`awk '/Total Number of Cores: /{print $NF}' < $tmpfile`
	    i=1
	    while [ $i -le $cores ]; do
		mhz="$mhz${mhz:+,}$speed"
		i=`expr $i + 1`
	    done
	    rm -rf $tmpfile
	    unset speed units cores i tmpfile
	    ;;
    esac
    echo "$mhz"
    unset mhz
}

# return true or false based on whether this machine has an IPv6 address
rbt_ipv6() {
    case `uname` in
	Windows_NT|CYGWIN_NT*)
	    ipconfig | grep 'IPv6 Address' >/dev/null && return 0
	    ;;
	Linux)
#	    /sbin/ifconfig | grep -v -e ' fe80::' -e ' ::1 ' | grep 'inet6' >/dev/null && return 0
	    # if there is an ipv6 lsmod then it might have ipv6 if not then it definitely does not
	    /sbin/lsmod | grep -i ipv6 >/dev/null || return 1
	    # assuming it got past the lsmod check if there is a ipv6 route then we definitely have ipv6
	    [ `/sbin/ip -6 route show default | wc -l` -ge 1 ] && return 0
	    ;;
	SunOS)
	    ;;
	AIX)
	    ;;
	HP-UX) ;;
	Darwin)
	    ifconfig | grep -v -e ' fe80::' -e ' ::1 ' | grep 'inet6' >/dev/null && return 0
	    ;;
    esac
    return 1
}

# return true or false based on whether WinRunner is installed and running on this system
#
# first look for wrun.ini and then if it exists look for a WinRunner process
#
rbt_winrunner() {
    for i in "$systemroot" "$SystemRoot" "$SYSTEMROOT"; do
	if [ -f "$i/wrun.ini" ]; then
	    ( ps -ef ; ps -Wef ) 2>&1 | grep -v grep | grep -i -e javasu -e wrun.exe -e crvw.exe >/dev/null
	    if [ $? -eq 0 ]; then
		return 0
	    fi
	    break
	fi
    done
    return 1
}

# return true or false based on whether ADE is on this system
#
rbt_ade() {
    # What I have found is that you need the ADE env vars setup
    # in $HOME/.csh.cshrc or you need to be able to do the following command and
    # see the ADE environment variables
    #
    # ssh host -l stjpg env | grep ^ADE
    #
    # /etc/profile and /etc/csh.login are run by login shells but not the shell
    # you get when you ssh in and run a command like that.
    #
    if [ -x /usr/local/bin/ade ]; then
	# /etc/profile isn't always run in an RQ env but
	# ADE_SITE is always in /etc/profile on "ADE" machines
	# required for the RQ CTS runs
	grep ADE_SITE /etc/profile >/dev/null
	if [ $? -eq 0 ] ; then
	    return 0
	fi
    fi
    return 1
}

#
# machines are excluded from dynamic test runs if they are "slow" as defined
# below or if they are listed as excluded in the btstat*rc file
#
# return true if this machine is excluded from dynamic test runs, false otherwise
# Usage: rbt_excluded
#
# return true if named machine is excluded from dynamic test runs, false otherwise
# Usage: rbt_excluded host
#
rbt_excluded() {
    : ${DEV_ROOT:?} ${NEW_SRC:?}
    rc=$DEV_ROOT/tools/wls/infra/build/statuspage/btstat${NEW_SRC}rc
    tmp1=/tmp/rbt_excluded.tmp1_$$
    tmp2=/tmp/rbt_excluded.tmp2_$$
    if [ ! -f "$rc" ]; then
	rc=/dev/null
    fi
    this_host="${1:-`uname_n`}"
    # "deconstruct" this using tmp files rather than a pipe because the grep's
    # seem to be hanging in the queue.  Even if it doesn't stop the hang least
    # I'll be able to see exactly which grep hangs
    awk '/^[ 	]*dynamicexclude/' "$rc" > $tmp1
    tr -d '\r' < $tmp1 > $tmp2
    awk '{for(i=2;i<=NF;i++)print $i}' < $tmp2 > $tmp1
    # FIXME: jun.li@bea.com Jan 22, 2008
    # we should change \r\n to \n, since grep can not honor \r\n to match
    # the end of line expression($)
    sed < $tmp1 's,\r\n,\n,' | grep -i "^$this_host$" >/dev/null
    status=$?
    rm -f $tmp1 $tmp2
    if [ $status -ne 0 -a "$1" = "" ]; then
	# get cpu speed, only key off the speed of the first cpu if there is more than one
	cpu_speed="`get_cpu | awk -F, '{print $1}'`"
	# skip slow machines
	if [ -n "$cpu_speed" ]; then
	  case $OS in
	  Windows_NT)
	    case `uname -m` in
	    i686)
	      tmp_uname_s="`uname -s`"
          echo $tmp_uname_s | grep -i 'WOW64' > /dev/null
	      # Cygwin on 64 bit and 32 bit windows reports i686 of "uname -m",
	      # but 64 bit cygwin has WOW64 in "uanem -s" results.
          if [ $? -eq 0 ]; then
            cpu_divisor=1
          else
	        cpu_divisor=2
          fi
	      unset tmp_uname_s
	      ;; # Cygwin
	    586)  cpu_divisor=2 ;; # MKS
	    8664) cpu_divisor=1 ;; # MKS
	    *)    cpu_divisor=1 ;;
	    esac
	    if [ $cpu_divisor -eq 2 ]; then
	      [ "$cpu_speed" -lt 1000 ] && status=0
	    else
	      [ "$cpu_speed" -lt 1400 ] && status=0
	    fi
	    unset cpu_divisor
	    ;;
	  Linux)		[ "$cpu_speed" -lt 2000 ] && status=0 ;;
	  SunOS)		[ "$cpu_speed" -lt    0 ] && status=0 ;;
	  esac
	fi
	unset cpu_speed
	# get amount of memory
	memory="`get_memory`"
	# skip low memory machines
	if [ -n "$memory" ]; then
	    [ "$OS" = "Linux" -a "$memory" -lt 5000 ] && status=0
	    [ "$memory" -lt 2000 ] && status=0
	fi
	unset memory

    fi
    return $status
}

#
# return true if this machine is excluded from dynamic nightly test runs, false otherwise
# Usage: rbt_nightly_excluded
#
# return true if named machine is excluded from dynamic nightly test runs, false otherwise
# Usage: rbt_nightly_excluded host
#
rbt_nightly_excluded() {
    : ${DEV_ROOT:?} ${SRC:?}
    rc=$DEV_ROOT/tools/wls/infra/build/statuspage/btstat${NEW_SRC}rc
    tmp1=/tmp/rbt_nightly_excluded.tmp1_$$
    tmp2=/tmp/rbt_nightly_excluded.tmp2_$$
    if [ ! -f "$rc" ]; then
	rc=/dev/null
    fi
    this_host="${1:-`uname_n`}"
    # "deconstruct" this using tmp files rather than a pipe because the grep's
    # seem to be hanging in the queue.  Even if it doesn't stop the hang least
    # I'll be able to see exactly which grep hangs
    awk '/^[ 	]*nightlyexclude/' "$rc" > $tmp1
    tr -d '\r' < $tmp1 > $tmp2
    awk '{for(i=2;i<=NF;i++)print $i}' < $tmp2 > $tmp1
    # FIXME: jun.li@bea.com Jan 22, 2008
    # we should change \r\n to \n, since grep can not honor \r\n to match
    # the end of line expression($)
    sed < $tmp1 's,\r\n,\n,' | grep -i "^$this_host$" >/dev/null
    status=$?
    rm -f $tmp1 $tmp2
    return $status
}

#
# return true if this machine is excluded from remote test runs, false otherwise
# Usage: rbt_rq_excluded
#
# return true if named machine is excluded from dynamic nightly test runs, false otherwise
# Usage: rbt_rq_excluded host
#
rbt_rq_excluded() {
    : ${DEV_ROOT:?} ${SRC:?}
    rc=$DEV_ROOT/tools/wls/infra/build/statuspage/btstat${NEW_SRC}rc
    tmp1=/tmp/rbt_rq_excluded.tmp1_$$
    tmp2=/tmp/rbt_rq_excluded.tmp2_$$
    if [ ! -f "$rc" ]; then
	rc=/dev/null
    fi
    this_host="${1:-`uname_n`}"
    # "deconstruct" this using tmp files rather than a pipe because the grep's
    # seem to be hanging in the queue.  Even if it doesn't stop the hang least
    # I'll be able to see exactly which grep hangs
    awk '/^[ 	]*nightlyonly/' "$rc" > $tmp1
    tr -d '\r' < $tmp1 > $tmp2
    awk '{for(i=2;i<=NF;i++)print $i}' < $tmp2 > $tmp1
    cat $tmp1 | sed 's,\r\n,\n,' | grep -i "^$this_host$" >/dev/null
    status=$?
    rm -f $tmp1 $tmp2
    return $status
}

#Return the physical memory amount in MB
get_memory() {
    case "`uname`" in
     Windows_NT|CYGWIN_NT-*)
        "${SystemRoot:-${SYSTEMROOT:-${systemroot:-c:/windows}}}"/system32/systeminfo | \
        awk '/Total Physical Memory:/{print $4}' | sed 's/,//g' | while read memamount; do
             echo "$memamount"
             unset memamount
         done
     ;;
     Linux)
         echo `free -m | awk '/Mem:/{print $2}'`
     ;;
     Solaris|SunOS)
         echo `/usr/sbin/prtconf | grep Memory | awk '{print $3}'`
     ;;
     HP-UX)
         amount_in_KB=`grep Physical /var/adm/syslog/syslog.log | awk '{print $7}'`
         amount_in_MB=`expr $amount_in_KB / 1024`
         echo $amount_in_MB
         unset amount_in_KB amount_in_MB
     ;;
     AIX)
         amount_in_KB=`/usr/sbin/lsattr -El sys0 -a realmem | awk '{print $2}'`
         amount_in_MB=`expr $amount_in_KB / 1024`
         echo $amount_in_MB
         unset amount_in_KB amount_in_MB
     ;;
     Darwin)
	 amount_in_GB=`system_profiler SPHardwareDataType | awk '/Memory: /{print $2}'`
         amount_in_MB=`expr $amount_in_GB '*' 1024`
         echo $amount_in_MB
         unset amount_in_MB amount_in_GB
     ;;
    esac
    unset amount amount_in_KB amount_in_MB amount_in_GB
}

# timeout does not exist on Darwin (MacOS) so use perl
which timeout >/dev/null
if [ $? -eq 1 ]; then
    timeout() {
	perl -e 'alarm shift; exec @ARGV' "$@"
    }
fi

rbt_rawssh() {
    : ${DEV_ROOT:?} ${SRC:?}
    if [ "$1" = "-v" ]; then
	verbose="true"
	shift
    else
	verbose="false"
    fi
    if [ "$1" = "-t" ]; then
	timeout="$2"
	shift 2
    else
	timeout=
    fi
    user="$1"
    shift
    pass="$1"
    shift
    host="$1"
    shift
    [ "$verbose" = "true" ] && echo "$@"
    if [ -n "$timeout" ]; then
	timeout "$timeout" ssh -x "$user@$host" -o BatchMode=true -o StrictHostKeyChecking=no "$@"
    else
	                   ssh -x "$user@$host" -o BatchMode=true -o StrictHostKeyChecking=no "$@"
    fi 2>&1 | sed -e '2,$n' -e '/^FIPS integrity verification test failed\./d'
}

rbt_raw() {
    : ${DEV_ROOT:?} ${SRC:?}
    src=$DEV_ROOT/tools/wls
    java -cp "$src/infra/build/jsch/jsch-0.1.50.jar$PS$src/infra/build" rbt "$@"
}

rbt_internal() {
    : ${DEV_ROOT:?} ${SRC:?} ${1:?host} ${2:?cmd} ${OS:?}
    src=$DEV_ROOT/tools/wls

    case "$OS" in
	SunOS)	BASENAME="basename" ;;
	*)	BASENAME="basename --";;
    esac


    # parse $1 in the form host, @host, or user@host into
    # remote_user/remote_host (default remote_user to bt)
    remote_user=""
    remote_host=$1
    if (echo $1 | grep @ >/dev/null); then
	remote_user="`echo $1 | cut -d@ -f1`"
	remote_host="`echo $1 | cut -d@ -f2`"
    fi
    if [ -z "$remote_user" ]; then
	remote_user=bt
    fi

    # shift off $1 so we can use $@ to get the cmd args
    shift

    CACHEFILE=$src/infra/.cache_$remote_user@$remote_host
    if [ -s $CACHEFILE ]; then
	if [ "`find $CACHEFILE -mtime +7`" != "" ]; then
	    # delete cachefiles after 7 days
	    rm -f $CACHEFILE
	fi
    fi
    if [ -s $CACHEFILE ]; then
	read clientdir remoteos p4_port p4_client < $CACHEFILE
    fi
    if [ ! -s $CACHEFILE -o "$p4_port" = "" -o "$p4_client" = "" ] ; then
	count=0
	tmpfile=`mktemp /tmp/tmpfile.XXXXXX`
	clientsfile=`mktemp /tmp/clients.XXXXXX`
	aime_users="wls wlsbt stjpg aime aime1 aime2 aime3 aime4 aime5 aime6"
	for port in perforce-coh.us.oracle.com:1666; do
	    export P4USER=bt
	    p4 -p "$port" clients > $clientsfile
	    aimec=""
	    for aimeu in $aime_users; do
		aimec="$aimec $aimeu.$remote_host"
	    done
	    for client in $remote_user.$remote_host $aimec; do
		grep -i "^Client $client " < $clientsfile > $tmpfile
		if [ $? -eq 0 ]; then
		    p4_port=$port
		    p4_client=$client
		    rm -f $clientsfile
		    break 2
		fi
	    done
	done

	# convert \'s to /
	clientdir="`awk '{print $5}' < $tmpfile | sed 's:[\\]:/:g'`"
	rm -f $clientsfile $tmpfile
	echo $clientdir | grep '[a-zA-Z]:' >/dev/null 2>&1
	if [ $? -eq 0 ]; then
	    remoteos=win
	else
	    remoteos=unix
	fi
	[ -z "$clientdir" ] && echo "WARNING: cannot find root of p4 client for $remote_host" 1>&2
	# cat doesn't translate \b \n etc like echo does
	cat <<EOF > $CACHEFILE
${clientdir:?} ${remoteos:?} ${p4_port:?} ${p4_client:?}
EOF
    fi
    # create a set of common p4 env vars + PATH for both win and unix
    p4opts="P4PORT=$p4_port P4CLIENT=$p4_client;export PATH P4PORT P4CLIENT"
    if [ "$remote_user" = "bt" ]; then
	echo "$p4_client" | grep -e ^wls\. -e ^wlsbt -e ^stjpg -e ^aime >/dev/null
	if [ $? -eq 0 ]; then
	    # if we are changing the ssh user from bt to ??? then hard code
	    # the p4 user to bt
	    p4opts="P4USER=bt $p4opts P4USER"
	    remote_user=`echo $p4_client | sed 's/\..*//'`
	fi
    fi
    if [ -n "$RBT_XFER" ]; then
	remotedir=$clientdir/dev/$SRC/wls/tools/weblogic/qa/tests
    else
	remotedir=$clientdir/dev/$SRC/tools/wls
    fi
    case $remoteos in
	win)
	    # always use sh -c because either sh or cmd might be used on
	    # windows boxes for rexec.  PWD on Cygwin can mess up p4.
	    cmd="sh -c \"(cd c:/temp&&cd $remotedir&&unset PWD"
	    cmd="$cmd;uname|grep CYGWIN>/dev/null&&PATH=/bin:/usr/bin:/usr/local/bin:'"'$PATH'"'"
	    cmd="$cmd;$p4opts"
	    cmd="$cmd&&$@ )2>&1\""
	    ;;
	unix)
	    cmd="sh -c \"(cd /tmp&&cd $remotedir"
	    cmd="$cmd&&PATH='"'$PATH'"':'"'$HOME'"'/bin:/usr/local/bin $p4opts"
	    cmd="$cmd&&$@ ) 2>&1\""
	    ;;
	*)
	    echo "FATAL: internal error ($remoteos)" 1>&2
	    return 1
	    ;;
    esac
    # get password from auth file
    unset AUTHFILE
    for i in \
	"$HOMEDRIVE$HOMEPATH/.auth_$remote_user" \
        "$HOME/.auth_$remote_user" \
        "~/.auth_$remote_user" \
        "c:/.auth_$remote_user" \
	; do
      if [ -f "$i" ]; then
	  AUTHFILE="$i"
	  break
      fi
    done
    if [ ! -f "$AUTHFILE" ]; then
	if [ "$remote_user" = "bt" ]; then
	    AUTH=4sure
	else
	    if [ "$remote_user" != "wls" ]; then
		echo "FATAL: please create .auth_$remote_user" 1>&2
		return 1
	    else
		AUTH=
	    fi
	fi
    else
	read AUTH < "$AUTHFILE"
    fi
    if [ "$VERBOSE" -eq 1 ]; then
	echo "[$remote_user@$remote_host] START `date` $clientdir $remoteos"
	verbose_flag="-v"
    else
	verbose_flag=
    fi
    if [ "$TIMEOUT" != "" ]; then
	timeout_flag="-t $TIMEOUT"
    else
	timeout_flag=""
    fi
    if [ -n "$RBT_XFER" ]; then
	${REXEC:?} -l $remote_user -p $AUTH $remote_host "$cmd"
    else
	value=`expr ${MAXHOSTNAME:-15} + 5`
	formated="`echo \[$remote_user@$remote_host\] | awk '{printf "%-'${value}'s", $1}'`"
        if [ "$remote_user" = "wls" -a "$OS" = "Linux" ]; then
	    rbt_rawssh $verbose_flag $timeout_flag "$remote_user" "$AUTH" "$remote_host" "$cmd"
        else
	    rbt_raw    $verbose_flag $timeout_flag "$remote_user" "$AUTH" "$remote_host" "$cmd"
        fi 2>&1 | sed "s/^/$formated /"
    fi
    unset AUTH AUTHFILE
    [ "$VERBOSE" -eq 1 ] && echo "[$remote_user@$remote_host] DONE `date`"
}

rbt() {
    : ${DEV_ROOT:?} ${SRC:?} ${1:?hosts} ${2:?cmd}
    if [ "$1" = "-v" ]; then
	VERBOSE=1
	shift
    else
	VERBOSE=0
    fi
    if [ "$1" = "-p" ]; then
	in_parallel="true"
	shift
    else
	in_parallel="false"
    fi
    if [ "$1" = "-t" ]; then
	TIMEOUT="$2"
	shift 2
    else
	TIMEOUT="0"
    fi
    if [ $# -lt 2 ]; then
	echo "Usage: $0 host,host,... cmd" 1>&2
	return 1
    fi
    case "$OS" in
	SunOS)	BASENAME="basename" ;;
	*)	BASENAME="basename --";;
    esac
    hosts="`echo $1 | sed 's/[, ][, ]*/ /g'`"
    shift
    tmpfile=`mktemp /tmp/hostname.XXXXXX`
    for i in $hosts; do
	echo $i
    done > $tmpfile
    MAXHOSTNAME=`awk < $tmpfile '{b=length($0);if(a < b)a=b}END{print a}'`
    rm -f $tmpfile
    export MAXHOSTNAME
    for i in $hosts; do
	case $in_parallel in
	    true)
		# MKS seems to have trouble with more than 5 concurrent background jobs
		tmpfile=/tmp/`$BASENAME $0`.tmp_$$
		jobs > $tmpfile
		if [ `wc -l < $tmpfile` -ge 5 ]; then
		    wait
		fi
		rm -f $tmpfile
		rbt_internal $i "$@" </dev/null &
		;;
	    false) rbt_internal $i "$@" ;;
	    *)     return 1 ;;
	esac
    done
    unset MAXHOSTNAME
    if [ "$in_parallel" = "true" ]; then
	wait
    fi
}

#
# Experimental: Transfer text files to a remote host
#
# Usage: rbt_put host files
# (files relative to tools/weblogic/qa/tests)
#
# Note: I have to use a local version of rexec because the java jrexec
# doesn't handle standard input properly
#
# Example: rbt_put foobar '*.out'
#
rbt_put() {
    : ${DEV_ROOT:?} ${SRC:?} ${1:?host} ${2:?files}
    qatests=$DEV_ROOT/tools/wls/tools/weblogic/qa/tests
    cd $qatests
    case "$OS" in
	SunOS|HP-UX)	REXEC="$DEV_ROOT/env/bin/$OS/rexec" ;;
	Linux)		REXEC=/usr/bin/rexec ;;
	Windows_NT)	REXEC="$ROOTDIR/mksnt/rexec.exe" ;;
	*)              echo "No rexec, unsupported OS" 1>&2 ; exit 1 ;;
    esac
    host="$1"
    shift
    # use ()'s to make absolutely sure RBT_XFER gets unset
    (
	RBT_XFER=true
	for i in $*; do
	    [ ! -f "$i" ] && continue
	    rbt_internal $host "cat > $i" < $i
	done
    )
    unset qatests REXEC host
}

#
# Experimental: Transfer text files from a remote host
#
# Usage: rbt_get host files
# (files relative to tools/weblogic/qa/tests)
#
# Note: I have to use a local version of rexec because the java jrexec
# doesn't handle standard input properly
#
# Example: rbt_get foobar '*.out'
#
rbt_get() {
    : ${DEV_ROOT:?} ${SRC:?} ${1:?host} ${2:?files}
    qatests=$DEV_ROOT/tools/wls/tools/weblogic/qa/tests
    cd $qatests
    case "$OS" in
	SunOS)		REXEC="$DEV_ROOT/env/bin/$OS/rexec"
	    		REMOVECR="dos2unix 2>/dev/null"
			;;
	HP-UX)		REXEC="$DEV_ROOT/env/bin/$OS/rexec"
	    		REMOVECR="dos2ux 2>/dev/null"
			;;
	Linux)		REXEC=/usr/bin/rexec
	    		REMOVECR="dos2unix"
			;;
	Windows_NT)	REXEC="$ROOTDIR/mksnt/rexec.exe"
	    		REMOVECR="flip"
			;;
	*)              echo "No rexec, unsupported OS" 1>&2 ; exit 1 ;;
    esac
    host="$1"
    shift
    # use ()'s to make absolutely sure RBT_XFER gets unset
    (
	RBT_XFER=true
	for i in `rbt_internal $host "\ls $* 2>/dev/null" | eval $REMOVECR`; do
	    rbt_internal $host "cat $i" > $i
	done
    )
    unset qatests REXEC host
}

# Used to kill dynamically generic jobs that might already be running.  Can
# also be used to kill any queued jobs *if* you know the machine name.
#
# dequeue a job from the generic queue, hunt down the machine it might be
# running on and killbt it on the machine
# rkill job.xxxxxx
#
# dequeue a job on a given machine and killbt it if it is running
# rkill machine job.xxxxxx
#
# dequeue a job on the local machine and killbt it if it is running.  Also
# see the .run file that the rdequeuer creates.
# rkill -l job.xxxxxx
rkill() {
    : ${DEV_ROOT:?} ${SRC:?} ${NEW_SRC:?}
    if [ -z "$RQSITE" ]; then
	. $DEV_ROOT/tools/wls/infra/infraenv.sh
    fi
    [ $# -le 0 ] && return
    if [ $# -eq 1 ]; then
	RQUEDIR=$RQSITE/$SRC/queued
	RSTATDIR=$RQSITE/$SRC/status
	if [ -f $RSTATDIR/$1 ]; then
	    machine=`tail -1 $RSTATDIR/$1 | tr -d '\000' | awk -F': ' '{print $2}'`
	    if [ -n "$machine" ]; then
		tmpfile=`mktemp -t rkill.XXXXXX`
		sed -n '/^.*: [^:]*: jenkins:/s/^.*: \([^:]*\): jenkins: \(.*\)$/\1 \2/p' $RSTATDIR/$1 | tr -d '\r' | tail -1 > $tmpfile
		read jmachine jurl < $tmpfile
		rm -f $tmpfile
		# if the jenkins build job URL machine is the same as the using the tail command then use jenkins to kill
		if [ "$machine" != "$jmachine" ]; then
		    rkill $machine $1
		else
		    # we update the jenkins URL from the central RQ and if that is the "machine" then it never started running
		    if [ "$machine" != "centralrq" ]; then
			jenkins_kill $jmachine $jurl $1
		    else
			rqstatus -j $1 "FATAL: this job has been killed"
			rqstatus -j $1 -u
		    fi
		fi
	    fi
	fi
	if [ -f $RQUEDIR/$1 ]; then
	    mv $RQUEDIR/$1 $RSTATDIR/
	    rqstatus -j $1 "FATAL: this job has been killed"
	    rqstatus -j $1 -u
	fi
	# make absolutely sure it is gone
	rm -f $RQUEDIR/$1
    elif [ $# -eq 2 ]; then
	if [ "$1" = "-l" ]; then
	    RQUEDIR=$DEV_ROOT/tools/wls/infra/test/queued
	    rdequeue -d $2
	    # do *not* start doing killbt's or anything on the centralrq
	    if [ -f /tmp/rdequeuer-$NEW_SRC.run -a "`uname_n`" != "centralrq" ]; then
		read jobid pid < /tmp/rdequeuer-$NEW_SRC.run
		if [ "$jobid" = "$2" -a -n "$pid" ]; then
		    echo "DEBUG: `date` running killbt, lock file is..."
		    if [ -s /tmp/rdequeuer-$NEW_SRC.lock ]; then
			cat /tmp/rdequeuer-$NEW_SRC.lock
			read REPLY < /tmp/rdequeuer-$NEW_SRC.lock
			kill -9 $REPLY
		    fi
		    $DEV_ROOT/tools/wls/infra/build/killbt.sh
		    # make sure it is marked busy, there seem to be
		    # problems sometimes with the rdequeuer -r starting up
		    # successfully again
		    rqstatus busy
		    sh $DEV_ROOT/tools/wls/infra/test/at_rdequeuer.sh -r
		fi
	    fi
	    rqstatus -j $2 "FATAL: this job has been killed (if running)"
	    rqstatus -j $2 -u
	else
	    rbt -t 120 "$1" "cd ../..&&. ./.rbt.envs&&cd tools/wls&&. ./infra/infraenv.sh&&. ./infra/build/rbt.sh&&rkill -l $2"
	fi
    fi
}

#---------
# obsolete not used much or at all anymore?
#---------
# return the number of builds in the Build Queue w/o label
jenkins_queued_internal() {
    curl --noproxy '*' --fail --silent --show-error "${JENKINS_URL}queue/api/json?pretty=true" | awk 'BEGIN{a=0} /"JOBFILE"/{a++} END{print a}'
}

#---------
# obsolete not used much or at all anymore?
#---------
# return the number of builds in the Build Queue, try hard to give the
# answer of zero, if the answer is one then wait up to 15 seconds for
# the answer to be zero, if it is greater than 1 quickly give up and
# return the number
# I have found that it takes 6 seconds or so for a build queue of one to be put on an agent
jenkins_queued() {
    queued=`jenkins_queued_internal`
    retries=0
    while [ $retries -le 15 -a $queued -ne 0 -a $queued -le 1 ]; do
	sleep 1
	queued=`jenkins_queued_internal`
	if [ $queued -eq 0 -o $queued -gt 1 ]; then
	    echo $queued
	    return
	fi
	retries=`expr $retries + 1`
    done
    echo $queued
}

jenkins_quiet_mode() {
    curl --noproxy '*' --fail --silent --show-error "${JENKINS_URL}/api/json?pretty=true" | grep '"quietingDown"' | grep --color=auto ': true'
}

jenkins_rqjob_buildable() {
    curl --noproxy '*' --fail --silent --show-error "${JENKINS_URL}/job/$JENKINS_RQJOB/api/json?pretty=true"  | grep '"buildable"' | grep ': true' >/dev/null
}

jenkins_queueid() {
    : ${1:?JOBID} # $2 = queue id, optional argument
    if [ -n "$2" ]; then
	REPLY="$2"
    else
	tmpfile=`mktemp -t queueid.XXXXXXX`
	unset REPLY
	# try up to three times sleeping one second between tries
	for i in 1 2 3; do
	    curl --noproxy '*' --fail --silent --show-error "${JENKINS_URL}queue/api/json?pretty=true" | rbt_python $DEV_ROOT/tools/wls/infra/build/get_queueid.py "$1" > $tmpfile
	    read < $tmpfile
	    if [ -n "$REPLY" ]; then
		break
	    else
		sleep 1
	    fi
	done
	rm -f $tmpfile
    fi
    if [ -n "$REPLY" ]; then
       if [ -z "$2" ]; then
	   rqstatus -j "$1" "moving to jenkins, queueid=$REPLY"
       fi
       url="${JENKINS_URL}queue/item/$REPLY"
       for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
	   sleep 1
	   curl --noproxy '*' --fail --silent --show-error "${JENKINS_URL}queue/item/$REPLY/api/json?pretty=true" | tee $tmpfile | grep '"number"' >/dev/null
	   if [ $? -eq 0 ]; then
	       url=`awk < $tmpfile -F'"' '/"url"/{a=$4} END{print a}'`
	       rqstatus -j "$1" "jenkins: $url"
	       break
	   fi
       done
       rm -f $tmpfile
    else
       rqstatus -j "$1" "moving to jenkins, queueid not found"
    fi
}

jenkins_qcount_internal() {
    : ${1:?labelExpression}
    curl --noproxy '*' --fail --silent --show-error "${JENKINS_URL}queue/api/json" | rbt_python $DEV_ROOT/tools/wls/infra/build/qcount.py "$1"
}

# return the number of builds in the build queue by label, try hard to give the
# answer of zero, if the answer is one then wait up to 15 seconds for
# the answer to be zero, if it is greater than 1 quickly give up and
# return the number
# I have found that it takes 6 seconds or so for a build queue of one to be put on an agent
jenkins_qcount() {
    : ${1:?labelExpression}
    queued=`jenkins_qcount_internal $1`
    retries=0
    while [ $retries -le 15 -a $queued -ne 0 -a $queued -le 1 ]; do
	sleep 1
	queued=`jenkins_qcount_internal $1`
	if [ $queued -eq 0 -o $queued -gt 1 ]; then
	    echo $queued
	    return
	fi
	retries=`expr $retries + 1`
    done
    # try to make sure that if there is a curl or network glitch we assume things are busy not empty
    echo ${queued:-9999}
}

# return the total number of jenkins agents free (total - busy) by label defaulting to linux&&rq
jenkins_free_agents() {
    if [ -z "$1" ]; then
	label="linux-ol8-rq&&rq"
    else
	label="$1"
    fi
    # use HTML encoding for | (label OR), it doesn't seem to be needed for & ( )
    label="`echo $label | sed -e 's/|/%7c/g'`"
    curl --noproxy '*' --fail --silent --show-error "${JENKINS_URL}label/$label/api/json" | rbt_python $DEV_ROOT/tools/wls/infra/build/free_agents.py
    unset label
}

# this is a REST crumb that is needed for some REST requests against *some* jenkins masters
# if the master requires it this will get a crumb and return the appropriate curl options to pass it on
# if the master does not require it this will return nothing
insert_crumb() {
    tmpfile=`mktemp -t crumb.XXXXXXX`
    COOKIEJAR="/tmp/cookie.${NEW_SRC:-$SRC}"
    CRUMB=`curl --cookie-jar "$COOKIEJAR"  --noproxy '*' --fail --silent --show-error ${JENKINS_URL}'crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)' 2> $tmpfile`
    echo "${CRUMB:+--cookie $COOKIEJAR --header }$CRUMB"
    unset CRUMB
    # ignore 404 because that is just due to the Jenkins server not respecting crumbs, but output other errors to stderr
    grep -v ' error: 404 Not Found' < $tmpfile 1>&2
    rm -f $tmpfile
}

jenkins_enqueue() {
    #
    # Usage:
    # jenkins_enqueue <JOBID> -l labelExpression
    #     enqueue JOBID to jenkins $JENKINS_RQJOB using label expression labelExpression
    #
    # jenkins_enqueue <JOBID> -h <HOST>
    #     enqueue JOBID to jenkins $JENKINS_RQJOB to host <HOST>
    #
    # doing the rqstatus creates the job file in
    # the status/ directory, normally this
    # wouldn't work but jenkins will take the job
    # file we sent in the form and create it
    # locally and it will make its way into the
    # status directory from there overwritting the
    # "moving to jenkins" message
    grep remotetestnew <$RQSITE/$SRC/queued/$1 >/dev/null
    if [ $? -eq 0 ]; then
	NEW=new
    else
	NEW=
    fi
    if [ "$2" = "-l" -a -n "$3" ]; then
	facount=`jenkins_free_agents "$3"`
	if [ "$facount" -le 0 ]; then
	    #echo "DEBUG2: skipping jenkins_enqueue for $1 because facount = $facount ($3)"
	    # if all these labels are busy then just skip enqueuing at this point
	    return
	else
	    true # echo "DEBUG2: continuing with jenkins_enqueue for $1 because facount = $facount ($3)"
	fi
	unset facount
	EXTRA=', {"name":"LABEL", "value":"'$3'"}'
    elif [ "$2" = "-h" -a -n "$3" ]; then
	if [ -n "`get_machines -jio \"$3\"`" ]; then
	    EXTRA=', {"name":"HOST", "value":"'$3'"}'
	else
	    #echo "DEBUG2: skipping jenkins_enqueue for $1 machine is not idle"
	    # if the machine isn't idle and online then just skip enqueuing at this point
	    return
	fi
    else
	echo "ERROR: unknown arguments (jenkins_enqueue $@)" 1>&2
	return 1
    fi
    if [ -s $RQSITE/$SRC/queued/$1 ]; then
	unset p4user p4email MAILTO
	p4user=`awk 'FNR == 2 {print $3}' < $RQSITE/$SRC/queued/$1`
	p4email="`p4 user -o $p4user | awk '/^Email:/{print $NF}'`"
	unset p4user
	MAILTO=`sed -n -e '2s/.* MAILTO=\([^ ]*\) .*/\1/p' -e '2s/.* MAILTO=\([^ ]*\)$/\1/p' < $RQSITE/$SRC/queued/$1 | sed 's/,/ /g'`
	if [ -n "$p4email" ]; then
	    MAILTO="$p4email${MAILTO:+ }$MAILTO"
	fi
	unset p4email
	if [ -n "$MAILTO" ]; then
	    MAILTO=', {"name":"MAILTO", "value":"'$MAILTO'"}'
	fi
	#FIXME requires anonymous access to be able to start builds
	curl --noproxy '*' --fail --silent --show-error --request POST `insert_crumb` --form file0=@$RQSITE/$SRC/queued/$1 --form json='{"parameter": [ {"name":"JOBFILE", "file":"file0"}, {"name":"SRC", "value":"'$SRC'"}, {"name":"JOBID", "value":"'$1'"}'"$EXTRA$MAILTO]}" "${JENKINS_URL}job/$JENKINS_RQJOB$NEW/build?token=2b2ed023db02"
	if [ $? -eq 0 ]; then
	    rqstatus -j "$1" "moving to jenkins"
	    jenkins_queueid "$1"
	else
	    echo "WARNING: jenkins REST to enqueue RQ job failed"
	fi
	unset MAILTO
    else
	echo "WARNING: skipping enqueuing $1 because job file not in queued/"
    fi
    set +x
    unset EXTRA NEW
}

jenkins_kill() {
    # jenkins_kill host url jobid
    # host is the jenkins agent the jobid was running on
    # url is the jenkins build job url
    #FIXME requires anonymous access to be able to start builds
    # doing the rqstatus creates the job file in
    # the status/ directory, normally this
    # wouldn't work but jenkins will take the job
    # file we sent in the form and create it
    # locally and it will make its way into the
    # status directory from there overwritting the
    # "moving to jenkins" message

    # jenkins_kill is used only when job file is in status folder
    grep remotetestnew <$RQSITE/$SRC/status/$3 >/dev/null
    if [ $? -eq 0 ]; then
	NEW=new
    else
	NEW=
    fi
    unset p4user p4email MAILTO
    p4user=`awk 'FNR == 2 {print $3}' < $RQSITE/$SRC/status/$3`
    p4email="`p4 user -o $p4user | awk '/^Email:/{print $NF}'`"
    unset p4user
    MAILTO=`sed -n -e '2s/.* MAILTO=\([^ ]*\) .*/\1/p' -e '2s/.* MAILTO=\([^ ]*\)$/\1/p' < $RQSITE/$SRC/status/$3 | sed 's/,/ /g'`
	if [ -n "$p4email" ]; then
	    MAILTO="$p4email${MAILTO:+ }$MAILTO"
	fi
	unset p4email
	if [ -n "$MAILTO" ]; then
	    MAILTO=', {"name":"MAILTO", "value":"'$MAILTO'"}'
	fi
    # if it is a dynamic jenkins/oci agent then convert from the hostname (jenkins-<ip address>-UUID) to the agent name (oci-compute-UUID)
    echo "$1" | grep "^jenkins-" >/dev/null
    if [ $? -eq 0 ]; then
	a=`echo "$1" | sed 's/jenkins-[0-9][0-9]*-[0-9][0-9]*-[0-9][0-9]*-[0-9][0-9]*-/oci-compute-/'`
	# get a into $1
	set "$a" "$2" "$3"
	unset a
    fi
    # because our windows hosts are often named <host>-win, this code should handle win hosts agents being named either <host>-win or plain <host>
    tmpfile=`mktemp -t kill.XXXXXXX`
    get_machines -j > $tmpfile
    grep "^$1-win$" < $tmpfile > /dev/null
    if [ $? -eq 0 ]; then
	jhost="$1-win"
    else
	jhost="$1"
    fi
    # only start a kill build if the jenkins agent exists
    grep "^$jhost$" < $tmpfile > /dev/null
    if [ $? -eq 0 ]; then
	# start the RQ job first so that it will run right after the build is killed
	curl --noproxy '*' --fail --silent --show-error --request POST `insert_crumb` --form json='{"parameter": [ {"name":"SRC", "value":"'$SRC'"}, {"name":"JOBID", "value":"'$3'"}, {"name":"HOST", "value":"'$jhost'"}, {"name":"KILL", "value":"true"}'"$MAILTO]}" "${JENKINS_URL}job/$JENKINS_RQJOB$NEW/build?token=2b2ed023db02" || echo "WARNING: jenkins REST to stop RQ job failed"
    else
	echo "WARNING: killing job $3 on non-existance Jenkins agent $jhost"
	rkill -l "$3"
    fi
    rm -f "$tmpfile"
    # note this requires Anonymous Users to Cancel a Job in Configure Global Security in Jenkins
    curl --noproxy '*' --fail --silent --show-error --request POST `insert_crumb` "${2}stop"
    unset MAILTO
    unset NEW
    unset jhost
}

# Used to move a job from the central queue to a machine's queue
#
# move job from central to local queue on machine
# Usage: rquemove machine job.xxxxxx
#
# move job from central to local queue on several machines, creating sub-jobs
# Usage: rquemove "machine machine ..." job.xxxxxx
#
# move job from central to local queue on this machine
# Usage: rquemove -l job.xxxxxx
#
# move job from the local queue to central
# Usage: rquemove -r job.xxxxxx
rquemove() {
    : ${DEV_ROOT:?} ${SRC:?}
    if [ $# -ne 2 ]; then
	echo "Usage: rquemove [machine(s)|-l|-r] job.XXXXXX" 1>&2
	return 1
    fi
    if [ "$1" = "-l" ]; then
	if [ -z "$RQSITE" ]; then
	    . $DEV_ROOT/tools/wls/infra/infraenv.sh
	fi
	mkdir -p $DEV_ROOT/tools/wls/infra/test/queued

	rqstatus busy

	# FIXME: herrlich@bea.com Mar 30, 2007
	# I checked the rdequeuer.sh.log on rq.bea.com and this check has only
	# been hit three times in dev/src and I do not think it helped (the machine
	# was in the process of restarting).  Also this logic is going to break the
	# multi-branch machine stuff I am working on.
	#	if [ "`rqstatus`" = "notrunning" ]; then
	#	    echo "FATAL: local dequeuer not running, `uname -a`, `date`"
	#	    return 1
	#	fi

	other_branch_queued_dir=""
        other_branch_queued_dir2=""
	echo "$DEV_ROOT" | grep -e /coherence-ce/ -e /release/ > /dev/null
	if [ $? -eq 0 ]; then
	    other_branch_queued_dir="$DEV_ROOT/../../*/tools/wls/infra/test/queued"
	    other_branch_queued_dir2="$DEV_ROOT/../../*/tools/common/wls/infra/test/queued"
	else
	    # I *think* this should also be looking in coherence-ce but I'm not sure it makes a difference now with the
	    # Jenkins RQ
	    other_branch_queued_dir="$DEV_ROOT/../release/*/tools/wls/infra/test/queued"
            other_branch_queued_dir2="$DEV_ROOT/../release/*/tools/common/wls/infra/test/queued"
	fi
	if [ -f $DEV_ROOT/tools/wls/infra/test/queued/$2 ]; then
	    echo "INFO: $2 already exists, removing ${RQSITE:?}/$SRC/queued/$2" 1>&2
	    rm ${RQSITE:?}/$SRC/queued/$2
	elif [ "`find $DEV_ROOT/../*/tools/wls/infra/test/queued $DEV_ROOT/../*/tools/common/wls/infra/test/queued -type f -name 'job.*' 2>/dev/null | wc -l`" -ge 1 ]; then
	    echo "INFO: a job is already queued for this machine, do not move $2 to this machine" 1>&2
	    return
	    elif [ "`find $other_branch_queued_dir other_branch_queued_dir2 -type f -name 'job.*' 2>/dev/null | wc -l`" -ge 1 ]; then
	            echo "INFO: a different series branch job is already queued for this machine, do not move $2 to this machine" 1>&2
	            return
	else
	    # this is important because it is uninterrruptable, the file
	    # will either get moved or not.  It is unlikely it will end up in
	    # both locations
	    # ATOMIC #
	    mv -f ${RQSITE:?}/$SRC/queued/$2 $DEV_ROOT/tools/wls/infra/test/queued
	fi
    elif [ "$1" = "-r" ]; then
	if [ -z "$RQSITE" ]; then
	    . $DEV_ROOT/tools/wls/infra/infraenv.sh
	fi
	if [ ! -f $DEV_ROOT/tools/wls/infra/test/queued/$2 ]; then
	    echo "WARNING: $2 does not exist!"
	    return
	fi
	mkdir -p $RQSITE/$SRC/queued/ $RQSITE/$SRC/status/
	# if it already has status then use that file for queued to continue
	# the status.  Note: there is a slight hole here where the job could
	# get duplicated... if these commands are killed after the mv but
	# before the rm.  This is unlikely since the mv is a slow (network)
	# command and the rm is a quick (local disk) command.
	if [ -f $RQSITE/$SRC/status/$2 ]; then
	    mv -f $RQSITE/$SRC/status/$2 $RQSITE/$SRC/queued/$2
	    rm -f $DEV_ROOT/tools/wls/infra/test/queued/$2
	    chmod a+rw $RQSITE/$SRC/queued/$2
	else
	    # ATOMIC #
	    mv -f $DEV_ROOT/tools/wls/infra/test/queued/$2 $RQSITE/$SRC/queued/
	    chmod a+rw $RQSITE/$SRC/queued/$2
	fi
    else
	echo "$1" | sed -e 's/^ *//' -e 's/ *$//' | grep -e " " -e "," >/dev/null
	if [ $? -ne 0 ]; then
	    if echo "$1" | grep ^jenkins= >/dev/null; then
		# code path for LABEL=<jenkins label expression>
		label="`echo \"$1\" | awk -F= '{print $2}'`"
		qcount=`jenkins_qcount $label`
		if [ $qcount -le 0 ]; then
		    #echo "DEBUG: continuing jenkins_enqueue for $2 because qcount = $qcount ($label)"
		    facount=`jenkins_free_agents "$label"`
		    if [ "$facount" -gt 0 ]; then
			#echo "DEBUG: continuing jenkins_enqueue for $2 because facount = $facount ($label)"
			jenkins_enqueue "$2" -l "$label"
		    else
			true #echo "DEBUG: skipping jenkins_enqueue for $2 because facount = $facount ($label)"
		    fi
		    unset facount
		else
		    true #echo "DEBUG: skipping jenkins_enqueue for $2 because qcount = $qcount ($label)"
		fi
		unset qcount
	    elif echo "$1" | grep ^jenkins@ >/dev/null; then
		# code path for HOST=<jenkins agent>
		agent=`echo "$1" | awk -F@ '{print $2}'`
		jenkins_enqueue "$2" -h "$agent"
            else # old use case, this is a host name that we can rbt into
		rbt -t 240 "$1" "cd ../..&&. ./.rbt.envs&&cd tools/wls&&. ./infra/build/rbt.sh&&rquemove -l $2"
            fi
	else
	    if [ -z "$RQSITE" ]; then
		. $DEV_ROOT/tools/wls/infra/infraenv.sh
	    fi
	    # allow spaces or commas
	    machines="`echo "$1" | sed 's/,/ /g'`"
	    extraopts=""
	    count=1
	    for i in $machines; do
		if [ $count -eq 1 ]; then
		    extraopts="RQHOST${count}=$i"
		else
		    extraopts="$extraopts RQHOST${count}=$i"
		fi
		count=`expr $count + 1`
	    done
#	    echo "DEBUG: extraopts=$extraopts" 1>&2
	    # make sure the jobcommand is handled so that no shell commands
	    # get translated like $VAR or `cmd`
	    jobcommand=`sed -n -e '2p' < "$RQSITE/$SRC/queued/$2"`
	    mv -f $RQSITE/$SRC/queued/$2 $RQSITE/$SRC/status/$2
	    machine_count=1
	    rqstatus -j "$2" "starting"
	    for machine in $machines; do
		(
		    echo "#,HOST=$machine,"
		    echo "$jobcommand $extraopts"
		) > "$RQSITE/$SRC/queued/$2-"`printf "%02d" $machine_count`
		rquemove "$machine" "$2-$machine_count"
		rqstatus -j "$2" "INFO: $2-$machine_count started on $machine"
		machine_count=`expr $machine_count + 1`
	    done
	    rqstatus -j "$2" "done"
	fi
    fi
}

#
# Used to enqueue stuff to run on nightly test machines after the regular
# nightly tests are done.
#
# to enqueue locally
# Usage: renqueue -l bla bla bla
# to enqueue locally with priority (priorities are 0 (high) to 9 (low = remote test))
# the -p is positional
# Usage: renqueue -l -p # bla bla bla
# to enqueue remotely
# Usage: renqueue host bla bla bla
# to enqueue remotely with priority
# Usage: renqueue host -p # bla bla bla
# to enqueue to the central queue with "attributes" ENVVAR=VALUE
# Usage: renqueue VAR=VAL bla bla bla
# to enqueue to the central queue with "attributes" ENVVAR=VALUE with priority
# the -p is positional
# Usage: renqueue VAR=VAL -p # bla bla bla
renqueue() {
    : ${DEV_ROOT:?} ${SRC:?}
    echo "$1" | grep = >/dev/null 2>&1
    if [ $? -eq 0 ]; then
	central_queue=true
	if [ -z "$RQSITE" ]; then
	    . $DEV_ROOT/tools/wls/infra/infraenv.sh
	fi
	RQUEDIR=${RQSITE:?}/$SRC/queued
    else
	central_queue=false
	RQUEDIR=$DEV_ROOT/tools/wls/infra/test/queued
    fi
    if [ "$1" = "-l" -o "$central_queue" = "true" ]; then
	envvars="$1"
	shift
	if [ "$1" = "-p" ]; then
	    priority="${2:?priority}."
	    shift 2
	else
	    priority=""
	fi
	: ${1:?params}
	[ ! -d $RQUEDIR ] && mkdir -p $RQUEDIR
	QUEUED_ID=job.$priority`date -u +%Y%m%d%H%M%S`.$RANDOM
	ENTRY=$RQUEDIR/$QUEUED_ID
	# this is mainly a workaround if RANDOM doesn't exist, which it
	# doesn't in pure Bourne shells like "ash" in Cygwin or sh on Solaris.
	# sleeping 1 second causes date to give a different job id
	if [ -f $ENTRY ]; then
	    sleep 1
	    QUEUED_ID=job.$priority`date -u +%Y%m%d%H%M%S`.$RANDOM
	    ENTRY=$RQUEDIR/$QUEUED_ID
	fi
	[ -f $ENTRY ] && echo "FATAL: $ENTRY already exists!" 1>&2 && return 1
	(
	    if [ "$central_queue" = "true" ]; then
		echo "#,$envvars," | sed \
		    -e 's/;/,;,/g' \
		    -e 's/,S=/,SITE=/g' \
		    -e 's/,SITE=sf,/,SITE=san-francisco,/g' \
		    -e 's/,SITE=lc,/,SITE=liberty-corner,/g' \
		    -e 's/,SITE=bu,/,SITE=burlington,/g' \
		    -e 's/,SITE=burl,/,SITE=burlington,/g' \
		    -e 's/,SITE=re,/,SITE=reno,/g' \
		    -e 's/,SITE=bg,/,SITE=bangalore,/g' \
                    -e 's/,O=/,OS=/g' \
		    -e 's/,OS=w2k,/,OS=W2K,/g' \
		    -e 's/,OS=w2k3,/,OS=W2K3,/g' \
		    -e 's/,OS=vista,/,OS=VISTA,/g' \
		    -e 's/,OS=Vista,/,OS=VISTA,/g' \
		    -e 's/,OS=Solaris,/,OS=Sol,/g' \
		    -e 's/,H=/,HOSTNAME=/g' \
		    -e 's/,,;/,;/' \
		    -e 's/;,,/;,/' \
		    -e 's/,,$/,/' \
		    -e 's/^#,,/#,/'
	    fi
	    a=""
	    # make sure null parameters are quoted
	    for i in "$@"; do
		if [ "$i" = "" ]; then
		    a="$a \"$i\""
		else
		    a="$a $i"
		fi
	    done
	    echo "$a"
	    unset a
	) > $ENTRY
	if [ "`uname`" = "Windows_NT" ]; then
	    # because ^M (carriage returns) can break commands when enqueued
	    # on windows and executed on UNIX.  For example, the command...
	    # sh ./foo.sh >foo.log 2>&1^M
	    # gets the error ": ambiguous redirect" because of the ^M and this
	    # it very tricky to figure out
	    flip -u $ENTRY
	fi
	unset envvars
    else
	: ${1:?host} ${2:?params}
	host="$1"
	shift
	rbt -t 120 "$host" "cd ../..&&. ./.rbt.envs&&cd tools/wls&&. ./infra/build/rbt.sh&&renqueue -l '$@'"
    fi
    unset central_queue RQUEDIR ENTRY
}

#
# Used to dequeue stuff from a local queue, enqueued by renqueue
#
# to get the item at the top of the queue (without removing)
# Usage: rdequeue
# env var QUEUED_ID, QUEUED_PARAMS, return status 1 on failure
#
# to get the item at the top of the queue (removing from queue)
# Usage: rdequeue -d
# env var QUEUED_PARAMS, return status 1 on failure
#
# to delete an item from queue
# Usage: rdequeue -d $QUEUED_ID
# return status 1 on failure
#
# to do one "loop" of generic queuing (moving dynamically queued jobs to machines)
# Usage: rdequeue -g
rdequeue() {
    : ${DEV_ROOT:?} ${SRC:?}
    if [ "$1" = "-g" ]; then
	rdequeue_generic
	return
    fi
    RQUEDIR=$DEV_ROOT/tools/wls/infra/test/queued
    unset QUEUED_ID QUEUED_PARAMS POST_DELETE
    [ ! -d $RQUEDIR ] && return 1
    if [ "$1" = "-d" ]; then
	if [ "$#" -eq 2 ]; then
	    rm -f $RQUEDIR/$2
	    return
	elif [ "$#" -eq 1 ]; then
	    POST_DELETE=true
	else
	    return 1
	fi
    fi
    ENTRY="`\ls $RQUEDIR/job.* 2>/dev/null | head -1`"
    [ -z "$ENTRY" ] && return 1
    # if the file is empty then wait 15 seconds in case it is being created.
    # If it is still empty then delete it and return 1.
    if [ ! -s "$ENTRY" ]; then
	sleep 15
	if [ ! -s "$ENTRY" ]; then
	    rm -f "$ENTRY"
	    return 1
	fi
    fi
    [ -d /tmp ] || mkdir /tmp
    grep -v '^#' < "$ENTRY" > /tmp/rdequeue.tmp
    status=$?
    [ $status -ne 0 ] && return $status
    read QUEUED_PARAMS < /tmp/rdequeue.tmp
    rm -f /tmp/rdequeue.tmp
    if [ "$POST_DELETE" = "true" ]; then
	rm -f $ENTRY
	unset POST_DELETE
	return
    fi
    QUEUED_ID=`basename $ENTRY`
}

# Run a execution daemon to dequeue and requeue generic jobs to
# available machines.
#
# This shouldn't really be called by the end user or other shell scripts.
# It should be called via "rdequeuer -g"
#
rdequeuer_generic() {
    : ${SRC:?}
    if [ -z "$RQSITE" ]; then
	. $DEV_ROOT/tools/wls/infra/infraenv.sh
    fi
    if [ "$1" = "-c" ]; then
	(
	    if [ -d "$RQSITE/$SRC/queued" ]; then
		cd "$RQSITE/$SRC/queued"
		# I do not know what a .swp file is but see them occasionally
		rm -f .*.swp .nfs*
		for i in job.*; do
		    [ ! -f "$i" ] && continue
		    # don't remove the remotetests
		    grep /remotetest $i > /dev/null
		    if [ $? -ne 0 ]; then
			rqstatus -j $i "FATAL: this job has been killed (if running)"
			rqstatus -j $i -u
			rm -f "$i"
		    fi
		done
	    fi
	)
	return
    fi
    # get local hostname, lowercase and remove any .bea*.com extension
    thishost="`uname_n`"
    central_lock=$RQSITE/$SRC/rdequeuer.lock

    # make sure all the appropriate directories exist
    # note that having base directories like $SRC and remotetests in there even while using -p
    # means that the mode applies to those directories also not just the lowest directory
    mkdir -m a+rwx -p \
	$RQSITE/common/available \
	$RQSITE/$SRC \
	$RQSITE/$SRC/available \
	$RQSITE/$SRC/queued \
	$RQSITE/$SRC/status \
	$RQSITE/$SRC/archive.status \
	$RQSITE/$SRC/remotetests \
	$RQSITE/$SRC/remotetests/autosubmit \
	$RQSITE/$SRC/remotetests/autocommit \
	$RQSITE/$SRC/remotetests/requeued \
	$RQSITE/$SRC/remotetests/zips
    # FIXME: herrlich@bea.com Nov 30, 2006
    # this is needed so that both the remote tests and the auto-submit
    # monkey can write into the directory since the auto-submit monkey
    # doesn't run as 'bt'.  The remote tests write jobid and .zip files and
    # the auto-submit monkey writes .log's.  And the remote test will delete
    # all the files.
    chmod a+rwx $RQSITE/$SRC/remotetests/autosubmit $RQSITE/$SRC/remotetests/autocommit
    # this is so that aime* accounts from adc* machines will work
    chmod a+rwx \
	$RQSITE/common/available \
	$RQSITE/$SRC/available \
	$RQSITE/$SRC/queued \
	$RQSITE/$SRC/status
    # this is so that start_integ.php and start_approve.php script from tamarac (apache) will work
    chmod a+rwx \
	$RQSITE/$SRC/remotetests/zips

    if [ ! -s $central_lock ]; then
	echo $thishost $$ > $central_lock
    fi

    echo "INFO: `date`: starting rdequeuer -g" 1>&2

    while true; do

	# because these directories get removed and recreated
	# (especially available) in several places that permissions
	# can be set incorrectly so that the aime*@adc* machines do
	# not work.  -maxdepth will only work on Linux or Cygwin (not
	# MKS) but I do not see the central dequeuer running even on
	# Windows anytime soon.
	find "$RQSITE/common/available" "$RQSITE/$SRC/available" "$RQSITE/$SRC/status" -maxdepth 0 ! -perm 777 | xargs -r chmod a+rwx

	if [ -s $central_lock ]; then
	    read host pid < $central_lock
	    if [ "$host" != "" -a "$host" != "$thishost" ]; then
		echo "FATAL: Can not run generic dequeuer on this machine ($thishost)." 1>&2
		echo "FATAL: It is already running on $host (pid = $pid)!" 1>&2
		exit 1
	    elif [ "$host" = "$thishost" -a "$pid" != "$$" ]; then
		kill_0 "$pid"
		if [ $? -eq 0 ]; then
		    echo "FATAL: Can not run generic dequeuer since one (pid = $pid) is already running!" 1>&2
		    exit 1
		else
		    echo $thishost $$ > $central_lock
		fi
	    fi
	    touch $central_lock
	else
	    echo "FATAL: central lock file $central_lock deleted while generic dequeuer was running!" 1>&2
	    echo "FATAL: generic dequeuer exiting!" 1>&2
	    exit 1
	fi

	rdequeue -g
	# load the rjob data, this has a lock file to prevent more than one copy and it will itself make
	# sure it isn't run more than once every 60 seconds or so
	$DEV_ROOT/tools/wls/infra/test/load.sh &
	# check for jobs where the jenkins build FAILED or was ABORTED and re-queue as necessary. works similar to load.sh
	$DEV_ROOT/tools/wls/infra/test/requeue_jenkins.sh &
	sleep 10
    done

    rm -f $central_lock
}

rbt_stat() {
    # return stat() mtime for the given file
    # Usage: rbt_stat filename
    if [ "$OS" = "" ]; then
	. ${DEV_ROOT:?}/tools/wls/infra/infraenv.sh
    fi
    : ${OS:?} ${1:?}
    case $OS in
	Windows_NT)
	    if [ "`which stat`" != "" ]; then
		stat -m "$1" | awk '{print $NF}'
	    fi
	    ;;
	Linux)      stat -t "$1" | awk '{print $14}' ;;
#	Solaris)    ?stat? "$1"
    esac
}

rbt_age() {
    # return number of seconds old a file is
    # Usage: rbt_age filename
    : ${1:?}
    mtime=`rbt_stat "$1"`
    nowfile="${TMP:-/tmp}/nowfile_$$"
    # create a file with a time of now so I can subtract the epoch
    # seconds from the mtime
    touch "$nowfile"
    now=`rbt_stat "$nowfile"`
    rm -f "$nowfile"
    if [ "$mtime" != "" -a "$now" != "" ]; then
	expr $now - $mtime
    fi
}

rdequeue_generic() {
    : ${DEV_ROOT:?} ${SRC:?}
    if [ -z "$RQSITE" ]; then
	. $DEV_ROOT/tools/wls/infra/infraenv.sh
    fi
    RQUEDIR=$RQSITE/$SRC/queued
    RSTATDIR=$RQSITE/$SRC/status
    RAVAILDIR=$RQSITE/$SRC/available
    RCOMAVAILDIR=$RQSITE/common/available
    RFLOATAVAILDIR=$RQSITE/../common/available
    unset QUEUED_ID QUEUED_PARAMS POST_DELETE
    [ ! -d $RQUEDIR -o ! -d $RAVAILDIR ] && return 1

    # handle job kill requests first
    cd $RQUEDIR
    # note can't use env var "i" because it will get overwritten by "rkill"
    for killfile in kill.job.*; do
	[ ! -f $killfile ] && continue
	echo "INFO: `date`: processing $killfile ..."
	jobid=`echo $killfile | sed 's/^kill\.//'`
	rkill $jobid
	rm $killfile
    done

    # next handle job syncronization requests
    cd $RQUEDIR
    for syncfile in sync.job.*; do
	[ ! -f $syncfile ] && continue
	echo "INFO: `date`: processing $syncfile ..."
	jobid=`echo $syncfile | sed 's/^sync\.//'`
	shortid=`echo $jobid | sed 's/-[0-9][0-9]*$//'`
	shortidfile=`find $RQSITE/$SRC/queued $RQSITE/$SRC/remotetests/autosubmit $RQSITE/$SRC/status -maxdepth 1 -type f -name "$shortid" -name "*[0-9]" | head -1`
	tmpfile=`mktemp syncfileXXXXXXXX`
	# generate a list of all the job id's minus the one passed in (this
	# job id's) To make sure we get them all combine two different lists,
	# the list of job files in the queued, autosubmit and status directories
	# which might be missing job id's that have been moved to RQ machines
	# but haven't written "starting..." status back to the status directory.
	# The list of files in the master job id file "Create to run".  This
	# format might change at some point and someone will forget to change
	# it here.
	(
	    echo $shortid
	    find $RQSITE/$SRC/queued $RQSITE/$SRC/remotetests/autosubmit $RQSITE/$SRC/status -maxdepth 1 -type f \( -name "$shortid" -o -name "$shortid-[0-9]*" \) -name "*[0-9]" | sed 's|^.*/\(job.*\)|\1|'
	     sed -n  '/ created to run /s/.*: INFO: \(job\.9\.[-0-9\.]*[0-9]\) created to run .*/\1/p' < $shortidfile
	) | sort -u | grep -v -x -F "$jobid" |\
	    while read syncjob; do
		echo $syncjob `rqstatus -j $syncjob`
	    done > $tmpfile
	chmod a+wr $tmpfile
	mv -f $tmpfile $RSTATDIR/$syncfile
	chmod a+wr $RSTATDIR/$syncfile
	rm -f $syncfile $tmpfile
    done

    # DEBUG: remove me
    #echo "debugging: stopping" ; return

    # this is necessary to pick up P4CONFIG properly for p4 commands
    cd $DEV_ROOT

    if [ -f $RQUEDIR/RESTART ]; then
	echo "`date`: beginning restart of generic queue, machines and nightly jobs"

	# since this happens on bt05 I need to do a sync to make sure the rc file is up to date with the kit
	syncto="#head"
	echo "`date`: syncing the branch to $syncto"
	p4 sync $DEV_ROOT/...$syncto | wc -l

	MACHINES="`get_machines`"

	echo "`date`: Marking all machines as busy"
	rqstatus -a busy
	# clear out all old generic jobs from the central queue
	rdequeuer -g -c
	# FIXME: herrlich@bea.com Aug 6, 2004
	# this should probably p4 print @srcrel-test eventually but I'll
	# just use #head for now.
	echo "`date`: Restarting all machines"
	rbt -p -t 800 "$MACHINES" "p4 print -q infra/build/restart.sh>restart.sh;sh ./restart.sh>infra/build/restart.log 2>&1"
	echo "`date`: Enqueuing all the generic runs from rc file to the central queue"
	renqueue_all_generics

	rm -f $RQUEDIR/RESTART
	echo "`date`: done restarting generic queue."
    fi

    if [ -f $RQUEDIR/PAUSE ]; then
	echo "`date`: skipping rdequeue_generic, generic queue is paused"
	return
    fi

    # if jenkins is in quiet mode (aka Shutdown Mode) then do not bother to enqueue RQ jobs
    if jenkins_quiet_mode; then
	linux=0 ; win=0; macosx=0
    elif ! jenkins_rqjob_buildable; then
	linux=0 ; win=0; macosx=0
    else
        # get the number of free agents this is used to enqueue an appropriate number of RQ jobs to jenkins before enqueuing to regular RQ machines
        linux=`jenkins_free_agents '(linux-ol8||linux-ol9)'`
        win=`jenkins_free_agents '((win-rq||windows-rq||win-${SRC}-rq||windows-${SRC}-rq)&&!slow)'`
        macosx=`jenkins_free_agents '((macosx-rq||macosx-${SRC}-rq)&&!slow)'`
    fi
    # to disable enqueuing RQ to jenkins *before* regular RQ jobs uncomment this line,  note that rq.pl will still enqueue to
    # jenkins if there are no regular RQ machines available
    #linux=0 ; win=0; macosx=0

    tmpfile=`mktemp /tmp/rbt_rq.tmpXXXXXXX`
    $DEV_ROOT/tools/wls/infra/build/rq.pl "${linux:-0}" "${win:-0}" "${macosx:-0}" > $tmpfile
    unset linux win macosx
    rq_count=`wc -l < $tmpfile`
    [ $rq_count -gt 1 ] && echo "DEBUG: `date`: rq.pl returned $rq_count jobs, rquemove-ing them..."
    start_secs=`date +%s`
    # tmpfile is being feed into this while loop at the "done" stmt, you cannot cat it into "cat | while" because it confuses the rquemove & wait jobs logic to create a pipe
    while read job RQHOSTS; do
#	echo "DEBUG: RQHOSTS=$RQHOSTS" 1>&2
	[ -z "$job" -o -z "$RQHOSTS" ] && continue
	for i in $RQHOSTS; do
	    if [ -f $RCOMAVAILDIR/$i ]; then
		# this is the "locking" that prevents more than one
		# branch from using a machine at the same time.
		# Before using a machine the machine "available"
		# file is moved from the common area to the branch
		# specific area.  To handle multi-machine allocations
		# *all* machines are moved to the branch available
		# directory before starting to run the job. If
		# more than one branch attempts this move command
		# only one branch will get the machine file.
		rm -f $RAVAILDIR/$i
		mv -f $RCOMAVAILDIR/$i $RAVAILDIR/
		if [ ! -f $RAVAILDIR/$i ]; then
		    # ack!! the move failed, just exit out of here
		    # and let the queuing happen again later (with
		    # a different set of machines)
		    RQHOSTS=""
		    break
		    # at this point we might be leaving some machine
		    # files in the branch specific areas.  This should
		    # hurt much, the rm -f *before* should help and
		    # eventually (10 minutes?) the machine will
		    # recreate it's available file in the common
		    # available directory
		fi
	    elif [ -f $RFLOATAVAILDIR/$i ]; then
		# the same logic as move $machine file from $RCOMAVAILDIR
		rm -f $RAVAILDIR/$i
		mv -f $RFLOATAVAILDIR/$i $RAVAILDIR/
		if [ ! -f $RAVAILDIR/$i ]; then
		    RQHOSTS=""
		    break
		fi
	    fi
	done
	rm -f $tmpfile
	[ -z "$RQHOSTS" ] && continue
	echo "INFO: `date`: enqueuing $job to $RQHOSTS `cat $RQUEDIR/$job`" 1>&2
	for i in $RQHOSTS; do
	    rm -f $RAVAILDIR/$i
	done
	if echo "$RQHOSTS" | grep -e ^jenkins[@=]. >/dev/null; then
	    # do *not* start jenkins jobs in the background, they are
	    # quick to start (using curl) and the build queue and
	    # label checks do not respond quickly enough to prevent
	    # enqueuing more jobs then there are agents to process
	    # them
	    rquemove "$RQHOSTS" $job </dev/null
	else
	    # Start in background to allow for slow ssh's.  Do not
	    # allow rquemove to read stdin and empty the list of rq.pl
	    # jobs
	    rquemove "$RQHOSTS" $job </dev/null &
	fi
	while true; do
	    # Check to see how long this has been running, don't rquemove jobs for longer than 5 minutes (300 seconds).  This can happen if lots of hosts are
	    # timing out.  Mar 4, 2015 this happened because the stjpg account was hanging on 44 machines ($HOME/.bash_history) and it caused rjobsync to start
	    # timing out and every subjob started sending email.  Anyway, normally 5 minutes should be enough time to rquemove all the jobs rq.pl can hand out
	    # and then those jobs will be rquemove'd the next time this routine is called.
	    # I just increase 150s limit for dispatching job, try dispatching more jobs, if this is not ok, then we can change back.
	    now_secs=`date +%s`
	    elapsed_secs=`expr $now_secs - $start_secs`
	    if [ $elapsed_secs -ge 450 ]; then
		wait # for rquemove's to complete
		break 2 # out of outer while loop
	    fi
	    rquemove_count=`jobs -r | wc -l`
	    if [ $rquemove_count -lt 10 ]; then
		[ $rquemove_count -gt 1 ] && echo "DEBUG: `date`: rquemove_count is only $rquemove_count, start another rquemove" 1>&2
		break # out of jobs checking loop
	    else
		sleep 1
	    fi
	done
    done < $tmpfile
    wait # for any rquemoves to complete
    rm -f $tmpfile
    return
}

# Used to run a execution daemon to dequeue and execute queued jobs.
#
# Usage: rdequeuer -c
# Clean out all entries from the local queue and return
# Usage: rdequeuer -c host1 [host2 ...]
# Clean out all entries from the queue on host1, host2, etc and return
# Usage: rdequeuer -g -c
# Clean out all entries from the generic queue
# Usage: rdequeuer -s
# Clean out all entries from the queue before starting
# Usage: rdequeuer -r
# (restart) Just start executing things.
# Usage: rdequeuer -o
# (restart) Just start executing things once... run until out of items and stop the loop
# Usage: rdequeuer -g
# Start an infinite loop repeatly dequeuing generic jobs to available machines
# Note: I explicitly require a parameter to avoid accidental execution of
# this function... for example, when someone was trying to use rdequeue
# or renqueue.
rdequeuer() {
    : ${DEV_ROOT:?} ${SRC:?}
    case "$1" in
	-c)
	    if [ $# -gt 1 ]; then
		shift
		# if mydevenv doesn't exist on the remote machine then
		# there probably aren't any queued jobs
		rbt -p -t 120 "$@" "cd ../..&&. ./.rbt.envs&&cd tools/wls&&. ./infra/build/rbt.sh&&rdequeuer -c"
	    else
		echo "INFO: `date`: clearing out all queued jobs." 1>&2
		(
		    if [ -d $DEV_ROOT/tools/wls/infra/test/queued ]; then
			cd $DEV_ROOT/tools/wls/infra/test/queued
			for i in job.*; do
			    [ ! -f "$i" ] && continue
			    grep /remotetest $i > /dev/null
			    if [ $? -eq 0 ]; then
				# return remotetest jobs to the central queue
				rqstatus -j $i "INFO: job being returned to the central queue, probably because when the nightlies start they kill and requeue running jobs."
				rquemove -r $i
			    else
				rqstatus -j $i "FATAL: this job has been killed (if running)"
			    fi
			    rqstatus -j $i -u
			    rm -f $i
			done
		    fi
		)

	    fi
	    return
	    ;;
	-s)
	    rdequeuer -c
	    echo "INFO: `date`: starting rdequeuer..." 1>&2
	    ;;
	-o) touch /tmp/stop_rdequeuer ;;
	-r) ;;
	-g)
	    shift
	    rdequeuer_generic "$@"
	    return
	    ;;
	*)
	    echo "Usage: rdequeuer [-c|-s|-r]" 1>&2
	    return 1
	    ;;
    esac

    if [ -f /mount/centralrq_data/$SRC/rdequeuer.lock -o "`uname_n`" = "centralrq" ]; then
	echo "FATAL: this appears to be the central dequeuer, use -g not -s or -r!" 1>&2
	exit 1
    fi

    # for the RQ to come up quicker in Docker/K8S/Jenkins agents
    if ! using_jenkins; then
	# Note that you *have* to do a killbt to restart this, if you restart
	# rdequeuer without a killbt then an already running test run will
	# probably killbt rdequeuer OR the first thing rdequeuer runs will
	# killbt the already running tests anyway.  I can't think of anyway to
	# cleanly restart rdequeuer *and* leave and existing testrun going
	$DEV_ROOT/tools/wls/infra/build/killbt.sh

	# see if this fixes the p4 hangs I have been seeing on lcr0107[89] May, 2012 in the Coherence RQ
	# there are other comments in infra scripts indicating that PWD can cause issues with p4
	unset PWD
	# give it a while for the killbt to let processes killed to settle down
	sleep 30
    fi

    if using_p4; then
    	export P4CLIENT=`echo $P4CLIENT | tr A-Z a-z`  #make p4client name lowercase in case of wlsbt.SLC02UWE
		# make sure clobber and rmdir are turned on
		p4 client -o | sed -e '/^Options:/s/ noclobber/ clobber/' -e '/^Options:/s/ normdir/ rmdir/' -e '/^Options:/s/ compress/ nocompress/' | p4 client -i
    fi

    case "`uname`" in
	Windows_NT|CYGWIN_NT-*)
	    mkdir -p c:/tmp
	    tmpfile=c:/tmp/regedit.input_$$
	    rm -f $tmpfile
	    # note that if the system is *not* currently logged into bt or
	    # the screen save is on, just setting the registry entry isn't
	    # going to fix it without manual intervention
	    cat <<EOF > $tmpfile
REGEDIT4

! disable the crash and reboot questions so as not to hang the auto-login
[HKEY_LOCAL_MACHINE\Software\Microsoft\Windows\CurrentVersion\Reliability]
"ShutdownReasonUI"=dword:00000000
! disable the screen saver
[HKEY_CURRENT_USER\Control Panel\Desktop]
"ScreenSaveActive"="0"
"SCRNSAVE.EXE"=-
! Allow VNC 3.x & TightVNC local loopback for ide DRT movies
![HKEY_CURRENT_USER\Console]
! Quick Edit mode in console windows (comment out as it is enabled by default since post-WinXP)
!"QuickEdit"=dword:00000001
![HKEY_LOCAL_MACHINE\SOFTWARE\ORL\WinVNC3]
!"AllowLoopback"=dword:00000001
! Set VNC 3.x & TightVNC password to the standard bt password
![HKEY_LOCAL_MACHINE\SOFTWARE\ORL\WinVNC3\Default]
!"Password"=hex:61,fe,b1,1f,64,94,a1,d1
![HKEY_LOCAL_MACHINE\SOFTWARE\RealVNC\WinVNC4]
! Allow VNC 4.* & RealVNC local loopback for ide DRT movies
!"AllowLoopback"=dword:00000001
! Set VNC 4.* & RealVNC password to the standard bt password
!"Password"=hex:61,fe,b1,1f,64,94,a1,d1
EOF
	    regedit /s $tmpfile
	    rm -f $tmpfile
	    if [ -d c:/cygdrive ]; then
		echo "WARNING: A c:/cygdrive directory exists!!"
		${MAILPROG}"FYI: A c:/cygdrive directory exists on $HOST in $SRC!" infra-rq_cn_grp@oracle.com < /dev/null
	    else
		touch c:/cygdrive
		chmod a= c:/cygdrive
	    fi
	    ;;
    esac

    cd $DEV_ROOT/tools/wls
    sleep_secs=0
    lock_file=/tmp/rdequeuer-$NEW_SRC.lock
    run_file=/tmp/rdequeuer-$NEW_SRC.run
    rm -f $run_file

    # just in case killbt didn't kill it above
    if [ -s "$lock_file" ]; then
	read REPLY < $lock_file
	kill -9 "$REPLY"
    fi

    # give it a few seconds for processes to settle down before checking them
    sleep 10

    status="`rqstatus`"
    if [ "$status" != "notrunning" ]; then
	echo "FATAL: `date`: pid in lock file ($lock_file) still running"
	echo "FATAL: rqstatus=$status"
	return 1
    fi
    unset status

    # cleanup various "tmp" dirs
    # this is basically a copy of some testrel_all.sh code
    uname -s | egrep -e Windows_NT -e CYGWIN > /dev/null
    if [ $? -ne 0 ]; then
	# remove all files owned by me older than 1 day
	find /tmp/. /var/tmp/. -type f -user ${LOGNAME:-wls} -mtime +1 | xargs rm -f
	find /tmp/. /var/tmp/. -type d -user ${LOGNAME:-wls} | xargs rmdir -p 2>/dev/null
    else
	# remove all files older than 1 day
	find c:/tmp/. c:/temp/. /tmp/. /temp/. "${TMP:-/tmp}" "${TEMP:-/tmp}" "${TMPDIR:-/tmp}" -type f -mtime +1 | xargs rm -f
        # remove all empty directories
	find c:/tmp/. c:/temp/. /tmp/. /temp/. "${TMP:-/tmp}"/. "${TEMP:-/tmp}"/. "${TMPDIR:-/tmp}"/. -type d | xargs rmdir -p 2>/dev/null
	# make sure /tmp still exists, rmdir -p might remove it if it is empty
	mkdir -p c:/tmp c:/temp /tmp /temp "${TMP:-/tmp}" "${TEMP:-/tmp}" "${TMPDIR:-/tmp}"
    fi

    echo "INFO: `date`: starting rdequeuer on $DEV_ROOT/tools/wls/infra/test/queued/..." 1>&2

    while true; do
	echo $$ > "$lock_file"
        # make sure p4 is working ok, wait for it to return if it isn't
	check_p4server_maybe_wait
	rdequeue
	if [ $? -eq 0 -a "$QUEUED_PARAMS" != "" -a "$QUEUED_ID" != "" ]; then
	    echo "INFO: `date`: about to run a dequeued job ($QUEUED_PARAMS|$QUEUED_ID)" 1>&2
	    echo "INFO: `date`: about to run a dequeued job" 1>&2
	    # check and clean(?) disk space
	    if [ "`rbt_check_disk`" = "low" ]; then
		echo "INFO: `date`: disk space is low, cleaning up..." 1>&2
		rbt_clean_disk
		if [ "`rbt_check_disk`" = "low" ]; then
		    msg="WARNING: disk space is low on $HOST in $SRC even after rbt_clean_disk"
		    (
			echo "To: infra-rq_cn_grp@oracle.com"
			echo ""
			echo "$msg"
			echo ""
			df
			echo "This job being run with low disk space..."
			echo "http://home.us.oracle.com/internal/$SRC/job.jsp?id=$QUEUED_ID"
		    ) | ${MAILPROG}"$msg" infra-rq_cn_grp@oracle.com
		fi
	    fi
	    # signal that this machine is now busy
	    rqstatus busy
	    rqstatus -j $QUEUED_ID starting
	    if using_jenkins; then
		# this is mainly used by rkill to find and kill this job if need be
		rqstatus -j $QUEUED_ID "jenkins: $BUILD_URL"
	    fi
	    if using_p4; then
		# this is needed to pick up new .cfg/.txt's if the rdequeuer
		# hasn't been restarted in > 24 hours
		# just syncto head 2016-7-20 ygeng
		#p4 labels | grep "^Label last_clean_test_platform_${SRC} "
		#if [ $? -eq 0 ]; then
		#    syncto="@last_clean_test_platform_${SRC}"
		#else
		syncto="#head"
		#fi
		p4 sync //dev/$SRC/tools/wls/infra/...$syncto
		# FIXME: herrlich@bea.com Jun 23, 2004
		# hack to speed development of remotetest
		echo "$QUEUED_PARAMS" | grep /remotetest > /dev/null
		if [ $? -eq 0 ]; then
		    p4 revert //dev/$SRC/tools/wls/infra/test/remotetest%%1 //dev/$SRC/tools/wls/infra/infraenv.sh
		    p4 sync //dev/$SRC/tools/wls/infra/test/remotetest%%1 //dev/$SRC/tools/wls/infra/infraenv.sh
		fi
		# p4 sync missing files, this needs to be done because jobs
		# can sync running files out of existance.
		p4 diff -sd //dev/$SRC/tools/wls/infra/... |\
		    sed 's/$/#have/' | p4 -x- sync -f
		echo "`date`: $QUEUED_ID - $QUEUED_PARAMS"
	    fi
	    # make sure we are cd'ed to the correct directory
	    # (rbt_clean_disk can mess it up)
	    cd $DEV_ROOT/tools/wls
	    # make the QUEUED_ID available to the job
	    RBT_QUEUED_ID=$QUEUED_ID
	    export RBT_QUEUED_ID
	    sh -c "$QUEUED_PARAMS" < /dev/null &
	    echo $QUEUED_ID $! > $run_file
	    wait $!
	    saved_status=$?
	    unset RBT_QUEUED_ID
	    (
		cd $DEV_ROOT/
		unset DEV_ROOT
		if [ "${BASH_VERSINFO[0]}" -eq 2 ]; then
		    BASH_SOURCE[0] = "./bin/cfglocal.sh"
		fi
		. ./.rbt.envs
		if [ "${BASH_VERSINFO[0]}" -eq 2 ]; then
		    unset BASH_SOURCE[0]
		fi
	    )
	    if [ $saved_status -eq 100 ]; then
		echo "`date`: Exit code 100 is detected, old rq daemon is failed to restart after BadMachine!" 1>&2
	    elif [ $saved_status -eq 0 ]; then
		echo "`date`: $QUEUED_ID - completed successfully" 1>&2
		rqstatus -j $QUEUED_ID done
	    else
		echo "`date`: $QUEUED_ID - exit with non-zero status ($saved_status)" 1>&2
		rqstatus -j $QUEUED_ID failed
	    fi
	    if using_p4; then
		# increment housekeeping counter
		host="`uname_n`"
		count=`p4 counter rq_count_$host`
		p4 counter rq_count_$host `expr ${count:-0} + 1`
		unset host count
	    fi
	    rdequeue -d $QUEUED_ID
	    rm -f $run_file
	    sleep_secs=0
	else
            # we tried to dequeue and found nothing
            # maybe this should be an rbt.sh function?
            # if we have a stop file then we're in jenkins or some other
            # environment that produces the stop file, remove it and exit
            echo "INFO: `date`: in dequeue loop, but nothing in queued"
            if [ -f /tmp/stop_rdequeuer ] ; then
	       # we still need to maybe do housekeeping on the $JENKINS_RQJOB jenkins agents
	       rbt_maybe_do_housekeeping
               echo "INFO: `date`: found /tmp/stop_rdequeuer, exiting"
	       break 10 # <-- over kill but we have had trouble with it stopping, the rm is down below after the rqstatus busy
            fi
            if using_p4; then
		# after 60 seconds also check for jobs in the other branch
		# /queued/ directories, if there are jobs then switch the queue
		# to the other branch
		if [ $sleep_secs -eq 0 -o $sleep_secs -gt 60 ]; then
		    moved_path=""
		    wls_dir=""
		    echo "$DEV_ROOT" | grep -e /coherence-ce/ -e /release/ >/dev/null
		    if [ $? -eq 0 ] ; then
			moved_path="../.."
			diff_branch_dir="/.."
			wls_dir=$DEV_ROOT/../../../../weblogic/dev
		    else
			moved_path=".."
			diff_branch_dir="/coherence-ce"
			wls_dir=$DEV_ROOT/../../../weblogic/dev
		    fi
		    rbt_maybe_switch_branches
		    rbt_maybe_switch_depots
		fi
	    fi

	    # only refresh my available status every 10 minutes, needed for
	    # systems not local to SF
	    if [ $sleep_secs -eq 0 -o $sleep_secs -gt 600 ]; then
		rqstatus available
		# if we have been idle for 10 minutes then do a check to see
		# if it is time to do housekeeping.  If we've been idle then
	        # the queue probably can handle this machine being out of the
	        # queue for the several minutes housekeeping will take.
		if [ $sleep_secs -ne 0 ]; then
		    rbt_maybe_do_housekeeping
		fi
		sleep_secs=0
	    fi
	    sleep 10
	    sleep_secs=`expr 10 + $sleep_secs`
	fi
    done
    rqstatus busy
    rm -f "$lock_file" /tmp/stop_rdequeuer
}

#
# This function is not meant to be used by end users, just internally
# by rqstatus.  Used by rqstatus in a few different places to
# efficiently remove available files.  This used to be done with...
# rm -f $RQSITE/*/available/$myhost but that was slow on some Windows
# boxes that where remote from RQSITE.  The following implementation
# proved much quicker.
rqstatus_rmavailable() {
    : ${RQSITE:?} ${1:?myhost}

    \ls $RQSITE/ | while read i; do
	rm -rf $i/available/$1
    done
}

#
# Used to report and query the availability of machines
# (used in the rdequeuer loop)
#
# return the status of this machine
# Status	Description
# ======	===========
# notrunning	no local dequeuer running
# available	running, idle and not paused
# busy		running a test that isn't a remote test
# busy remote	running a remote test
# busy testrel	running a testrel.sh
# paused	available but paused
# Usage: rqstatus
#
# Mark yourself (this machine) as available
# Usage: rqstatus available
#
# Mark yourself (this machine) as busy
# Usage: rqstatus busy
#
# Mark all machines as busy
# Usage: rqstatus -a busy
#
# Pause the generic queue
# Usage: rqstatus -g pause
#
# Resume the generic queue
# Usage: rqstatus -g resume
#
# Restart the generic queue
# Usage: rqstatus -g restart
#
# Update add an INFO: message for this machine
# Usage: rqstatus -i status message
#
# List all available machines
# Usage: rqstatus -l
#
# List details of a particular available machine (listed from -l)
# Usage: rqstatus -l hostname
#
# Update the status of a particular job
# Usage: rqstatus -j jobid status
#
# Update the owner of this jobid
# Usage: rqstatus -j jobid -u username
#
# Clear the owner of this jobid
# This is done automatically by...
#    rqstatus -j jobid done
#    rkill jobid
# Usage: rqstatus -j jobid -u
#
rqstatus() {
    : ${DEV_ROOT:?} ${SRC:?}
    if [ -z "$RQSITE" ]; then
	. $DEV_ROOT/tools/wls/infra/infraenv.sh
    fi
    # if RQSITE is temporarily unavailable sometimes waiting a bit helps
    if [ ! -d "$RQSITE/." ]; then
        echo "RQSITE=$RQSITE is not available, sleeping 1 minute..."
	sleep 60
	if [ ! -d "$RQSITE/." ]; then
	    echo "RQSITE=$RQSITE is still not available, sleeping another minute..."
	    sleep 60
	    if [ ! -d "$RQSITE/." ]; then
	        echo "RQSITE=$RQSITE is still not available, continuing anyway..."
	    fi
	fi
    fi
    [ -d "$RQSITE/common/available" ] || mkdir -p "$RQSITE/common/available"
    [ -d "$RQSITE/$SRC/available" ]   || mkdir -p "$RQSITE/$SRC/available"
    [ -d "$RQSITE/$SRC/status" ]      || mkdir -p "$RQSITE/$SRC/status"
    myhost="`uname_n`"
    export myhost
    machstat="$RQSITE/$SRC/status/machine.`date -u +%Y%m%d`.$myhost"
    export machstat
    if [ ! -f "$machstat" ]; then
	touch "$machstat"
	chmod a+rw "$machstat"
    fi
    if [ $# -eq 1 ]; then
	case "$1" in
	    available)
		: ${SITE:?}
		if using_p4; then
		    p4 client -o > t.dat_$$
		    grep '//dev/'$SRC'/\.\.\.' >/dev/null < t.dat_$$
		    platform_ok=$?
		    rm t.dat_$$
		# else
		#    platform_ok=1
		# fi
		fi
                tmpfile=`mktemp /tmp/available.XXXXXX`
		(
		    # don't need HOST because that is the filename
		    echo OS=$OSABR
		    echo SITE=$SITE
		    cpu=`get_cpu`
		    if [ -n "$cpu" ]; then
			echo CPU=$cpu
		    fi
		    unset cpu
		    mem=`get_memory`
		    if [ -n "$mem" ]; then
			echo MEM=$mem
		    fi
		    unset mem
		    disk=`get_disk`
		    if [ -n "$disk" ]; then
			echo DISK=$disk
		    fi
		    unset disk
		    if [ $platform_ok -eq 0 ]; then
			echo PLATFORM=ok
		    fi
#		    if rbt_ipv6; then
#			echo IPV6=true
#		    else
#			echo IPV6=false
#		    fi
		    if rbt_excluded; then
			echo RBT_EXCLUDED=true
		    fi
		    if rbt_nightly_excluded; then
			echo RBT_NIGHTLY_EXCLUDED=true
		    fi
		    if rbt_rq_excluded; then
			echo RBT_RQ_EXCLUDED=true
		    fi
		    if rbt_winrunner; then
			echo WINRUNNER=true
		    fi
		    if rbt_ade; then
			echo ADE=true
                    fi
		    # in SRC= for each branch sync'ed on this box
		    # for multi-branch machines
		    p4 have //dev/%%1/tools/wls/infra/build/rbt.sh |\
			sed -n 's|//dev/\([^/]*\)/.*|\1|p' |\
			while read i; do
			    echo SRC=coherence/$i
			done
		    p4 have //dev/coherence-ce/%%1/tools/wls/infra/build/rbt.sh |\
			sed -n 's|//dev/coherence-ce/\([^/]*\)/.*|\1|p' |\
			while read i; do
			     echo SRC=coherence/coherence-ce/$i
			done
		    p4 have //dev/release/%%1/tools/wls/infra/build/rbt.sh |\
			sed -n 's|//dev/release/\([^/]*\)/.*|\1|p' |\
			while read i; do
			     echo SRC=coherence/release/$i
			done
		    p4 have //dev/release/%%1/tools/common/wls/infra/build/rbt.sh |\
			sed -n 's|//dev/release/\([^/]*\)/.*|\1|p' |\
			while read i; do
			     echo SRC=coherence/release/$i
			done
			# check whether it has WLS RQ codes
			wls_dir=""
			echo "$DEV_ROOT" | grep -e /coherence-ce/ -e /release/ >/dev/null
			if [ $? -eq 0 ] ; then
			    wls_dir="$DEV_ROOT/../../../../weblogic/dev"
			else
			    wls_dir="$DEV_ROOT/../../../weblogic/dev"
			fi
			if [ -d "$wls_dir" ]; then
			myhostname="`uname -n | tr A-Z a-z`"
			myid="`logname`"
			p4 -u bt -p p4jrep:7999 -c $myid.$myhostname have //depot/dev/%%1/wls/infra/build/rbt.sh |\
			sed -n 's|//depot/dev/\([^/]*\)/.*|\1|p' |\
			while read i; do
			    echo SRC=$i
			done
			unset myhostname myid
			fi
		) > $tmpfile
		# do this in two steps (write out data, move file) so that we
		# never get partial files on RQSITE
		# if we have more than one branch sync'ed then make ourselves
		# available to the common remote queue
		rqstatus_rmavailable $myhost
		if [ `grep ^SRC= < $tmpfile | sort -u | wc -l` -gt 1 ]; then
		  cat $tmpfile | grep '^SRC=' | grep -v 'SRC=coherence' > /dev/null
		  if [ $? -eq 0 ]; then
		    mv -f $tmpfile $RQSITE/../common/available/$myhost
		    chmod ugo=rw $RQSITE/../common/available/$myhost
		  else
		    mv -f $tmpfile $RQSITE/common/available/$myhost
		    chmod ugo=rw $RQSITE/common/available/$myhost
		  fi
		else
		    mv -f $tmpfile $RQSITE/$SRC/available/$myhost
		    chmod ugo=rw $RQSITE/$SRC/available/$myhost
		fi
		unset platform_ok
		echo "# `date_u`: available" >> "$machstat"
		;;
	    busy)
		rqstatus_rmavailable $myhost
		# try really again if it doesn't work
		if [ $? -ne 0 ]; then
		    sleep 1
		    rqstatus_rmavailable $myhost
		fi
		echo "# `date_u`: busy" >> "$machstat"
		;;
	    -l)
		(cd $RQSITE/$SRC/available ; \ls -1)
		;;
	    -a)
	        status=`rqstatus`
		echo "$SRC: $status"
		if [ "$status" = "notrunning" ]; then
			# Checking //dev/%%1 coherence branches, //dev/release/%%1 coherence branches and  WLS branches in p4jrep:7999 depot
			# Some paths is different when in base branch and release coherence branches
			echo "$DEV_ROOT" | grep -e /coherence-ce/ -e /release/ >/dev/null
			if [ $? -eq 0 ] ; then
			   moved_path="../.."
			   release_branch_dir=".."
			   wls_dir=$DEV_ROOT/../../../../weblogic/dev
			else
			   moved_path=".."
			   release_branch_dir="../release"
			   wls_dir=$DEV_ROOT/../../../weblogic/dev
			fi
			# checking WLS branches in p4jrep:7999 depot
			p4 -p p4jrep:7999 have //depot/dev/%%1/wls/infra/build/rbt.sh |\
			sed -n 's|//depot/dev/\([^/]*\)/.*|\1|p' |\
			while read i; do
			if [ "$i" != "$SRC" ]; then
			   (
			    cd $wls_dir/$i/wls
			    if [ -f mydevenv.sh ]; then
			        . ./mydevenv.sh
			        . infra/build/rbt.sh
			        echo "$SRC: `rqstatus`"
			    fi
			   )
			fi
			done
			# check //dev/%%1 coherence branches
			p4 have //dev/%%1/tools/wls/infra/build/rbt.sh |\
			sed -n 's|//dev/\([^/]*\)/.*|\1|p' |\
			while read i; do
			if [ "$i" != "$SRC" ]; then
			   (
			    cd $DEV_ROOT/$moved_path/$i
			    if [ -f ./.rbt.envs ]; then
			       . ./.rbt.envs
			       . tools/wls/infra/build/rbt.sh
			       echo "$SRC: `rqstatus`"
			    fi
			   )
			fi
			done
			# check //dev/release/%%1 coherence branches
			p4 have //dev/release/%%1/tools/wls/infra/build/rbt.sh //dev/release/%%1/tools/common/wls/infra/build/rbt.sh |\
			sed -n 's|//dev/release/\([^/]*\)/.*|\1|p' |\
			while read i; do
			if [ "$i" != "$SRC" ]; then
			   (
			    cd $DEV_ROOT/$release_branch_dir/$i
			    if [ -f ./.rbt.envs ]; then
			        . ./.rbt.envs
			        if [ -f tools/wls/infra/build/rbt.sh ]; then
			            . tools/wls/infra/build/rbt.sh
			        else
			            . tools/common/wls/infra/build/rbt.sh
			        fi
			        echo "$SRC: `rqstatus`"
			    fi
			   )
			fi
			done
		fi
		;;
	    *)
		echo "ERROR: unknown argument ($1)" 1>&2
		return 1
		;;
	esac
    elif [ $# -eq 2 -a "$1" = "-l" ]; then
	if [ -f "$RQSITE/$SRC/available/$2" ]; then
	    echo HOST=$2
	    cat "$RQSITE/$SRC/available/$2"
	fi
    elif [ $# -eq 2 -a "$1" = "-g" -a "$2" = "pause" ]; then
	mkdir -p "$RQSITE/$SRC/queued"
	(
	    uname -a
	    date
	) > "$RQSITE/$SRC/queued/PAUSE"
    elif [ $# -eq 2 -a "$1" = "-g" -a "$2" = "restart" ]; then
	mkdir -p "$RQSITE/$SRC/queued"
	(
	    uname -a
	    date
	) > "$RQSITE/$SRC/queued/RESTART"
    elif [ $# -eq 2 -a "$1" = "-g" -a "$2" = "resume" ]; then
	rm -f "$RQSITE/$SRC/queued/PAUSE"
    elif [ $# -eq 2 -a "$1" = "-a" -a "$2" = "busy" ]; then
	rm -rf "$RQSITE/$SRC/available"
	mkdir  "$RQSITE/$SRC/available"
    elif [ $# -eq 2 -a "$1" = "-j" ]; then
	if [ -f "$DEV_ROOT/tools/wls/infra/test/queued/$2" -o -f "$RQSITE/$SRC/queued/$2" ]; then
	    echo queued
	    return
	elif [ -f "$RQSITE/$SRC/status/$2" ]; then
	    status="$RQSITE/$SRC/status/$2"
            tmpfile=`mktemp /tmp/jobstatus.XXXXXX`
	    awk  '
BEGIN                      {pass_count=0; start_count=0; fatal_count=0; fatal_submit_count=0; done_count=0; failed_count=0}
/INFO: p4 resolving files/ {pass_count=0; start_count=0; fatal_count=0; fatal_submit_count=0; done_count=0; failed_count=0}
/INFO: test .* passed/     {pass_count++}
/INFO: test .* starting/   {start_count++}
/FATAL: /                  {fatal_count++}
/FATAL:  auto-submit failed/ {fatal_submit_count++}
/: done$/                  {done_count++}
/: failed$/                {failed_count++}
END                        {print pass_count, start_count, fatal_count, fatal_submit_count, done_count, failed_count}
	     ' $status > $tmpfile
	    read pass_count start_count fatal_count fatal_submit_count done_flag failed_flag < $tmpfile
	    rm -f $tmpfile
	    if [ "$fatal_count" -gt 0 -a "$fatal_count" -gt "$fatal_submit_count" ]; then
		echo failed
		return
	    else
		# rjobsync is must only be used at the end of a job so also use that to check for "done"
		# do not look for sync.* in queued because I only want the last sync'er to call and complete rjobsync
		if [ "$done_flag" -gt 0 -o "$failed_flag" -gt 0 -o -f "$RQSITE/$SRC/status/sync.$2" ]; then
		    if [ $pass_count -eq $start_count -a $start_count -gt 0 ]; then
			echo success
			return
		    else
			echo failed
			return
		    fi
		else
		    echo running
		    return
		fi
	    fi
	else
	    echo unknown
	    return
	fi
    elif [ $# -eq 3 -a "$1" = "-j" ]; then
        if [ "$3" = "-u" ]; then
	    rm -f "$RQSITE/$SRC/status/$2.owner"
	else
	    if [ ! -f "$RQSITE/$SRC/status/$2" -a -f "$DEV_ROOT/tools/wls/infra/test/queued/$2" ]; then
		# this really should be done as a function of rdequeue
		# (ex. -l or something)
		# use -i so I don't overwrite an existing file which might happen if there are intermittent nfs issues with RQSITE
		# use echo n | cp -i instead of cp -n because MKS cp does not have cp -n
		echo n | cp -i "$DEV_ROOT/tools/wls/infra/test/queued/$2" "$RQSITE/$SRC/status/$2"
	    fi
	    if [ ! -f "$RQSITE/$SRC/status/$2" ]; then
		# this really should be done as a function of rdequeue
		# (ex. -l or something)
		# use -i so I don't overwrite an existing file which might happen if there are intermittent nfs issues with RQSITE
		# use echo n | cp -i instead of cp -n because MKS cp does not have cp -n
		echo n | cp -i "$RQSITE/$SRC/queued/$2" "$RQSITE/$SRC/status/$2"
	    fi
	    # make sure this job id is not queued (sometimes rquemove fails or things get duplicated)
	    rm -f "$RQSITE/$SRC/queued/$2"
	    # FIXME: alan.herrlich@oracle.com Jun 21, 2014
	    # this should still work even if the job has been archived (/archive.status/)
	    # long running jobs that still appear to be running aren't properly killed when they have been archived
	    echo "# `date_u`: $myhost: $3" >> "$RQSITE/$SRC/status/$2"
	    sleep 5
	    echo "# `date_u`: $2: $3" >> "$machstat"
	    if [ "$3" = "done" ]; then
		rqstatus -j $2 -u
	    fi
	fi
    elif [ $# -eq 4 -a "$1" = "-j" -a "$3" = "-u" ]; then
        [ "$LOGNAME" != "p4as" ] && echo "$4" > "$RQSITE/$SRC/status/$2.owner" || true
    elif [ $# -ge 2 -a "$1" = "-i" ]; then
	shift
	echo "# `date_u`: INFO: $@" >> "$machstat"
    elif [ $# -ge 0 ]; then
	lock_file=/tmp/rdequeuer-$NEW_SRC.lock
	run_file=/tmp/rdequeuer-$NEW_SRC.run
	tra_run_file=/tmp/testrel_all-$NEW_SRC.run
	pause_file=$RQSITE/$SRC/queued/PAUSE
	if [ ! -s $lock_file ]; then
	    if [ ! -s $tra_run_file ]; then
		echo notrunning
	    else
		read cfg pid < $tra_run_file
		kill_0 ${pid:?}
		if [ $? -ne 0 ]; then
		    echo notrunning
		else
		    echo busy testrel
		fi
	    fi
	else
	    read pid < $lock_file
	    kill_0 ${pid:?}
	    if [ $? -ne 0 ]; then
		if [ ! -s $tra_run_file ]; then
		    echo notrunning
		else
		    read cfg pid < $tra_run_file
		    kill_0 ${pid:?}
		    if [ $? -ne 0 ]; then
			echo notrunning
		    else
			echo busy testrel
		    fi
		fi
	    else
		if [ ! -f $run_file ]; then
		    if [ -f $pause_file ]; then
			echo paused
		    else
			echo available
		    fi
		else
		    read jobid pid < $run_file
		    grep /remotetest $DEV_ROOT/tools/wls/infra/test/queued/$jobid >/dev/null
		    if [ $? -eq 0 ]; then
			echo busy remote
		    else
			grep /testrel $DEV_ROOT/tools/wls/infra/test/queued/$jobid >/dev/null
			if [ $? -eq 0 ]; then
			    echo busy testrel
			else
			    echo busy
			fi
		    fi
		fi
	    fi
	fi
    else
	echo "ERROR: unknown arguments ($@)" 1>&2
	return 1
    fi
}

# syncronize with a group of other jobs
# This uses the central generic dequeuer as a simple lock manager
# We want only one job to find that it is the last successful job of the group.
# This is used by the remotetests to send email.
rjobsync() {
    : ${SRC:?} ${1:?this job id} ${2:?job ids to sync with}
    if [ -z "$RQSITE" ]; then
	. $DEV_ROOT/tools/wls/infra/infraenv.sh
    fi
    # DEBUG: remove me
    #RQSITE=$HOME/rqtst
    logfile=$RQSITE/$SRC/status/sync.$1
    quefile=$RQSITE/$SRC/queued/sync.$1
    shift
    # only do rjobsync once per job run, if called a second time just return the previous result
    # on job retry the sync.JOBID* files will be deleted to start again
    if [ -s "$logfile" ]; then
	cat "$logfile"
        rm -f "$quefile"
	return
    fi
    rm -f "$quefile" "$logfile"
    for i in "$@"; do
	echo "$i"
    done > "$quefile"
    count=0
    sleep_seconds=1
    # this should be a bit more than a RQ generic queue restart, as of Nov 21, 2014 it took about 35 minutes.  2700s is 45m.
    max_seconds=2700
    while [ "$count" -lt "$max_seconds" -a ! -r "$logfile" ]; do
        count=`expr $count + $sleep_seconds`
        sleep $sleep_seconds
    done
    if [ -r $logfile ]; then
        cat $logfile
    else
        echo "timeout waiting for rjobsync to happen, waited $count seconds"
	return 1
    fi
    # note that the quefile probably does *not* need to be deleted since the central RQ should do this if everything
    # went normally (no timeout).  Note that we should *not* remove logfile, the existance of logfile is used by
    # rqstatus -j to figure out if this RQ job has completed or not... *if* we delete logfile here then between that
    # delete and the remotetest.sh code after rjobsync writes "done" or "failed" to the job status file another
    # rjobsync command could be processed and falsely think that this RQ job has not completed yet.
    rm -f "$quefile"
}

#
# check to see if the disk space is greater than a fixed number of kilobytes
# free and the disk is less than a certain percentage free.  Return the
# string "ok" if the disk is ok and the string "low" if the disk space is low.
#
# check for at least 4,000,000 kb free and 97% free
# (remote.all took 3,887,080 kb in src on Apr 26, 2007)
# (remoteDRT_kitbuild took 10,526,796 kb in src on Apr 26, 2007)
#
# Usage: rbt_check_disk
#
# check for at least a 100 kb free
#
# Usage: rbt_check_disk 100
#
# check for 90% free
#
# Usage: rbt_check_disk "" 90
#
# check for 101 kb free and 51% free
#
# Usage: rbt_check_disk 101 51
#
rbt_check_disk() {
    : ${DEV_ROOT:?} ${OS:?}
    case $OS in
	Windows_NT|Linux) usage="`df -P $DEV_ROOT/. | tail -1`";;
	SunOS)		  usage="`df -k $DEV_ROOT/. | tail -1`";;
	HP-UX)		  usage="`bdf $DEV_ROOT/.   | tail -1`";;
	*)		  usage="`df -k $DEV_ROOT/. | tail -1`";;
    esac
    kb="`echo $usage | awk '{print $(NF-2)}'`"
    perc="`echo $usage | awk '{print $(NF-1)}' | sed 's/%//g'`"
#    echo "INFO: available disk space in kb is $kb, percentage free is $perc" 1>&2
    # 10485760 = 10G / 1024 (1024 is because df reports in 1024 blocks)
    # expr `echo 10M | numfmt --from=iec` / 1024
    if [ "$kb" -lt "${1:-10485760}" -o "$perc" -ge ${2:-97} ]; then
	echo low
    else
	echo ok
    fi
    unset usage kb perc
}

rbt_clean_shortnames() {
    # rbt_clean_shortnames dir
    # cd to the given dir and then rename all directories to a nice
    # shortname (1, 2, 3, etc) then recurse into each of those directories.
    # Used to delete directories where the names are too long.
    (
	cd "${1:?}"
	count=1
	for i in */; do
	    while [ -d "$i" ]; do
		while [ -d "$count" -a "$count" -le 200 ]; do
		    count=`expr $count + 1`
		done
		mv -f -- "`basename $i`" "$count"
		if [ "$count" -gt 200 ]; then
		    count=1
		    break
		fi
	    done
	done
	for i in */; do
	    [ -d "$i" ] && rbt_clean_shortnames "$i"
	done
    )
}

rbt_clean_recursive() {
    (
        old_pwd=`pwd`
        cd "${1:?}"
        cmd /c dir /a:-d/b | ${JAVA_HOME}/bin/java -cp $BT xargs_rm $ENCODING 2>/dev/null
        for k in *; do
            if [ -d "$k" ]; then
                cd $k
                cmd /c dir /a:-d/b | ${JAVA_HOME}/bin/java -cp $BT xargs_rm $ENCODING 2>/dev/null
                cd ..
                cmd /c rd /s /q "$k" 2>/dev/null
            fi
            [ -d "$k" ] && rbt_clean_shortnames "$k"
            [ -d "$k" ] && rbt_clean_recursive "$k"
            cmd /c rd /s /q "$k" 2>/dev/null
        done
        cd $old_pwd
        unset old_pwd
    )
}

rbt_clean() {
    : ${DEV_ROOT:?}

    cleandirs="
        /scratch/$LOGNAME/view_storage/${LOGNAME}_ctstest.SAVED.*
        /scratch/$LOGNAME/view_storage/${LOGNAME}_ctstest#zombie*
        /scratch/$LOGNAME/cts_work
        /tmp/wls_tmp-*
        /tmp/wlstTemp*
        /tmp/RarArchive_extract_temp*
        /tmp/cts_work
        /tmp/cts_dep
        $DEV_ROOT/build
        $HOME/.m2/repository/com/oracle/coherence
        $HOME/.gradle
"

    count=0
    reserved_pwd=`pwd`
    if [ "`cmd.exe /c date /t 2>/dev/null`" != "" ]; then
	for j in $cleandirs; do
	  # only do this on directories that exist.  Otherwise the cmd
	  # commands below end up being run on files like delete_* or src*
	  # which end up recursively deleting all files matching the *
	  [ -d "$j" ] && (
	      topdir="`dirname "$j"`"
	      subdir="`basename "$j"`"
	      if [ -d "$topdir" ]; then
		  cd "$topdir"
		  echo y | cmd /c cacls "$subdir" /t /p everyone:f >/dev/null
                  cmd /c dir /a:-d/s/b "$subdir" | ${JAVA_HOME}/bin/java -cp $BT xargs_rm $ENCODING 2>/dev/null
		  cmd /c rd /s /q "$subdir" 2>/dev/null
		  #[ -d "$subdir" ] && rbt_clean_shortnames "$subdir"
		  [ -d "$subdir" ] && rbt_clean_recursive "$subdir"
		  [ -d "$subdir" ] && cmd /c rd /s /q "$subdir" 2>/dev/null
		  [ -d "$subdir" ] && cmd /c rename "$subdir" "delete_`date -u +%Y%m%d%H%M%S`_$count"
                  count=`expr $count + 1`
	      fi
	  )
	done
    else
	rm -rf $cleandirs
	# make sure find always finds something
	mkdir -p /tmp/.appmergegen_delete.me
	# these tmp dirs seem to collect quickly
	find /tmp/. /var/tmp/. -type d -user ${LOGNAME:-bt} -name ".appmergegen_*" | xargs rm -rf
    fi
    cd $reserved_pwd
    unset reserved_pwd
}

rbt_p4clean() {
    #FIXME: Liu Bo, 2008/03/25
    #default encoding for xargs_rm.java is UTF8, it will cause
    #some multibyte filename such as Chinese can't be recognized,
    #so need to add the ablility to pass ENCODING to xargs_rm.java
    #tools/wls/infra/build/not_in_p4.sh | java -cp tools/wls/infra/build xargs_rm
    ENCODING=${1:-$ENCODING}
    rbt_clean
    cd $DEV_ROOT
    grep DEV_ROOT= .rbt.envs >/dev/null && unset DEV_ROOT
    if [ "${BASH_VERSINFO[0]}" -eq 2 ]; then
    	BASH_SOURCE[0] = "./bin/cfglocal.sh"
    fi
    . ./.rbt.envs
    if [ "${BASH_VERSINFO[0]}" -eq 2 ]; then
	unset BASH_SOURCE[0]
    fi
    cd tools/wls/infra/build
    ${JAVA_HOME}/bin/javac -source 17 -target 17 xargs_rm.java
    cd $DEV_ROOT
    tools/wls/infra/build/not_in_p4.sh | ${JAVA_HOME}/bin/java -cp tools/wls/infra/build xargs_rm $ENCODING
    xargs < /tmp/rmdirs.dat rmdir -p 2>&1 | grep -iv "not empty" | grep -iv "invalid argument"
    rm -f /tmp/rmdirs.dat
}

rbt_clean_disk() {
    : ${DEV_ROOT:?} ${OS:?}
    (
	# remove all eclipse-tools directories that are not under p4 control
	cd $DEV_ROOT/..
	tmpfile=`mktemp /tmp/p4dirs.XXXXXX`
	p4 dirs %%1/eclipse-tools/%%2 > $tmpfile
	for i in */eclipse-tools/*; do
	    if [ -d "$i" ]; then
		grep "/$i$" < $tmpfile >/dev/null
		if [ -s "$tmpfile" -a $? -ne 0 ]; then
		    rm -rf "$i"
		fi
	    fi
	done
	rm -f $tmpfile
    )

    p4 sync //...#have &
    rm -rf \
	/src*_all_files.zip \
	c:/j2eetck \
	c:/j2eetck_docs \
	c:/j2sdkee1.4 \
	c:/jee5sdk \
	c:/jee5tck \
	$HOME/.gradle/caches \
	/scratch/$LOGNAME/repository \
	$HOME/.m2/repository/com/oracle/coherence \
        "$DEV_ROOT/../.."/repository.cache \
        "$DEV_ROOT/../../.."/gradle_cache \
        "$DEV_ROOT/../.."/jrtmp* \
        "$DEV_ROOT/../.."/src*_* \
        "$DEV_ROOT/../.."/wls_*_* \
        "$DEV_ROOT/../.."/wlstmp* \
        "$DEV_ROOT/../.."/queuetmpclient \
        "$DEV_ROOT/../.."/infrabuild \
        "$DEV_ROOT/../.."/crmsimulationdeploy \
	"$DEV_ROOT/.."/src*/tools/common/wls/infra/test/qa* \
	"$DEV_ROOT/.."/src*/infra/test/qa* \
	"$DEV_ROOT/.."/src*/build \
	"$DEV_ROOT/.."/src*/external/j*/j*.zip* \
	"$DEV_ROOT/.."/src*/external/j*/j*.tar* \
	"$DEV_ROOT/.."/wls_*/tools/wls/infra/test/qa* \
	"$DEV_ROOT/.."/wls_*/infra/test/qa* \
	"$DEV_ROOT/.."/wls_*/build \
	"$DEV_ROOT/.."/wls_*/external/j*/j*.zip* \
	"$DEV_ROOT/.."/wls_*/external/j*/j*.tar* \
	"$DEV_ROOT/.."/*-undelete \
	"$DEV_ROOT/../.."/localinstall \
	"$DEV_ROOT/../.."/localunzip
    USERNAME=`id > id.dat ; cut < id.dat -d '(' -f 2 | cut -d ')' -f 1`
    rm -f id.dat
    USERNAME=`basename $USERNAME`
    uname -s | grep _NT >/dev/null
    if [ $? -ne 0 ]; then
	# remove all files owned by me older than 1 day
	find /tmp/. /var/tmp/. -type f -user $USERNAME -mtime +1 | xargs rm -f
	# remove all empty directories
	find /tmp/* /var/tmp/* -type d | xargs rmdir -p 2>/dev/null
    else
	SR="${SystemRoot:-${SYSTEMROOT:-${systemroot:-c:/WINDOWS}}}"
	uname -s | grep -i CYGWIN >/dev/null
	if [ $? -eq 0 ]; then
	    SR="`cygpath $SR`"
	fi
	rm -f "${SR:?}"/KB*.log
	# remove all files older than 1 day
	find /tmp/. /temp/. c:/tmp c:/temp "${TMP:-/tmp}" "${TEMP:-/tmp}" "${TMPDIR:-/tmp}" "${SR:?}"/TEMP -type f -mtime +1 | while read i; do rm -f "$i"; done
        # remove all empty directories
	find /tmp/* /temp/* c:/tmp/* c:/temp/* "${TMP:-/tmp}"/* "${TEMP:-/tmp}"/* "${TMPDIR:-/tmp}"/* "${SR:?}"/TEMP/* -type d | while read i; do rmdir -p "$i" 2>/dev/null; done
	# make sure /tmp still exists, rmdir -p might remove it if it is empty
	mkdir -p "${TMP:-/tmp}" "${TEMP:-/tmp}" "${TMPDIR:-/tmp}" "${SR:?}"/TEMP
    fi
    wait
    # this, in combo with sync #have improves p4 client performance
    p4 fstat //depot/fix_client_map
}

rbt_defrag_if() {
    which defrag.exe > /dev/null
    if [ $? -eq 0 ]; then
	drive=`echo ${DEV_ROOT:?} | awk -F: '{print $1 ":"}'`
	defrag -a ${drive:?} | grep -i "you should defragment this volume"
	if [ $? -eq 0 ]; then
	    defrag $drive
	fi
    fi
}

rbt_do_housekeeping() {
    echo "INFO: `date`: starting housekeeping..." 1>&2
    # set this before the diff/sync -f because if someone kills because it
    # was taking too long or something we don't want it to immediately try
    # to do it again.
    host="`uname_n`"
    p4 counter rq_count_$host 0
    # record the timestamp in seconds when housekeeping was done
    p4 counter rq_housekeeping_$host `date -u +%s`
    unset host
    rqstatus busy
    rbt_clean_disk
# FIXME: herrlich@bea.com Jan 28, 2007
# should defrag??
#    rbt_defrag_if
    p4 diff -se //dev/$SRC/... | p4 -x- sync -f
    echo "INFO: `date`: done housekeeping." 1>&2
}

rbt_maybe_do_housekeeping() {
    host="`uname_n`"
    # check the timestamp when the housekeeping was last done
    housekeeping=`p4 counter rq_housekeeping_$host`
    now=`date -u +%s`
    # if housekeeping counter isn't set then set to today in order to ignore it
    [ $housekeeping -eq 0 ] && housekeeping=$now
    hk_diff=`expr $now - $housekeeping`
    count=`p4 counter rq_count_$host`
    # 604800  = 60 * 60 * 24 *  7 (1 week)
    # 1209600 = 60 * 60 * 24 * 14 (2 weeks)
    if [ "${count:-0}" -gt 100 -o "$hk_diff" -ge 1209600 ]; then
	rbt_do_housekeeping
    fi
    unset host count
}

check_p4server_maybe_wait() {
    # make sure p4server is working ok and wait if it isn't
    while using_p4 && [ -z "`p4 counter change`" ]; do
	echo "INFO: `date`: 'p4 counter change' did not return anything (p4 down?)!" 1>&2
	echo "INFO: `date`: sleeping for a minute" 1>&2
        sleep 60
	# I have seen cases where devenv fails because of a home http error and P4PORT is never set
	# and this loop will loop forever checking the default p4 port
	p4 info 2>&1 | grep perforce:1666
	if [ $? -eq 0 ]; then
	    cd $DEV_ROOT/
	    if [ -f .rbt.envs ]; then
		. ./.rbt.envs
	    fi
	    if [ -f ../../.rbt.envs ]; then
		. ../../.rbt.envs
	    fi
	fi
    done
}

# check the queued directories of all branches except for this branch
# and if there are jobs queued to other branches then stop the rdequeuer
# here and start it on the other branch
rbt_maybe_switch_branches() {
    # find job files for other branch series: ..$diff_branch_dir/*/tools/... and same branch series ../*/tools/...
    # I do not know what a .swp file is but see them occasionally
    find $DEV_ROOT/../*/tools/wls/infra/test/queued $DEV_ROOT/../*/tools/common/wls/infra/test/queued $DEV_ROOT/..$diff_branch_dir/*/tools/wls/infra/test/queued $DEV_ROOT/..$diff_branch_dir/*/tools/common/wls/infra/test/queued -type f ! -name ".*.swp" ! -name ".nfs*" 2>&1 |\
    grep -iv $DEV_ROOT/$moved_path/$SRC/tools/wls/infra/test/queued/ |\
    grep -iv "No such file or directory" | grep -iv "unable to access" |\
    head -1 |\
    while read REPLY; do
	# we will only get here if there is at least one job
	# queued to another branch
	   otherdir=`dirname "$REPLY"`		# strip job filename
	   otherdir=`dirname "$otherdir"`	# strip /queued
	   infradir=`dirname "$otherdir"`  # strip /test
	   rqstatus -i "switching to other coherence branch, queued job is $REPLY"
	   rqstatus busy
           # make sure some basic branch files exist in the other branch
	   p4dirs=${infradir}/...
	   p4 diff -se $p4dirs
	   p4 diff -sd $p4dirs
	   unset p4dirs
	   sh $otherdir/at_rdequeuer.sh -r
	   # sleep for 5 minutes for the restart to kill us, this
	   # is so that if this script is run via the queue if will
	   # not dequeue the next entry before the rdequeuer starts
	   sleep 300
    done
}

# check infra/test/queued dir of branches weblogic/dev/* on depot p4jrep:7999
# WLS branches have different file structure against coherence source branches
rbt_maybe_switch_depots() {
       # test whether $wls_dir is valid dir, if not we do not need to do anything
       if [ -d "$wls_dir" ]; then
	   # find job files, exclude *.swp and *.nfs files
	   find $wls_dir/*/wls/infra/test/queued -type f ! -name ".*.swp" ! -name ".nfs*" | head -1 |\
	   while read REPLY; do
	      # we will only get here if there is at least one job
	      # queued to another branch
	      otherdir=`dirname "$REPLY"`		# strip job filename
	      otherdir=`dirname "$otherdir"`	# strip /queued
	      branch=`dirname "$otherdir"`	# strip /test
	      branch=`dirname "$branch"`	# strip /infra
	      branch=`dirname "$branch"`	# strip /wls
	      branch=`basename "$branch"`	# grab SRC
	      rqstatus -i "switching to branch $branch on WLS depot (p4jrep:7999)"
	      rqstatus busy
              # make sure basic files of WLS branches exist
	      p4dirs=
	      for i in %%1 env/... wls/%%1 wls/infra/... 3rdparty/jrexec/...; do
	         p4dirs="$p4dirs //depot/dev/$branch/$i"
	      done
	      myhostname="`uname -n | tr A-Z a-z`"
	      myid="`logname`"
	      p4 -u bt -p p4jrep:7999 -c $myid.$myhostname sync $p4dirs
	      p4 -u bt -p p4jrep:7999 -c $myid.$myhostname diff -se $p4dirs
	      p4 -u bt -p p4jrep:7999 -c $myid.$myhostname diff -sd $p4dirs
	      unset p4dirs myid myhostname
	      sh $otherdir/at_rdequeuer.sh -r
	      # sleep for 5 minutes for the restart to kill us, this
	      # is so that if this script is run via the queue if will
	      # not dequeue the next entry before the rdequeuer starts
	      sleep 300
          done
      fi
}

BT=${DEV_ROOT:?}/tools/wls/infra/build

if [ "`uname`" = "Windows_NT" ]; then
    PS=";"
    SAFEECHO="print -r"
elif uname | grep CYGWIN_NT- >/dev/null; then
    PS=";"
else
    PS=":"
fi
export PS

# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr

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

if $TEST $BT/rbt.java -nt $BT/rbt.class ; then
    which ${JAVA_HOME}/bin/javac 2>&1 >/dev/null
    if [ $? -eq 0 ]; then
	rm -f $BT/rbt.class
	${JAVA_HOME}/bin/javac -source 17 -target 17 -classpath $BT/jsch/jsch-0.1.50.jar $BT/rbt.java
    fi
fi

if $TEST $BT/xargs_rm.java -nt $BT/xargs_rm.class; then
    which ${JAVA_HOME}/bin/javac 2>&1 >/dev/null
    if [ $? -eq 0 ]; then
        ${JAVA_HOME}/bin/javac -source 17 -target 17 $BT/xargs_rm.java
    fi
fi

unset TEST

JENKINS_URL="${JENKINS_URL_ctl:-http://wls-jenkins.us.oracle.com/}"
export JENKINS_URL
JENKINS_RQJOB=COHRQ
export JENKINS_RQJOB
