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
public class LibrariesForLibsInPluginsBlock extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final HibernateLibraryAccessors laccForHibernateLibraryAccessors = new HibernateLibraryAccessors(owner);
    private final JunitLibraryAccessors laccForJunitLibraryAccessors = new JunitLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibsInPluginsBlock(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

        /**
         * Creates a dependency provider for actuator (org.springframework.boot:spring-boot-starter-actuator)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getActuator() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("actuator");
    }

        /**
         * Creates a dependency provider for apacheCommons (org.apache.commons:commons-lang3)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getApacheCommons() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("apacheCommons");
    }

        /**
         * Creates a dependency provider for assertj (org.assertj:assertj-core)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getAssertj() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("assertj");
    }

        /**
         * Creates a dependency provider for busRabbit (org.springframework.cloud:spring-cloud-starter-bus-amqp)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getBusRabbit() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("busRabbit");
    }

        /**
         * Creates a dependency provider for common (ir.ebb:common)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getCommon() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("common");
    }

        /**
         * Creates a dependency provider for configClient (org.springframework.cloud:spring-cloud-starter-config)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getConfigClient() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("configClient");
    }

        /**
         * Creates a dependency provider for dataJpa (org.springframework.boot:spring-boot-starter-data-jpa)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getDataJpa() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("dataJpa");
    }

        /**
         * Creates a dependency provider for dataRedis (org.springframework.boot:spring-boot-starter-data-redis)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getDataRedis() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("dataRedis");
    }

        /**
         * Creates a dependency provider for fastjson2 (com.alibaba.fastjson2:fastjson2)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getFastjson2() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("fastjson2");
    }

        /**
         * Creates a dependency provider for grpcNetty (io.grpc:grpc-netty-shaded)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getGrpcNetty() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("grpcNetty");
    }

        /**
         * Creates a dependency provider for grpcProtobuf (io.grpc:grpc-protobuf)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getGrpcProtobuf() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("grpcProtobuf");
    }

        /**
         * Creates a dependency provider for grpcSpringBoot (net.devh:grpc-spring-boot-starter)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getGrpcSpringBoot() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("grpcSpringBoot");
    }

        /**
         * Creates a dependency provider for grpcStub (io.grpc:grpc-stub)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getGrpcStub() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("grpcStub");
    }

        /**
         * Creates a dependency provider for httpClient (org.apache.httpcomponents.client5:httpclient5)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getHttpClient() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("httpClient");
    }

        /**
         * Creates a dependency provider for junitLauncher (org.junit.platform:junit-platform-launcher)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getJunitLauncher() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("junitLauncher");
    }

        /**
         * Creates a dependency provider for jupiter (org.testcontainers:junit-jupiter)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getJupiter() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("jupiter");
    }

        /**
         * Creates a dependency provider for kafka (org.springframework.kafka:spring-kafka)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getKafka() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("kafka");
    }

        /**
         * Creates a dependency provider for liquibase (org.liquibase:liquibase-core)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getLiquibase() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("liquibase");
    }

        /**
         * Creates a dependency provider for lombok (org.projectlombok:lombok)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getLombok() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("lombok");
    }

        /**
         * Creates a dependency provider for oauthResourceServer (org.springframework.boot:spring-boot-starter-oauth2-resource-server)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getOauthResourceServer() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("oauthResourceServer");
    }

        /**
         * Creates a dependency provider for pekko (org.apache.pekko:pekko-actor-typed_3)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getPekko() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("pekko");
    }

        /**
         * Creates a dependency provider for pekkoSlf4j (org.apache.pekko:pekko-slf4j_3)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getPekkoSlf4j() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("pekkoSlf4j");
    }

        /**
         * Creates a dependency provider for postgres (org.postgresql:postgresql)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getPostgres() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("postgres");
    }

        /**
         * Creates a dependency provider for protobuf (com.google.protobuf:protobuf-java)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getProtobuf() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("protobuf");
    }

        /**
         * Creates a dependency provider for quartz (org.springframework.boot:spring-boot-starter-quartz)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getQuartz() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("quartz");
    }

        /**
         * Creates a dependency provider for shedlockJdbcProvider (net.javacrumbs.shedlock:shedlock-provider-jdbc-template)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getShedlockJdbcProvider() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("shedlockJdbcProvider");
    }

        /**
         * Creates a dependency provider for shedlockSpring (net.javacrumbs.shedlock:shedlock-spring)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getShedlockSpring() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("shedlockSpring");
    }

        /**
         * Creates a dependency provider for slerlc (ir.ebb.oms:slerlc)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getSlerlc() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("slerlc");
    }

        /**
         * Creates a dependency provider for springBoot (org.springframework.boot:spring-boot-starter)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getSpringBoot() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("springBoot");
    }

        /**
         * Creates a dependency provider for springRetry (org.springframework.retry:spring-retry)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getSpringRetry() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("springRetry");
    }

        /**
         * Creates a dependency provider for springSecurity (org.springframework.boot:spring-boot-starter-security)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getSpringSecurity() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("springSecurity");
    }

        /**
         * Creates a dependency provider for swagger (org.springdoc:springdoc-openapi-starter-webmvc-ui)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getSwagger() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("swagger");
    }

        /**
         * Creates a dependency provider for test (org.springframework.boot:spring-boot-starter-test)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getTest() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("test");
    }

        /**
         * Creates a dependency provider for testContainers (org.springframework.boot:spring-boot-testcontainers)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getTestContainers() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("testContainers");
    }

        /**
         * Creates a dependency provider for testContainersPostgresql (org.testcontainers:postgresql)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getTestContainersPostgresql() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("testContainersPostgresql");
    }

        /**
         * Creates a dependency provider for uuidCreator (com.github.f4b6a3:uuid-creator)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getUuidCreator() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("uuidCreator");
    }

        /**
         * Creates a dependency provider for validation (org.springframework.boot:spring-boot-starter-validation)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getValidation() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("validation");
    }

        /**
         * Creates a dependency provider for web (org.springframework.boot:spring-boot-starter-web)
         * This dependency was declared in settings file 'settings.gradle'
     * @deprecated Will be removed in Gradle 9.0.
         */
    @Deprecated
        public Provider<MinimalExternalModuleDependency> getWeb() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("web");
    }

    /**
     * Returns the group of libraries at hibernate
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public HibernateLibraryAccessors getHibernate() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
        return laccForHibernateLibraryAccessors;
    }

    /**
     * Returns the group of libraries at junit
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public JunitLibraryAccessors getJunit() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
        return laccForJunitLibraryAccessors;
    }

    /**
     * Returns the group of versions at versions
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Returns the group of bundles at bundles
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public BundleAccessors getBundles() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
        return baccForBundleAccessors;
    }

    /**
     * Returns the group of plugins at plugins
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class HibernateLibraryAccessors extends SubDependencyFactory {

        public HibernateLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for jpamodelgen (org.hibernate.orm:hibernate-jpamodelgen)
             * This dependency was declared in settings file 'settings.gradle'
         * @deprecated Will be removed in Gradle 9.0.
             */
        @Deprecated
            public Provider<MinimalExternalModuleDependency> getJpamodelgen() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
                return create("hibernate.jpamodelgen");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class JunitLibraryAccessors extends SubDependencyFactory {

        public JunitLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for params (org.junit.jupiter:junit-jupiter-params)
             * This dependency was declared in settings file 'settings.gradle'
         * @deprecated Will be removed in Gradle 9.0.
             */
        @Deprecated
            public Provider<MinimalExternalModuleDependency> getParams() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
                return create("junit.params");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

            /**
             * Creates a dependency bundle provider for grpcBundle which is an aggregate for the following dependencies:
             * <ul>
             *    <li>io.grpc:grpc-netty-shaded</li>
             *    <li>io.grpc:grpc-stub</li>
             *    <li>io.grpc:grpc-protobuf</li>
             *    <li>net.devh:grpc-spring-boot-starter</li>
             * </ul>
             * This bundle was declared in settings file 'settings.gradle'
             * @deprecated Will be removed in Gradle 9.0.
             */
            @Deprecated
            public Provider<ExternalModuleDependencyBundle> getGrpcBundle() {
                org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
                return createBundle("grpcBundle");
            }

            /**
             * Creates a dependency bundle provider for postgresBundle which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.springframework.boot:spring-boot-starter-data-jpa</li>
             *    <li>org.postgresql:postgresql</li>
             * </ul>
             * This bundle was declared in settings file 'settings.gradle'
             * @deprecated Will be removed in Gradle 9.0.
             */
            @Deprecated
            public Provider<ExternalModuleDependencyBundle> getPostgresBundle() {
                org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
                return createBundle("postgresBundle");
            }

            /**
             * Creates a dependency bundle provider for security which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.springframework.boot:spring-boot-starter-oauth2-resource-server</li>
             *    <li>org.springframework.boot:spring-boot-starter-security</li>
             * </ul>
             * This bundle was declared in settings file 'settings.gradle'
             * @deprecated Will be removed in Gradle 9.0.
             */
            @Deprecated
            public Provider<ExternalModuleDependencyBundle> getSecurity() {
                org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
                return createBundle("security");
            }

            /**
             * Creates a dependency bundle provider for shedlockBundle which is an aggregate for the following dependencies:
             * <ul>
             *    <li>net.javacrumbs.shedlock:shedlock-spring</li>
             *    <li>net.javacrumbs.shedlock:shedlock-provider-jdbc-template</li>
             * </ul>
             * This bundle was declared in settings file 'settings.gradle'
             * @deprecated Will be removed in Gradle 9.0.
             */
            @Deprecated
            public Provider<ExternalModuleDependencyBundle> getShedlockBundle() {
                org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
                return createBundle("shedlockBundle");
            }

            /**
             * Creates a dependency bundle provider for testBundle which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.springframework.boot:spring-boot-starter-test</li>
             *    <li>org.testcontainers:junit-jupiter</li>
             *    <li>org.junit.platform:junit-platform-launcher</li>
             *    <li>org.springframework.boot:spring-boot-testcontainers</li>
             *    <li>org.junit.jupiter:junit-jupiter-params</li>
             *    <li>org.testcontainers:postgresql</li>
             *    <li>org.assertj:assertj-core</li>
             * </ul>
             * This bundle was declared in settings file 'settings.gradle'
             * @deprecated Will be removed in Gradle 9.0.
             */
            @Deprecated
            public Provider<ExternalModuleDependencyBundle> getTestBundle() {
                org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
                return createBundle("testBundle");
            }

            /**
             * Creates a dependency bundle provider for webBundle which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.springframework.boot:spring-boot-starter-web</li>
             *    <li>org.springdoc:springdoc-openapi-starter-webmvc-ui</li>
             * </ul>
             * This bundle was declared in settings file 'settings.gradle'
             * @deprecated Will be removed in Gradle 9.0.
             */
            @Deprecated
            public Provider<ExternalModuleDependencyBundle> getWebBundle() {
                org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
                return createBundle("webBundle");
            }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

}
