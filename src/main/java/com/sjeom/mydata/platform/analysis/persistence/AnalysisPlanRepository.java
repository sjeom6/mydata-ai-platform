package com.sjeom.mydata.platform.analysis.persistence;

import java.time.LocalDate;
import java.util.Optional;

public interface AnalysisPlanRepository {

    PlanSaveResult saveIfAbsent(StoredAnalysisPlan plan);

    Optional<StoredAnalysisPlan> findByPlanIdVersionAndDataAsOf(
            String planId,
            String planVersion,
            LocalDate dataAsOf
    );
}
