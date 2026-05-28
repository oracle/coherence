#!/bin/sh
# Copyright 2006 BEA Systems, Inc.
# make sure new versions of bash >= 3.2.9-11 on Cygwin ignore carriage returns
set -o | grep igncr >/dev/null && set -o igncr # comment required

cd `dirname $0`

./at.sh rdequeuer.sh "$@"
