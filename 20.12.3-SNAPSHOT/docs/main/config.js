function createConfig() {
    return {
        home: "docs/about/01_overview",
        release: "20.12.3-SNAPSHOT",
        releases: [
            "20.12.3-SNAPSHOT"
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
            path: '/coherence-jvisualvm/README',
            meta: {
                h1: 'Coherence VisualVM Plugin',
                title: 'Coherence VisualVM Plugin',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('coherence-jvisualvm-README', '/coherence-jvisualvm/README', {})
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
        { header: 'Documentation' },
        {
            title: 'Official Documentation',
            action: 'import_contacts',
            href: 'https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/index.html',
            target: '_blank'
        },
        {
            title: 'About',
            action: 'assistant',
            group: '/about',
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
                { href: '/docs/core/04_portable_types', title: 'Portable Types' }
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
            group: '/coherence-metrics',
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
        },
        { divider: true },
        { header: 'API Documentation' },
        {
            title: 'Coherence CE Java API',
            action: 'library_books',
            href: 'https://oracle.github.io/coherence/20.12.3-SNAPSHOT/api/java/index.html',
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
        },
        { divider: true },
        { header: 'Additional Resources' },
        {
            title: 'Slack',
            action: 'fa-slack',
            href: 'https://join.slack.com/t/oraclecoherence/shared_invite/enQtNzcxNTQwMTAzNjE4LTJkZWI5ZDkzNGEzOTllZDgwZDU3NGM2YjY5YWYwMzM3ODdkNTU2NmNmNDFhOWIxMDZlNjg2MzE3NmMxZWMxMWE',
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