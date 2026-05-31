package com.akitflow.common.tenant;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;

/**
 * Base class for tenant-scoped entities. Carries the shared tenant filter and the
 * auto-set entity listener so they don't have to be repeated on every entity.
 *
 * <p>No {@code organizationId} field is declared here on purpose — it stays on each
 * subclass. The {@link Filter} condition (defined via {@code @FilterDef} in
 * {@code com.akitflow.common.domain}) targets the SQL column {@code organization_id},
 * so it works regardless of where the Java field is declared.
 *
 * <p>Subclasses must still {@code implements TenantScoped} (they hold the field and
 * getters/setters); the {@link TenantEntityListener} relies on that interface.
 */
@MappedSuperclass
@Filter(name = "tenantFilter")
@EntityListeners(TenantEntityListener.class)
public abstract class TenantScopedEntity {
}
