function createConfig() {
    return {
        home: "docs/about/01_overview",
        release: "24.09",
        releases: [
            "24.09"
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
            path: '/docs/about/04_important',
            meta: {
                h1: 'Important Changes in Coherence Community Edition (CE)',
                title: 'Important Changes in Coherence Community Edition (CE)',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-about-04_important', '/docs/about/04_important', {})
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
            path: '/docs/core/02_topics',
            meta: {
                h1: 'Topics Management',
                title: 'Topics Management',
                h1Prefix: null,
                description: 'Coherence Core Improvements',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-02_topics', '/docs/core/02_topics', {})
        },
        {
            path: '/docs/core/03_microprofile_health',
            meta: {
                h1: 'Microprofile Health',
                title: 'Microprofile Health',
                h1Prefix: null,
                description: 'Coherence Core Improvements',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-03_microprofile_health', '/docs/core/03_microprofile_health', {})
        },
        {
            path: '/docs/core/04_gradle',
            meta: {
                h1: 'Gradle POF Plugin',
                title: 'Gradle POF Plugin',
                h1Prefix: null,
                description: 'Coherence Core Improvements',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-04_gradle', '/docs/core/04_gradle', {})
        },
        {
            path: '/docs/core/05_response_caching',
            meta: {
                h1: 'CDI Response Caching',
                title: 'CDI Response Caching',
                h1Prefix: null,
                description: 'Coherence Core Improvements',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-05_response_caching', '/docs/core/05_response_caching', {})
        },
        {
            path: '/docs/core/06_virtual_threads',
            meta: {
                h1: 'Virtual Threads Support',
                title: 'Virtual Threads Support',
                h1Prefix: null,
                description: 'Coherence Core Improvements',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-06_virtual_threads', '/docs/core/06_virtual_threads', {})
        },
        {
            path: '/docs/core/07_sorted_views',
            meta: {
                h1: 'Sorted Views',
                title: 'Sorted Views',
                h1Prefix: null,
                description: 'Coherence Core Improvements',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-07_sorted_views', '/docs/core/07_sorted_views', {})
        },
        {
            path: '/docs/core/08_vector_db',
            meta: {
                h1: 'Vector DB',
                title: 'Vector DB',
                h1Prefix: null,
                description: 'Coherence Core Improvements',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-08_vector_db', '/docs/core/08_vector_db', {})
        },
        {
            path: '/docs/core/09_queues',
            meta: {
                h1: 'Queues',
                title: 'Queues',
                h1Prefix: null,
                description: 'Coherence Core Improvements',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-09_queues', '/docs/core/09_queues', {})
        },
        {
            path: '/docs/core/10_grpc',
            meta: {
                h1: 'Coherence gRPC Server',
                title: 'Coherence gRPC Server',
                h1Prefix: null,
                description: 'Coherence gRPC',
                keywords: 'coherence, java, gRPC, Helidon, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-10_grpc', '/docs/core/10_grpc', {})
        },
        {
            path: '/docs/core/11_otel',
            meta: {
                h1: 'OpenTelemetry Support',
                title: 'OpenTelemetry Support',
                h1Prefix: null,
                description: 'OpenTelemetry Support',
                keywords: 'coherence, java, distrbuted-tracing, opentelemetry, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('docs-core-11_otel', '/docs/core/11_otel', {})
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
            path: '/examples/guides/050-bootstrap/README',
            meta: {
                h1: 'Bootstrap Coherence',
                title: 'Bootstrap Coherence',
                h1Prefix: null,
                description: 'Coherence Guides',
                keywords: 'coherence, java, documentation, guides',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-050-bootstrap-README', '/examples/guides/050-bootstrap/README', {})
        },
        {
            path: '/examples/guides/070-coherence-extend/README',
            meta: {
                h1: 'Coherence*Extend',
                title: 'Coherence*Extend',
                h1Prefix: null,
                description: 'Provides a guide for clients to connect to a Coherence Cluster via Coherence*Extend',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-070-coherence-extend-README', '/examples/guides/070-coherence-extend/README', {})
        },
        {
            path: '/examples/guides/090-health-checks/README',
            meta: {
                h1: 'Health Checks',
                title: 'Health Checks',
                h1Prefix: null,
                description: 'Coherence Guides',
                keywords: 'coherence, java, documentation, guides, health, docker, containers',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-090-health-checks-README', '/examples/guides/090-health-checks/README', {})
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
                h1: 'Querying Caches',
                title: 'Querying Caches',
                h1Prefix: null,
                description: 'Provides a guide for querying Coherence caches',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-110-queries-README', '/examples/guides/110-queries/README', {})
        },
        {
            path: '/examples/guides/120-built-in-aggregators/README',
            meta: {
                h1: 'Built-In Aggregators',
                title: 'Built-In Aggregators',
                h1Prefix: null,
                description: 'Coherence Guides Aggregations',
                keywords: 'coherence, java, documentation, aggregation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-120-built-in-aggregators-README', '/examples/guides/120-built-in-aggregators/README', {})
        },
        {
            path: '/examples/guides/121-custom-aggregators/README',
            meta: {
                h1: 'Custom Aggregators',
                title: 'Custom Aggregators',
                h1Prefix: null,
                description: 'Coherence Guides Aggregations',
                keywords: 'coherence, java, documentation, aggregation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-121-custom-aggregators-README', '/examples/guides/121-custom-aggregators/README', {})
        },
        {
            path: '/examples/guides/124-views/README',
            meta: {
                h1: 'Views',
                title: 'Views',
                h1Prefix: null,
                description: 'Provides a guide for creating views using ContinuousQueryCache',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-124-views-README', '/examples/guides/124-views/README', {})
        },
        {
            path: '/examples/guides/125-streams/README',
            meta: {
                h1: 'Streams',
                title: 'Streams',
                h1Prefix: null,
                description: 'Coherence Guides Streams',
                keywords: 'coherence, java, documentation, streams',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-125-streams-README', '/examples/guides/125-streams/README', {})
        },
        {
            path: '/examples/guides/128-entry-processors/README',
            meta: {
                h1: 'Entry Processors',
                title: 'Entry Processors',
                h1Prefix: null,
                description: 'Provides a guide for creating Entry Processors',
                keywords: 'coherence, java, documentation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-128-entry-processors-README', '/examples/guides/128-entry-processors/README', {})
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
            path: '/examples/guides/140-client-events/README',
            meta: {
                h1: 'Client Events',
                title: 'Client Events',
                h1Prefix: null,
                description: 'Coherence Guides Client Events',
                keywords: 'coherence, java, documentation, client events',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-140-client-events-README', '/examples/guides/140-client-events/README', {})
        },
        {
            path: '/examples/guides/142-server-events/README',
            meta: {
                h1: 'Server-Side Events',
                title: 'Server-Side Events',
                h1Prefix: null,
                description: 'Coherence Guides Client Events',
                keywords: 'coherence, java, documentation, server side events, server events',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-142-server-events-README', '/examples/guides/142-server-events/README', {})
        },
        {
            path: '/examples/guides/145-durable-events/README',
            meta: {
                h1: 'Durable Events',
                title: 'Durable Events',
                h1Prefix: null,
                description: 'Coherence Guides Durable Events',
                keywords: 'coherence, java, documentation, durable events',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-145-durable-events-README', '/examples/guides/145-durable-events/README', {})
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
            path: '/examples/guides/195-bulk-loading-caches/README',
            meta: {
                h1: 'Bulk Loading Caches',
                title: 'Bulk Loading Caches',
                h1Prefix: null,
                description: 'Bulk loading data into caches',
                keywords: 'coherence, java, documentation, guides, data, bulk, db',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-195-bulk-loading-caches-README', '/examples/guides/195-bulk-loading-caches/README', {})
        },
        {
            path: '/examples/guides/200-federation/README',
            meta: {
                h1: 'Federation',
                title: 'Federation',
                h1Prefix: null,
                description: 'Coherence Tutorials',
                keywords: 'coherence, java, documentation, tutorials, federation',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-200-federation-README', '/examples/guides/200-federation/README', {})
        },
        {
            path: '/examples/guides/210-ssl/README',
            meta: {
                h1: 'Securing with SSL',
                title: 'Securing with SSL',
                h1Prefix: null,
                description: 'Coherence Guides Securing With SSL',
                keywords: 'coherence, java, documentation, security, SSL, securing',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-210-ssl-README', '/examples/guides/210-ssl/README', {})
        },
        {
            path: '/examples/guides/220-performance/README',
            meta: {
                h1: 'Performance over Consistency & Availability',
                title: 'Performance over Consistency & Availability',
                h1Prefix: null,
                description: 'Coherence Guides Client Events',
                keywords: 'coherence, java, documentation, consistency, availability,, read-locator, primary, backup, async, backup',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-220-performance-README', '/examples/guides/220-performance/README', {})
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
            path: '/examples/guides/510-executor/README',
            meta: {
                h1: 'The Coherence Executor Service',
                title: 'The Coherence Executor Service',
                h1Prefix: null,
                description: 'Coherence Guides',
                keywords: 'coherence, java, documentation, guides',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-510-executor-README', '/examples/guides/510-executor/README', {})
        },
        {
            path: '/examples/guides/600-response-caching/README',
            meta: {
                h1: 'CDI Response Caching',
                title: 'CDI Response Caching',
                h1Prefix: null,
                description: 'Coherence Guides',
                keywords: 'coherence, java, documentation, guides',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-600-response-caching-README', '/examples/guides/600-response-caching/README', {})
        },
        {
            path: '/examples/guides/905-key-association/README',
            meta: {
                h1: 'Key Association',
                title: 'Key Association',
                h1Prefix: null,
                description: 'Coherence Guides',
                keywords: 'coherence, java, documentation, guides',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-905-key-association-README', '/examples/guides/905-key-association/README', {})
        },
        {
            path: '/examples/guides/906-partition-level-transactions/README',
            meta: {
                h1: 'Partition Level Transactions',
                title: 'Partition Level Transactions',
                h1Prefix: null,
                description: 'Coherence Guides',
                keywords: 'coherence, java, documentation, guides',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-906-partition-level-transactions-README', '/examples/guides/906-partition-level-transactions/README', {})
        },
        {
            path: '/examples/guides/910-multi-cluster-client/README',
            meta: {
                h1: 'Multi-Cluster Client',
                title: 'Multi-Cluster Client',
                h1Prefix: null,
                description: 'Coherence Guides',
                keywords: 'coherence, java, documentation, guides',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-guides-910-multi-cluster-client-README', '/examples/guides/910-multi-cluster-client/README', {})
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
            path: '/examples/tutorials/200-persistence/README',
            meta: {
                h1: 'Persistence',
                title: 'Persistence',
                h1Prefix: null,
                description: 'Coherence Tutorials',
                keywords: 'coherence, java, documentation, tutorials, Persistence',
                customLayout: null,
                hasNav: true
            },
            component: loadPage('examples-tutorials-200-persistence-README', '/examples/tutorials/200-persistence/README', {})
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
            path: '/coherence-mp/health/README',
            meta: {
                h1: 'Coherence MicroProfile Health',
                title: 'Coherence MicroProfile Health',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('coherence-mp-health-README', '/coherence-mp/health/README', {})
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
            path: '/plugins/maven/pof-maven-plugin/README',
            meta: {
                h1: 'POF Maven Plugin',
                title: 'POF Maven Plugin',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('plugins-maven-pof-maven-plugin-README', '/plugins/maven/pof-maven-plugin/README', {})
        },
        {
            path: '/coherence-concurrent/README',
            meta: {
                h1: 'Distributed Concurrency',
                title: 'Distributed Concurrency',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('coherence-concurrent-README', '/coherence-concurrent/README', {})
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
            path: '/coherence-cdi-server/README',
            meta: {
                h1: 'Coherence CDI',
                title: 'Coherence CDI',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('coherence-cdi-server-README', '/coherence-cdi-server/README', {})
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
                hasNav: false
            },
            component: loadPage('coherence-micrometer-README', '/coherence-micrometer/README', {})
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
                hasNav: false
            },
            component: loadPage('coherence-grpc-README', '/coherence-grpc/README', {})
        },
        {
            path: '/plugins/gradle/README',
            meta: {
                h1: 'POF Gradle Plugin',
                title: 'POF Gradle Plugin',
                h1Prefix: null,
                description: null,
                keywords: null,
                customLayout: null,
                hasNav: false
            },
            component: loadPage('plugins-gradle-README', '/plugins/gradle/README', {})
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
            path: '/examples/setup/intellij',
            meta: {
                h1: 'Import a Project Into IntelliJ IDEA',
                title: 'Import a Project Into IntelliJ IDEA',
                h1Prefix: null,
                description: 'Coherence Guides Import Project Into Intellij',
                keywords: 'coherence, import, intellij',
                customLayout: null,
                hasNav: false
            },
            component: loadPage('examples-setup-intellij', '/examples/setup/intellij', {})
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
        {
            groups: [
                {
                    title: 'Documentation',
                    group: '/docs',
                    items: [
                        {
                            title: 'Official Documentation',
                            action: 'import_contacts',
                            href: 'https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/',
                            target: '_blank'
                        },
                        {
                            title: 'About',
                            action: 'assistant',
                            group: '/docs/about',
                            items: [
                                { href: '/docs/about/01_overview', title: 'Overview' },
                                { href: '/docs/about/02_introduction', title: 'Introduction' },
                                { href: '/docs/about/03_quickstart', title: 'Quick Start' },
                                { href: '/docs/about/04_important', title: 'Important Changes in Coherence Community Edition (CE)' }
                            ]
                        },
                        {
                            title: 'Core Improvements',
                            action: 'fa-cubes',
                            group: '/core',
                            items: [
                                { href: '/docs/core/01_overview', title: 'Overview' },
                                { href: '/docs/core/02_topics', title: 'Topics Management' },
                                { href: '/docs/core/03_microprofile_health', title: 'Microprofile Health' },
                                { href: '/docs/core/04_gradle', title: 'Gradle POF Plugin' },
                                { href: '/docs/core/05_response_caching', title: 'CDI Response Caching' },
                                { href: '/docs/core/06_virtual_threads', title: 'Virtual Threads Support' },
                                { href: '/docs/core/07_sorted_views', title: 'Sorted Views' },
                                { href: '/docs/core/08_vector_db', title: 'Vector DB' },
                                { href: '/docs/core/09_queues', title: 'Queues' },
                                { href: '/docs/core/10_grpc', title: 'Coherence gRPC Server' },
                                { href: '/docs/core/11_otel', title: 'OpenTelemetry Support' }
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
                                { href: '/examples/guides/050-bootstrap/README', title: 'Bootstrap Coherence' },
                                { href: '/examples/guides/070-coherence-extend/README', title: 'Coherence*Extend' },
                                { href: '/examples/guides/090-health-checks/README', title: 'Health Checks' },
                                { href: '/examples/guides/100-put-get-remove/README', title: 'Put Get and Remove Operations' },
                                { href: '/examples/guides/110-queries/README', title: 'Querying Caches' },
                                { href: '/examples/guides/120-built-in-aggregators/README', title: 'Built-In Aggregators' },
                                { href: '/examples/guides/121-custom-aggregators/README', title: 'Custom Aggregators' },
                                { href: '/examples/guides/124-views/README', title: 'Views' },
                                { href: '/examples/guides/125-streams/README', title: 'Streams' },
                                { href: '/examples/guides/128-entry-processors/README', title: 'Entry Processors' },
                                { href: '/examples/guides/130-near-caching/README', title: 'Near Caching' },
                                { href: '/examples/guides/140-client-events/README', title: 'Client Events' },
                                { href: '/examples/guides/142-server-events/README', title: 'Server-Side Events' },
                                { href: '/examples/guides/145-durable-events/README', title: 'Durable Events' },
                                { href: '/examples/guides/190-cache-stores/README', title: 'Cache Stores' },
                                { href: '/examples/guides/195-bulk-loading-caches/README', title: 'Bulk Loading Caches' },
                                { href: '/examples/guides/200-federation/README', title: 'Federation' },
                                { href: '/examples/guides/210-ssl/README', title: 'Securing with SSL' },
                                { href: '/examples/guides/220-performance/README', title: 'Performance over Consistency & Availability' },
                                { href: '/examples/guides/460-topics/README', title: 'Topics' },
                                { href: '/examples/guides/510-executor/README', title: 'The Coherence Executor Service' },
                                { href: '/examples/guides/600-response-caching/README', title: 'CDI Response Caching' },
                                { href: '/examples/guides/905-key-association/README', title: 'Key Association' },
                                { href: '/examples/guides/906-partition-level-transactions/README', title: 'Partition Level Transactions' },
                                { href: '/examples/guides/910-multi-cluster-client/README', title: 'Multi-Cluster Client' }
                            ]
                        },
                        {
                            title: 'Tutorials',
                            action: 'fa-graduation-cap',
                            group: '/tutorials',
                            items: [
                                { href: '/examples/tutorials/000-overview', title: 'Tutorials Overview' },
                                { href: '/examples/tutorials/200-persistence/README', title: 'Persistence' },
                                { href: '/examples/tutorials/500-graphql/README', title: 'GraphQL' }
                            ]
                        },
                        {
                            title: 'Miscellaneous',
                            action: 'widgets',
                            group: '/miscellaneous',
                            items: [
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
                            href: 'https://oracle.github.io/coherence/24.09/api/java/index.html',
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