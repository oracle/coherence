/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

/*
 * ViewModel for Contacts page.
 */
define(['ojs/ojvalidator-numberrange', 'ojs/ojconverter-number', 'ojs/ojconverter-datetime', 'rest-examples/common', 'knockout', 'ojs/ojbootstrap', 'ojs/ojarraydataprovider', 'ojs/ojvalidation-base', 'ojs/ojdatacollection-utils', 'ojs/ojknockout',
        'ojs/ojlabel', 'ojs/ojtable', 'ojs/ojinputtext', 'ojs/ojformlayout', 'ojs/ojmessages', 'ojs/ojdatetimepicker', 'ojs/ojselectcombobox', 'ojs/ojdialog'],
    function (NumberRangeValidator, NumberConverter, DateTimeConverter, common, ko, Bootstrap, ArrayDataProvider, ValidationBase, DataCollectionEditUtils) {

        function Page3ViewModel() {
            var root = this;

            this.contactsObservableArray = ko.observableArray([]);
            this.dataprovider = new ArrayDataProvider(this.contactsObservableArray, {keyAttributes: 'key'});

            this.DEFAULT_QUERY = "/cache/contacts.json;sort=lastName:asc";
            this.MA_QUERY = "/cache/contacts.json;sort=lastName:asc?q=homeAddress.state is 'MA'";
            this.AGE_QUERY = '/cache/contacts.json;sort=age';
            this.STATE_QUERY = '/cache/contacts.json;sort=workAddress.state';
            this.ZIP_QUERY = '/cache/contacts.json;sort=homeAddress.zipCode:desc';

            this.query = ko.observable(this.DEFAULT_QUERY);
            this.currentURL = ko.observable(common.getCurrentURL());
            this.dialogTitle = ko.observable("");

            this.newFirstName = ko.observable("");
            this.newLastName = ko.observable("");
            this.contactCount = ko.observable(0);

            this.stateData = ko.observableArray([]);
            this.countryData = ko.observableArray([]);

            this.updateDialogTitle = ko.observable();
            this.updateType = "";

            this.newStreet1 = ko.observable("");
            this.newStreet2 = ko.observable("");
            this.newCity = ko.observable("");
            this.newState = ko.observable("");
            this.newCountry = ko.observable("");
            this.newZipCode = ko.observable("");

            this.updateFirstName = "";
            this.updateLastName = "";

            // messages related objects
            this.applicationMessages = ko.observableArray([]);
            this.messageAttributes = common.MESSAGE_ATTRS;

            this.numberConverter = new NumberConverter.IntlNumberConverter();
            this.dateConverter = new DateTimeConverter.IntlDateTimeConverter();

            this.closeMessageHandler = function (event) {
                // Remove from bound observable array
                root.applicationMessages.remove(event.detail.message);
            };

            this.getAddress = function (address) {
                return address.street1 +
                    (address.street2 !== undefined ? " " + address.street2 + " " : "") +
                    address.city + " " +
                    address.state + " " +
                    address.country + " " +
                    address.zipCode;
            };

            // Refresh data from Coherence
            this.refresh = function () {
                common.getContacts(
                    root.query(),
                    function (data) {  // success
                        var newArray = [];
                        root.contactCount(data !== undefined ? data.length : 0);
                        // create extra fields for
                        data.forEach(function (entry) {
                            entry.homeAddress = root.getAddress(entry.homeAddress);
                            entry.workAddress = root.getAddress(entry.workAddress);
                            var phoneNumber = "";
                            if (entry.phoneNumbers.work !== undefined) {
                                var phone = entry.phoneNumbers.work;
                                phoneNumber = phone.accessCode + " " + phone.countryCode + " " + phone.areaCode + " " + phone.localNumber;
                            }
                            entry.phoneNumber = phoneNumber;
                            newArray.push(entry);
                        });
                        root.contactsObservableArray(newArray);
                        document.getElementById('contactsTable').refresh();
                    },
                    function (jqXHR, exception) {
                        console.log('Get for contacts failed. ' + exception);
                    }
                );
            };

            this.handleHomeUpdate = function (event, context) {
                root.showUpdateDialog("home", event, context);
            };

            this.handleWorkUpdate = function (event, context) {
                root.showUpdateDialog("work", event, context);
            };

            // Show the address update diaglog for the given type
            this.showUpdateDialog = function (type, event, context) {
                var firstName = context.row.firstName;
                var lastName = context.row.lastName;
                common.getContact(firstName, lastName,
                    function (contact) {
                        root.updateDialogTitle(type === "home" ? "Update Home Address" : "Update Work Address");
                        root.updateType = type;
                        var address = (type === "home" ? contact.homeAddress : contact.workAddress);

                        root.updateFirstName = firstName;
                        root.updateLastName = lastName;
                        root.newStreet1(address.street1);
                        root.newStreet2(address.street2);
                        root.newCity(address.city);
                        root.newState(address.state);
                        root.newCountry(address.country);
                        root.newZipCode(address.zipCode);
                        root.openUpdateDialog();
                    },
                    function (jqXHR, exception) {
                        alert('Get for Contact failed. ' + exception.status + " " + exception.statusText);
                    });
            };

            // Save an updated address
            this.saveUpdatedAddress = function () {
                // validation
                if (root.newStreet1 === undefined || root.newStreet1 === "") {
                    alert("Please enter at least street address 1");
                } else {
                    common.getContact(root.updateFirstName, root.updateLastName,
                        function (contact) {
                            var updatedAddress = {};
                            updatedAddress.street1 = root.newStreet1();
                            updatedAddress.street2 = root.newStreet2();
                            updatedAddress.city = root.newCity();
                            updatedAddress.state = root.newState();
                            updatedAddress.country = root.newCountry();
                            updatedAddress.zipCode = root.newZipCode();

                            if (root.updateType === "work") {
                                contact.workAddress = updatedAddress;
                            } else {
                                contact.homeAddress = updatedAddress;
                            }

                            console.log(JSON.stringify(contact));

                            // save the contact
                            common.updateContact(root.updateFirstName + "_" + root.updateLastName, contact,
                                function () {
                                    root.closeUpdateDialog();
                                    root.refresh();
                                },
                                function (jqXHR, exception) {
                                    if (jqXHR.status !== 200) {
                                        alert('Update for Contact failed. ' + exception);
                                    } else {
                                        root.closeUpdateDialog();
                                        root.refresh();
                                    }
                                });
                        },
                        function (jqXHR, exception) {
                            alert('Get for Contact failed. ' + exception.status + " " + exception.statusText);
                        });
                }
            };

            this.showAll = function () {
                root.query(root.DEFAULT_QUERY);
                root.refresh();
            };

            this.MAResidents = function () {
                root.query(root.MA_QUERY);
                root.refresh();
            };

            this.sortByAge = function () {
                root.query(root.AGE_QUERY);
                root.refresh();
            };

            this.sortByWorkState = function () {
                root.query(root.STATE_QUERY);
                root.refresh();
            };

            this.sortByZipDesc = function () {
                root.query(root.ZIP_QUERY);
                root.refresh();
            };

            this.handleDelete = function (event, context) {
                var key = context.row.firstName + "_" + context.row.lastName;
                var deleteContact = window.confirm('Are you sure you want to delete ' + key + '?');
                if (deleteContact) {
                    common.deleteContact(key,
                        function (response) {
                            root.refresh();
                            root.applicationMessages.push(common.createNotification("confirmation", "Deleted Contact " + key,
                                "DELETE " + common.getCurrentURL() + "/cache/contacts/" + key));
                        },
                        common.showAlert);
                }
            };

            // Create a random contact with a given name
            this.createRandomContact = function () {
                if (root.newLastName() === "" || root.newLastName() === undefined ||
                    root.newFirstName() === "" || root.newFirstName() === undefined) {
                    alert('Please enter both first and last name.');
                } else {
                    if (root.newFirstName().includes(" ") || root.newLastName().includes(" ")) {
                        alert('Names must not include any spaces.');
                    } else {
                        root.createContact(root.newFirstName(), root.newLastName(), true, root.closeDialogAndRefresh);
                    }
                }
            };

            this.closeDialog = function (event) {
                document.getElementById('addDialog').close();
            };

            this.closeUpdateDialog = function (event) {
                document.getElementById('updateDialog').close();
            };

            this.closeDialogAndRefresh = function () {
                root.closeDialog();
                root.refresh();
            };

            this.openDialog = function (event) {
                document.getElementById('addDialog').open();
            };

            this.openUpdateDialog = function (event) {
                document.getElementById('updateDialog').open();
            };

            this.addNewContact = function () {
                root.newLastName("");
                root.newFirstName("");
                root.openDialog();
            };

            // View entry as XML
            this.viewAsXML = function (event, context) {
                var url = '/cache/contacts/' + context.row.firstName + "_" + context.row.lastName + '.xml';
                $.ajax({
                    url: url,
                    type: 'GET',
                    error: function (jqXHR, exception) {
                    },
                    success: function (data) {
                        var xmlString = new XMLSerializer().serializeToString(data.documentElement);
                        alert('GET ' + common.getCurrentURL() + url +
                            '\n\n' + xmlString);
                    }
                });
            };


            // Create a contact with given name and random attributes
            this.createContact = function (firstName, lastName, refresh, callback) {
                var myPromise = common.createContact(firstName, lastName);
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

            // Populate default contacts with random names generated
            // from http://listofrandomnames.com
            this.populateDefaults = function () {
                var calls = new Array(30);
                var i = 0;

                calls[i++] = root.createContact('Brittaney', 'Beeman', false);
                calls[i++] = root.createContact('Rey', 'Garza', false);
                calls[i++] = root.createContact('Laura', 'Schnieders', false);
                calls[i++] = root.createContact('Scott', 'Veasley', false);
                calls[i++] = root.createContact('Lashawnda', 'Pisano', false);
                calls[i++] = root.createContact('Darleen', 'Hammon', false);
                calls[i++] = root.createContact('Ashlyn', 'Dahlstrom', false);
                calls[i++] = root.createContact('Particia', 'Canady', false);
                calls[i++] = root.createContact('Beatriz', 'Merrigan', false);
                calls[i++] = root.createContact('Shawn', 'Villacorta', false);
                calls[i++] = root.createContact('Verda', 'Newbrough', false);
                calls[i++] = root.createContact('Arcelia', 'Cade', false);
                calls[i++] = root.createContact('Levi', 'Willard', false);
                calls[i++] = root.createContact('Lynn', 'Dalley', false);
                calls[i++] = root.createContact('Retha', 'Brian', false);
                calls[i++] = root.createContact('Desiree', 'Celestine', false);
                calls[i++] = root.createContact('Dodie', 'Mcghee', false);
                calls[i++] = root.createContact('Will', 'Plewa', false);
                calls[i++] = root.createContact('Leatrice', 'Bushard', false);
                calls[i++] = root.createContact('Brice', 'Eakins', false);
                calls[i++] = root.createContact('Izola', 'Grahm', false);
                calls[i++] = root.createContact('Dot', 'Lansford', false);
                calls[i++] = root.createContact('Diamond', 'Dobbins', false);
                calls[i++] = root.createContact('Deon', 'Soule', false);
                calls[i++] = root.createContact('Lieselotte', 'Borchardt', false);
                calls[i++] = root.createContact('Corene', 'Lipsett', false);
                calls[i++] = root.createContact('Gerard', 'Rumore', false);
                calls[i++] = root.createContact('Jay', 'Leisy', false);
                calls[i++] = root.createContact('Sari', 'Searle', false);
                calls[i++] = root.createContact('Pamelia', 'Tookes', false);

                // wait for all requests to finish
                Promise.all(calls).then(root.refresh).catch(root.refresh);
            };

            // lifecycle methods

            this.connected = function (info) {
                root.refresh();

                common.getStateData(function (data) {
                    var newArray = [];
                    data.forEach(function (entry) {
                        newArray.push({value: entry.code, label: entry.name});
                    });
                    root.stateData(newArray);
                }, function () {
                });

                common.getCountryData(function (data) {
                    var newArray = [];
                    data.forEach(function (entry) {
                        newArray.push({value: entry.code, label: entry.name});
                    });
                    root.countryData(newArray);
                }, function () {
                });
            };

            this.transitionCompleted = function (info) {
                root = this;
            };

        }

        return new Page3ViewModel();
    }
);
