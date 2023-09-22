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

window.allComponents["docFooter"] = {
    init: function(){
        // create a script element
        const scriptElt = document.createElement("script");
        scriptElt.id = "doc-footer";
        scriptElt.type = "text/x-template";
        scriptElt.text =
        `<v-footer app color="white" height="100px" class="main-footer layout wrap">
            <v-layout row grow class="ma-0">
            <v-flex
                v-if="previous.route"
                xs12
                v-bind:class="{ 'xs6': next.route }"
                class="primary pa-0">
                <v-list v-bind:class="[previous.color]" dark class="py-0">
                <v-list-tile :to="previous.route" ripple>
                    <v-icon dark class="mr-5 hidden-xs-only">arrow_back</v-icon>
                    <v-list-tile-content>
                    <v-list-tile-sub-title>Previous</v-list-tile-sub-title>
                    <v-list-tile-title v-text="previous.name"></v-list-tile-title>
                    </v-list-tile-content>
                </v-list-tile>
                </v-list>
            </v-flex>
            <v-flex
                v-if="next.route && next.route !== '*'"
                xs12
                v-bind:class="{ 'xs6': previous.route }"
                class="pa-0">
                <v-list v-bind:class="[next.color]" dark class="py-0">
                <v-list-tile :to="next.route" ripple>
                    <v-list-tile-content>
                    <v-list-tile-sub-title class="text-xs-right">Next</v-list-tile-sub-title>
                    <v-list-tile-title v-text="next.name" class="text-xs-right"></v-list-tile-title>
                    </v-list-tile-content>
                    <v-icon dark class="ml-5 hidden-xs-only">arrow_forward</v-icon>
                </v-list-tile>
                </v-list>
            </v-flex>
            </v-layout>
        </v-footer>`;

        // insert it in the document
        const firstScriptElt = document.getElementsByTagName('script')[0];
        firstScriptElt.parentNode.insertBefore(scriptElt, firstScriptElt);

        Vue.component('docFooter', {
            template: '#doc-footer',
            computed: {
              previous () {
                return this.$store.state.previous;
              },
              next () {
                return this.$store.state.next;
              }
            }
        });
    }
};
