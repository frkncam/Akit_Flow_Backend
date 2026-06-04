package com.muhur.common.query;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Custom resolver'ı kaydeder ve Spring Data'nın yerleşik QuerydslPredicateArgumentResolver'ını
 * adapter listesinden çıkarır (her ikisi de Predicate tipini yakalamak ister → çakışma önlenir).
 *
 * <p>Adapter, {@link ObjectProvider} ile tembel alınır ve düzenleme tüm singleton'lar kurulduktan
 * sonra ({@link SmartInitializingSingleton}) yapılır — bu, WebMvcConfigurer ↔ RequestMappingHandlerAdapter
 * arasındaki döngüsel bağımlılığı kırar.
 */
@RequiredArgsConstructor
public class CommonPredicateWebConfig implements WebMvcConfigurer, SmartInitializingSingleton {

    private final CommonPredicateResolver commonPredicateResolver;
    private final ObjectProvider<RequestMappingHandlerAdapter> adapterProvider;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(commonPredicateResolver);
    }

    @Override
    public void afterSingletonsInstantiated() {
        RequestMappingHandlerAdapter adapter = adapterProvider.getObject();
        var argumentResolvers = new ArrayList<>(requireNonNull(adapter.getArgumentResolvers()));
        argumentResolvers.removeIf(r -> r instanceof QuerydslPredicateArgumentResolver);
        adapter.setArgumentResolvers(argumentResolvers);
    }
}
