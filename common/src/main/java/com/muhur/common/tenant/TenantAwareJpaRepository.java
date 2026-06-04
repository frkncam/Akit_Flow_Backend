package com.muhur.common.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.util.Optional;

public class TenantAwareJpaRepository<T, ID> extends SimpleJpaRepository<T, ID> {

    private final EntityManager em;
    private final Class<T> domainType;

    public TenantAwareJpaRepository(JpaEntityInformation<T, ?> info, EntityManager em) {
        super(info, em);
        this.em = em;
        this.domainType = info.getJavaType();
    }

    @Override
    public Optional<T> findById(ID id) {
        // Non-tenant-scoped entities fall back to standard em.find
        if (!TenantScoped.class.isAssignableFrom(domainType)) {
            return super.findById(id);
        }
        // em.find bypasses @Filter → convert to Criteria query so the filter applies
        CriteriaQuery<T> cq = em.getCriteriaBuilder().createQuery(domainType);
        Root<T> root = cq.from(domainType);
        cq.select(root).where(em.getCriteriaBuilder().equal(root.get("id"), id));
        return em.createQuery(cq).getResultStream().findFirst();
    }
}
