/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

/**
 * Common functions for use within the rest-examples project.
 */
define([ 'ojs/ojcore', 'jquery' ],
    function(oj, $) {

    return {

        getCountryData: function(callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/countries.json;sort=name:asc',
                type: 'GET',
                dataType: 'json',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        getStateData: function(callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/states.json;sort=name:asc',
                type: 'GET',
                dataType: 'json',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        createContact: function(firstName, lastName) {
            return $.ajax({
                url:  '/cache/contacts/' + firstName + '_' + lastName + '/random-contact-creator()',
                type: 'POST'
                });
        },

        updateContact: function(key, data, callbackSuccess, callbackError) {
            return $.ajax({
                url:  '/cache/contacts/' + key,
                type: 'PUT',
                data: JSON.stringify(data),
                dataType: 'json',
                contentType: "application/json; charset=utf-8",
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                })
        },

        getContacts: function(url, callbackSuccess, callbackError) {
            $.ajax({
                url:  url,
                type: 'GET',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        getContact: function(firstName, lastName, callbackSuccess, callbackError) {
            var url = '/cache/contacts/' + firstName + "_" + lastName + ".json";
            $.ajax({
                url:  url,
                type: 'GET',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        deleteContact: function(key, callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/contacts/' + key,
                type: 'DELETE',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        getJSONData: function(url, callbackSuccess, callbackError) {
            $.ajax({
                url:  url,
                type: 'GET',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        createJSON: function(key, data, callbackSuccess, callbackError) {
            return $.ajax({
                url:  '/cache/json/' + key + '.json',
                type: 'PUT',
                data: JSON.stringify(data),
                dataType: 'json',
                contentType: "application/json; charset=utf-8"
                });
        },

        incrementAge: function(key, callbackSuccess, callbackError) {
            return $.ajax({
                url:  '/cache/json/' + key + '/increment(age,1)',
                type: 'POST',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        deleteJSON: function(key, callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/json/' + key,
                type: 'DELETE',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        getStaticData: function(callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/static-content/entries',
                type: 'GET',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        deleteStatic: function(key, callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/static-content/' + key,
                type: 'DELETE',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        getDepartmentData: function(callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/departments.json;sort=deptCode:asc',
                type: 'GET',
                dataType: 'json',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        getProductsForDepartment: function(deptCode, callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/products.json?q=deptCode%20is%20%27' + deptCode + '%27',
                type: 'GET',
                dataType: 'json',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        deleteDepartment: function(deptCode, callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/departments/' + deptCode,
                type: 'DELETE',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        deleteProduct: function(productId, callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/products/' + productId,
                type: 'DELETE',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        receiveProduct: function(productId,qty, callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/products/' + productId + '/increment(qtyOnHand,' + qty + ')',
                type: 'POST',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        increaseAllPrices: function(percent, callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/products/price-adjust(' + percent + ')',
                type: 'POST',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        createDepartment: function(deptCode, deptName, callbackSuccess, callbackError) {
            var newDept = '{ "deptCode" : "' + deptCode + '", "name" : "' + deptName + '"}';
            return $.ajax({
                url:  '/cache/departments/' + deptCode,
                type: 'PUT',
                data: newDept,
                dataType: 'json',
                contentType: "application/json; charset=utf-8"
                });
        },

        getProductData: function(callbackSuccess, callbackError) {
            $.ajax({
                url:  '/cache/products.json;sort=productId:asc',
                type: 'GET',
                dataType: 'json',
                error:   function(jqXHR, exception) { callbackError(jqXHR, exception); },
                success: function(data) { callbackSuccess(data) ;}
                });
        },

        createProduct: function(productId, name, price, deptCode, qtyOnHand) {
           var newProduct = '{ "productId" : ' + productId + ', ' +
                          ' "name" : "'      + name      + '", ' +
                          ' "price" : '     + price     + ', ' +
                          ' "deptCode" : "'  + deptCode  + '", ' +
                          ' "qtyOnHand" : ' + qtyOnHand + '  ' +
                          '}';
           return $.ajax({
                url:  '/cache/products/' + productId,
                type: 'PUT',
                data: newProduct,
                dataType: 'json',
                contentType: "application/json; charset=utf-8"
                });
        },

        showAlert: function(message) {
            alert('Error ' + message.status + ', ' + message.data);
        },

        /**
         * Validate JSON string and return true if it is valid JSON.
         */
        isValidJson: function(jsonString) {
           try {
               JSON.parse(jsonString);
               return true;
           }
           catch (e) {
               return false;
           }
        },

        /**
         * Returns a translated message with %1, %2, %3, %4 replaced
         * with param1, param2, param3, param4.
         */
        getMessageWithParams: function(message, param1, param2, param3, param4) {

            if (param1 === undefined) {
                return message;
            }
            else {
                 var msg = message;
                // at least param1 is present
                msg = msg.replace("%1", param1);
                if (param2 !== undefined) {
                    msg = msg.replace("%2", param2);
                }
                if (param3 !== undefined) {
                    msg = msg.replace("%3", param3);
                }
                if (param4 !== undefined) {
                    msg = msg.replace("%4", param4);
                }
            return msg;
            }
        },
        
        /**
         * Returns a formatted byte string with the correct suffix.
         */
        getFormattedBytes: function (size) {
            if (isNaN(size))
                size = 0;

            if (size < 1024)
                return size + ' B';

            size /= 1024;

            if (size < 1024)
                return size.toFixed(0) + ' KB';

            size /= 1024;

            if (size < 1024)
                return size.toFixed(0) + ' MB';

            size /= 1024;

            if (size < 1024)
                return size.toFixed(1) + ' GB';

            size /= 1024;

            return size.toFixed(1) + ' TB';
        }   ,

        // Hide an element
        hideElement: function(name) {
            var element = document.getElementById(name);
            if (element != undefined && element != null) {
                element.style.display = 'none';
            }
        },

        // Show an element
        showElement: function(name) {
            var element = document.getElementById(name);
            if (element != undefined && element != null) {
                element.style.display = 'inline';
            }
        },

        // disable an element
        disableElement: function(name) {
            document.getElementById(name).disabled = true;
        },

        // Enable an element
        enableElement: function(name) {
            document.getElementById(name).disabled = false;
        },

        // number converters
        numberConverter: function(decimalPoints) {
            return oj.Validation.converterFactory('number')
                .createConverter({style:'decimal', decimalFormat:'standard', maximumFractionDigits: decimalPoints});
        },

        /**
         * Creates a notification for oj-message to display.
         */
        createNotification: function(severity, summary, detail) {
            return {
              severity: severity,
              summary:  summary,
              detail:   detail,
              closeAffordance: "defaults",
              autoTimeout: 5000

            };
            //  icon: "/application/ojet/img/tick.png"
        },

        getCurrentURL: function() {
            return window.location.protocol + '//' + window.location.host;
        },

        // constants

        MESSAGE_ATTRS:
             {
                "my": {"vertical" :"top", "horizontal": "right"},
                "at": {"vertical": "top", "horizontal": "right"},
                "of": "#cluster-overview-container"
            }
    }
});

