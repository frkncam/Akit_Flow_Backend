package com.muhur.ai.interceptor;

import com.muhur.ai.config.AppProperties;
import com.muhur.ai.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final AppProperties props;
    private final ConcurrentHashMap<Long, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String orgIdHeader = request.getHeader("X-Org-Id");
        if (orgIdHeader == null) {
            return true;
        }

        Long orgId = Long.parseLong(orgIdHeader);
        Bucket bucket = buckets.computeIfAbsent(orgId, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(props.rateLimit().requestsPerHour())
                        .refillIntervally(props.rateLimit().requestsPerHour(),
                                Duration.ofHours(1))
                        .build())
                .build());

        if (bucket.tryConsume(1)) {
            return true;
        }

        log.warn("Rate limit asildi. orgId={}, limit={}/saat", orgId,
                props.rateLimit().requestsPerHour());
        throw new RateLimitExceededException(
                "Saatlik AI istek limitiniz doldu (" + props.rateLimit().requestsPerHour()
                        + " istek/saat). Lutfen daha sonra tekrar deneyin."
        );
    }
}
