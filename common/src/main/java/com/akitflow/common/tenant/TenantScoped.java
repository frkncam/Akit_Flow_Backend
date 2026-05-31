package com.akitflow.common.tenant;

public interface TenantScoped {
    Long getOrganizationId();
    void setOrganizationId(Long organizationId);
}
