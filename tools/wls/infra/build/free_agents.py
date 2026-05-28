#
# parse the json that comes back from jenkins REST api json label/LABEL/api/json
# return the number of free agents (total minur busy executors), make sure you
# handle empty stdin (ie the curl failed) and return something reasonable (zero)
#
import sys
import json

try:
    data = json.load(sys.stdin)
    print str(data["totalExecutors"] - data["busyExecutors"])

except ValueError:  # includes ValueError: No JSON object could be decoded
    print 0
