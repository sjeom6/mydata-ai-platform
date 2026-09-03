# PoC 구현 계획

## 1. 목표

첫 번째 Vertical Slice는 LLM과 실제 DB 없이 고정된 `AnalysisPlan`과 합성 데이터를 사용한다.

다음 흐름이 한 번에 동작하는 상태를 목표로 한다.

```text
합성 카드 거래 데이터
  -> 최근 3개월 카페 소비 집계
  -> 월평균 10만 원 이상 고객 선별
  -> 판매 가능한 카드 검색
  -> 예상 연간 혜택 계산
  -> 고객별 최적 카드 추천
```

## 2. 패키지 구조

```text
com.sjeom.mydata.platform
├── analysis
│   ├── domain       AnalysisPlan, PlanStep, 조건 모델
│   ├── application  Plan 실행 유스케이스
│   └── validation   Plan 검증
├── tool
│   ├── domain       AiDataTool, 실행 컨텍스트와 결과
│   └── consumption  소비 집계 Tool
├── customer
│   └── domain       CustomerSignal, OpportunityScore
├── product
│   └── domain       ProductCandidate, ProductRecommendation
├── audit
│   └── domain       AuditRecord
└── support
    └── fixture      합성 데이터
```

PoC 초기에는 단일 Gradle 모듈을 유지한다. 패키지 경계를 먼저 검증하고 독립 배포 필요성이 생길 때만 멀티 모듈로 분리한다.

## 3. 최소 데이터 구조

### 고객

- `customerKey`: 가명 고객키

### 카드 거래

- `transactionId`: 거래 식별자
- `customerKey`: 가명 고객키
- `occurredAt`: 거래 시각
- `category`: 소비 카테고리
- `amount`: 승인 금액
- `status`: 승인, 취소, 환불 상태
- `originalTransactionId`: 취소·환불 대상 거래

### 카드 상품

- `productId`: 상품 식별자
- `name`: 상품명
- `saleStatus`: 판매 가능 상태
- `validFrom`, `validTo`: 판매 유효기간
- `benefitCategory`: 혜택 카테고리
- `discountRate`: 할인율
- `monthlyDiscountLimit`: 월 할인한도
- `minimumPreviousMonthSpend`: 전월실적 조건
- `complianceApproved`: 준법 승인 여부

첫 단계에서는 메모리 기반 합성 데이터를 사용하고 저장소 인터페이스 뒤에 둔다. 실제 DB Schema가 확정되면 Adapter만 교체한다.

## 4. AnalysisPlan 초안

```json
{
  "planVersion": "1.0",
  "segmentCode": "COFFEE_HEAVY_USER",
  "period": {
    "type": "RELATIVE_MONTH",
    "value": 3
  },
  "conditions": [
    {
      "category": "CAFE",
      "metric": "MONTHLY_AVG_AMOUNT",
      "operator": "GTE",
      "value": 100000,
      "currency": "KRW"
    }
  ],
  "toolSteps": [
    "GET_CONSUMPTION_SUMMARY",
    "FILTER_CUSTOMER_SEGMENT",
    "SEARCH_CARD_PRODUCTS",
    "CALCULATE_EXPECTED_BENEFIT",
    "RANK_RECOMMENDATIONS"
  ],
  "productMatching": {
    "productType": "CREDIT_CARD",
    "benefitCategory": "CAFE",
    "rankingMetric": "EXPECTED_ANNUAL_BENEFIT"
  },
  "policyStatus": "CURRENTLY_ALLOWED"
}
```

## 5. 첫 번째 Data Tool

```java
public interface AiDataTool<I, O> {
    String name();
    Class<I> inputType();
    Class<O> outputType();
    ToolExecutionResult<O> execute(I input, ToolExecutionContext context);
}
```

첫 구현은 `GetConsumptionSummaryTool`로 한다.

- 입력: 기준일, 조회 개월 수, 소비 카테고리
- 출력: 고객별 총 사용금액, 월평균 사용금액, 승인 건수
- 제한: 최대 조회기간 12개월, 허용 카테고리만 사용
- 처리: 취소와 환불을 원승인 금액에서 차감
- 보호: 원문 거래나 개인식별정보를 LLM용 결과에 포함하지 않음

## 6. 구현 순서

1. 공통 도메인 타입과 `AiDataTool` 계약 정의
2. 합성 거래 데이터와 조회 Port 구현
3. `GetConsumptionSummaryTool` 구현
4. 소비 집계 단위 테스트 작성
5. 고객 선별, 상품 검색, 혜택 계산, 추천 순으로 Tool 추가
6. 고정 `AnalysisPlan`을 실행하는 통합 테스트 작성
7. Vertical Slice가 안정된 후 Plan JSON Schema와 Validator 구현

## 7. 첫 단계 테스트

- 카페 승인 거래 합계와 건수
- 카페 외 업종 제외
- 조회기간 밖 거래 제외
- 취소·환불 금액 차감
- 월 경계와 월평균 계산
- 빈 거래 고객 처리
- 12개월 초과 요청 차단
- 허용되지 않은 카테고리 차단
- 동일 입력의 결과 결정성

## 8. 구현 전 확인이 필요한 사항

아래 항목은 현재 미확정으로 두고, 첫 Vertical Slice에서는 괄호 안의 기본값을 사용한다.

1. 실제 DB 종류와 Schema (메모리 합성 데이터)
2. 사내 Private LLM 제품과 호출 방식 (`LlmClient` Port만 마련하고 연동 보류)
3. 카페 업종 분류 기준 (합성 데이터의 `CAFE` 코드)
4. 취소·환불 연결 방식 (`originalTransactionId` 사용)
5. 카드 혜택 계산 규칙 (단순 할인율, 월 한도, 전월실적만 적용)

