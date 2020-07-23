/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

/*
 * ViewModel for Products page.
 */
define(['ojs/ojvalidator-numberrange', 'ojs/ojconverter-number', 'rest-examples/common', 'knockout', 'ojs/ojbootstrap', 'ojs/ojarraydataprovider', 'ojs/ojvalidation-base', 'ojs/ojdatacollection-utils', 'jquery', 'ojs/ojknockout',
        'ojs/ojvalidation-number', 'ojs/ojlabel', 'ojs/ojtable', 'ojs/ojinputtext', 'ojs/ojformlayout', 'ojs/ojdatetimepicker', 'ojs/ojselectcombobox', 'ojs/ojmessages', 'ojs/ojdialog'],
    function (NumberRangeValidator, NumberConverter, common, ko, Bootstrap, ArrayDataProvider, ValidationBase, DataCollectionEditUtils, $) {

        function Page2ViewModel() {
            var root = this;

            this.deptObservableArray = ko.observableArray([]);
            this.dataprovider = new ArrayDataProvider(this.deptObservableArray, {keyAttributes: 'productId'});

            this.newProductId = ko.observable(0);
            this.newProductName = ko.observable("");
            this.newPrice = ko.observable(0);
            this.newDeptCode = ko.observable("");
            this.newQtyOnHand = ko.observable(0);

            this.addDefaultProductsDisabled = ko.observable(true);

            this.editRow = ko.observable();

            this.departmentList = ko.observableArray([]);

            // messages related objects
            this.applicationMessages = ko.observableArray([]);
            this.messageAttributes = common.MESSAGE_ATTRS;

            this.closeMessageHandler = function (event) {
                // Remove from bound observable array
                this.applicationMessages.remove(event.detail.message);
            };

            //// NUMBER AND DATE CONVERTER ////
            var priceValidator = new NumberRangeValidator({
                min: 0.01,
                max: 500000
            });
            var rangeValidator = new NumberRangeValidator({min: 0, max: 1000});

            var priceOptions = {style: 'currency', currency: 'USD'};
            this.priceConverter = ValidationBase.Validation.converterFactory("number").createConverter(priceOptions);

            this.validators = [rangeValidator];
            this.priceValidators = [priceValidator];

            this.numberConverter = new NumberConverter.IntlNumberConverter();

            this.beforeRowEditEndListener = function (event) {
                // the DataCollectionEditUtils.basicHandleRowEditEnd is a utility method
                // which will handle validation of editable components and also handle
                // canceling the edit
                var detail = event.detail;
                if (DataCollectionEditUtils.basicHandleRowEditEnd(event, detail) === false) {
                    event.preventDefault();
                } else {
                    var updatedData = event.target.getDataForVisibleRow(detail.rowContext.status.rowIndex).data;
                    // save the updated product
                    root.putProduct(updatedData.productId, updatedData.name, updatedData.price, updatedData.deptCode, updatedData.qtyOnHand, true);
                }
            };

            this.handleUpdate = function (event, context) {
                this.editRow({rowKey: context.key});
            }.bind(this);

            this.handleDone = function (event, context) {
                this.editRow({rowKey: null});
            }.bind(this);

            this.handleDelete = function (event, context) {
                this.deleteProduct(context.key);
            }.bind(this);

            this.handleReceive = function (event, context) {
                this.receiveProduct(context.key);
            }.bind(this);

            // Refresh data from Coherence
            this.refresh = function () {
                common.getProductData(
                    function (data) {  // success
                        // add in value on hand
                        var newArray = [];
                        data.forEach(function (entry) {
                            entry.valueOnHand = entry.qtyOnHand * entry.price;
                            newArray.push(entry);
                        });
                        root.deptObservableArray(newArray);
                        document.getElementById('productsTable').refresh();
                    },
                    function (jqXHR, exception) {
                        console.log('Get for departments failed. ' + exception);
                    }
                );
            };

            // Add a new product
            this.putProduct = function (productId, name, price, deptCode, qtyOnHand, refresh, callback) {
                var myPromise = common.createProduct(productId, name, price, deptCode, qtyOnHand);
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

            this.closeDialog = function (event) {
                document.getElementById('addDialog').close();
            };

            this.closeDialogAndRefresh = function () {
                root.closeDialog();
                root.refresh();
            };

            this.openDialog = function (event) {
                root.newProductId(0);
                root.newProductName("");
                root.newPrice(100.0);
                root.newDeptCode("");
                root.newQtyOnHand(0);

                document.getElementById('addDialog').open();
            };

            // Add an individual product
            this.addProduct = function () {
                if (root.newProductId() === undefined || root.newProductId() === 0 ||
                    root.newProductName() === "" || root.newProductName() === undefined ||
                    root.newDeptCode() === "" || root.newDeptCode() === undefined ||
                    root.newPrice() === 0) {
                    alert('Please enter at least product id, name, price and department');
                } else {
                    root.putProduct(root.newProductId(), root.newProductName(), root.newPrice(), root.newDeptCode(), root.newQtyOnHand(), true,
                        function () {
                            root.closeDialogAndRefresh();
                            root.applicationMessages.push(common.createNotification("confirmation", "Added Product " + root.newProductId(),
                                "PUT " + common.getCurrentURL() + "/cache/product/" + root.newProductId()));
                        });
                }
            };

            // Add default products
            this.addDefaultProducts = function () {
                var calls = new Array(17);
                var i = 0;
                calls[i++] = root.putProduct(1, 'Apple MacBook Air 13 inch 1.4GHz i5 256GB', 1399, '01', 0, false);
                calls[i++] = root.putProduct(2, 'Apple MacBook Pro with Retina Display 13 inch: 2.7GHz i5 256GB', 2097, '01', 0, false);
                calls[i++] = root.putProduct(3, 'Lenovo Z50-70 15.6inch i7 Laptop', 1118, '01', 0, false);

                calls[i++] = root.putProduct(10, 'Samsung Galaxy S6 32GB - Blue', 942, '02', 0, false);
                calls[i++] = root.putProduct(11, 'Samsung Galaxy S6 Edge 64GB - Gold', 1127, '02', 0, false);
                calls[i++] = root.putProduct(12, 'Apple iPhone 6 64GB', 1144, '02', 0, false);
                calls[i++] = root.putProduct(13, 'Apple iPhone 6 Plus 64GB', 1244, '02', 0, false);

                calls[i++] = root.putProduct(15, 'TomTom Via 280 GPS Navigator', 176, '02', 0, false);
                calls[i++] = root.putProduct(16, 'TomTom GO 60 Lifetime Maps & Traffic GPS', 249, '02', 0, false);
                calls[i++] = root.putProduct(17, 'Garmin Approach G3 Handheld GPS', 198, '02', 0, false);

                calls[i++] = root.putProduct(20, 'Xbox One With Kinect', 598, '03', 0, false);
                calls[i++] = root.putProduct(21, 'PS4 Console', 550, '03', 0, false);

                calls[i++] = root.putProduct(30, 'LG 84 inch 4K Ultra HD LED LCD 3D Capable Smart TV', 12000, '04', 0, false);
                calls[i++] = root.putProduct(31, 'LG 55 inch Full HD OLED 3D Capable Smart Curved TV', 3400, '04', 0, false);

                calls[i++] = root.putProduct(40, 'Beats by Dr. Dre Solo 2 On-Ear Headphones', 248, '05', 0, false);
                calls[i++] = root.putProduct(41, 'Sennheiser Wireless RF Headphones', 129, '05', 0, false);
                calls[i++] = root.putProduct(42, 'Sennheiser HD558 Headphones', 298, '05', 0, false);

                // wait for all requests to finish
                Promise.all(calls).then(root.refresh).catch(root.refresh);
            };

            // Delete a product
            this.deleteProduct = function (productId) {
                var deleteDept = window.confirm('Are you sure you want to delete product id ' + productId + '?');
                if (deleteDept) {
                    common.deleteProduct(productId,
                        function (response) {
                            root.refresh();
                            root.applicationMessages.push(common.createNotification("confirmation", "Deleted Product " + productId,
                                "DELETE " + common.getCurrentURL() + "/cache/products/" + productId));
                        },
                        common.showAlert);
                }
            };

            // Receive a quantity of product
            this.receiveProduct = function (productId) {
                var val = parseInt(prompt('Enter the number of products to receive', '10'));
                if (isNaN(val) == false) {
                    common.receiveProduct(productId, val, root.refresh, common.showAlert);
                    root.push(common.createNotification("confirmation", "Received Product " + productId,
                        "POST " + common.getCurrentURL() + '/cache/products/' + productId + '/increment(qtyOnHand,' + val + ')'));
                }
            };

            // Increase all prices of products
            this.increaseAllPrices = function () {
                var val = parseFloat(prompt('Enter the price adjustment for all products. (e.g. 1.1 = +%10)', '1.1'));
                if (isNaN(val) == false) {
                    common.increaseAllPrices(val, root.refresh, common.showAlert);
                    root.applicationMessages.push(common.createNotification("confirmation", "Increased all prices",
                        "POST " + common.getCurrentURL() + 'cache/products/price-adjust(' + val + ')'));
                }
            };

            // lifecycle methods

            this.connected = function (info) {
                root.refresh();
            };

            this.transitionCompleted = function (info) {
                root = this;

                // populate department data for dropdown
                common.getDepartmentData(
                    function (data) {  // success
                        if (data.length === undefined || data.length == 0) {
                            alert('Please navigate to the Departments tab to add departments before adding products');
                        } else {
                            var newArray = [];
                            data.forEach(function (data) {
                                newArray.push({
                                    value: data.deptCode,
                                    label: data.name
                                });
                            });
                            root.departmentList(newArray);
                            root.addDefaultProductsDisabled(false);
                        }
                    },
                    function (jqXHR, exception) {
                        console.log('Get for departments failed. ' + exception);
                    }
                );
            };
        }

        return new Page2ViewModel();
    }
);
