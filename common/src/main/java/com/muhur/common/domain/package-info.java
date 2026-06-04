@org.hibernate.annotations.FilterDef(
        name = "tenantFilter",
        parameters = @org.hibernate.annotations.ParamDef(name = "organizationId", type = Long.class),
        defaultCondition = "organization_id = :organizationId"
)
package com.muhur.common.domain;
