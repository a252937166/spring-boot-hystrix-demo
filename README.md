# spring-boot-hystrix-demo

参考了大多数文章，大多使用的是spring-cloud的整合方式，如果只是单独使用spring-boot的话，这种方式引用了太多无用的依赖，而且没有明明没有使用spring-cloud，pom中有个spring-cloud开头的依赖，有强迫症的我实在接受不了，所以花了些时间自己研究了一下如何快速简洁地单独整合hystrix。

# maven

```xml
        <dependency>
            <groupId>com.netflix.hystrix</groupId>
            <artifactId>hystrix-javanica</artifactId>
            <version>1.5.2</version>
        </dependency>
```
hystrix-javanica中包含了hystrix-core，并且提供了`@HystrixCommand`等注解，如果只hystrix-core是没法使用`@HystrixCommand`等注解的。

# config

```java
import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HystrixConfig {

    @Bean
    public HystrixCommandAspect hystrixCommandAspect() {
        return new HystrixCommandAspect();
    }
}
```
我们看其源码：

```java

@Aspect
public class HystrixCommandAspect {
    private static final Map<HystrixCommandAspect.HystrixPointcutType, HystrixCommandAspect.MetaHolderFactory> META_HOLDER_FACTORY_MAP;

    public HystrixCommandAspect() {
    }

    @Pointcut("@annotation(com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand)")
    public void hystrixCommandAnnotationPointcut() {
    }

    @Pointcut("@annotation(com.netflix.hystrix.contrib.javanica.annotation.HystrixCollapser)")
    public void hystrixCollapserAnnotationPointcut() {
    }

    @Around("hystrixCommandAnnotationPointcut() || hystrixCollapserAnnotationPointcut()")
    public Object methodsAnnotatedWithHystrixCommand(ProceedingJoinPoint joinPoint) throws Throwable {
...
	}
}

```

其中定义了`HystrixCommand`这个切入点，这就是`hystrix`最简单的原理，在有`@HystrixCommand`注解的方法前后进行增强处理。

至此，第一步整合已经成功了，随便写一个方法测验，返回`fail`。
```java

@HystrixCommand(commandProperties = {
//    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds",value = "5000")
    },fallbackMethod = "fail1"
    )
    public String t() throws InterruptedException {

        Thread.sleep(5000);
        return tService.t();
    }
    private String fail1() {
        System.out.println("fail1");
        return "fail1";
    }
    
```

# 实现可读取配置文件

此时，我们在配置文件中进行设置：

```profile
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds=6000
```

在默认时间`1000ms`之后，还是返回`fail`，说明配置没有生效，但是如果我们直接引用`spring-cloud-starter-netflix-hystrix`依赖的话，配置是生效的，那么`spring-cloud-starter-netflix-hystrix`是如何读取的配置文件，并且修改的默认参数，这个就需要我们阅读源码，并且手动`debug`慢慢研究了。
我大概花了3-4小时的时间，对两种jar包的引用方式作对比。这简单说明一下。
我们的切入口是`HystrixPropertiesCommandDefault`这个类，每个有`HystrixCommand`注解的方法，第一次执行时，会通过这个类初始化配置参数。
设置参数的入口是`HystrixCommandProperties`的`getProperty`方法：

```java
    private static HystrixProperty<Integer> getProperty(String propertyPrefix, HystrixCommandKey key, String instanceProperty, Integer builderOverrideValue, Integer defaultValue) {
        return forInteger()
                .add(propertyPrefix + ".command." + key.name() + "." + instanceProperty, builderOverrideValue)
                .add(propertyPrefix + ".command.default." + instanceProperty, defaultValue)
                .build();
    }

```

然后一步一步debug，最终到`ConcurrentCompositeConfiguration`的`getList()`方法时发现：`spring-cloud-starter-netflix-hystrix`的`configList`比`hystrix-javanica`的`configList`多一个`ConfigurableEnvironmentConfiguration`。

```java
    @Override
    public List getList(String key, List defaultValue)
    {
        List<Object> list = new ArrayList<Object>();

        // add all elements from the first configuration containing the requested key
        Iterator<AbstractConfiguration> it = configList.iterator();
        if (overrideProperties.containsKey(key)) {
            appendListProperty(list, overrideProperties, key);
        }
        ...
	}
```

![enter image description here](https://qiniu.ouyanglol.com/blog/springboot%E6%9C%80%E7%AE%80%E6%96%B9%E5%BC%8F%E6%95%B4%E5%90%88hystrix%E4%BB%A5%E5%8F%8A%E6%A0%B9%E6%8D%AE%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6%E8%AE%BE%E7%BD%AE%E9%BB%98%E8%AE%A4%E5%8F%82%E6%95%B01.png)
<center>图(1)</center>

![enter image description here](https://qiniu.ouyanglol.com/blog/springboot%E6%9C%80%E7%AE%80%E6%96%B9%E5%BC%8F%E6%95%B4%E5%90%88hystrix%E4%BB%A5%E5%8F%8A%E6%A0%B9%E6%8D%AE%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6%E8%AE%BE%E7%BD%AE%E9%BB%98%E8%AE%A4%E5%8F%82%E6%95%B02.png)
<center>图(2)</center>

经查询，`ConfigurableEnvironmentConfiguration`在`spring-cloud-netflix-archaius`，还是有`spring-cloud`的命名，不想直接引入，看其源码后，发现这个bean并不复杂，可以直接复制到我们的项目中，删除掉多余的代码就行了。

ArchaiusAutoConfiguration:
```java
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

```

ConfigurableEnvironmentConfiguration:
```java
/*
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

package com.ouyanglol.hytrixdemo.archaius;

import org.apache.commons.configuration.AbstractConfiguration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EnvironmentConfiguration wrapper class providing further configuration possibilities.
 *
 * @author Spencer Gibb
 */
public class ConfigurableEnvironmentConfiguration extends AbstractConfiguration {

	private final ConfigurableEnvironment environment;

	public ConfigurableEnvironmentConfiguration(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	@Override
	protected void addPropertyDirect(String key, Object value) {

	}

	@Override
	public boolean isEmpty() {
		return !getKeys().hasNext(); // TODO: find a better way to do this
	}

	@Override
	public boolean containsKey(String key) {
		return this.environment.containsProperty(key);
	}

	@Override
	public Object getProperty(String key) {
		return this.environment.getProperty(key);
	}

	@Override
	public Iterator<String> getKeys() {
		List<String> result = new ArrayList<>();
		for (Map.Entry<String, PropertySource<?>> entry : getPropertySources()
				.entrySet()) {
			PropertySource<?> source = entry.getValue();
			if (source instanceof EnumerablePropertySource) {
				EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
				for (String name : enumerable.getPropertyNames()) {
					result.add(name);
				}
			}
		}
		return result.iterator();
	}

	private Map<String, PropertySource<?>> getPropertySources() {
		Map<String, PropertySource<?>> map = new LinkedHashMap<>();
		MutablePropertySources sources = (this.environment != null
				? this.environment.getPropertySources()
				: new StandardEnvironment().getPropertySources());
		for (PropertySource<?> source : sources) {
			extract("", map, source);
		}
		return map;
	}

	private void extract(String root, Map<String, PropertySource<?>> map,
			PropertySource<?> source) {
		if (source instanceof CompositePropertySource) {
			for (PropertySource<?> nest : ((CompositePropertySource) source)
					.getPropertySources()) {
				extract(source.getName() + ":", map, nest);
			}
		}
		else {
			map.put(root + source.getName(), source);
		}
	}

}

```

至此，配置文件中的hystrix的相关配置就生效了。
