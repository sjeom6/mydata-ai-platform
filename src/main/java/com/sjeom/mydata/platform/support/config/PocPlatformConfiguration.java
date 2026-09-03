package com.sjeom.mydata.platform.support.config;

import com.sjeom.mydata.platform.ai.application.BusinessAnalysisExecutionService;
import com.sjeom.mydata.platform.ai.llm.LlmClient;
import com.sjeom.mydata.platform.analysis.application.AnalysisPlanExecutionFacade;
import com.sjeom.mydata.platform.analysis.application.AnalysisPlanRegistrationService;
import com.sjeom.mydata.platform.analysis.application.FixedPlanExecutionService;
import com.sjeom.mydata.platform.analysis.application.PreviousMonthSpendProvider;
import com.sjeom.mydata.platform.analysis.input.AnalysisPlanJsonReader;
import com.sjeom.mydata.platform.analysis.persistence.AnalysisPlanRepository;
import com.sjeom.mydata.platform.analysis.persistence.AnalysisPlanSnapshotFactory;
import com.sjeom.mydata.platform.analysis.persistence.InMemoryAnalysisPlanRepository;
import com.sjeom.mydata.platform.analysis.validation.AnalysisPlanValidator;
import com.sjeom.mydata.platform.audit.persistence.AuditRecordRepository;
import com.sjeom.mydata.platform.audit.persistence.InMemoryAuditRecordRepository;
import com.sjeom.mydata.platform.product.domain.CardProduct;
import com.sjeom.mydata.platform.product.domain.ProductSaleStatus;
import com.sjeom.mydata.platform.support.fixture.InMemoryCardProductRepository;
import com.sjeom.mydata.platform.support.fixture.InMemoryConsumptionTransactionRepository;
import com.sjeom.mydata.platform.support.fixture.PocPreviousMonthSpendProvider;
import com.sjeom.mydata.platform.support.fixture.PocLlmClient;
import com.sjeom.mydata.platform.tool.benefit.CalculateExpectedBenefitTool;
import com.sjeom.mydata.platform.tool.consumption.CardTransaction;
import com.sjeom.mydata.platform.tool.consumption.ConsumptionCategory;
import com.sjeom.mydata.platform.tool.consumption.GetConsumptionSummaryTool;
import com.sjeom.mydata.platform.tool.consumption.TransactionStatus;
import com.sjeom.mydata.platform.tool.product.SearchCardProductsTool;
import com.sjeom.mydata.platform.tool.recommendation.RankRecommendationsTool;
import com.sjeom.mydata.platform.tool.segment.FilterCustomerSegmentTool;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("poc")
public class PocPlatformConfiguration {

    @Bean
    Clock platformClock() {
        return Clock.systemUTC();
    }

    @Bean
    AnalysisPlanValidator analysisPlanValidator() {
        return new AnalysisPlanValidator();
    }

    @Bean
    AnalysisPlanJsonReader analysisPlanJsonReader() {
        return new AnalysisPlanJsonReader();
    }

    @Bean
    AnalysisPlanRepository analysisPlanRepository() {
        return new InMemoryAnalysisPlanRepository();
    }

    @Bean
    AuditRecordRepository auditRecordRepository() {
        return new InMemoryAuditRecordRepository();
    }

    @Bean
    InMemoryConsumptionTransactionRepository consumptionTransactionRepository() {
        return new InMemoryConsumptionTransactionRepository(List.of(
                approved("T-01", "CUST-ANON-A", "2026-07-10", "130000"),
                approved("T-02", "CUST-ANON-A", "2026-08-10", "130000"),
                approved("T-03", "CUST-ANON-A", "2026-09-01", "130000"),
                approved("T-04", "CUST-ANON-B", "2026-07-11", "110000"),
                approved("T-05", "CUST-ANON-B", "2026-08-11", "110000"),
                approved("T-06", "CUST-ANON-B", "2026-09-02", "110000"),
                approved("T-07", "CUST-ANON-C", "2026-07-12", "20000"),
                approved("T-08", "CUST-ANON-C", "2026-08-12", "20000"),
                approved("T-09", "CUST-ANON-C", "2026-09-03", "20000")
        ));
    }

