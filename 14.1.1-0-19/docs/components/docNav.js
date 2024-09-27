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

/* global Vue, navItems, config */

window.allComponents['docNav'] = {
    init: function(){
        // create a script element
        const scriptElt = document.createElement("script");
        scriptElt.id = "doc-nav";
        scriptElt.type = "text/x-template";
        scriptElt.text =
        `<v-navigation-drawer v-model="isActive" fixed dark app>
            <v-toolbar flat dark class="transparent">
            <v-list class="pa-0 vuetify">
                <v-list-tile tag="div">
                <v-list-tile-avatar>
                    <router-link to="/">
                        <div v-if="navLogo" class="navLogo">
                            <img :src="navLogo">
                        </div>
                        <v-icon v-else-if="navIcon">{{ navIcon }}</v-icon>
                    </router-link>
                </v-list-tile-avatar>
                <v-list-tile-content>
                    <v-list-tile-title>{{ navTitle }}</v-list-tile-title>
                    <v-list-tile-sub-title>
                    <v-menu v-if="releases.length > 1">
                        <span flat slot="activator">
                        Version: {{ release === releases[0] ? \`(\${release})\` : release }}
                        <v-icon dark>arrow_drop_down</v-icon>
                        </span>
                        <v-list>
                        <v-list-tile to="/" v-for="(release, i) in releases" v-if="i === 0" v-bind:key="release">
                            <v-list-tile-title>{{ release }}</v-list-tile-title>
                        </v-list-tile>
                        <v-list-tile tag="a" v-else :href="\`/releases/\${release}\`">
                            <v-list-tile-title>{{ release }}</v-list-tile-title>
                        </v-list-tile>
                        </v-list>
                    </v-menu>
                    <span v-else>
                        Version: {{ release === releases[0] ? \`(\${release})\` : release }}
                    </span>
                    </v-list-tile-sub-title>
                </v-list-tile-content>
                </v-list-tile>
            </v-list>
            </v-toolbar>
            <v-divider></v-divider>
            <v-list dense>
            <template v-for="item in items">
                <v-list-group v-if="item.items" v-bind:group="item.group">
                <v-list-tile slot="item" ripple>
                    <v-list-tile-action>
                    <v-icon dark>{{ item.action }}</v-icon>
                    </v-list-tile-action>
                    <v-list-tile-content>
                    <v-list-tile-title>{{ item.title }}</v-list-tile-title>
                    </v-list-tile-content>
                    <v-list-tile-action>
                    <v-icon dark>keyboard_arrow_down</v-icon>
                    </v-list-tile-action>
                </v-list-tile>
                <v-list-tile
                    v-for="subItem in item.items" v-bind:key="subItem.title"
                    v-bind="{ to: !subItem.target ? subItem.href : null,  href: subItem.target && subItem.href }"
                    @click.native="setIsSearching(false)"
                    ripple
                    v-bind:disabled="subItem.disabled"
                    v-bind:target="subItem.target">
                    <v-list-tile-content>
                    <v-list-tile-title>{{ subItem.title }}</v-list-tile-title>
                    </v-list-tile-content>
                    <v-list-tile-action v-if="subItem.action">
                    <v-icon dark :class="[subItem.actionClass || 'success--text']">{{ subItem.action }}</v-icon>
                    </v-list-tile-action>
                </v-list-tile>
                </v-list-group>
                <v-subheader v-else-if="item.header" dark>{{ item.header }}</v-subheader>
                <v-divider v-else-if="item.divider"></v-divider>
                <v-list-tile 
                v-bind="{ to: !item.target ? item.href : null, href: item.target && item.href }"
                @click.native="setIsSearching(false)"
                ripple
                v-bind:disabled="item.disabled"
                v-bind:target="item.target"
                rel="noopener"
                v-else>
                <v-list-tile-action>
                    <v-icon dark>{{ item.action }}</v-icon>
                </v-list-tile-action>
                <v-list-tile-content>
                    <v-list-tile-title>{{ item.title }}</v-list-tile-title>
                </v-list-tile-content>
                <v-list-tile-action v-if="item.subAction">
                    <v-icon dark class="success--text">{{ item.subAction }}</v-icon>
                </v-list-tile-action>
                <v-chip
                    v-else-if="item.chip"
                    label
                    small
                    color="blue lighten-2"
                    text-color="white"
                    class="caption mx-0">{{ item.chip }}</v-chip>
                </v-list-tile>
            </template>
            </v-list>
        </v-navigation-drawer>`;

        // insert it in the document
        const firstScriptElt = document.getElementsByTagName('script')[0];
        firstScriptElt.parentNode.insertBefore(scriptElt, firstScriptElt);

        Vue.component('docNav', {
            template: '#doc-nav',
            data: function () {
                return {
                    navIcon: config.navIcon,
                    navLogo: config.navLogo,
                    navTitle: config.navTitle,
                    release: config.release,
                    releases: config.releases,
                    items: navItems
                };
            },
            computed: {
                filter() {
                    const color = this.$store.state.currentColor;
                    let hue = 0;
                    if (color === 'purple') {
                        hue = 420;
                    } else if (color === 'darken-3 pink') {
                        hue = 480;
                    } else if (color === 'indigo') {
                        hue = 370;
                    } else if (color === 'cyan') {
                        hue = 337;
                    } else if (color === 'teal') {
                        hue = 315;
                    }
                    return {
                        filter: `hue-rotate(${hue}deg)`
                    };
                },
                isActive: {
                    get() {
                        return this.$store.state.sidebar;
                    },
                    set(val) {
                        this.$store.commit('vuetify/SIDEBAR', val);
                    }
                }
            },
            methods: {
                setIsSearching(val) {
                    this.$store.commit('sitegen/ISSEARCHING', val);
                }
            }
        });
    }
};
