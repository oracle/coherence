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

/* global Vue */

window.allComponents["docView"] = {
    init: function(){
        // create a script element
        const scriptElt = document.createElement("script");
        scriptElt.id = "doc-view";
        scriptElt.type = "text/x-template";
        scriptElt.text =
        `<v-flex xs12 lg11 class="view">
            <slot></slot>
        </v-flex>`;

        // insert it in the document
        const firstScriptElt = document.getElementsByTagName('script')[0];
        firstScriptElt.parentNode.insertBefore(scriptElt, firstScriptElt);

        Vue.component('docView', {
            template: '#doc-view'
        });
    }
};
