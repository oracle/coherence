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

window.allComponents["defaultView"] = {
    init: function(){
        // create a script element
        const scriptElt = document.createElement("script");
        scriptElt.id = "default-view";
        scriptElt.type = "text/x-template";
        scriptElt.text =
        `<v-app light>
            <docNav></docNav>
            <docToolbar></docToolbar>
            <v-content>
            <v-container fluid>
                <v-slide-x-reverse-transition mode="out-in" @after-leave="afterLeave">
                <router-view></router-view>
                </v-slide-x-reverse-transition>
            </v-container>
            </v-content>
            <docFooter></docFooter>
        </v-app>`;

        // insert it in the document
        const firstScriptElt = document.getElementsByTagName('script')[0];
        firstScriptElt.parentNode.insertBefore(scriptElt, firstScriptElt);

        window.scrollFix = function (hashtag) {
                var element = document.querySelector(hashtag);
                if(element){
                    var initialY = window.pageYOffset;
                    var elementY = initialY + element.getBoundingClientRect().top;
                    var targetY = document.body.scrollHeight - elementY < window.innerHeight
                        ? document.body.scrollHeight - window.innerHeight
                        : elementY;

                    // offset
                    targetY -= 75;
                    window.scrollTo(0, targetY);
                }
        };

        Vue.component('defaultView', {
            template: '#default-view',
            mounted () {
              if (this.$route.hash) {
                setTimeout(() => scrollFix(this.$route.hash), 2);
              }
            },
            methods: {
                afterLeave () {
                    if (this.$route.hash) {
                      setTimeout(() => scrollFix(this.$route.hash), 2);
                    } else {
                        window.scrollTo(0, 0);
                    }
                }
            }
        });
    }
};
