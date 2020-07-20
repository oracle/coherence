/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

/*
 * ViewModel for Departments page.
 */
define(['rest-examples/common', 'knockout', 'ojs/ojbootstrap', 'ojs/ojarraydataprovider', 'ojs/ojvalidation-base', 'ojs/ojdatacollection-utils', 'ojs/ojknockout',
        'ojs/ojlabel', 'ojs/ojtable', 'ojs/ojinputtext', 'ojs/ojformlayout', 'ojs/ojmessages', 'ojs/ojdialog'],
    function (common, ko, Bootstrap, ArrayDataProvider, ValidationBase, DataCollectionEditUtils) {

        function Page1ViewModel() {
            var root = this;

            this.deptObservableArray = ko.observableArray([]);
            this.dataprovider = new ArrayDataProvider(this.deptObservableArray, {keyAttributes: 'deptCode'});

            this.newDeptCode = ko.observable("");
            this.newDeptName = ko.observable("");

            this.editRow = ko.observable();

            // messages related objects
            this.applicationMessages = ko.observableArray([]);
            this.messageAttributes = common.MESSAGE_ATTRS;

            this.closeMessageHandler = function (event) {
                // Remove from bound observable array
                root.applicationMessages.remove(event.detail.message);
            };

            this.beforeRowEditEndListener = function (event) {
                // the DataCollectionEditUtils.basicHandleRowEditEnd is a utility method
                // which will handle validation of editable components and also handle
                // canceling the edit
                var detail = event.detail;
                if (DataCollectionEditUtils.basicHandleRowEditEnd(event, detail) === false) {
                    event.preventDefault();
                } else {
                    var updatedData = event.target.getDataForVisibleRow(detail.rowContext.status.rowIndex).data;
                    // document.getElementById('rowDataDump').value = (JSON.stringify(updatedData));
                    // save the updated department
                    root.putDepartment(updatedData.deptCode, updatedData.name, true);
                }
            };

            this.handleUpdate = function (event, context) {
                this.editRow({rowKey: context.key});
            }.bind(this);

            this.handleDone = function (event, context) {
                this.editRow({rowKey: null});
            }.bind(this);

            this.handleDelete = function (event, context) {
                this.deleteDepartment(context.key);
            }.bind(this);

            // called to refresh data from Coherence
            this.refresh = function () {
                common.getDepartmentData(
                    function (data) {  // success
                        root.deptObservableArray(data);
                        document.getElementById('departmentsTable').refresh();
                    },
                    function (jqXHR, exception) {
                        console.log('Get for departments failed. ' + exception);
                    }
                );
            };

            // called to add a new department
            this.putDepartment = function (deptCode, deptName, refresh, callback) {
                var myPromise = common.createDepartment(deptCode, deptName);
                if (refresh === false) {
                    // if we are not refreshing straight away then return the promise
                    // so we can wait on them all
                    return myPromise;
                }
                if (myPromise !== undefined) {
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

            this.closeDialog = function (event) {
                document.getElementById('addDialog').close();
            };

            this.closeDialogAndRefresh = function () {
                root.closeDialog();
                root.refresh();
            };

            this.openDialog = function (event) {
                root.newDeptCode("");
                root.newDeptName("");
                document.getElementById('addDialog').open();
            };

            // Add an individual department
            this.addDepartment = function () {
                if (root.newDeptCode() === "" || root.newDeptCode() === undefined ||
                    root.newDeptName() === "" || root.newDeptName() === undefined) {
                    alert('Please enter both department code and name');
                } else {
                    root.putDepartment(root.newDeptCode(), root.newDeptName(), true, root.closeDialogAndRefresh);
                    root.applicationMessages.push(common.createNotification("confirmation", "Added Department " + root.newDeptCode(),
                        "PUT " + common.getCurrentURL() + "/cache/departments/" + root.newDeptCode()));
                }
            };


            // Add default departments
            this.addDefaultDepartments = function () {
                var calls = new Array(10);
                var i = 0;

                calls[i++] = root.putDepartment('01', 'Computers & Tablets', false);
                calls[i++] = root.putDepartment('02', 'Cameras, Phones & GPS', false);
                calls[i++] = root.putDepartment('03', 'Games & Gaming Consoles', false);
                calls[i++] = root.putDepartment('04', 'TV & Home Theatre', false);
                calls[i++] = root.putDepartment('05', 'Headphones & Audio', false);
                calls[i++] = root.putDepartment('06', 'Kitchen Appliance', false);
                calls[i++] = root.putDepartment('07', 'Heating & Cooling', false);
                calls[i++] = root.putDepartment('08', 'Connected Fitness & Health', false);
                calls[i++] = root.putDepartment('09', 'Beds & Manchester', false);
                calls[i++] = root.putDepartment('10', 'Sports & Camping', false);

                // wait for all requests to finish
                Promise.all(calls).then(root.refresh).catch(root.refresh);
            };

            // Delete a department
            this.deleteDepartment = function (deptCode) {
                // check that there are no products with this department
                var hasProducts = false;
                var myPromises = new Array(1);
                common.getProductsForDepartment(deptCode,
                    function (response) {
                        if (response.length > 0) {
                            alert('There are products belonging to department ' + deptCode +
                                ', unable to delete');
                        } else {
                            var deleteDept = window.confirm('Are you sure you want to delete department ' + deptCode + '?');
                            if (deleteDept) {
                                common.deleteDepartment(deptCode,
                                    function () {
                                        root.refresh();
                                        root.applicationMessages.push(common.createNotification("confirmation", "Deleted Department " + deptCode,
                                            "DELETE " + common.getCurrentURL() + "/cache/departments/" + deptCode));
                                    },
                                    function (jqXHR, exception) {
                                        console.log("Error: " + jqXHR + ", " + exception);
                                    });
                            }
                        }
                    },
                    function (jqXHR, exception) {
                        console.log("Error: " + jqXHR + ", " + exception);
                    });
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
