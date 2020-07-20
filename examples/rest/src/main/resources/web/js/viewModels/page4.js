/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

/*
 * ViewModel for Server Sent Events page.
 */
define(['rest-examples/common', 'knockout', 'ojs/ojbootstrap', 'ojs/ojarraydataprovider', 'ojs/ojvalidation-base', 'ojs/ojdatacollection-utils', 'jquery', 'ojs/ojknockout',
        'ojs/ojlabel', 'ojs/ojtable', 'ojs/ojinputtext', 'ojs/ojformlayout', 'ojs/ojdatetimepicker', 'ojs/ojselectcombobox', 'ojs/ojmessages', 'ojs/ojradioset', 'ojs/ojdialog'],
    function (common, ko, Bootstrap, ArrayDataProvider, ValidationBase, DataCollectionEditUtils, $) {

        function Page4ViewModel() {
            var root = this;

            var STOPPED = "Stopped";
            var STARTED = "Started";

            this.contactsListening = ko.observable(false);
            this.productsListening = ko.observable(false);
            this.departmentsListening = ko.observable(false);

            this.contactsStatus = ko.observable(STOPPED);
            this.departmentsStatus = ko.observable(STOPPED);
            this.productsStatus = ko.observable(STOPPED);

            this.contactQuery = ko.observable("all");
            this.productsQuery = ko.observable("all");

            // Display message if we are running under IE
            if (typeof (bIsIE) != 'undefined') {
                alert('Internet Explorer does not support Server Sent Events.\n' +
                    'Refer to http://www.w3schools.com/html/html5_serversentevents.asp');
            }

            function MetricsModel() {
                var self = this;
                self.insertCount = ko.observable(0);
                self.updateCount = ko.observable(0);
                self.deleteCount = ko.observable(0);
                self.totalCount = ko.observable(0);

                self.lastEventType = ko.observable("N/A");
                self.lastEventKey = ko.observable("N/A");
                self.lastEventOldValue = ko.observable("N/A");
                self.lastEventNewValue = ko.observable("N/A");
            }

            this.contactMetrics = new MetricsModel();
            this.productsMetrics = new MetricsModel();
            this.departmentsMetrics = new MetricsModel();

            // messages related objects
            this.applicationMessages = ko.observableArray([]);
            this.messageAttributes = common.MESSAGE_ATTRS;

            this.closeMessageHandler = function (event) {
                // Remove from bound observable array
                this.applicationMessages.remove(event.detail.message);
            };

            this.getContactsListeningText = function (value) {
                if (value === "all") {
                    return "All";
                } else if (value === "greater") {
                    return "Age >= 45";
                } else {
                    return "Age < 45";
                }
            };

            this.getProductsListeningText = function (value) {
                if (value === "all") {
                    return "All";
                } else {
                    return "price >= $1000";
                }
            };

            this.resetAll = function () {
                location.href = location.href;
            };

            // Start SSE listening for contacts
            this.startContacts = function () {
                root.contactsListening(true);
                root.contactsStatus(STARTED + ", " + root.getContactsListeningText(root.contactQuery()));

                var query = "";
                if (root.contactQuery() === "greater") {
                    query = "?q=age%20>=%2045";
                } else if (root.contactQuery() === "less") {
                    query = '?q=age%20<%2045';
                }

                var eventSourceContacts = new EventSource('/cache/contacts' + query);

                eventSourceContacts.addEventListener('insert', function (event) {
                    root.contactMetrics.insertCount(root.contactMetrics.insertCount() + 1);
                    root.contactMetrics.totalCount(root.contactMetrics.totalCount() + 1);
                    root.updateEvent(root.contactMetrics, JSON.parse(event.data), 'insert');
                });

                eventSourceContacts.addEventListener('update', function (event) {
                    root.contactMetrics.totalCount(root.contactMetrics.totalCount() + 1);
                    root.contactMetrics.updateCount(root.contactMetrics.updateCount() + 1);
                    root.updateEvent(root.contactMetrics, JSON.parse(event.data), 'update');
                });

                eventSourceContacts.addEventListener('delete', function (event) {
                    root.contactMetrics.totalCount(root.contactMetrics.totalCount() + 1);
                    root.contactMetrics.deleteCount(root.contactMetrics.deleteCount() + 1);
                    root.updateEvent(root.contactMetrics, JSON.parse(event.data), 'delete');
                });
            };

            // Start SSE listening for products
            this.startProducts = function () {
                root.productsListening(true);
                root.productsStatus(STARTED + ", " + root.getProductsListeningText(root.productsQuery()));

                var query = "";
                if (root.productsQuery() === "greater") {
                    query = "?q=price%20>=%201000d";
                }

                var eventSourceProducts = new EventSource('/cache/products' + query);

                eventSourceProducts.addEventListener('insert', function (event) {
                    root.productsMetrics.insertCount(root.productsMetrics.insertCount() + 1);
                    root.productsMetrics.totalCount(root.productsMetrics.totalCount() + 1);
                    root.updateEvent(root.productsMetrics, JSON.parse(event.data), 'insert');
                });

                eventSourceProducts.addEventListener('update', function (event) {
                    root.productsMetrics.totalCount(root.productsMetrics.totalCount() + 1);
                    root.productsMetrics.updateCount(root.productsMetrics.updateCount() + 1);
                    root.updateEvent(root.productsMetrics, JSON.parse(event.data), 'update');
                });

                eventSourceProducts.addEventListener('delete', function (event) {
                    root.productsMetrics.totalCount(root.productsMetrics.totalCount() + 1);
                    root.productsMetrics.deleteCount(root.productsMetrics.deleteCount() + 1);
                    root.updateEvent(root.productsMetrics, JSON.parse(event.data), 'delete');
                });
            };

            // Start SSE listening for departments
            this.startDepartments = function () {
                root.departmentsListening(true);
                root.departmentsStatus(STARTED);

                var eventSourceDepartments = new EventSource('/cache/departments');

                eventSourceDepartments.addEventListener('insert', function (event) {
                    root.departmentsMetrics.insertCount(root.departmentsMetrics.insertCount() + 1);
                    root.departmentsMetrics.totalCount(root.departmentsMetrics.totalCount() + 1);
                    root.updateEvent(root.departmentsMetrics, JSON.parse(event.data), 'insert');
                });

                eventSourceDepartments.addEventListener('update', function (event) {
                    root.departmentsMetrics.totalCount(root.departmentsMetrics.totalCount() + 1);
                    root.departmentsMetrics.updateCount(root.departmentsMetrics.updateCount() + 1);
                    root.updateEvent(root.departmentsMetrics, JSON.parse(event.data), 'update');
                });

                eventSourceDepartments.addEventListener('delete', function (event) {
                    root.departmentsMetrics.totalCount(root.departmentsMetrics.totalCount() + 1);
                    root.departmentsMetrics.deleteCount(root.departmentsMetrics.deleteCount() + 1);
                    root.updateEvent(root.departmentsMetrics, JSON.parse(event.data), 'delete');
                });
            };

            // Update the event data on screen
            root.updateEvent = function (metrics, eventData, eventType) {
                metrics.lastEventType(eventType);
                if (metrics === root.contactMetrics) {
                    metrics.lastEventKey(eventData.key.firstName + '_' + eventData.key.lastName);
                } else if (metrics === root.productsMetrics) {
                    metrics.lastEventKey(eventData.productId);
                } else {
                    metrics.lastEventKey(eventData.deptCode);
                }

                metrics.lastEventOldValue('N/A');
                metrics.lastEventNewValue('N/A');

                if (eventType === 'insert' || eventType === 'update') {
                    metrics.lastEventNewValue(JSON.stringify(eventData.newValue, null, 2));
                }
                if (eventType === 'delete' || eventType === 'update') {
                    metrics.lastEventOldValue(JSON.stringify(eventData.oldValue, null, 2));
                }
            };

            // lifecycle methods

            this.transitionCompleted = function (info) {
                root = this;
            };

        }

        return new Page4ViewModel();
    }
);
