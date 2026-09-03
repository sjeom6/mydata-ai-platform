package com.sjeom.mydata.platform.audit.persistence;

import com.sjeom.mydata.platform.audit.domain.AuditRecord;
import java.util.Optional;
import java.util.UUID;

public interface AuditRecordRepository {

    void save(AuditRecord auditRecord);

    Optional<AuditRecord> findByExecutionId(UUID executionId);
}
