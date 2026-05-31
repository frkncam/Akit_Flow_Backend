package com.akitflow.common.query;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanOperation;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.ListPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Query param string'ini ("title=~söz&status=ACTIVE,DRAFT&value>=1000") QueryDSL Predicate'e çevirir.
 * grid-common PredicateCreator'ının AkitFlow uyarlamasıdır (exception ve enum parse farklı).
 */
@Slf4j
@UtilityClass
public class PredicateCreator {

    private static final String KEYWORD_EQUAL = "=";
    private static final String KEYWORD_NOT_EQUAL = "!=";
    private static final String KEYWORD_AND = "&";
    private static final String KEYWORD_OR = ";";
    private static final String KEYWORD_OR_ATTRIBUTE = ",";
    private static final String KEYWORD_GREATER = ">";
    private static final String KEYWORD_GREATER_OR_EQUAL = ">=";
    private static final String KEYWORD_LOWER = "<";
    private static final String KEYWORD_LOWER_OR_EQUAL = "<=";
    private static final String KEYWORD_LIKE = "=~";
    private static final String PREDICATE_ANY = "any";
    private static final String ASTERISK = "*";

    public static Predicate processRequest(@NotNull MultiValueMap<String, String> queryParams,
                                           @NotNull Class<?> root) {
        EntityPath<?> entityPath = getEntityPath(root);
        Predicate queryParamsPredicate = createQueryParamsPredicate(entityPath, queryParams);
        List<Predicate> predicateList = Objects.isNull(queryParamsPredicate)
                ? new ArrayList<>()
                : Collections.singletonList(queryParamsPredicate);
        return calculatePredicate(predicateList);
    }

    private static Predicate calculatePredicate(List<Predicate> predicateList) {
        if (CollectionUtils.isEmpty(predicateList)) {
            return new BooleanBuilder(); // boş = tümü (tenant filtresi yine de uygulanır)
        }
        Predicate finalPredicate = predicateList.get(0);
        for (int i = 1; i < predicateList.size(); i++) {
            finalPredicate = Expressions.predicate(Ops.AND, finalPredicate, predicateList.get(i));
        }
        return finalPredicate;
    }

    private static Predicate createQueryParamsPredicate(EntityPath<?> entityPath,
                                                        MultiValueMap<String, String> queryParams) {
        if (queryParams.isEmpty()) {
            return null;
        }
        String filterQuery = convertQueryString(queryParams);
        if (filterQuery.isEmpty()) {
            return null;
        }
        return processQuery(filterQuery, entityPath);
    }

    private static Predicate processQuery(@NotNull String filterQuery, @NotNull EntityPath<?> entityPath) {
        Predicate finalPredicate = null;
        String[] andArray = filterQuery.split(KEYWORD_AND);

        for (String andItem : andArray) {
            Predicate currentPredicate = null;
            for (String orItem : andItem.split(KEYWORD_OR)) {
                try {
                    currentPredicate = processOrQuery(orItem, currentPredicate, entityPath);
                } catch (PredicateException e) {
                    throw e;
                } catch (Exception e) {
                    log.warn("PredicateCreator->processQuery: {}", e.getMessage());
                }
            }
            if (finalPredicate == null) {
                finalPredicate = currentPredicate;
            } else if (currentPredicate != null) {
                finalPredicate = Expressions.predicate(Ops.AND, finalPredicate, currentPredicate);
            }
        }
        return finalPredicate;
    }

    private static Predicate processOrQuery(@NotNull String orItem, @Nullable Predicate basePredicate,
                                            @NotNull EntityPath<?> entityPath) {
        Predicate currentPredicate = null;

        if (orItem.contains(KEYWORD_GREATER_OR_EQUAL)) {
            currentPredicate = processAttributeOrQuery(orItem.split(KEYWORD_GREATER_OR_EQUAL), entityPath, Ops.GOE);
        } else if (orItem.contains(KEYWORD_GREATER)) {
            currentPredicate = processAttributeOrQuery(orItem.split(KEYWORD_GREATER), entityPath, Ops.GT);
        } else if (orItem.contains(KEYWORD_LOWER_OR_EQUAL)) {
            currentPredicate = processAttributeOrQuery(orItem.split(KEYWORD_LOWER_OR_EQUAL), entityPath, Ops.LOE);
        } else if (orItem.contains(KEYWORD_LOWER)) {
            currentPredicate = processAttributeOrQuery(orItem.split(KEYWORD_LOWER), entityPath, Ops.LT);
        } else if (orItem.contains(KEYWORD_LIKE)) {
            currentPredicate = processAttributeOrQuery(orItem.split(KEYWORD_LIKE), entityPath, Ops.STRING_CONTAINS_IC);
        } else if (orItem.contains(KEYWORD_NOT_EQUAL)) {
            currentPredicate = processAttributeOrQuery(orItem.split(KEYWORD_NOT_EQUAL), entityPath, Ops.NE);
        } else if (orItem.contains(KEYWORD_EQUAL)) {
            currentPredicate = processAttributeOrQuery(orItem.split(KEYWORD_EQUAL), entityPath, Ops.EQ);
        }

        if (basePredicate == null) {
            return currentPredicate;
        }
        return Expressions.predicate(Ops.OR, basePredicate, currentPredicate);
    }

