package com.muhur.common.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;

@Aspect
public class TenantFilterAspect {

    public static final String FILTER_NAME = "tenantFilter";
    public static final String PARAM = "organizationId";

    @PersistenceContext
    private EntityManager em;

    /** Before every Spring Data repository call, enable the tenant filter on the active session if a context is present. */
    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void enableTenantFilter() {
        Long org = TenantContext.currentOrganizationId();
        if (org == null) {
            return; // public / event / scheduler → filter stays disabled
        }
        Session session = em.unwrap(Session.class);
        session.enableFilter(FILTER_NAME).setParameter(PARAM, org);
    }
}
