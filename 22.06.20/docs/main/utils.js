/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* global superagent, Vue */

function createPageTemplateElt(id, text){
    if(document.getElementById(id) === null){
        const scriptElt = document.createElement("script");
        scriptElt.id = id;
        scriptElt.type = "text/x-template";
        scriptElt.text = text;
        document.body.appendChild(scriptElt);
    }
}

function createPageCustomElt(id, src, onload){
    if(document.getElementById(id) === null){
        const scriptElt = document.createElement("script");
        scriptElt.id = id;
        scriptElt.src = src;
        scriptElt.onload = onload;
        document.body.appendChild(scriptElt);
    }
}

function loadPage(id, targetPath, compDef, customJsPath) {
    if (compDef === undefined) {
        compDef = {};
    }
    compDef.template = "#" + id;
    return Vue.component(id, () => {
        return new Promise(function (resolve, reject) {

            // template exists in the dom
            if (document.getElementById(id) !== null) {
                resolve(compDef);
                return;
            }

            // download the template
            const page = "pages" + targetPath + ".js";
            superagent.get(page).end(function (error, response) {

                // error loading page
                if (error) {
                    reject("load of " + page + " failed" + error);
                    return;
                }

                // resolve template only
                if(customJsPath === undefined){
                    createPageTemplateElt(id, response.text);
                    resolve(compDef);
                    return;
                }

                // resolve template and custom bindings
                createPageCustomElt(id + "_custom", "pages/" + customJsPath, function(){
                    const custom = window.allCustoms[id];
                    if(custom.methods !== undefined){
                        compDef.methods = custom.methods;
                    }
                    if(custom.data !== undefined){
                        compDef.data = custom.data;
                    }
                    createPageTemplateElt(id, response.text);
                    resolve(compDef);
                });
            });
        });
    });
}

function capitalize(src) {
    return `${src.charAt(0).toUpperCase()}${src.slice(1)}`;
}