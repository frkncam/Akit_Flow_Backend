package com.muhur.contract.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * String-based Thymeleaf engine used to render contract templates whose
 * source HTML is stored in the database (contract_templates.content).
 *
 * Note: This bean is independent from Spring Boot's default Thymeleaf
 * auto-config (which resolves templates from classpath:/templates/). We
 * inject this engine directly into PdfRenderingService.
 */
@Configuration
public class ThymeleafConfig {

    @Bean
    public StringTemplateResolver stringTemplateResolver() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false); // DB-driven, may change anytime
        return resolver;
    }

    @Bean(name = "stringTemplateEngine")
    public SpringTemplateEngine stringTemplateEngine(StringTemplateResolver stringTemplateResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(stringTemplateResolver);
        return engine;
    }
}
