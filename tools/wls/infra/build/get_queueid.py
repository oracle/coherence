#
# parse the json that comes back from jenkins REST api json queue/api/json
# return the queue "id" that matches the RQ job parameter JOBID
# handle empty stdin (ie the curl failed) and return nothing
#
import sys
import json

if len(sys.argv) != 2:
    print "Usage: " + sys.argv[0] + " jobId"
    sys.exit()
    
try:
    data = json.load(sys.stdin)

    for item in data["items"]:
        foundit = False
        for action in item["actions"]:
            if action.has_key("parameters"):
                for param in action["parameters"]:
                    if param.has_key("name"):
                        if param["name"] == "JOBID" and param["value"] == sys.argv[1]:
                            foundit = True
                    if param.has_key("_class"):
                        if param["_class"] == "org.jvnet.jenkins.plugins.nodelabelparameter.LabelParameterValue":
                            if param["name"] == "JOBID" and param["value"] == sys.argv[1]:
                                foundit = True
        task = item["task"]
        if task.has_key("name"):
            if ( task["name"] == "RQ" or task["name"] == "RQnew" or task["name"] == "RQwithP4" or task["name"] == "RQwithP4new" or task["name"] == "COHRQ" or task["name"] == "COHRQnew" ) and foundit:
                print str(item["id"])
                sys.exit()

except ValueError:  # includes ValueError: No JSON object could be decoded
    sys.exit()
