function createConfig() {
    return {
        home: "docs/about/01_overview",
        release: "20.06",
        releases: [
            "20.06"
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
                h1: 'Coherence Java Client',
                title: 'Coherence Java Client',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('coherence-java-client-README', '/coherence-java-client/README', {})
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
            title: 'CDI',
            action: 'extension',
            group: '/coherence-cdi-server',
            items: [
                { href: '/coherence-cdi-server/README', title: 'Coherence CDI' }
            ]
        },
        {
            title: 'Microprofile',
            action: 'widgets',
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
                { href: '/coherence-java-client/README', title: 'Coherence Java Client' }
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
            href: 'https://oracle.github.io/coherence/20.06/api/java/index.html',
            target: '_blank'
        },
        {
            title: 'Coherence CE .Net API',
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