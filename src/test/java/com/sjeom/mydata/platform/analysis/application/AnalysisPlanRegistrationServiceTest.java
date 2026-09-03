package com.sjeom.mydata.platform.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sjeom.mydata.platform.analysis.domain.AnalysisCondition;
import com.sjeom.mydata.platform.analysis.domain.AnalysisPeriod;
import com.sjeom.mydata.platform.analysis.domain.AnalysisPlan;
import com.sjeom.mydata.platform.analysis.domain.ComparisonOperator;
import com.sjeom.mydata.platform.analysis.domain.ConditionMetric;
import com.sjeom.mydata.platform.analysis.domain.PeriodType;
import com.sjeom.mydata.platform.analysis.domain.PolicyStatus;
import com.sjeom.mydata.platform.analysis.domain.ProductMatching;
import com.sjeom.mydata.platform.analysis.domain.ProductType;
import com.sjeom.mydata.platform.analysis.domain.RankingMetric;
import com.sjeom.mydata.platform.analysis.persistence.AnalysisPlanSnapshotFactory;
import com.sjeom.mydata.platform.analysis.persistence.InMemoryAnalysisPlanRepository;
import com.sjeom.mydata.platform.analysis.persistence.StoredAnalysisPlan;
import com.sjeom.mydata.platform.analysis.validation.AnalysisPlanValidator;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalysisPlanRegistrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryAnalysisPlanRepository repository = new InMemoryAnalysisPlanRepository();
    private final AnalysisPlanRegistrationService service = new AnalysisPlanRegistrationService(
            new AnalysisPlanValidator(),
            new AnalysisPlanSnapshotFactory(CLOCK),
            repository
    );

    @Test
    void storesValidatedPlanWithReproducibilityMetadata() {
        PlanRegistrationResult result = service.register(
                plan(new BigDecimal("100000"), PolicyStatus.CURRENTLY_ALLOWED),
                context(LocalDate.of(2026, 9, 3))
        );

        assertThat(result.status()).isEqualTo(PlanRegistrationStatus.REGISTERED);
        StoredAnalysisPlan stored = result.storedPlan();
        assertThat(stored.planId()).isEqualTo("PLAN-20260903-0001");
        assertThat(stored.planVersion()).isEqualTo("1.0");
        assertThat(stored.dataAsOf()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(stored.requesterId()).isEqualTo("business-user");
        assertThat(stored.purpose()).isEqualTo("CARD_RECOMMENDATION");
        assertThat(stored.createdAt()).isEqualTo(NOW);
        assertThat(stored.contentHash()).matches("[0-9a-f]{64}");
        assertThat(stored.planSnapshotJson()).contains("\"segmentCode\":\"COFFEE_HEAVY_USER\"");
        assertThat(repository.findByPlanIdVersionAndDataAsOf(
                stored.planId(),
                stored.planVersion(),
                stored.dataAsOf()
        ))
                .contains(stored);
    }

    @Test
    void treatsSamePlanAndDataDateAsIdempotent() {
        AnalysisPlan plan = plan(new BigDecimal("100000"), PolicyStatus.CURRENTLY_ALLOWED);
        ToolExecutionContext context = context(LocalDate.of(2026, 9, 3));

        PlanRegistrationResult first = service.register(plan, context);
        PlanRegistrationResult second = service.register(plan, context);

        assertThat(first.status()).isEqualTo(PlanRegistrationStatus.REGISTERED);
        assertThat(second.status()).isEqualTo(PlanRegistrationStatus.ALREADY_EXISTS);
        assertThat(second.storedPlan()).isEqualTo(first.storedPlan());
    }

    @Test
    void rejectsChangedContentUnderSamePlanIdAndVersion() {
        service.register(
                plan(new BigDecimal("100000"), PolicyStatus.CURRENTLY_ALLOWED),
                context(LocalDate.of(2026, 9, 3))
        );

        PlanRegistrationResult result = service.register(
                plan(new BigDecimal("150000"), PolicyStatus.CURRENTLY_ALLOWED),
                context(LocalDate.of(2026, 9, 3))
        );

        assertThat(result.status()).isEqualTo(PlanRegistrationStatus.CONFLICT);
        assertThat(result.reasonCodes()).containsExactly("PLAN_VERSION_CONFLICT");
        assertThat(result.storedPlan().plan().conditions().getFirst().value())
                .isEqualByComparingTo("100000");
    }

    @Test
    void rejectsPlanThatDoesNotPassPolicyValidationWithoutSavingIt() {
        PlanRegistrationResult result = service.register(
                plan(new BigDecimal("100000"), PolicyStatus.REVIEW_REQUIRED),
                context(LocalDate.of(2026, 9, 3))
        );

        assertThat(result.status()).isEqualTo(PlanRegistrationStatus.REJECTED);
        assertThat(result.storedPlan()).isNull();
        assertThat(result.reasonCodes()).containsExactly("POLICY_NOT_ALLOWED");
        assertThat(repository.findByPlanIdVersionAndDataAsOf(
                "PLAN-20260903-0001",
                "1.0",
                LocalDate.of(2026, 9, 3)
        )).isEmpty();
    }

    @Test
    void storesSamePlanForDifferentDataDatesAsSeparateSnapshots() {
        PlanRegistrationResult first = service.register(
                plan(new BigDecimal("100000"), PolicyStatus.CURRENTLY_ALLOWED),
                context(LocalDate.of(2026, 9, 3))
        );
        PlanRegistrationResult second = service.register(
                plan(new BigDecimal("100000"), PolicyStatus.CURRENTLY_ALLOWED),
                context(LocalDate.of(2026, 9, 4))
        );

        assertThat(second.status()).isEqualTo(PlanRegistrationStatus.REGISTERED);
        assertThat(second.storedPlan().contentHash()).isNotEqualTo(first.storedPlan().contentHash());
    }

    private static AnalysisPlan plan(BigDecimal threshold, PolicyStatus policyStatus) {
        return new AnalysisPlan(
                "PLAN-20260903-0001",
                "1.0",
                "COFFEE_HEAVY_USER",
                new AnalysisPeriod(PeriodType.RELATIVE_MONTH, 3),
                List.of(new AnalysisCondition(
                        ConsumptionCategory.CAFE,
                        ConditionMetric.MONTHLY_AVG_AMOUNT,
                        ComparisonOperator.GTE,
                        threshold,
                        "KRW"
                )),
                AnalysisPlanValidator.SUPPORTED_TOOL_SEQUENCE,
                new ProductMatching(
                        ProductType.CREDIT_CARD,
                        ConsumptionCategory.CAFE,
                        RankingMetric.EXPECTED_ANNUAL_BENEFIT
                ),
                policyStatus
        );
    }

    private static ToolExecutionContext context(LocalDate dataAsOf) {
        return new ToolExecutionContext(
                UUID.fromString("a12c1de7-ce9f-4a21-b8cc-c66b085d5a8d"),
                "business-user",
                "CARD_RECOMMENDATION",
                NOW,
                dataAsOf,
                100
        );
    }
}
