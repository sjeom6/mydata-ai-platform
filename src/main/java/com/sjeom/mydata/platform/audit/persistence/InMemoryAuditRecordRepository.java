package com.sjeom.mydata.platform.audit.persistence;

import com.sjeom.mydata.platform.audit.domain.AuditRecord;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAuditRecordRepository implements AuditRecordRepository {

    private final Map<UUID, AuditRecord> records = new ConcurrentHashMap<>();

    @Override
    public void save(AuditRecord auditRecord) {
        AuditRecord existing = records.putIfAbsent(auditRecord.executionId(), auditRecord);
        if (existing != null) {
            throw new IllegalStateException("audit record already exists for executionId");
        }
    }

    @Override
    public Optional<AuditRecord> findByExecutionId(UUID executionId) {
        return Optional.ofNullable(records.get(executionId));
    }
}
