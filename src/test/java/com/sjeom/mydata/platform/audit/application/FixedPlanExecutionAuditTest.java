package com.sjeom.mydata.platform.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sjeom.mydata.platform.analysis.application.AnalysisExecutionStatus;
import com.sjeom.mydata.platform.analysis.application.FixedPlanExecutionService;
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
import com.sjeom.mydata.platform.analysis.validation.AnalysisPlanValidator;
import com.sjeom.mydata.platform.audit.domain.AuditOutcome;
import com.sjeom.mydata.platform.audit.domain.AuditRecord;
import com.sjeom.mydata.platform.audit.domain.ToolAuditRecord;
import com.sjeom.mydata.platform.audit.persistence.InMemoryAuditRecordRepository;
import com.sjeom.mydata.platform.product.domain.CardProduct;
import com.sjeom.mydata.platform.product.domain.ProductSaleStatus;
import com.sjeom.mydata.platform.support.fixture.InMemoryCardProductRepository;
import com.sjeom.mydata.platform.support.fixture.InMemoryConsumptionTransactionRepository;
import com.sjeom.mydata.platform.tool.benefit.CalculateExpectedBenefitTool;
import com.sjeom.mydata.platform.tool.consumption.CardTransaction;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import com.sjeom.mydata.platform.tool.consumption.GetConsumptionSummaryTool;
import com.sjeom.mydata.platform.tool.consumption.TransactionStatus;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionContext;
import com.sjeom.mydata.platform.tool.domain.ToolExecutionStatus;
import com.sjeom.mydata.platform.tool.product.SearchCardProductsTool;
import com.sjeom.mydata.platform.tool.recommendation.RankRecommendationsTool;
import com.sjeom.mydata.platform.tool.segment.FilterCustomerSegmentTool;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FixedPlanExecutionAuditTest {

    private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID EXECUTION_ID = UUID.fromString("a12c1de7-ce9f-4a21-b8cc-c66b085d5a8d");

    @Test
    void storesFiveToolExecutionsWithoutCustomerKeysInSummaries() {
        InMemoryAuditRecordRepository auditRepository = new InMemoryAuditRecordRepository();
        FixedPlanExecutionService service = service(auditRepository);

        assertThat(service.execute(
                plan(PolicyStatus.CURRENTLY_ALLOWED),
                context(),
                Map.of("CUST-ANON-001", new BigDecimal("400000"))
        ).status()).isEqualTo(AnalysisExecutionStatus.SUCCESS);

        AuditRecord audit = auditRepository.findByExecutionId(EXECUTION_ID).orElseThrow();
        assertThat(audit.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(audit.analysisPlanId()).isEqualTo("PLAN-20260903-AUDIT");
        assertThat(audit.planVersion()).isEqualTo("1.0");
        assertThat(audit.dataAsOf()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(audit.modelVersion()).isEqualTo("NOT_USED");
        assertThat(audit.promptVersion()).isEqualTo("NOT_USED");
        assertThat(audit.ruleVersion()).isEqualTo("POC-RULE-1.0");
        assertThat(audit.toolExecutions())
                .extracting(ToolAuditRecord::toolName)
                .containsExactlyElementsOf(AnalysisPlanValidator.SUPPORTED_TOOL_SEQUENCE);
        assertThat(audit.toolExecutions())
                .extracting(ToolAuditRecord::status)
                .containsOnly(ToolExecutionStatus.SUCCESS);
        assertThat(audit.toolExecutions().getLast().outputSummary())
                .containsEntry("recommendationCount", "1");
        assertThat(audit.toolExecutions().stream()
                .flatMap(tool -> tool.inputSummary().values().stream()))
                .noneMatch(value -> value.contains("CUST-ANON-001"));
        assertThat(audit.toolExecutions().stream()
                .flatMap(tool -> tool.outputSummary().values().stream()))
                .noneMatch(value -> value.contains("CUST-ANON-001"));
    }

    @Test
    void storesPolicyRejectionWithoutCallingTools() {
        InMemoryAuditRecordRepository auditRepository = new InMemoryAuditRecordRepository();
        FixedPlanExecutionService service = service(auditRepository);

        service.execute(plan(PolicyStatus.REVIEW_REQUIRED), context(), Map.of());

        AuditRecord audit = auditRepository.findByExecutionId(EXECUTION_ID).orElseThrow();
        assertThat(audit.outcome()).isEqualTo(AuditOutcome.REJECTED);
        assertThat(audit.reasonCodes()).containsExactly("POLICY_NOT_ALLOWED");
        assertThat(audit.toolExecutions()).isEmpty();
    }

    private static FixedPlanExecutionService service(InMemoryAuditRecordRepository auditRepository) {
        InMemoryConsumptionTransactionRepository transactionRepository =
                new InMemoryConsumptionTransactionRepository(List.of(
                        approved("T-1", "2026-07-01", "100000"),
                        approved("T-2", "2026-08-01", "100000"),
                        approved("T-3", "2026-09-01", "100000")
                ));
        InMemoryCardProductRepository productRepository = new InMemoryCardProductRepository(List.of(
                new CardProduct(
                        "CARD-A",
                        "Coffee Card",
                        ProductSaleStatus.ON_SALE,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        ConsumptionCategory.CAFE,
                        new BigDecimal("0.10"),
                        new BigDecimal("10000"),
                        new BigDecimal("300000"),
                        true
                )
        ));
        return new FixedPlanExecutionService(
                new AnalysisPlanValidator(),
                auditRepository,
                CLOCK,
                new GetConsumptionSummaryTool(transactionRepository, CLOCK),
                new FilterCustomerSegmentTool(CLOCK),
                new SearchCardProductsTool(productRepository, CLOCK),
                new CalculateExpectedBenefitTool(CLOCK),
                new RankRecommendationsTool(CLOCK)
        );
    }

    private static AnalysisPlan plan(PolicyStatus policyStatus) {
        return new AnalysisPlan(
                "PLAN-20260903-AUDIT",
                "1.0",
                "COFFEE_HEAVY_USER",
                new AnalysisPeriod(PeriodType.RELATIVE_MONTH, 3),
                List.of(new AnalysisCondition(
                        ConsumptionCategory.CAFE,
                        ConditionMetric.MONTHLY_AVG_AMOUNT,
                        ComparisonOperator.GTE,
                        new BigDecimal("100000"),
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

    private static CardTransaction approved(String transactionId, String occurredOn, String amount) {
        return new CardTransaction(
                transactionId,
                "CUST-ANON-001",
                LocalDate.parse(occurredOn),
                ConsumptionCategory.CAFE,
                new BigDecimal(amount),
                TransactionStatus.APPROVED,
                null
        );
    }

    private static ToolExecutionContext context() {
        return new ToolExecutionContext(
                EXECUTION_ID,
                "business-user",
                "CARD_RECOMMENDATION",
                NOW,
                LocalDate.of(2026, 9, 3),
                100
        );
    }
}
