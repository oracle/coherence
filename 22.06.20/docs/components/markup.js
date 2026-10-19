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

/* global Vue, hljs */

window.allComponents["markup"] = {
    init: function(){
        // create a script element
        const scriptElt = document.createElement("script");
        scriptElt.id = "markup";
        scriptElt.type = "text/x-template";
        scriptElt.text =
        `<div class="markup-container">
            <div v-if="title" class="block-title"><span v-html="title"></span></div>
            <div class="markup" v-bind:data-lang="lang">
               <pre><code v-bind:class="lang" ref="markup"><slot/></code></pre>
               <div class="markup__copy">
                   <v-icon v-on:click="copyMarkup">content_copy</v-icon>
                   <v-slide-x-reverse-transition mode="out-in">
                   <span class="markup__copy__message"
                         :class="{'markup__copy__message--active': copied}">Copied</span>
                   </v-slide-x-reverse-transition>
               </div>
            </div>
        </div>`;

        // insert it in the document
        const firstScriptElt = document.getElementsByTagName('script')[0];
        firstScriptElt.parentNode.insertBefore(scriptElt, firstScriptElt);

        Vue.component('markup', {
            template: '#markup',
            data: function() {
              return {
                copied: false,
                content: ''
              };
            },
            props: {
              lang: {
                type: String,
                required: false
              },
              title: {
                type: String,
                required: false
              }
            },
            mounted: function () {
              hljs.highlightBlock(this.$refs.markup);
            },
            methods: {
              copyMarkup () {
                const markup = this.$refs.markup;
                markup.setAttribute('contenteditable', 'true');
                markup.focus();
                document.execCommand('selectAll', false, null);
                this.copied = document.execCommand('copy');
                markup.removeAttribute('contenteditable');
                setTimeout(() => this.copied = false, 2000);
              }
            }
        });
    }
};
