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

/* global Vue, lunr, search_index, searchIndex */

window.allComponents['docToolbar'] = {
    init: function(){
        // create a script element
        const scriptElt = document.createElement("script");
        scriptElt.id = "doc-toolbar";
        scriptElt.type = "text/x-template";
        scriptElt.text =
        `<div>
            <v-toolbar fixed dark app :color="$store.state.currentColor" id="main-toolbar">
            <v-toolbar-side-icon @click.stop="toggleSidebar"></v-toolbar-side-icon>
            <v-btn icon large @click="setIsSearching(true)">
                <v-icon>search</v-icon>
            </v-btn>
            <v-slide-x-reverse-transition mode="out-in">
                <v-toolbar-title
                style="position: relative"
                v-bind:key="$store.state.h1" class="d-flex" v-html="$store.state.h1"></v-toolbar-title>
            </v-slide-x-reverse-transition>
            </v-toolbar>
            <v-toolbar :color="$store.state.currentColor"
            fixed
            dark
            app
            :manual-scroll="!$store.state.isSearching"
            ref="toolbar"
            flat>
              <div v-if="$store.state.isSearching" class="search-container">
                <v-text-field
                    placeholder="Search"
                    prepend-icon="search"
                    id="search"
                    clearable
                    single-line
                    solo
                    key="search"
                    v-model="search"
                    ref="search"
                    autocapitalize="off"
                    autocorrect="off"
                    autocomplete="off"
                    spellcheck="false"
                    class="search-input"
                    light>
                </v-text-field>

                <div class="search-overlay" @click="setIsSearching(false)"></div>
                <div class="search-output">
                  <div class="search-result-meta">{{searchMeta}}</div>
                  <div class="search-result">
                    <ol>
                      <li v-for="result in results">
                        <router-link @click.native="setIsSearching(false)"
                                     :to="result.doc.location"
                                     :title="result.doc.title">
                            <article>
                                <h1 v-html="result.doc.h1"></h1>
                                <p v-if="result.doc.text" v-html="result.doc.text"></p>
                            </article>
                        </router-link>
                        <router-link v-for="section in result.sections"
                                     :key="section.location"
                                     @click.native="setIsSearching(false)"
                                     :to="section.location"
                                     :title="section.title">
                            <article>
                                <h1 v-html="section.h1"></h1>
                                <p v-html="section.text"></p>
                            </article>
                        </router-link>
                      </li>
                    </ol>
                  </div>
                </div>
              </div>
            </v-toolbar>
        </div>`;

        // insert it in the document
        const firstScriptElt = document.getElementsByTagName('script')[0];
        firstScriptElt.parentNode.insertBefore(scriptElt, firstScriptElt);

        var messages = {
            placeholder: "Type to start searching",
            none: "No matching documents",
            one: "1 matching document",
            other: "# matching documents"
        };

        const truncate = (string, n) => {
            let i = n;
            if (string.length > i) {
                while (string[i] !== " " && --i > 0)
                    ;
                return `${string.substring(0, i)}...`;
            }
            return string;
        };

        Vue.component('docToolbar', {
            template: '#doc-toolbar',
            data: function () {
                return{
                    results: [],
                    values_: null,
                    index_: null,
                    searchMeta: messages.placeholder,
                    search: ''
                };
            },

            watch: {
                search(val) {
                    /* Abort early, if index is not build or input hasn't changed */
                    if (!this.index_ || val === this.value_)
                        return;

                    this.value_ = val;

                    /* Abort early, if search input is empty */
                    if (this.value_ === null || this.value_.length === 0) {
                        this.results = [];
                        this.searchMeta = messages.placeholder;
                        return;
                    }

                    /* Perform search on index and group sections by document */
                    const result = this.index_

                            /* Append trailing wildcard to all terms for prefix querying */
                            .query(query => {
                                this.value_.toLowerCase().split(" ")
                                        .filter(Boolean)
                                        .forEach(term => {
                                            query.term(term, {wildcard: lunr.Query.wildcard.LEADING | lunr.Query.wildcard.TRAILING});
                                        });
                            })

                            /* Process query results */
                            .reduce((items, item) => {
                                const doc = this.docs_.get(item.ref);
                                //if (doc.parent) {
                                //  const ref = doc.parent.location
                                //  items.set(ref, (items.get(ref) || []).concat(item))
                                //} else {
                                const ref = doc.location;
                                items.set(ref, (items.get(ref) || []));
                                //}
                                return items;
                            }, new Map);

                    /* Assemble regular expressions for matching */
                    const matches = [];
                    this.value_.toLowerCase().split(" ")
                            .filter(Boolean)
                            .forEach(query => {
                                matches.push(new RegExp(`(|${lunr.tokenizer.separator})(${query})`, "img"));
                            });

                    const highlight = (_, separator, token) =>
                            `${separator}<em>${token}</em>`;

                    /* Reset stack and render results */
                    this.results = [];
                    result.forEach((items, ref) => {
                        const doc = this.docs_.get(ref);

                        const entry = {};

                        /* render article */
                        entry.doc = {};
                        entry.doc.location = doc.location;
                        entry.doc.title = doc.title;
                        entry.doc.h1 = doc.title;
                        entry.doc.text = doc.text;
                        matches.forEach(match => {
                            entry.doc.h1 = entry.doc.h1.replace(match, highlight);
                            entry.doc.text = entry.doc.text.replace(match, highlight);
                        });

                        /* render sections */
                        entry.sections = items.map(item => {
                            const section = this.docs_.get(item.ref);
                            const sectionEntry = {};
                            sectionEntry.location = section.location;
                            sectionEntry.title = section.title;
                            sectionEntry.h1 = section.title;
                            sectionEntry.text = truncate(section.text, 400);
                            matches.forEach(match => {
                                sectionEntry.h1 = sectionEntry.h1.replace(match, highlight);
                                sectionEntry.text = sectionEntry.text.replace(match, highlight);
                            });
                            return sectionEntry;
                        });

                        this.results.push(entry);
                    });

                    /* Update search metadata */
                    switch (result.size) {
                        case 0:
                            this.searchMeta = messages.none;
                            break;
                        case 1:
                            this.searchMeta = messages.one;
                            break;
                        default:
                            this.searchMeta = messages.other.replace("#", result.size);
                    }
                }
            },
            mounted() {
                this.init();
            },
            methods: {
                init() {
                    const _this = this;
                    searchIndex.then(function (search_index) {
                        _this.initDocSearch(search_index);
                    }).catch(function (ex){
                        console.error("searchIndex error", ex);
                    });
                },
                setIsSearching(val) {
                    this.$refs.toolbar.isScrolling = !val;
                    this.$store.commit('sitegen/ISSEARCHING', val);
                    if (val) {
                        this.$nextTick(() => {
                            this.$refs.search.focus();
                        });
                    } else {
                        this.search = null;
                    }
                },
                initDocSearch(search_index) {
                    /* Preprocess and index sections and documents */
                    const data = search_index.docs;
                    this.docs_ = data.reduce((docs, doc) => {
                        const [path, hash] = doc.location.split("#");

                        /* Associate section with parent document */
                        if (hash) {
                            doc.parent = docs.get(path);

                            /* Override page title with document title if first section */
                            if (doc.parent && !doc.parent.done) {
                                doc.parent.title = doc.title;
                                doc.parent.text = doc.text;
                                doc.parent.done = true;
                            }
                        }

                        /* Index sections and documents, but skip top-level headline */
                        if (!doc.parent || doc.parent.title !== doc.title)
                            docs.set(doc.location, doc);
                        return docs;
                    }, new Map);

                    /* eslint-disable no-invalid-this */
                    const docs = this.docs_,
                            lang = this.lang_;

                    /* Create stack and index */
                    this.stack_ = [];
                    this.index_ = lunr(function () {
                        const filters = {
                            "search.pipeline.trimmer": lunr.trimmer,
                            "search.pipeline.stopwords": lunr.stopWordFilter
                        };

                        /* Disable stop words filter and trimmer, if desired */
                        const pipeline = Object.keys(filters).reduce((result, name) => {
                            result.push(filters[name]);
                            return result;
                        }, []);

                        /* Remove stemmer, as it cripples search experience */
                        this.pipeline.reset();
                        if (pipeline)
                            this.pipeline.add(...pipeline);

                        /* Index fields */
                        this.field("title", {boost: 10});
                        this.field("text");
                        this.ref("location");

                        /* Index documents */
                        docs.forEach(doc => this.add(doc));
                    });
                },
                toggleSidebar() {
                    this.$store.commit('vuetify/SIDEBAR', !this.$store.state.sidebar);
                }
            }
        });
    }
};
