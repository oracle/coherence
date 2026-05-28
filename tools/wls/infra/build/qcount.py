#
# parse the json that comes back from jenkins REST api json queue/api/json
# count all queued builds for the "RQ" and "RQnew" job where the LABEL parameter is equal
# to the argument.  the label value might be "(linux&&rq)" or "new" for example,  make sure you
# handle empty stdin (ie the curl failed) and return something reasonable (9999)
#
import sys
import json

if len(sys.argv) != 2:
    print "Usage: " + sys.argv[0] + " labelExpression"
    sys.exit()
    
try:
    data = json.load(sys.stdin)

    count = 0
    for item in data["items"]:
        foundit = False
        for action in item["actions"]:
            if action.has_key("parameters"):
                for param in action["parameters"]:
                    if param.has_key("name"):
                        if param["name"] == "LABEL" and param["value"] == sys.argv[1]:
                                foundit = True
                    if param.has_key("_class"):
                        if param["_class"] == "org.jvnet.jenkins.plugins.nodelabelparameter.LabelParameterValue":
                            if param["name"] == "LABEL" and param["value"] == sys.argv[1]:
                                foundit = True
        task = item["task"]
        if task.has_key("name"):
            if ( task["name"] == "RQ" or task["name"] == "RQnew" or task["name"] == "RQwithP4" or task["name"] == "RQwithP4new" or task["name"] == "COHRQ" or task["name"] == "COHRQnew" ) and foundit:
                count += 1
    print count

except ValueError:  # includes ValueError: No JSON object could be decoded
    print 9999
