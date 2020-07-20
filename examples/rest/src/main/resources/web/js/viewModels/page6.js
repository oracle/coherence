/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

/*
 * ViewModel for Binary Pass Through page.
 */
define(['ojs/ojconverter-number', 'rest-examples/common', 'knockout', 'ojs/ojbootstrap', 'ojs/ojarraydataprovider', 'ojs/ojvalidation-base', 'ojs/ojknockout',
        'ojs/ojlabel', 'ojs/ojtable',  'ojs/ojinputtext', 'ojs/ojformlayout', 'ojs/ojmessages', 'ojs/ojfilepicker',
        'ojs/ojbutton', 'ojs/ojdialog', 'ojs/ojvalidation-number'],
    function(NumberConverter, common, ko, Bootstrap, ArrayDataProvider, ValidationBase) {

        function Page6ViewModel() {
            var root = this;

            var maxFileSize   = 50;
            var MB            = 1024 * 1024;
            var MAX_SIZE      = maxFileSize * MB;

            this.deptObservableArray = ko.observableArray([]);
            this.dataprovider        = new ArrayDataProvider(this.deptObservableArray, {keyAttributes: 'key'});
            
            this.editRow = ko.observable();

            // messages related objects
            this.applicationMessages = ko.observableArray([]);
            this.messageAttributes   = common.MESSAGE_ATTRS;

            this.closeMessageHandler = function(event) {
                // Remove from bound observable array
                root.applicationMessages.remove(event.detail.message);
            };

            this.bytesConverter = ValidationBase.Validation.converterFactory("number").createConverter();
            this.formatSize = function(size) {
                if (isNaN(size))
                    size = 0;

                if (size < 1024)
                    return size + ' Bytes';

                size /= 1024;

                if (size < 1024)
                    return size.toFixed(2) + ' KB';

                size /= 1024;

                if (size < 1024)
                    return size.toFixed(2) + ' MB';

                size /= 1024;

                if (size < 1024)
                    return size.toFixed(2) + ' GB';

                size /= 1024;

                return size.toFixed(2) + ' TB';
                };

            this.bytesConverter.format = this.formatSize;

            // called to refresh data from Coherence
            this.refresh = function() {
                common.getStaticData(
                    function(data) {  // success
                        root.deptObservableArray(data);
                        document.getElementById('staticTable').refresh();
                    },
                    function(jqXHR, exception) { console.log('Get for static data failed. ' + exception); }
                );

            };

            // event to fire when file is selected
            this.selectListener = function(event) {
                var file     = event.detail.files[0];
                var fileType = file.type;
                if (file.size > MAX_SIZE) {
                    alert("File " + file.name + " is greater than " + MAX_SIZE + ", bytes. Please choose another");
                }
                else {
                    var fileSize = root.formatSize(file.size);
                    var message  = 'Are you sure you want to upload the file "' + file.name + '", size ' + fileSize + '?';
                    if (window.confirm(message)) {
                        var reader = new FileReader();

                        reader.addEventListener("loadend", function(e) {
                            var uploadData = e.target.result;

                            $.ajax({
                                url:  '/cache/static-content/' + file.name,
                                headers: {'Content-Type' : fileType },
                                type: 'PUT',
                                data: uploadData,
                                processData: false,
                                contentType: fileType
                                })
                                .then( function(response) {
                                    root.refresh();
                                    root.applicationMessages.push(common.createNotification("confirmation", "Uploaded file  " + file.name + ', size=' + fileSize,
                            "PUT " + common.getCurrentURL() + '/cache/static-content/' + file.name));
                                })
                                .catch(function (message) {
                                    if (message) {
                                        alert('Unable to upload file ' + file.name + '. HTTP Error ' + message.status + " - " + message.statusText);
                                    }
                                });
                        });

                        reader.readAsArrayBuffer(file);
                    }
                }
            };

            // Delete an entry
            this.handleDelete = function(event, context) {
                var key = context.row.key;
                var deleteStatic = window.confirm('Are you sure you want to delete entry named ' + key + '?');
                if (deleteStatic) {
                    common.deleteStatic(key,
                   function (response) {
                        root.refresh();
                        root.applicationMessages.push(common.createNotification("confirmation", "Deleted Binary Content " + key,
                            "DELETE " + common.getCurrentURL() + "/cache/static-content/entries/" + key));
                    },
                    common.showAlert);
                }
            };

            this.handleDownload = function(event, context) {
                window.open('/cache/static-content/' + context.key);
            };

            // lifecycle methods

            this.connected = function(info) {
                root.refresh();
            };

            this.transitionCompleted = function(info) {
                 root = this;
            };

        }

        return new Page6ViewModel();
    }
);
