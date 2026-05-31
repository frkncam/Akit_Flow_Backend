package com.akitflow.common.config;

import com.akitflow.common.scheduler.FailedEventRetryScheduler;
import com.akitflow.common.tenant.TenantFilterAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableScheduling
@EntityScan(basePackages = "com.akitflow.common.domain")
@EnableJpaRepositories(basePackages = "com.akitflow.common.repository")
@ConditionalOnClass(FailedEventRetryScheduler.class)
@Import(FailedEventRetryScheduler.class)
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "org.springframework.security.core.context.SecurityContextHolder")
    public TenantFilterAspect tenantFilterAspect() {
        return new TenantFilterAspect();
    }
}
