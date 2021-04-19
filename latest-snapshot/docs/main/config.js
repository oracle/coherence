function createConfig() {
    return {
        home: "docs/about/01_overview",
        release: "21.06-SNAPSHOT",
        releases: [
            "21.06-SNAPSHOT"
        ],
        pathColors: {
            "*": "blue-grey"
        },
        theme: {
            primary: '#1976D2',
            secondary: '#424242',
            accent: '#82B1FF',
            error: '#FF5252',
            info: '#2196F3',
            success: '#4CAF50',
            warning: '#FFC107'
        },
        navTitle: 'Oracle Coherence CE',
        navIcon: null,
        navLogo: 'docs/images/logo.png'
    };
}

function createRoutes(){
    return [
        {
            path: '/docs/about/01_overview',
            meta: {
                h1: 'Overview',
                title: 'Overview',
                h1Prefix: null,
                description: 'Oracle Coherence CE Documentation',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-about-01_overview', '/docs/about/01_overview', {})
        },
        {
            path: '/docs/about/02_introduction',
            meta: {
                h1: 'Introduction',
                title: 'Introduction',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-about-02_introduction', '/docs/about/02_introduction', {})
        },
        {
            path: '/docs/about/03_quickstart',
            meta: {
                h1: 'Quick Start',
                title: 'Quick Start',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-about-03_quickstart', '/docs/about/03_quickstart', {})
        },
        {
            path: '/docs/core/01_overview',
            meta: {
                h1: 'Overview',
                title: 'Overview',
                h1Prefix: null,
                description: 'Coherence Core Improvements',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-01_overview', '/docs/core/01_overview', {})
        },
        {
            path: '/docs/core/02_bootstrap',
            meta: {
                h1: 'Bootstrap API',
                title: 'Bootstrap API',
                h1Prefix: null,
                description: 'Bootstrap a Coherence application',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-02_bootstrap', '/docs/core/02_bootstrap', {})
        },
        {
            path: '/docs/core/03_parallel_recovery',
            meta: {
                h1: 'Parallel Recovery',
                title: 'Parallel Recovery',
                h1Prefix: null,
                description: 'Coherence Recovery in Parallel',
                keywords: 'coherence, persistence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-03_parallel_recovery', '/docs/core/03_parallel_recovery', {})
        },
        {
            path: '/docs/core/04_portable_types',
            meta: {
                h1: 'Portable Types',
                title: 'Portable Types',
                h1Prefix: null,
                description: 'Coherence Portable Types',
                keywords: 'coherence, serialization, pof, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-04_portable_types', '/docs/core/04_portable_types', {})
        },
        {
            path: '/docs/core/05_repository',
            meta: {
                h1: 'Repository API',
                title: 'Repository API',
                h1Prefix: null,
                description: 'Coherence Repository API',
                keywords: 'coherence, DDD, repository, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-05_repository', '/docs/core/05_repository', {})
        },
        {
            path: '/coherence-cdi-server/README',
            meta: {
                h1: 'Coherence CDI',
                title: 'Coherence CDI',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('coherence-cdi-server-README', '/coherence-cdi-server/README', {})
        },
        {
            path: '/coherence-mp/README',
            meta: {
                h1: 'Coherence MP',
                title: 'Coherence MP',
                h1Prefix: null,
                description: 'Oracle Coherence documentation',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('coherence-mp-README', '/coherence-mp/README', {})
        },
        {
            path: '/coherence-mp/config/README',
            meta: {
                h1: 'Coherence MicroProfile Config',
                title: 'Coherence MicroProfile Config',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('coherence-mp-config-README', '/coherence-mp/config/README', {})
        },
        {
            path: '/coherence-mp/metrics/README',
            meta: {
                h1: 'Coherence MicroProfile Metrics',
                title: 'Coherence MicroProfile Metrics',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('coherence-mp-metrics-README', '/coherence-mp/metrics/README', {})
        },
        {
            path: '/coherence-grpc/README',
            meta: {
                h1: 'Coherence gRPC',
                title: 'Coherence gRPC',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('coherence-grpc-README', '/coherence-grpc/README', {})
        },
        {
            path: '/coherence-grpc-proxy/README',
            meta: {
                h1: 'Coherence gRPC Server',
                title: 'Coherence gRPC Server',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('coherence-grpc-proxy-README', '/coherence-grpc-proxy/README', {})
        },
        {
            path: '/coherence-java-client/README',
            meta: {
                h1: 'Coherence Java gRPC Client',
                title: 'Coherence Java gRPC Client',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('coherence-java-client-README', '/coherence-java-client/README', {})
        },
        {
            path: '/coherence-micrometer/README',
            meta: {
                h1: 'Coherence Micrometer Metrics',
                title: 'Coherence Micrometer Metrics',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('coherence-micrometer-README', '/coherence-micrometer/README', {})
        },
        {
            path: '/plugins/maven/pof-maven-plugin/README',
            meta: {
                h1: 'POF Maven Plugin',
                title: 'POF Maven Plugin',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('plugins-maven-pof-maven-plugin-README', '/plugins/maven/pof-maven-plugin/README', {})
        },
        {
            path: '/coherence-docker/README',
            meta: {
                h1: 'Coherence OCI Image',
                title: 'Coherence OCI Image',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('coherence-docker-README', '/coherence-docker/README', {})
        },
        {
            path: '/examples/README',
            meta: {
                h1: 'Guides & Tutorials Overview',
                title: 'Guides & Tutorials Overview',
                h1Prefix: null,
                description: 'Coherence Guides',
                keywords: 'coherence, java, documentation, guides, tutorials',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-README', '/examples/README', {})
        },
        {
            path: '/examples/guides/000-overview',
            meta: {
                h1: 'Guides Overview',
                title: 'Guides Overview',
                h1Prefix: null,
                description: 'Coherence Guides',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-000-overview', '/examples/guides/000-overview', {})
        },
        {
            path: '/examples/guides/100-put-get-remove/README',
            meta: {
                h1: 'Put Get and Remove Operations',
                title: 'Put Get and Remove Operations',
                h1Prefix: null,
                description: 'Coherence Guides Basic NamedMap put, get and remove operations',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-100-put-get-remove-README', '/examples/guides/100-put-get-remove/README', {})
        },
        {
            path: '/examples/guides/110-queries/README',
            meta: {
                h1: 'Querying',
                title: 'Querying',
                h1Prefix: null,
                description: 'Coherence Guides',
                keywords: 'coherence, java, documentation, guides, query',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-110-queries-README', '/examples/guides/110-queries/README', {})
        },
        {
            path: '/examples/guides/130-near-caching/README',
            meta: {
                h1: 'Near Caching',
                title: 'Near Caching',
                h1Prefix: null,
                description: 'Coherence Guides Near Caching',
                keywords: 'coherence, java, documentation, near-cache, near cache',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-130-near-caching-README', '/examples/guides/130-near-caching/README', {})
        },
        {
            path: '/examples/guides/190-cache-stores/README',
            meta: {
                h1: 'Cache Stores',
                title: 'Cache Stores',
                h1Prefix: null,
                description: 'Use and configure Cache Stores within Coherence',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-190-cache-stores-README', '/examples/guides/190-cache-stores/README', {})
        },
        {
            path: '/examples/guides/460-topics/README',
            meta: {
                h1: 'Topics',
                title: 'Topics',
                h1Prefix: null,
                description: 'Coherence Tutorials',
                keywords: 'coherence, java, documentation, tutorials',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-460-topics-README', '/examples/guides/460-topics/README', {})
        },
        {
            path: '/examples/tutorials/000-overview',
            meta: {
                h1: 'Tutorials Overview',
                title: 'Tutorials Overview',
                h1Prefix: null,
                description: 'Coherence Tutorials',
                keywords: 'coherence, java, documentation, tutorials',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-tutorials-000-overview', '/examples/tutorials/000-overview', {})
        },
        {
            path: '/examples/tutorials/500-graphql/README',
            meta: {
                h1: 'GraphQL',
                title: 'GraphQL',
                h1Prefix: null,
                description: 'Coherence Tutorials',
                keywords: 'coherence, java, documentation, tutorials',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-tutorials-500-graphql-README', '/examples/tutorials/500-graphql/README', {})
        },
        {
            path: '/examples/internal/template/README',
            meta: {
                h1: 'Title',
                title: 'Title',
                h1Prefix: null,
                description: 'Coherence Guides',
                keywords: 'coherence, java, documentation, guides',
                customLayout: null,
                hasNav: false
            },
            component: loadPage('examples-internal-template-README', '/examples/internal/template/README', {})
        },
        {
            path: '/examples/internal/includes/simple-build',
            meta: {
                h1: 'Simple Build',
                title: 'Simple Build',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('examples-internal-includes-simple-build', '/examples/internal/includes/simple-build', {})
        },
        {
            path: '/coherence-helidon-client/README',
            meta: {
                h1: 'Coherence Java CDI Client',
                title: 'Coherence Java CDI Client',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('coherence-helidon-client-README', '/coherence-helidon-client/README', {})
        },
        {
            path: '/docs/README',
            meta: {
                h1: 'Coherence Documentation Module',
                title: 'Coherence Documentation Module',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('docs-README', '/docs/README', {})
        },
        {
            path: '/examples/internal/includes/what-you-need',
            meta: {
                h1: 'What You Need',
                title: 'What You Need',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('examples-internal-includes-what-you-need', '/examples/internal/includes/what-you-need', {})
        },
        {
            path: '/', redirect: '/docs/about/01_overview'
        },
        {
            path: '*', redirect: '/'
        }
    ];
}

function createNav(){
    return [
        {
            groups: [
                {
                    title: 'Documentation',
                    group: '/docs',
                    items: [
                        {
                            title: 'Official Documentation',
                            action: 'import_contacts',
                            href: 'https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/index.html',
                            target: '_blank'
                        },
                        {
                            title: 'About',
                            action: 'assistant',
                            group: '/docs/about',
                            items: [
                                { href: '/docs/about/01_overview', title: 'Overview' },
                                { href: '/docs/about/02_introduction', title: 'Introduction' },
                                { href: '/docs/about/03_quickstart', title: 'Quick Start' }
                            ]
                        },
                        {
                            title: 'Core Improvements',
                            action: 'fa-cubes',
                            group: '/core',
                            items: [
                                { href: '/docs/core/01_overview', title: 'Overview' },
                                { href: '/docs/core/02_bootstrap', title: 'Bootstrap API' },
                                { href: '/docs/core/03_parallel_recovery', title: 'Parallel Recovery' },
                                { href: '/docs/core/04_portable_types', title: 'Portable Types' },
                                { href: '/docs/core/05_repository', title: 'Repository API' }
                            ]
                        },
                        {
                            title: 'CDI Support',
                            action: 'extension',
                            group: '/coherence-cdi-server',
                            items: [
                                { href: '/coherence-cdi-server/README', title: 'Coherence CDI' }
                            ]
                        },
                        {
                            title: 'MicroProfile Support',
                            action: 'fa-cogs',
                            group: '/coherence-mp',
                            items: [
                                { href: '/coherence-mp/README', title: 'Coherence MP' },
                                { href: '/coherence-mp/config/README', title: 'Coherence MicroProfile Config' },
                                { href: '/coherence-mp/metrics/README', title: 'Coherence MicroProfile Metrics' }
                            ]
                        },
                        {
                            title: 'Coherence gRPC',
                            action: 'settings_ethernet',
                            group: '/coherence-grpc',
                            items: [
                                { href: '/coherence-grpc/README', title: 'Coherence gRPC' },
                                { href: '/coherence-grpc-proxy/README', title: 'Coherence gRPC Server' },
                                { href: '/coherence-java-client/README', title: 'Coherence Java gRPC Client' }
                            ]
                        },
                        {
                            title: 'Coherence Metrics',
                            action: 'speed',
                            group: '/coherence-micrometer',
                            items: [
                                { href: '/coherence-micrometer/README', title: 'Coherence Micrometer Metrics' }
                            ]
                        },
                        {
                            title: 'Plugins',
                            action: 'fa-plug',
                            group: '/plugins',
                            items: [
                                { href: '/plugins/maven/pof-maven-plugin/README', title: 'POF Maven Plugin' }
                            ]
                        },
                        {
                            title: 'Container Images',
                            action: 'fa-th',
                            group: '/coherence-docker',
                            items: [
                                { href: '/coherence-docker/README', title: 'Coherence OCI Image' }
                            ]
                        }
                    ]
                },
                {
                    title: 'Guides and Tutorials',
                    group: '/examples',
                    items: [
                        {
                            title: 'Overview',
                            action: null,
                            group: '/examples',
                            items: [
                                { href: '/examples/README', title: 'Guides & Tutorials Overview' }
                            ]
                        },
                        {
                            title: 'Guides',
                            action: 'explore',
                            group: '/guides',
                            items: [
                                { href: '/examples/guides/000-overview', title: 'Guides Overview' },
                                { href: '/examples/guides/100-put-get-remove/README', title: 'Put Get and Remove Operations' },
                                { href: '/examples/guides/110-queries/README', title: 'Querying' },
                                { href: '/examples/guides/130-near-caching/README', title: 'Near Caching' },
                                { href: '/examples/guides/190-cache-stores/README', title: 'Cache Stores' },
                                { href: '/examples/guides/460-topics/README', title: 'Topics' }
                            ]
                        },
                        {
                            title: 'Tutorials',
                            action: 'fa-graduation-cap',
                            group: '/tutorials',
                            items: [
                                { href: '/examples/tutorials/000-overview', title: 'Tutorials Overview' },
                                { href: '/examples/tutorials/500-graphql/README', title: 'GraphQL' }
                            ]
                        }
                    ]
                },
                {
                    title: 'API Documentation',
                    group: '/api-docs',
                    items: [
                        {
                            title: 'Coherence CE Java API',
                            action: 'library_books',
                            href: 'https://oracle.github.io/coherence/21.06-SNAPSHOT/api/java/index.html',
                            target: '_blank'
                        },
                        {
                            title: 'Coherence CE .NET Core API',
                            action: 'library_books',
                            href: 'https://coherence.community/14.1.1.0/api/dotnet-core/index.html',
                            target: '_blank'
                        },
                        {
                            title: 'Coherence CE .NET API',
                            action: 'library_books',
                            href: 'https://oracle.github.io/coherence/14.1.1.0/api/dotnet/index.html',
                            target: '_blank'
                        },
                        {
                            title: 'Coherence CE C++ API',
                            action: 'library_books',
                            href: 'https://oracle.github.io/coherence/14.1.1.0/api/cpp/index.html',
                            target: '_blank'
                        },
                        {
                            title: 'Coherence CE JavaScript API',
                            action: 'library_books',
                            href: 'https://oracle.github.io/coherence/20.06/api/js/index.html',
                            target: '_blank'
                        }
                    ]
                },
            ]
        }
        ,{ header: 'Additional Resources' },
        {
            title: 'Slack',
            action: 'fa-slack',
            href: 'https://join.slack.com/t/oraclecoherence/shared_invite/enQtNzcxNTQwMTAzNjE4LTJkZWI5ZDkzNGEzOTllZDgwZDU3NGM2YjY5YWYwMzM3ODdkNTU2NmNmNDFhOWIxMDZlNjg2MzE3NmMxZWMxMWE',
            target: '_blank'
        },
        {
            title: 'Blog',
            action: 'library_books',
            href: 'https://medium.com/oracle-coherence',
            target: '_blank'
        },
        {
            title: 'Web Site',
            action: 'fa-globe',
            href: 'https://coherence.community/',
            target: '_blank'
        },
        {
            title: 'GitHub',
            action: 'fa-github-square',
            href: 'https://github.com/oracle/coherence',
            target: '_blank'
        }
    ];
}