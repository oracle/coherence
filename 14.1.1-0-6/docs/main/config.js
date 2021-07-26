function createConfig() {
    return {
        home: "docs/about/01_overview",
        release: "14.1.1.0.6",
        releases: [
            "14.1.1.0.6"
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
                description: 'Oracle Coherence documentation',
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
            path: '/coherence-grpc-proxy-client/README',
            meta: {
                h1: 'Coherence gRPC Client',
                title: 'Coherence gRPC Client',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('coherence-grpc-proxy-client-README', '/coherence-grpc-proxy-client/README', {})
        },
        {
            path: '/coherence-cdi/README',
            meta: {
                h1: 'Coherence CDI',
                title: 'Coherence CDI',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('coherence-cdi-README', '/coherence-cdi/README', {})
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
                hasNav: false
            },
            component: loadPage('coherence-mp-README', '/coherence-mp/README', {})
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
                hasNav: false
            },
            component: loadPage('coherence-mp-metrics-README', '/coherence-mp/metrics/README', {})
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
                hasNav: false
            },
            component: loadPage('coherence-grpc-proxy-README', '/coherence-grpc-proxy/README', {})
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
            path: '/coherence-mp/config/README',
            meta: {
                h1: 'Coherence MicroProfile Config',
                title: 'Coherence MicroProfile Config',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('coherence-mp-config-README', '/coherence-mp/config/README', {})
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
        { header: 'Core documentation' },
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
            title: 'Coherence OCI Images',
            action: 'fa-th',
            group: '/coherence-docker',
            items: [
                { href: '/coherence-docker/README', title: 'Coherence OCI Image' }
            ]
        },
        { divider: true },
        { header: 'Documentation' },
        {
            title: 'Official Documentation',
            action: 'import_contacts',
            href: 'https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/index.html',
            target: '_blank'
        },
        {
            title: 'Coherence CE Java API',
            action: 'library_books',
            href: 'https://oracle.github.io/coherence/14.1.1.0/api/java/index.html',
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
        { divider: true },
        { header: 'Additional Resources' },
        {
            title: 'Slack',
            action: 'fa-slack',
            href: 'https://join.slack.com/t/oraclecoherence/shared_invite/enQtNzcxNTQwMTAzNjE4LTJkZWI5ZDkzNGEzOTllZDgwZDU3NGM2YjY5YWYwMzM3ODdkNTU2NmNmNDFhOWIxMDZlNjg2MzE3NmMxZWMxMWE',
            target: '_blank'
        },
        {
            title: 'Coherence Community',
            action: 'people',
            href: 'https://oracle.github.io/coherence/index.html',
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