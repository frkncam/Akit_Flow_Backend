package com.akitflow.common.query;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromRequest;

import com.querydsl.core.types.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
public class CommonPredicateResolver implements HandlerMethodArgumentResolver {

    private static final List<String> PAGINATION_PARAMS = List.of("offset", "limit", "sort", "paged", "page", "size");

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.hasParameterAnnotation(CommonPredicate.class);
    }

    @Override
    public Predicate resolveArgument(MethodParameter methodParameter,
                                     ModelAndViewContainer modelAndViewContainer,
                                     NativeWebRequest nativeWebRequest,
                                     WebDataBinderFactory webDataBinderFactory) {
        CommonPredicate attr = methodParameter.getParameterAnnotation(CommonPredicate.class);
        if (Objects.isNull(attr)) {
            throw new IllegalStateException("@CommonPredicate annotation bulunamadı.");
        }
        HttpServletRequest request = ((ServletWebRequest) nativeWebRequest).getRequest();
        MultiValueMap<String, String> queryParams = prepareQueryParameters(request);
        return PredicateCreator.processRequest(queryParams, attr.root());
    }

    private static MultiValueMap<String, String> prepareQueryParameters(HttpServletRequest request) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.addAll(fromRequest(request).build().getQueryParams());
        PAGINATION_PARAMS.forEach(queryParams::remove);
        return queryParams;
    }
}
