package com.akitflow.common.tenant;

import jakarta.persistence.PrePersist;

public class TenantEntityListener {

    @PrePersist
    public void onCreate(Object entity) {
        if (entity instanceof TenantScoped t && t.getOrganizationId() == null) {
            Long org = TenantContext.currentOrganizationId();
            if (org != null) {
                t.setOrganizationId(org); // event consumer already has org set → will not override
            }
        }
    }
}
