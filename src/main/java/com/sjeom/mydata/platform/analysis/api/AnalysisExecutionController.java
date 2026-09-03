package com.sjeom.mydata.platform.analysis.api;

import com.sjeom.mydata.platform.analysis.application.AnalysisPlanExecutionFacade;
import com.sjeom.mydata.platform.analysis.application.AnalysisRequestResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@Validated
@RestController
@Profile("poc")
@RequestMapping("/api/v1/analysis-plans")
public class AnalysisExecutionController {

    private final AnalysisPlanExecutionFacade facade;

    public AnalysisExecutionController(AnalysisPlanExecutionFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/execute")
    public ResponseEntity<AnalysisExecutionApiResponse> execute(
            @RequestBody String planJson,
            @RequestHeader("X-Requester-Id") @NotBlank String requesterId,
            @RequestHeader("X-Business-Purpose") @NotBlank String purpose,
            @RequestHeader("X-Data-As-Of")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAsOf,
            @RequestHeader(value = "X-Max-Result-Count", defaultValue = "100")
            @Min(1) @Max(1000) int maxResultCount
    ) {
        AnalysisRequestResult result = facade.execute(
                planJson,
                requesterId,
                purpose,
                dataAsOf,
                maxResultCount
        );
        return ResponseEntity
                .status(httpStatus(result))
                .body(AnalysisExecutionApiResponse.from(result));
    }

    private static int httpStatus(AnalysisRequestResult result) {
        return switch (result.status()) {
            case SUCCESS -> 200;
            case INVALID_PLAN -> 400;
            case PLAN_CONFLICT -> 409;
            case EXECUTION_REJECTED -> 422;
            case FAILED -> 500;
        };
    }
}
