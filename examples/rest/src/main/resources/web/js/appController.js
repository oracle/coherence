/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

/*
 * Main controller for rest-examples application.
 */
define(['knockout', 'ojs/ojmodule-element-utils', 'ojs/ojknockouttemplateutils', 'ojs/ojcorerouter', 'ojs/ojmodulerouter-adapter', 'ojs/ojknockoutrouteradapter',
        'ojs/ojurlparamadapter', 'ojs/ojresponsiveutils', 'ojs/ojresponsiveknockoututils', 'ojs/ojarraydataprovider',
        'ojs/ojoffcanvas', 'rest-examples/common', 'ojs/ojmodule-element', 'ojs/ojknockout'],
    function (ko, moduleUtils, KnockoutTemplateUtils, CoreRouter, ModuleRouterAdapter, KnockoutRouterAdapter,
              UrlParamAdapter, ResponsiveUtils, ResponsiveKnockoutUtils, ArrayDataProvider,
              OffcanvasUtils, common) {
        function ControllerViewModel() {
            var self = this;
            this.common = common;
            this.KnockoutTemplateUtils = KnockoutTemplateUtils;

            self.aboutDialogOpen = function () {
                document.getElementById('aboutDialog').open();
            };

            // Handle announcements sent when pages change, for Accessibility.
            this.manner = ko.observable('polite');
            this.message = ko.observable();
            announcementHandler = (event) => {
                this.message(event.detail.message);
                this.manner(event.detail.manner);
            };

            document.getElementById('globalBody').addEventListener('announce', announcementHandler, false);

            // Media queries for responsive layouts
            const smQuery = ResponsiveUtils.getFrameworkQuery(ResponsiveUtils.FRAMEWORK_QUERY_KEY.SM_ONLY);
            this.smScreen = ResponsiveKnockoutUtils.createMediaQueryObservable(smQuery);
            const mdQuery = ResponsiveUtils.getFrameworkQuery(ResponsiveUtils.FRAMEWORK_QUERY_KEY.MD_UP);
            this.mdScreen = ResponsiveKnockoutUtils.createMediaQueryObservable(mdQuery);

            let navData = [
                { path: '', redirect: 'page1' },
                {
                    path: 'page1',
                    detail: {
                        label: 'Departments',
                        iconClass: 'oj-navigationlist-item-icon fa fa-building'
                    }
                },
                {
                    path: 'page2',
                    detail: {
                        label: 'Products',
                        iconClass: 'oj-navigationlist-item-icon fa fa-list'
                    }
                },
                {
                    path: 'page3',
                    detail: {
                        label: 'Contacts',
                        iconClass: 'oj-navigationlist-item-icon fa fa-users'
                    }
                },
                {
                    path: 'page4',
                    detail: {
                        label: 'Server Sent Events',
                        iconClass: 'oj-navigationlist-item-icon fa fa-arrows'
                    }
                },
                {
                    path: 'page5',
                    detail: {
                        label: 'JSON Pass Through',
                        iconClass: 'oj-navigationlist-item-icon fa fa-table'
                    }
                },
                {
                    path: 'page6',
                    detail: {
                        label: 'Binary Pass Through',
                        iconClass: 'oj-navigationlist-item-icon fa fa-upload'
                    }
                }
            ];

            // Router setup
            let router = new CoreRouter(navData, {
                urlAdapter: new UrlParamAdapter()
            });
            router.sync();

            this.moduleAdapter = new ModuleRouterAdapter(router);
            this.selection = new KnockoutRouterAdapter(router);

            // Setup the navDataProvider with the routes, excluding the first redirected
            // route.
            this.navDataProvider = new ArrayDataProvider(navData.slice(1), {keyAttributes: "path"});

            // Drawer
            // Close offcanvas on medium and larger screens
            this.mdScreen.subscribe(() => { OffcanvasUtils.close(this.drawerParams);});
            this.drawerParams = {
                displayMode: 'push',
                selector: '#navDrawer',
                content: '#pageContent'
            };
            // Called by navigation drawer toggle button and after selection of nav drawer item
            this.toggleDrawer = () => {
                this.navDrawerOn = true;
                return OffcanvasUtils.toggle(this.drawerParams);
            };

            // Header
            // Application Name used in Branding Area
            self.appName = ko.observable('Coherence REST Examples');
            // User Info used in Global Navigation area

            // Footer
            function footerLink(name, id, linkTarget) {
                this.name = name;
                this.linkId = id;
                this.linkTarget = linkTarget;
            }

            // Footer
            this.footerLinks = [
                new footerLink('Coherence Community', 'cohCommunity', 'https://coherence.community/'),
                new footerLink('About Oracle', 'aboutOracle', 'http://www.oracle.com/us/corporate/index.html#menu-about'),
                new footerLink('Contact Us', 'contactUs', 'http://www.oracle.com/us/corporate/contact/index.html'),
                new footerLink('Legal Notices', 'legalNotices', 'http://www.oracle.com/us/legal/index.html'),
                new footerLink('Terms Of Use', 'termsOfUse', 'http://www.oracle.com/us/legal/terms/index.html'),
                new footerLink('Your Privacy Rights', 'yourPrivacyRights', 'http://www.oracle.com/us/legal/privacy/index.html')
            ];
        }

        return new ControllerViewModel();
    });