    @Bean
    InMemoryCardProductRepository cardProductRepository() {
        return new InMemoryCardProductRepository(List.of(
                product("CARD-A", "Coffee Premium Card", "0.10", "10000", "300000"),
                product("CARD-B", "Coffee Everyday Card", "0.05", "20000", "0")
        ));
    }

    @Bean
    PreviousMonthSpendProvider previousMonthSpendProvider() {
        return new PocPreviousMonthSpendProvider(Map.of(
                "CUST-ANON-A", new BigDecimal("400000"),
                "CUST-ANON-B", new BigDecimal("100000"),
                "CUST-ANON-C", new BigDecimal("500000")
        ));
    }

    @Bean
    GetConsumptionSummaryTool getConsumptionSummaryTool(
            InMemoryConsumptionTransactionRepository repository,
            Clock clock
    ) {
        return new GetConsumptionSummaryTool(repository, clock);
    }

    @Bean
    FilterCustomerSegmentTool filterCustomerSegmentTool(Clock clock) {
        return new FilterCustomerSegmentTool(clock);
    }

    @Bean
    SearchCardProductsTool searchCardProductsTool(
            InMemoryCardProductRepository repository,
            Clock clock
    ) {
        return new SearchCardProductsTool(repository, clock);
    }

    @Bean
    CalculateExpectedBenefitTool calculateExpectedBenefitTool(Clock clock) {
        return new CalculateExpectedBenefitTool(clock);
    }

    @Bean
    RankRecommendationsTool rankRecommendationsTool(Clock clock) {
        return new RankRecommendationsTool(clock);
    }

    @Bean
    AnalysisPlanRegistrationService analysisPlanRegistrationService(
            AnalysisPlanValidator validator,
            AnalysisPlanRepository repository,
            Clock clock
    ) {
        return new AnalysisPlanRegistrationService(
                validator,
                new AnalysisPlanSnapshotFactory(clock),
                repository
        );
    }

    @Bean
    FixedPlanExecutionService fixedPlanExecutionService(
            AnalysisPlanValidator validator,
            AuditRecordRepository auditRepository,
            Clock clock,
            GetConsumptionSummaryTool consumptionTool,
            FilterCustomerSegmentTool segmentTool,
            SearchCardProductsTool productsTool,
            CalculateExpectedBenefitTool benefitTool,
            RankRecommendationsTool recommendationsTool
    ) {
        return new FixedPlanExecutionService(
                validator,
                auditRepository,
                clock,
                consumptionTool,
                segmentTool,
                productsTool,
                benefitTool,
                recommendationsTool
        );
    }

    @Bean
    AnalysisPlanExecutionFacade analysisPlanExecutionFacade(
            AnalysisPlanJsonReader jsonReader,
            AnalysisPlanRegistrationService registrationService,
            FixedPlanExecutionService executionService,
            PreviousMonthSpendProvider spendProvider,
            Clock clock
    ) {
        return new AnalysisPlanExecutionFacade(
                jsonReader,
                registrationService,
                executionService,
                spendProvider,
                clock
        );
    }

    @Bean
    LlmClient llmClient() {
        return new PocLlmClient();
    }

    @Bean
    BusinessAnalysisExecutionService businessAnalysisExecutionService(
            LlmClient llmClient,
            AnalysisPlanJsonReader jsonReader,
            AnalysisPlanExecutionFacade executionFacade
    ) {
        return new BusinessAnalysisExecutionService(llmClient, jsonReader, executionFacade);
    }

    private static CardTransaction approved(
            String transactionId,
            String customerKey,
            String occurredOn,
            String amount
    ) {
        return new CardTransaction(
                transactionId,
                customerKey,
                LocalDate.parse(occurredOn),
                ConsumptionCategory.CAFE,
                new BigDecimal(amount),
                TransactionStatus.APPROVED,
                null
        );
    }

    private static CardProduct product(
            String productId,
            String name,
            String discountRate,
            String monthlyLimit,
            String minimumPreviousMonthSpend
    ) {
        return new CardProduct(
                productId,
                name,
                ProductSaleStatus.ON_SALE,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                ConsumptionCategory.CAFE,
                new BigDecimal(discountRate),
                new BigDecimal(monthlyLimit),
                new BigDecimal(minimumPreviousMonthSpend),
                true
        );
    }
}
