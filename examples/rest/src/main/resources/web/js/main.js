/**
 * @license
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates.
 * Licensed under The Universal Permissive License (UPL), Version 1.0
 * as shown at https://oss.oracle.com/licenses/upl/
 * @ignore
 */
'use strict';

/**
 * Example of Require.js boostrap javascript
 */

 // The UserAgent is used to detect IE11. Only IE11 requires ES5.
(function () {

  function _ojIsIE11() {
    var nAgt = navigator.userAgent;
    return nAgt.indexOf('MSIE') !== -1 || !!nAgt.match(/Trident.*rv:11./);
  };
  var _ojNeedsES5 = _ojIsIE11();

  requirejs.config(
    {
      baseUrl: 'js',
      waitSeconds: 200,

      paths:
      /* DO NOT MODIFY
      ** All paths are dynamicaly generated from the path_mappings.json file.
      ** Add any new library dependencies in path_mappings json file
      */
// injector:mainReleasePaths

{
  "knockout":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/knockout/knockout-3.5.1.debug",
  "jquery":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/jquery/jquery-3.5.1",
  "jqueryui-amd":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/jquery/jqueryui-amd-1.12.1",
  "promise":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/es6-promise/es6-promise",
  "hammerjs":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/hammer/hammer-2.0.8",
  "ojdnd":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/dnd-polyfill/dnd-polyfill-1.0.2",
  "persist":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/persist/debug",
  "text":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/require/text",
  "signals":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/js-signals/signals",
  "touchr":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/touchr/touchr",
  "regenerator-runtime":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/regenerator-runtime/runtime",
  "corejs":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/corejs/shim",
  "customElements":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/webcomponents/custom-elements.min",
  "proj4":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/proj4js/dist/proj4",
  "css":"https://static.oracle.com/cdn/jet/9.0.0/3rdparty/require-css/css",
  "css-builder":"libs/require-css/css-builder",
  "normalize":"libs/require-css/normalize"
}

// endinjector
    }
  );
}());

/**
 * A top-level require call executed by the Application.
 * Although 'ojcore' and 'knockout' would be loaded in any case (they are specified as dependencies
 * by the modules themselves), we are listing them explicitly to get the references to the 'oj' and 'ko'
 * objects in the callback
 */
require(['ojs/ojbootstrap',  'knockout', 'appController',  'ojs/ojlogger', 'ojs/ojknockout',
  'ojs/ojmodule', 'ojs/ojrouter', 'ojs/ojnavigationlist', 'ojs/ojbutton', 'ojs/ojtoolbar', 'promise',
  'ojs/ojlistview', 'ojs/ojcollectiontabledatasource', 'ojs/ojtable'],
  function (Bootstrap, ko, app, Logger) { // this callback gets executed when all required modules are loaded
    Bootstrap.whenDocumentReady().then(
      function() {

        function init() {
          // Bind your ViewModel for the content of the whole page body.
          ko.applyBindings(app, document.getElementById('globalBody'));
        }

        // If running in a hybrid (e.g. Cordova) environment, we need to wait for the deviceready
        // event before executing any code that might interact with Cordova APIs or plugins.
        if (document.body.classList.contains('oj-hybrid')) {
          document.addEventListener("deviceready", init);
        } else {
          init();
        }
      });
  }
);
