package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the `libs` extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

        /**
         * Creates a dependency provider for apacheCommons (org.apache.commons:commons-lang3)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getApacheCommons() {
            return create("apacheCommons");
    }

        /**
         * Creates a dependency provider for assertj (org.assertj:assertj-core)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getAssertj() {
            return create("assertj");
    }

        /**
         * Creates a dependency provider for cronUtils (com.cronutils:cron-utils)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getCronUtils() {
            return create("cronUtils");
    }

        /**
         * Creates a dependency provider for grpcNetty (io.grpc:grpc-netty-shaded)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getGrpcNetty() {
            return create("grpcNetty");
    }

        /**
         * Creates a dependency provider for grpcProtobuf (io.grpc:grpc-protobuf)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getGrpcProtobuf() {
            return create("grpcProtobuf");
    }

        /**
         * Creates a dependency provider for grpcStub (io.grpc:grpc-stub)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getGrpcStub() {
            return create("grpcStub");
    }

        /**
         * Creates a dependency provider for hikari (com.zaxxer:HikariCP)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getHikari() {
            return create("hikari");
    }

        /**
         * Creates a dependency provider for jacksonDatabind (com.fasterxml.jackson.core:jackson-databind)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getJacksonDatabind() {
            return create("jacksonDatabind");
    }

        /**
         * Creates a dependency provider for jacksonJsr310 (com.fasterxml.jackson.datatype:jackson-datatype-jsr310)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getJacksonJsr310() {
            return create("jacksonJsr310");
    }

        /**
         * Creates a dependency provider for junitJupiter (org.junit.jupiter:junit-jupiter)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getJunitJupiter() {
            return create("junitJupiter");
    }

        /**
         * Creates a dependency provider for junitLauncher (org.junit.platform:junit-platform-launcher)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getJunitLauncher() {
            return create("junitLauncher");
    }

        /**
         * Creates a dependency provider for junitParams (org.junit.jupiter:junit-jupiter-params)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getJunitParams() {
            return create("junitParams");
    }

        /**
         * Creates a dependency provider for kafkaClients (org.apache.kafka:kafka-clients)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getKafkaClients() {
            return create("kafkaClients");
    }

        /**
         * Creates a dependency provider for liquibase (org.liquibase:liquibase-core)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getLiquibase() {
            return create("liquibase");
    }

        /**
         * Creates a dependency provider for logback (ch.qos.logback:logback-classic)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getLogback() {
            return create("logback");
    }

        /**
         * Creates a dependency provider for lombok (org.projectlombok:lombok)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getLombok() {
            return create("lombok");
    }

        /**
         * Creates a dependency provider for nimbusJwt (com.nimbusds:nimbus-jose-jwt)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getNimbusJwt() {
            return create("nimbusJwt");
    }

        /**
         * Creates a dependency provider for pekko (org.apache.pekko:pekko-actor-typed_3)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getPekko() {
            return create("pekko");
    }

        /**
         * Creates a dependency provider for pekkoHttpBom (org.apache.pekko:pekko-http-bom_3)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getPekkoHttpBom() {
            return create("pekkoHttpBom");
    }

        /**
         * Creates a dependency provider for pekkoHttpCore (org.apache.pekko:pekko-http-core_3)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getPekkoHttpCore() {
            return create("pekkoHttpCore");
    }

        /**
         * Creates a dependency provider for pekkoHttpJackson (org.apache.pekko:pekko-http-jackson_3)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getPekkoHttpJackson() {
            return create("pekkoHttpJackson");
    }

        /**
         * Creates a dependency provider for pekkoSlf4j (org.apache.pekko:pekko-slf4j_3)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getPekkoSlf4j() {
            return create("pekkoSlf4j");
    }

        /**
         * Creates a dependency provider for pekkoStream (org.apache.pekko:pekko-stream_3)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getPekkoStream() {
            return create("pekkoStream");
    }

        /**
         * Creates a dependency provider for postgres (org.postgresql:postgresql)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getPostgres() {
            return create("postgres");
    }

        /**
         * Creates a dependency provider for protobuf (com.google.protobuf:protobuf-java)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getProtobuf() {
            return create("protobuf");
    }

        /**
         * Creates a dependency provider for testcontainers (org.testcontainers:testcontainers)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getTestcontainers() {
            return create("testcontainers");
    }

        /**
         * Creates a dependency provider for testcontainersJunit (org.testcontainers:junit-jupiter)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getTestcontainersJunit() {
            return create("testcontainersJunit");
    }

        /**
         * Creates a dependency provider for testcontainersPostgresql (org.testcontainers:postgresql)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getTestcontainersPostgresql() {
            return create("testcontainersPostgresql");
    }

        /**
         * Creates a dependency provider for uuidCreator (com.github.f4b6a3:uuid-creator)
         * This dependency was declared in settings file 'settings.gradle'
         */
        public Provider<MinimalExternalModuleDependency> getUuidCreator() {
            return create("uuidCreator");
    }

    /**
     * Returns the group of versions at versions
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Returns the group of bundles at bundles
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Returns the group of plugins at plugins
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class VersionAccessors extends VersionFactory  {

        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Returns the version associated to this alias: grpc (1.76.0)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in settings file 'settings.gradle'
             */
            public Provider<String> getGrpc() { return getVersion("grpc"); }

            /**
             * Returns the version associated to this alias: jackson (2.17.2)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in settings file 'settings.gradle'
             */
            public Provider<String> getJackson() { return getVersion("jackson"); }

            /**
             * Returns the version associated to this alias: junit (5.10.3)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in settings file 'settings.gradle'
             */
            public Provider<String> getJunit() { return getVersion("junit"); }

            /**
             * Returns the version associated to this alias: pekko (1.6.0)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in settings file 'settings.gradle'
             */
            public Provider<String> getPekko() { return getVersion("pekko"); }

            /**
             * Returns the version associated to this alias: testcontainers (1.20.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in settings file 'settings.gradle'
             */
            public Provider<String> getTestcontainers() { return getVersion("testcontainers"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

            /**
             * Creates a dependency bundle provider for grpcBundle which is an aggregate for the following dependencies:
             * <ul>
             *    <li>io.grpc:grpc-netty-shaded</li>
             *    <li>io.grpc:grpc-stub</li>
             *    <li>io.grpc:grpc-protobuf</li>
             * </ul>
             * This bundle was declared in settings file 'settings.gradle'
             */
            public Provider<ExternalModuleDependencyBundle> getGrpcBundle() {
                return createBundle("grpcBundle");
            }

            /**
             * Creates a dependency bundle provider for httpBundle which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.apache.pekko:pekko-http-core_3</li>
             *    <li>org.apache.pekko:pekko-http-bom_3</li>
             *    <li>org.apache.pekko:pekko-http-jackson_3</li>
             * </ul>
             * This bundle was declared in settings file 'settings.gradle'
             */
            public Provider<ExternalModuleDependencyBundle> getHttpBundle() {
                return createBundle("httpBundle");
            }

            /**
             * Creates a dependency bundle provider for jacksonBundle which is an aggregate for the following dependencies:
             * <ul>
             *    <li>com.fasterxml.jackson.core:jackson-databind</li>
             *    <li>com.fasterxml.jackson.datatype:jackson-datatype-jsr310</li>
             * </ul>
             * This bundle was declared in settings file 'settings.gradle'
             */
            public Provider<ExternalModuleDependencyBundle> getJacksonBundle() {
                return createBundle("jacksonBundle");
            }

            /**
             * Creates a dependency bundle provider for pekkoBundle which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.apache.pekko:pekko-actor-typed_3</li>
             *    <li>org.apache.pekko:pekko-slf4j_3</li>
             *    <li>org.apache.pekko:pekko-stream_3</li>
             * </ul>
             * This bundle was declared in settings file 'settings.gradle'
             */
            public Provider<ExternalModuleDependencyBundle> getPekkoBundle() {
                return createBundle("pekkoBundle");
            }

            /**
             * Creates a dependency bundle provider for testBundle which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.junit.jupiter:junit-jupiter</li>
             *    <li>org.junit.jupiter:junit-jupiter-params</li>
             *    <li>org.junit.platform:junit-platform-launcher</li>
             *    <li>org.assertj:assertj-core</li>
             *    <li>org.testcontainers:testcontainers</li>
             *    <li>org.testcontainers:postgresql</li>
             *    <li>org.testcontainers:junit-jupiter</li>
             * </ul>
             * This bundle was declared in settings file 'settings.gradle'
             */
            public Provider<ExternalModuleDependencyBundle> getTestBundle() {
                return createBundle("testBundle");
            }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

}
