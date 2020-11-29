package com.ouyanglol.hytrixdemo.archaius;/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicURLConfiguration;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PreDestroy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


@Lazy(false)
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ConcurrentCompositeConfiguration.class,
        ConfigurationBuilder.class})
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class ArchaiusAutoConfiguration {

    private static final Log log = LogFactory.getLog(ArchaiusAutoConfiguration.class);

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    private static DynamicURLConfiguration defaultURLConfig;

    @PreDestroy
    public void close() {
        if (defaultURLConfig != null) {
            defaultURLConfig.stopLoading();
        }
        setStatic(ConfigurationManager.class, "instance", null);
        setStatic(ConfigurationManager.class, "customConfigurationInstalled", false);
        setStatic(DynamicPropertyFactory.class, "config", null);
        setStatic(DynamicPropertyFactory.class, "initializedWithDefaultConfig", false);
        setStatic(DynamicProperty.class, "dynamicPropertySupportImpl", null);
        initialized.compareAndSet(true, false);
    }

    @Bean
    public static ConfigurableEnvironmentConfiguration configurableEnvironmentConfiguration(ConfigurableEnvironment env, ApplicationContext context) {
        Map<String, AbstractConfiguration> abstractConfigurationMap = context.getBeansOfType(AbstractConfiguration.class);
        List<AbstractConfiguration> externalConfigurations = new ArrayList<>(abstractConfigurationMap.values());
        ConfigurableEnvironmentConfiguration envConfig = new ConfigurableEnvironmentConfiguration(env);
        configureArchaius(envConfig, env, externalConfigurations);
        return envConfig;
    }

    protected static void configureArchaius(ConfigurableEnvironmentConfiguration envConfig, ConfigurableEnvironment env, List<AbstractConfiguration> externalConfigurations) {
        if (initialized.compareAndSet(false, true)) {
            ConcurrentCompositeConfiguration config = new ConcurrentCompositeConfiguration();
            if (externalConfigurations != null) {
                for (AbstractConfiguration externalConfig : externalConfigurations) {
                    config.addConfiguration(externalConfig);
                }
            }
            config.addConfiguration(envConfig,
                    ConfigurableEnvironmentConfiguration.class.getSimpleName());

            defaultURLConfig = new DynamicURLConfiguration();

            addArchaiusConfiguration(config);
        } else {
            // TODO: reinstall ConfigurationManager
            log.warn(
                    "Netflix ConfigurationManager has already been installed, unable to re-install");
        }
    }

    private static void addArchaiusConfiguration(
            ConcurrentCompositeConfiguration config) {
        if (ConfigurationManager.isConfigurationInstalled()) {
            AbstractConfiguration installedConfiguration = ConfigurationManager
                    .getConfigInstance();
            if (installedConfiguration instanceof ConcurrentCompositeConfiguration) {
                ConcurrentCompositeConfiguration configInstance = (ConcurrentCompositeConfiguration) installedConfiguration;
                configInstance.addConfiguration(config);
            } else {
                installedConfiguration.append(config);
                if (!(installedConfiguration instanceof AggregatedConfiguration)) {
                    log.warn(
                            "Appending a configuration to an existing non-aggregated installed configuration will have no effect");
                }
            }
        } else {
            ConfigurationManager.install(config);
        }
    }

    private static void setStatic(Class<?> type, String name, Object value) {
        // Hack a private static field
        Field field = ReflectionUtils.findField(type, name);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, null, value);
    }


    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(value = "archaius.propagate.environmentChangedEvent",
            matchIfMissing = true)
    protected static class PropagateEventsConfiguration {

        @Autowired
        private Environment env;

    }

}
