#
# parse the json that comes back from jenkins REST api json computer/api/json
# return all the jenkins agents (displayName's)
#
import sys
import json

try:
    data = json.load(sys.stdin)
except ValueError:
    data = []
    sys.exit()

jio = False
if len(sys.argv) == 2:
    if sys.argv[1] == "-jio":
        jio = True;

count = 0
if ("computer") in data:
    for item in data["computer"]:
        if item["_class"] == "hudson.slaves.SlaveComputer":
            if jio:
                if item["idle"] and not item["offline"]:
                    print (item["displayName"])
            else:
                print (item["displayName"])
else:
    item = data
    if item["_class"] == "hudson.slaves.SlaveComputer":
        if jio:
            if item["idle"] and not item["offline"]:
                print (item["displayName"])
        else:
            print (item["displayName"])
