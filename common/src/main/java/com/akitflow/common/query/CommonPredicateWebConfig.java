package com.akitflow.common.query;

import static java.util.Objects.requireNonNull;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Custom resolver'ı kaydeder ve Spring Data'nın yerleşik QuerydslPredicateArgumentResolver'ını
 * adapter listesinden çıkarır (her ikisi de Predicate tipini yakalamak ister → çakışma önlenir).
 */
@RequiredArgsConstructor
public class CommonPredicateWebConfig implements WebMvcConfigurer {

    private final CommonPredicateResolver commonPredicateResolver;
    private RequestMappingHandlerAdapter adapter;

    @Autowired
    public void setAdapter(RequestMappingHandlerAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(commonPredicateResolver);
    }

    @PostConstruct
    public void removeDefaultQuerydslResolver() {
        var argumentResolvers = new ArrayList<>(requireNonNull(this.adapter.getArgumentResolvers()));
        argumentResolvers.removeIf(r -> r instanceof QuerydslPredicateArgumentResolver);
        this.adapter.setArgumentResolvers(argumentResolvers);
    }
}
