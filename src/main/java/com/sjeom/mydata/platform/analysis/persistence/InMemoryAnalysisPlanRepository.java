package com.sjeom.mydata.platform.analysis.persistence;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class InMemoryAnalysisPlanRepository implements AnalysisPlanRepository {

    private final Map<PlanKey, StoredAnalysisPlan> plans = new ConcurrentHashMap<>();

    @Override
    public PlanSaveResult saveIfAbsent(StoredAnalysisPlan candidate) {
        PlanKey key = new PlanKey(candidate.planId(), candidate.planVersion(), candidate.dataAsOf());
        AtomicReference<PlanSaveStatus> status = new AtomicReference<>();

        StoredAnalysisPlan stored = plans.compute(key, (ignored, existing) -> {
            if (existing == null) {
                status.set(PlanSaveStatus.SAVED);
                return candidate;
            }
            boolean sameContent = existing.contentHash().equals(candidate.contentHash())
                    && existing.planSnapshotJson().equals(candidate.planSnapshotJson());
            status.set(sameContent ? PlanSaveStatus.ALREADY_EXISTS : PlanSaveStatus.CONFLICT);
            return existing;
        });
        return new PlanSaveResult(status.get(), stored);
    }

    @Override
    public Optional<StoredAnalysisPlan> findByPlanIdVersionAndDataAsOf(
            String planId,
            String planVersion,
            LocalDate dataAsOf
    ) {
        return Optional.ofNullable(plans.get(new PlanKey(planId, planVersion, dataAsOf)));
    }

    private record PlanKey(String planId, String planVersion, LocalDate dataAsOf) {
    }
}
