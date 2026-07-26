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

/* global Vue, releases, Vuex, Vuetify, superagent */

const config = createConfig();
const navItems = createNav();
const searchIndex = new Promise(function(resolve, reject){
    superagent.get("main/search-index.json").end(function (error, response) {
      if(error){
        reject("unable to load search index: " + error);
        return;
      }
      resolve(JSON.parse(response.text));
    });
});

function main() {

    // add components
    for (var compKey in window.allComponents) {
        comp = window.allComponents[compKey];
        comp.init();
    }

    Vuetify.default(window.Vue, {
        theme: config.theme
    });

    Vue.use(Vuex);
    const store = new Vuex.Store({
        state: {
            isSearching: false,
            h1: null,
            sidebar: null,
            currentColor: 'transparent',
            previous: {},
            next: {},
            navGroup: null,
        },
        actions: {},
        mutations: {
            'sitegen/ISSEARCHING'(state, payload){
                state.isSearching = payload;
            },
            'vuetify/COLOR'(state, payload) {
                state.currentColor = payload;
            },
            'vuetify/PREVIOUS'(state, payload) {
                state.previous = payload;
            },
            'vuetify/NEXT'(state, payload) {
                state.next = payload;
            },
            'vuetify/SIDEBAR'(state, payload) {
                state.sidebar = payload;
            },
            'vuetify/H1'(state, payload) {
                state.h1 = payload;
            },
            'vuetify/NAVGROUP'(state, payload) {
                state.navGroup = payload;
            }
        },
        getters: {}
    });

    const router = new VueRouter({ routes: createRoutes() });
    sync(store, router);

    new Vue({
        router,
        store
    }).$mount("#app");
}