    private static Predicate processAttributeOrQuery(@NotNull String[] keyValue, @NotNull EntityPath<?> entityPath,
                                                     @NotNull Ops operation) {
        if (keyValue.length < 2) {
            throw new PredicateException("Geçersiz filtre ifadesi.");
        }
        Predicate parentPredicate = null;
        String key = generateFullKey(entityPath.getClass(), keyValue[0]);
        String[] orArray = keyValue[1].split(KEYWORD_OR_ATTRIBUTE);
        Path<?> path = getPath(key, entityPath);

        for (String orItem : orArray) {
            BooleanOperation currentPredicate;
            if (orItem.equals(ASTERISK)) {
                currentPredicate = Expressions.predicate(Ops.IS_NULL, path);
            } else if (path instanceof DateTimePath) {
                LocalDateTime dateTimeValue = LocalDateTime.parse(orItem);
                Expression<LocalDateTime> truncatedPath = Expressions.dateTimeTemplate(
                        LocalDateTime.class, "DATE_TRUNC('minute', {0})", path);
                currentPredicate = Expressions.booleanOperation(operation, truncatedPath,
                        Expressions.constant(dateTimeValue));
            } else {
                currentPredicate = Expressions.booleanOperation(operation, path,
                        Expressions.constant(convertConstant(path, orItem)));
            }
            parentPredicate = (parentPredicate == null)
                    ? currentPredicate
                    : Expressions.predicate(Ops.OR, parentPredicate, currentPredicate);
        }
        return parentPredicate;
    }

    /** Self-referencing alan varsa base path ekler (grid-common ile aynı davranış). */
    private static String generateFullKey(@NotNull Class<?> entityClass, @NotNull String key) {
        String basePath = null;
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getType().equals(entityClass)) {
                basePath = field.getName() + ".";
                break;
            }
        }
        if (StringUtils.hasText(basePath) && !key.contains(basePath)) {
            key = basePath + key;
        }
        return key;
    }

    /** Nokta ile ayrılmış nested alanları Q-type üzerinde reflection ile yürür; koleksiyonlarda .any() kullanır. */
    private static Path<?> getPath(@NotNull String key, @NotNull EntityPath<?> entityPath) {
        Path<?> path = entityPath;
        for (String eachKey : key.split("\\.")) {
            try {
                Field field = path.getClass().getDeclaredField(eachKey);
                if (field.getType().equals(ListPath.class)) {
                    path = (Path<?>) field.getType().getMethod(PREDICATE_ANY).invoke(field.get(path));
                } else {
                    path = (Path<?>) field.get(path);
                }
            } catch (Exception e) {
                throw new PredicateException("Bilinmeyen filtre alanı: " + eachKey);
            }
        }
        return path;
    }

    private static EntityPath<?> getEntityPath(@NotNull Class<?> entityClass) {
        return new SimpleEntityPathResolver("").createPath(entityClass);
    }

    private static String convertQueryString(@NotNull MultiValueMap<String, String> queryParams) {
        String queryString = UriComponentsBuilder.newInstance().queryParams(queryParams).build().toUriString();
        return queryString.isEmpty()
                ? ""
                : URLDecoder.decode(queryString, StandardCharsets.UTF_8).substring(1).replace("'", "");
    }

    private static Object convertConstant(@NotNull Path<?> path, @NotNull String constant) {
        if (path instanceof StringPath) {
            return constant;
        } else if (path instanceof EnumPath) {
            return parseEnum(path, constant);
        } else if (path instanceof DateTimePath) {
            return parseDateTime(path, constant);
        } else if (path instanceof BooleanPath) {
            return Boolean.valueOf(constant);
        } else if (path instanceof NumberPath) {
            return parseNumber(constant);
        } else if (path instanceof ComparablePath) {
            return parseUUID(constant);
        }
        return null;
    }

    private static UUID parseUUID(String constant) {
        try {
            return UUID.fromString(constant);
        } catch (IllegalArgumentException e) {
            throw new PredicateException("Geçersiz UUID: " + constant);
        }
    }

    private static Object parseDateTime(Path<?> path, String constant) {
        if (path.getType().getName().contains("LocalDateTime")) {
            return LocalDateTime.parse(constant);
        }
        return LocalDate.parse(constant);
    }

    private static Object parseNumber(String constant) {
        try {
            return Integer.parseInt(constant);
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(constant);
            } catch (NumberFormatException ex) {
                throw new PredicateException("Geçersiz sayı: " + constant);
            }
        }
    }

    /**
     * AkitFlow uyarlaması: grid-common 'fromValue' static metodunu çağırıyordu; AkitFlow enum'larında o yok.
     * Standart Enum.valueOf kullanılır (büyük/küçük harf duyarlı — frontend tam değer göndermeli).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnum(Path<?> path, String constant) {
        try {
            return Enum.valueOf((Class<Enum>) path.getType(), constant);
        } catch (IllegalArgumentException e) {
            throw new PredicateException(
                    path.getType().getSimpleName() + " için geçersiz değer: " + constant);
        }
    }
}
