/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

/*
 * ViewModel for JSON Passthrough Data page.
 */
define(['rest-examples/common', 'knockout', 'ojs/ojbootstrap', 'ojs/ojarraydataprovider', 'ojs/ojvalidation-base', 'ojs/ojknockout',
        'ojs/ojlabel', 'ojs/ojtable', 'ojs/ojinputtext', 'ojs/ojformlayout', 'ojs/ojmessages', 'ojs/ojdialog', 'ojs/ojvalidation-number'],
    function (common, ko, Bootstrap, ArrayDataProvider, ValidationBase) {

        function Page1ViewModel() {
            var root = this;

            this.jsonObservableArray = ko.observableArray([]);
            this.dataprovider = new ArrayDataProvider(this.jsonObservableArray, {keyAttributes: 'key'});

            this.DEFAULT_QUERY = "/cache/json/entries.json;sort=age";
            this.PERTH_QUERY = '/cache/json/entries.json?q=address.city is \'Perth\'';
            this.TEEAGERS_QUERY = '/cache/json/entries.json?q=age between 13 and 19';
            this.OVER_30s_QUERY = '/cache/json/entries.json?q=age > 29';
            this.AGE_DESC_QUERY = '/cache/json/entries.json;sort=age:desc';
            this.NAME_QUERY = '/cache/json/entries.json;sort=name';
            this.CITY_QUERY = '/cache/json/entries.json;sort=address.city';

            this.query = ko.observable(this.DEFAULT_QUERY);
            this.currentURL = ko.observable(common.getCurrentURL());

            this.newKey = ko.observable("");
            this.newJSON = ko.observable("");
            this.dialogTitle = ko.observable("");
            this.readOnly = ko.observable(false);

            this.minAge = ko.observable(0);
            this.maxAge = ko.observable(0);

            // messages related objects
            this.applicationMessages = ko.observableArray([]);
            this.messageAttributes = common.MESSAGE_ATTRS;

            this.closeMessageHandler = function (event) {
                // Remove from bound observable array
                root.applicationMessages.remove(event.detail.message);
            };

            this.jsonConverter = ValidationBase.Validation.converterFactory("number").createConverter();

            this.jsonConverter.format = function (data) {
                var json = JSON.stringify(data);
                console.log("JSON is " + json);
                return json;
            };

            // Refresh data from Coherence
            this.refresh = function () {
                common.getJSONData(
                    root.query(),
                    function (data) {  // success
                        root.jsonObservableArray(data);
                        document.getElementById('jsonTable').refresh();
                    },
                    function (jqXHR, exception) {
                        console.log('Get for JSON data failed. ' + exception);
                    }
                );
                
                // get min and max ages for all objects
                // using aggregators over native JSON!
                $.ajax({
                    url: '/cache/json/long-min(age)',
                    type: 'GET',
                    error: function (jqXHR, exception) {
                    },
                    success: function (data) {
                        root.minAge(data);
                    }
                });

                $.ajax({
                    url: '/cache/json/long-max(age)',
                    type: 'GET',
                    error: function (jqXHR, exception) {
                    },
                    success: function (data) {
                        root.maxAge(data);
                    }
                });
            };

            this.handleUpdate = function (event, context) {
                root.newKey(context.row.key);
                root.newJSON(JSON.stringify(context.row.value, null, 2));
                root.dialogTitle("Update JSON Entry");
                root.readOnly(true);
                root.openDialog();
            };

            // Increment the age
            this.handleIncrementAge = function (event, context) {
                var key = context.row.key;
                common.incrementAge(key,
                    function () {
                        root.refresh();
                        root.applicationMessages.push(common.createNotification("confirmation", "Incremented age for " + key,
                            "POST " + common.getCurrentURL() + '/cache/json/' + key + '/increment(age,1)'));
                    },
                    function (message) {
                        alert('Error ' + message.status + ', ' + message.statusText);
                    });
            };

            this.showAll = function () {
                root.query(root.DEFAULT_QUERY);
                root.refresh();
            };

            this.perthResidents = function () {
                root.query(root.PERTH_QUERY);
                root.refresh();
            };

            this.showTeenagers = function () {
                root.query(root.TEEAGERS_QUERY);
                root.refresh();
            };

            this.showOver30s = function () {
                root.query(root.OVER_30s_QUERY);
                root.refresh();
            };

            this.sortByAge = function () {
                root.query(root.DEFAULT_QUERY);
                root.refresh();
            };

            this.sortByAgeDesc = function () {
                root.query(root.AGE_DESC_QUERY);
                root.refresh();
            };

            this.sortByName = function () {
                root.query(root.NAME_QUERY);
                root.refresh();
            };

            this.sortByCity = function () {
                root.query(root.CITY_QUERY);
                root.refresh();
            };

            // Delete an entry
            this.handleDelete = function (event, context) {
                var key = context.row.key;
                var deleteJSON = window.confirm('Are you sure you want to delete JSON entry ' + key + '?');
                if (deleteJSON) {
                    common.deleteJSON(key,
                        function (response) {
                            root.refresh();
                            root.applicationMessages.push(common.createNotification("confirmation", "Deleted JSON Entry " + key,
                                "DELETE " + common.getCurrentURL() + "/cache/json/" + key));
                        },
                        common.showAlert);
                }
            };

            // Save the JSON data
            this.saveJSONData = function () {
                if (root.newKey() === "" || root.newKey() === undefined ||
                    root.newJSON() === "" || root.newJSON() === undefined) {
                    alert('Please enter both key and JSON.');
                } else {
                    var finalData;
                    // validate the JSON
                    try {
                        finalData = JSON.parse(root.newJSON());
                    } catch (e) {
                        alert("JSON data is invalid.");
                        return;
                    }

                    // it is valid, so add it
                    root.addJSONEntry(root.newKey(), finalData, true,
                        function () {
                            root.closeDialog();
                            root.refresh();
                        });
                }
            };

            this.closeDialog = function (event) {
                document.getElementById('addDialog').close();
            };

            this.closeDialogAndRefresh = function () {
                root.closeDialog();
                root.refresh();
            };

            this.openDialog = function (event) {
                document.getElementById('addDialog').open();
            };

            this.addNewJSONEntry = function () {
                root.newKey("");
                root.newJSON("");
                root.dialogTitle("Add New JSON Entry");
                root.readOnly(false);
                root.openDialog();
            };


            // Add a new entry
            this.addJSONEntry = function (key, data, refresh, callback) {
                var myPromise = common.createJSON(key, data);
                if (refresh === false) {
                    // if we are not refreshing straight away then return the promise
                    // so we can wait on them all
                    return myPromise;
                }
                if (myPromise != undefined) {
                    myPromise.then(
                        function () {
                            if (refresh) {
                                root.refresh();
                                if (callback !== undefined) {
                                    callback();
                                }
                            }
                        }).catch(
                        function (message) {
                            if (message.status === 200) {
                                if (callback !== undefined) {
                                    callback();
                                }
                            } else if (message) {
                                alert('Error ' + message.status + ', ' + message.data);
                            }
                        });
                }
            };

            // Add default JSON Data with random names generated
            // from http://listofrandomnames.com
            this.addDefaultJSONData = function () {
                var calls = new Array(11);
                var i = 0;

                calls[i++] = root.addJSONEntry('1', {
                    name: "Brittaney Beeman",
                    age: 25,
                    address: {
                        line1: "123 Railway Parade",
                        city: "Perth",
                        country: "Australia"
                    }
                }, false);
                calls[i++] = root.addJSONEntry('2', {
                    name: "Rey Garza",
                    age: 26,
                    address: {
                        line1: "55 Railway Parade",
                        city: "Perth",
                        country: "Australia"
                    }
                }, false);
                calls[i++] = root.addJSONEntry('3', {
                    name: "Laura Schnieders",
                    age: 45,
                    address: {
                        line1: "Unit 1, 222 Railway Parade",
                        city: "Sydney",
                        country: "Australia"
                    }
                }, false);
                calls[i++] = root.addJSONEntry('4', {
                    name: "Lashawnda Pisano",
                    age: 55,
                    address: {
                        line1: "30 Railway Parade",
                        city: "Sydney",
                        country: "Australia"
                    }
                }, false);
                calls[i++] = root.addJSONEntry('5', {
                    name: "Darleen Hammon",
                    age: 18,
                    address: {
                        line1: "443 Railway Parade",
                        city: "Perth",
                        country: "Australia"
                    }
                }, false);
                calls[i++] = root.addJSONEntry('6', {
                    name: "Ashlyn Dahlstrom",
                    age: 13,
                    address: {
                        line1: "52 Railway Parade",
                        city: "Sydney",
                        country: "Australia"
                    }
                }, false);
                calls[i++] = root.addJSONEntry('7', {
                    name: "Timothy Dahlstrom",
                    age: 12,
                    address: {
                        line1: "111 Railway Parade",
                        city: "Melbourne",
                        country: "Australia"
                    }
                }, false);
                calls[i++] = root.addJSONEntry('8', {
                    name: "Retha Brian",
                    age: 12,
                    address: {
                        line1: "551 Railway Parade",
                        city: "Brisbane",
                        country: "Australia"
                    }
                }, false);
                calls[i++] = root.addJSONEntry('9', {
                    name: "Izola Jones",
                    age: 39,
                    address: {
                        line1: "88 Railway Parade",
                        city: "Melbourne",
                        country: "Australia"
                    }
                }, false);
                calls[i++] = root.addJSONEntry('10', {
                    name: "Corene Bell",
                    age: 12,
                    address: {
                        line1: "123 Railway Parade",
                        city: "Perth",
                        country: "Australia"
                    }
                }, false);
                calls[i++] = root.addJSONEntry('11', {
                    name: "Pamelia Wynton",
                    age: 12,
                    address: {
                        line1: "2 Railway Parade",
                        city: "Brisbane",
                        country: "Australia"
                    }
                }, false);

                // wait for all requests to finish
                Promise.all(calls).then(root.refresh).catch(root.refresh);
            };
            
            // lifecycle methods

            this.connected = function (info) {
                root.refresh();
            };

            this.transitionCompleted = function (info) {
                root = this;
            };
        }

        return new Page1ViewModel();
    }
);
