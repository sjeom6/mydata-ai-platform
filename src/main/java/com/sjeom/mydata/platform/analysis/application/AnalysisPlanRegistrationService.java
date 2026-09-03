package com.sjeom.mydata.platform.analysis.application;

import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import com.sjeom.mydata.platform.analysis.persistence.AnalysisPlanRepository;
import com.sjeom.mydata.platform.analysis.persistence.AnalysisPlanSnapshotFactory;
import com.sjeom.mydata.platform.analysis.persistence.PlanSaveResult;
import com.sjeom.mydata.platform.analysis.persistence.PlanSaveStatus;
import com.sjeom.mydata.platform.analysis.persistence.StoredAnalysisPlan;
import com.sjeom.mydata.platform.analysis.validation.AnalysisPlanValidator;
import com.sjeom.mydata.platform.analysis.validation.PlanValidationError;
import com.sjeom.mydata.platform.analysis.validation.PlanValidationResult;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import java.util.List;
import java.util.Objects;

public final class AnalysisPlanRegistrationService {

    private final AnalysisPlanValidator validator;
    private final AnalysisPlanSnapshotFactory snapshotFactory;
    private final AnalysisPlanRepository repository;

    public AnalysisPlanRegistrationService(
            AnalysisPlanValidator validator,
            AnalysisPlanSnapshotFactory snapshotFactory,
            AnalysisPlanRepository repository
    ) {
        this.validator = Objects.requireNonNull(validator);
        this.snapshotFactory = Objects.requireNonNull(snapshotFactory);
        this.repository = Objects.requireNonNull(repository);
    }

    public PlanRegistrationResult register(AnalysisPlan plan, ToolExecutionContext context) {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(context, "context must not be null");

        PlanValidationResult validation = validator.validate(plan);
        if (!validation.isValid()) {
            List<String> reasonCodes = validation.errors().stream()
                    .map(PlanValidationError::code)
                    .toList();
            return new PlanRegistrationResult(
                    PlanRegistrationStatus.REJECTED,
                    null,
                    reasonCodes
            );
        }

        StoredAnalysisPlan candidate = snapshotFactory.create(plan, context);
        PlanSaveResult saveResult = repository.saveIfAbsent(candidate);
        return switch (saveResult.status()) {
            case SAVED -> new PlanRegistrationResult(
                    PlanRegistrationStatus.REGISTERED,
                    saveResult.storedPlan(),
                    List.of()
            );
            case ALREADY_EXISTS -> new PlanRegistrationResult(
                    PlanRegistrationStatus.ALREADY_EXISTS,
                    saveResult.storedPlan(),
                    List.of()
            );
            case CONFLICT -> new PlanRegistrationResult(
                    PlanRegistrationStatus.CONFLICT,
                    saveResult.storedPlan(),
                    List.of("PLAN_VERSION_CONFLICT")
            );
        };
    }
}
