# 마이데이터 AI 사업활용 플랫폼 - Codex 개발 인수인계서

## 1. 문서 목적

이 문서는 Codex에서 **마이데이터 AI 사업활용 플랫폼**의 PoC 설계와 개발을 이어가기 위한 기준 문서다.

이 프로젝트는 단순한 마이데이터 챗봇이나 특정 상품 추천 기능 하나를 만드는 것이 아니다. 사업 아이디어가 늘어날 때마다 개발자가 SQL과 Java 로직을 새로 작성하지 않아도, 사업 담당자가 자연어로 분석 목적을 정의하고 AI가 사전에 허용된 데이터 도구를 조합하여 고객 Signal과 추천 후보를 만들어내는 플랫폼을 목표로 한다.

핵심 가치는 다음 한 문장으로 정의한다.

> **마이데이터 활용 아이디어가 늘어나도 개발량이 비례해서 늘어나지 않는 Private AI 기반 데이터 활용 플랫폼**

---

## 2. 프로젝트 배경

### 2.1 해결하려는 문제

기존 방식에서는 새로운 사업 아이디어가 나올 때마다 개발자가 아래 작업을 반복해야 한다.

1. 대상 데이터와 테이블 파악
2. 고객 추출 SQL 작성
3. 집계 및 조건 판정 로직 개발
4. 상품 매칭 로직 개발
5. API, 테스트, 배포

예를 들어 `커피 소비가 많은 고객에게 커피 혜택 카드를 추천`한 뒤 `해외여행 가능 고객`, `대환 가능 고객`, `여유자금 증가 고객` 등으로 요구가 늘어나면 개발량도 거의 선형으로 증가한다.

이 플랫폼은 AI가 사업 목적을 해석하고, 플랫폼이 제공하는 공통 Data Tool을 조합해 분석 계획을 생성하도록 하여 반복 개발을 줄인다.

### 2.2 Private AI가 필요한 이유

마이데이터에는 금융거래, 자산, 대출, 카드 승인내역 등 민감한 개인신용정보가 포함된다. 외부 생성형 AI API로 원문 데이터를 전송할 수 없으므로 회사 내부망에서 운영되는 다음 형태를 전제로 한다.

- On-premise LLM 또는 Private LLM
- 외부 인터넷 및 외부 AI API로 고객 원천데이터 전송 금지
- 최소 필요 데이터만 모델에 제공
- 고객 식별자는 가능한 경우 가명 또는 내부 대체키 사용
- AI 입력, 도구 호출, 판단 근거, 결과에 대한 감사 추적 보장

Private LLM 자체가 목적은 아니다. 민감정보를 외부로 유출하지 않으면서 사업 분석 자동화를 구현하기 위한 기술적 수단이다.

---

## 3. 정책 가이드에서 반영할 원칙

참고 기준은 금융위원회 `AI 시대 국민의 일상과 생업을 잇는 마이데이터 발전방안(안)`(2026.8)이다.

### 3.1 정책 방향

- 기존 마이데이터의 단순 조회·분석 중심 `read` 기능을 실제 조치 수행인 `write` 단계까지 확대
- 개인뿐 아니라 개인사업자·소상공인을 포함하도록 서비스 범위 확대 추진
- 금융정보 외에 상거래, 공공, 공과금, 가상자산, 정책금융 정보 등 활용범위 확대 추진
- 계열사 공동활용 및 본업 데이터와 마이데이터 간 결합 기준 명확화 추진
- AI Agent가 이용자를 대리하여 금융행위를 수행할 수 있는 여건 마련 추진
- AI Agent의 대리실행 전 과정에 대한 기록·보관 및 이용자 설명의무 도입 추진

### 3.2 현재 가능 범위와 미래 범위 구분

모든 기능은 아래 상태값으로 구분한다.

| 상태 | 의미 | 기본 처리 |
|---|---|---|
| `CURRENTLY_ALLOWED` | 현재 법령·내부규정에서 가능한 기능 | PoC 또는 운영 검토 가능 |
| `PILOT_OR_SANDBOX` | 혁신금융서비스 등 별도 절차가 필요한 기능 | 시뮬레이션만 수행, 실제 실행 차단 |
| `FUTURE_POLICY` | 법·시행령 개정 이후 가능성이 있는 기능 | 설계 확장점만 마련 |
| `REVIEW_REQUIRED` | 법무·준법·정보보호 검토가 필요한 기능 | 검토 승인 전 실행 금지 |

정책 문서는 개선안이며 일부는 법 개정·시행령 개정 전이다. 문서에 제시됐다는 이유만으로 현재 허용된 기능으로 간주하지 않는다.

### 3.3 감사 및 설명 가능성

AI 분석과 향후 Action 실행에 대해 최소한 아래 정보를 구조적으로 저장한다.

- 요청자와 요청 목적
- 사용한 데이터 종류와 기준 시점
- 적용한 기간, 조건, 임계값
- AI가 생성한 분석 계획과 버전
- 호출한 Data Tool과 입력·출력 요약
- 적용한 Rule 및 Rule 버전
- 발견한 Signal, 점수, 추천 후보
- 추천 또는 실행 사유
- 수행 내용과 외부 제공 정보
- 실행 여부, 승인자, 동의 근거
- 실행 결과와 실패 사유
- 사용한 모델·프롬프트·상품 데이터 버전

---

## 4. 목표 사용자와 사용 흐름

### 4.1 1차 사용자

- 사업 담당자: 자연어로 분석 목적과 대상 조건을 정의
- 데이터·플랫폼 운영자: 데이터 카탈로그, Tool, Rule, 권한 관리
- 개발자: 공통 분석 도구와 안전한 실행 엔진 제공
- 준법·보안 담당자: 데이터 사용범위, 동의, 설명·감사 기록 검토

### 4.2 목표 흐름

```text
[사업 담당자의 자연어 요청]
        ↓
[AI Business Agent]
  - 목적/대상/기간 해석
  - 필요한 Tool 선택
        ↓
[구조화된 Analysis Plan]
        ↓ 검증 및 승인
[Deterministic Execution Engine]
  - SQL/Java/Rule로 조회·집계·계산
        ↓
[Signal 및 상품 후보 생성]
        ↓
[Private LLM의 결과 설명]
        ↓
[Opportunity DB]
        ↓
[CRM / APP / 상담원용 API]
```

AI는 **무엇을 분석할지 판단하고 계획을 생성**하며, 플랫폼은 **허용된 범위에서 정확한 조회와 계산을 실행**한다.

---

## 5. 1차 PoC 범위

### 5.1 대표 Use Case

> 최근 3개월간 카페 소비가 많은 고객을 찾고, 회사가 취급하는 카드 중 해당 고객의 실제 소비패턴에 가장 유리한 카드를 추천한다.

### 5.2 입력 예시

```text
최근 3개월 동안 카페 소비가 많은 고객을 찾아서
우리 카드 중 예상 혜택이 가장 높은 카드를 추천해줘.
월평균 카페 지출 10만원 이상인 고객을 대상으로 해줘.
```

### 5.3 AI가 생성해야 할 분석 정의 예시

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

### 5.4 결과 예시

```json
{
  "customerKey": "CUST-ANON-123456",
  "signal": {
    "code": "COFFEE_HEAVY_USER",
    "score": 92,
    "reasonCodes": [
      "CAFE_MONTHLY_AVG_OVER_100K",
      "CAFE_FREQUENCY_HIGH"
    ]
  },
  "recommendation": {
    "productId": "CARD-A",
    "expectedAnnualBenefit": 151000,
    "currency": "KRW",
    "reason": "최근 3개월 카페 이용금액과 이용 빈도를 기준으로 비교한 결과입니다."
  },
  "analysisPlanId": "PLAN-20260902-0001",
  "generatedAt": "2026-09-02T09:00:00Z"
}
```

### 5.5 PoC에서 제외할 항목

- AI가 임의 SQL을 생성하여 운영 DB에서 직접 실행하는 기능
- 고객 동의 없이 실제 마케팅을 발송하는 기능
- 카드 발급, 대출 신청 등 실제 금융행위 자동 실행
- 외부 LLM API에 마이데이터 원문 전송
- 대규모 Vector DB 구축
- MCP 도입 자체를 목표로 한 별도 플랫폼 개발
- 완전 자율형 Multi-Agent 구조

---

## 6. 책임 분리 원칙

| 영역 | 담당 | 원칙 |
|---|---|---|
| 사업 목적 해석 | Private LLM | 자연어를 구조화된 Plan으로 변환 |
| 데이터 의미 탐색 | LLM + 데이터 카탈로그 | 허용된 메타데이터만 참조 |
| 데이터 조회 | Data Tool | 사전 정의된 파라미터와 권한으로만 실행 |
| 금액·금리·혜택 계산 | Java/SQL Rule Engine | 결정론적으로 계산하고 테스트 가능해야 함 |
| 고객 세그먼트 생성 | Execution Engine | 승인된 조건과 Plan에 따라 실행 |
| 상품 후보 검색 | 상품 DB / Product Tool | 판매 가능하고 유효한 상품만 반환 |
| 결과 설명 | Private LLM | 계산값을 변경하지 않고 설명만 생성 |
| 실제 금융행위 | Action Engine | 별도 동의·승인·규제 검토 후에만 실행 |

LLM 출력은 신뢰할 수 없는 입력으로 간주한다. 모든 Plan은 JSON Schema 검증, 허용 Tool 검증, 값 범위 검증, 정책 검증을 통과해야 실행할 수 있다.

---

## 7. 제안 아키텍처

### 7.1 주요 컴포넌트

```text
mydata-ai-platform
├── ai-orchestrator
│   ├── 자연어 요청 해석
│   ├── Tool 선택 및 Analysis Plan 생성
│   └── 결과 설명 생성
├── plan-validator
│   ├── JSON Schema 검증
│   ├── Tool Allowlist 검증
│   ├── 데이터 권한 및 정책 검증
│   └── 비용·대상 규모 제한
├── data-catalog
│   ├── 데이터셋·필드 의미
│   ├── 민감도 및 사용 목적
│   └── 사용 가능한 Metric·Dimension
├── data-tool
│   ├── 소비 분석 Tool
│   ├── 고객 세그먼트 Tool
│   ├── 상품 검색 Tool
│   └── 혜택 계산 Tool
├── execution-engine
│   ├── Plan 단계 실행
│   ├── Rule Engine
│   └── 재현 가능한 결과 생성
├── opportunity
│   ├── Signal·Score·추천 결과 저장
│   └── CRM/APP 제공 API
└── audit
    ├── 모델·프롬프트·Plan 버전
    ├── Tool 실행 이력
    └── 추천·실행 사유 및 결과
```

### 7.2 기본 기술 방향

신규 프로젝트 기준의 우선 검토 스택은 다음과 같다.

- Java 21
- Spring Boot 3.5.x
- Gradle 8.14.x
- Oracle 19c 또는 PoC용 대체 RDB
- 사내 배포 가능한 Private LLM
- LLM 연동부는 특정 모델 SDK에 강결합하지 않도록 Adapter 패턴 적용
- API와 결과 모델은 JSON Schema 또는 OpenAPI로 명시

실제 저장소에 이미 기술 스택이나 규칙이 있다면 해당 저장소의 `AGENTS.md`, 빌드 설정, 기존 코드 규칙을 우선한다.

---

## 8. 핵심 도메인 모델

초기에는 아래 모델을 우선 정의한다.

- `BusinessAnalysisRequest`: 사업 담당자의 원문 요청, 목적, 요청자
- `AnalysisPlan`: AI가 만든 구조화된 실행 계획
- `PlanStep`: Tool, 입력, 선행 단계, 예상 출력
- `DataToolDefinition`: Tool 이름, 설명, 허용 입력, 반환 Schema, 권한
- `DataCatalogItem`: 데이터셋/필드 의미, 민감도, 사용목적, 보유기간
- `CustomerSignal`: 고객 상태 또는 사업 기회 Signal
- `OpportunityScore`: Signal의 우선순위 점수와 산식/근거
- `ProductCandidate`: 판매 가능한 상품 후보
- `ProductRecommendation`: 고객별 추천 상품과 예상 혜택
- `ReasonCode`: 설명 가능한 표준 근거 코드
- `AuditRecord`: 요청부터 결과까지의 추적 정보
- `PolicyDecision`: 허용, 승인 필요, 차단 및 그 사유

### 8.1 Tool 인터페이스 예시

```java
public interface AiDataTool<I, O> {
    String name();
    Class<I> inputType();
    Class<O> outputType();
    ToolExecutionResult<O> execute(I input, ToolExecutionContext context);
}
```

초기 Tool 후보:

1. `GetConsumptionSummaryTool`
2. `FilterCustomerSegmentTool`
3. `SearchCardProductsTool`
4. `CalculateExpectedBenefitTool`
5. `RankRecommendationsTool`

Tool은 고객 원천데이터 전체를 LLM에 반환하지 않는다. LLM에는 업무 수행에 필요한 집계값과 비식별 결과만 최소한으로 제공한다.

---

## 9. 데이터 카탈로그 최소 항목

각 데이터 항목에는 최소한 다음 메타데이터가 필요하다.

| 항목 | 설명 |
|---|---|
| Dataset/Field ID | 시스템 내부 고유 식별자 |
| Business Name | 사업 담당자가 이해할 명칭 |
| Description | 데이터 의미와 산출 기준 |
| Data Type | 타입 및 단위 |
| Classification | 개인정보·개인신용정보·민감정보 등 분류 |
| Permitted Purposes | 허용된 사용 목적 |
| Prohibited Uses | 금지된 분석 또는 제공 범위 |
| Retention | 보유 및 결과 유지기간 |
| Quality Rule | 누락·중복·지연 기준 |
| Owner | 데이터 책임자 |
| Available Metrics | 합계, 평균, 빈도, 증감률 등 |
| Available Dimensions | 업종, 기간, 기관, 상품유형 등 |

LLM에 실제 테이블명과 컬럼을 그대로 공개하기보다, 업무 의미 중심의 카탈로그와 허용 Metric/Dimension을 제공한다.

---

## 10. MCP와 Vector DB 적용 방침

### 10.1 MCP

1차 PoC에서는 MCP가 필수는 아니다. Spring Boot 내부 Tool 인터페이스로 먼저 구현한다. 단, 향후 MCP Server로 노출하기 쉽도록 Tool의 입력·출력 Schema, 설명, 오류 코드, 권한을 명확히 분리한다.

MCP 도입 검토 시점:

- 여러 LLM 또는 Agent가 같은 Tool을 공유해야 할 때
- Tool 수가 증가하여 표준 검색·호출·권한 관리가 필요할 때
- 다른 내부 AI 플랫폼이 마이데이터 Tool을 사용해야 할 때

### 10.2 Vector DB

마이데이터 거래내역과 집계 같은 정형 데이터 분석에는 기존 RDB와 Data Tool을 사용한다. Vector DB는 다음 비정형 지식이 충분히 누적됐을 때 도입한다.

- 카드 상품설명서
- 약관 및 정책 문서
- 상품별 혜택 안내서
- 마케팅·준법 가이드

즉, `MCP = AI와 기능 연결`, `Vector DB = AI와 비정형 지식 검색`, `Data Tool = AI와 정형 마이데이터 분석 연결`로 구분한다.

---

## 11. 보안·준법 필수 요구사항

- LLM은 DB 계정이나 자유 SQL 실행 권한을 갖지 않는다.
- 조회 가능한 데이터셋, Metric, 기간, 최대 대상건수를 Allowlist로 관리한다.
- 고객 식별정보는 가명키로 치환하고 꼭 필요한 경우에만 후속 시스템에서 재결합한다.
- 프롬프트와 로그에 불필요한 원문 거래내역을 저장하지 않는다.
- 입력 데이터와 결과 데이터의 접근권한을 분리한다.
- 상품 추천은 판매 가능 여부, 유효기간, 대상 조건, 준법 승인 상태를 검증한다.
- 모델이 만든 금액이나 예상 혜택을 그대로 사용하지 않고 Rule Engine 계산값만 사용한다.
- 대량 고객 추출·마케팅 활용에는 별도 승인 단계와 처리량 제한을 둔다.
- 모든 결과에 데이터 기준시점, 모델 버전, Plan 버전, Rule 버전을 기록한다.
- Prompt Injection, Tool 오용, 과도한 데이터 조회, 결과 재식별 가능성을 테스트한다.
- 실제 Action 기능은 동의 범위, 목적, 방법, 기한, 철회, 책임자, 실행 결과 설명을 별도 관리한다.

---

## 12. 단계별 개발 계획

### Phase 0. 저장소 및 환경 확인

Codex는 구현 전에 다음을 먼저 확인한다.

1. 저장소 구조와 `AGENTS.md`
2. 현재 Java, Spring Boot, Gradle 버전
3. 기존 DB Schema 및 샘플 데이터 존재 여부
4. 사용 가능한 사내 LLM과 Tool Calling 지원 여부
5. 폐쇄망 배포 방식과 모델 호출 방식
6. 테스트·보안·로깅 관련 기존 공통 모듈

확인할 수 없는 부분은 추측해 운영 의존 코드를 만들지 말고, 인터페이스와 Mock으로 격리한 뒤 명시적인 TODO로 남긴다.

### Phase 1. LLM 없이 결정론적 Vertical Slice

- PoC용 샘플 데이터셋 작성 또는 기존 데이터 Adapter 구현
- 소비 집계 Tool 구현
- 고객 세그먼트 Tool 구현
- 카드 상품 검색 Tool 구현
- 예상 혜택 계산 Tool 구현
- 고정된 Analysis Plan을 실행하여 고객별 추천 결과 생성
- 단위 테스트와 통합 테스트 작성

### Phase 2. Analysis Plan 계약

- `AnalysisPlan` JSON Schema 정의
- Plan Validator 구현
- 허용 Tool, 파라미터 범위, 기간, 최대 대상건수 검증
- Plan 저장 및 버전 관리
- 동일 Plan과 동일 데이터 기준시점에서 결과 재현 가능성 검증

### Phase 3. Private LLM 연결

- `LlmClient` Port와 모델별 Adapter 분리
- 자연어 요청을 Analysis Plan으로 변환
- 구조화 출력 및 Schema 검증 실패 시 제한된 재시도
- 모델이 허용되지 않은 Tool이나 필드를 요청하면 차단
- LLM 미연결 환경에서도 Mock Adapter로 전체 테스트 가능하게 구성

### Phase 4. 감사·설명 및 사업 활용 API

- Audit Record 저장
- Signal/Opportunity 표준 결과 저장
- 고객별 추천근거와 Reason Code 제공
- CRM/APP가 조회할 수 있는 API 제공
- 민감정보 마스킹과 권한별 응답 분리

### Phase 5. 플랫폼성 검증

두 번째 Use Case로 `해외여행 가능 고객 → 해외결제 특화 카드/환전 서비스 추천`을 추가한다.

이때 평가할 핵심 질문:

> 신규 SQL·Java 로직을 거의 추가하지 않고 기존 Tool 조합과 카탈로그·상품 데이터 설정만으로 구현 가능한가?

새로운 업무마다 전용 코드가 대량 발생하면 Tool의 추상화 수준을 재검토한다.

### Phase 6. 확장 검토

- 상품·약관 RAG 및 Vector DB
- Tool 공유를 위한 MCP Server
- 실제 Action Tool과 Human-in-the-loop 승인
- 개인사업자 마이데이터 Use Case
- 계열사 공동활용 및 데이터 결합 정책 엔진

---

## 13. 테스트 전략

### 13.1 필수 테스트

- 카테고리별 소비 집계 정확성
- 월평균과 이용빈도 계산 경계값
- 카드별 할인한도·전월실적·제외조건 계산
- 동일 입력에 대한 Rule Engine 결과 결정성
- 잘못된 Analysis Plan 차단
- 존재하지 않거나 허용되지 않은 Tool 요청 차단
- 과도한 기간·대상건수 요청 차단
- LLM Schema 오류 및 Timeout 시 안전한 실패
- 개인식별정보의 LLM 입력·로그 유출 여부
- Audit Record 누락 여부
- 상품 데이터 기준일이 지난 추천 차단

### 13.2 평가 데이터셋

PoC에는 정답을 확인할 수 있는 합성 고객 데이터를 만든다.

- 명확한 카페 고이용 고객
- 금액은 높지만 빈도가 낮은 고객
- 빈도는 높지만 총액이 낮은 고객
- 취소·환불 거래가 포함된 고객
- 카페와 유사하지만 제외해야 하는 업종
- 카드 전월실적을 충족하지 못하는 고객
- 할인한도 때문에 다른 카드가 더 유리한 고객

---

## 14. PoC 성공 기준

아래 조건을 모두 만족해야 한다.

1. 자연어 사업 요청을 유효한 `AnalysisPlan`으로 변환한다.
2. AI가 DB에 직접 SQL을 실행하지 않고 허용된 Tool만 사용한다.
3. 금액과 혜택 계산은 코드/Rule Engine이 담당한다.
4. 고객별 Signal, Score, 추천상품, Reason Code를 구조화해 저장한다.
5. 요청부터 결과까지 감사 추적이 가능하다.
6. 민감한 고객 원천데이터가 외부망으로 전송되지 않는다.
7. 두 번째 Use Case 추가 시 기존 공통 Tool 재사용률이 높다.
8. 결과의 정확성을 합성 정답 데이터로 검증할 수 있다.
9. 잘못되거나 위험한 Plan은 실행 전에 차단된다.
10. 기능별 정책 상태와 승인 필요 여부를 구분할 수 있다.

`AI가 자연어 답변을 잘한다`는 것만으로는 성공으로 보지 않는다.

---

## 15. Codex 작업 지시

Codex는 이 문서를 읽은 뒤 아래 순서로 진행한다.

1. 저장소와 기존 코드를 조사하고 현재 상태를 요약한다.
2. 불명확하지만 구현을 막지 않는 사항은 합리적 가정을 명시하고 진행한다.
3. 보안·규제·DB Schema·Private LLM 제품처럼 결과를 크게 바꾸는 미확정 사항은 질문 목록으로 분리한다.
4. 먼저 최소 Vertical Slice를 제안하고 파일 단위 구현 계획을 작성한다.
5. 계획 승인 후 작은 단위로 구현하고 각 단계마다 테스트한다.
6. 기존 사용자 변경사항과 무관한 코드를 수정하지 않는다.
7. 특정 LLM 제품에 종속된 코드는 Adapter 뒤로 격리한다.
8. 실제 고객 데이터가 없으면 합성 데이터와 명확한 Adapter 경계를 사용한다.
9. 모든 중요한 판단은 코드, Schema, 테스트, Audit 구조로 재현 가능하게 남긴다.
10. MVP를 벗어나는 MCP, Vector DB, Multi-Agent, 실제 금융행위 실행은 별도 제안으로 분리한다.

### Codex 첫 응답에서 제시할 내용

- 현재 저장소 조사 결과
- 제안 모듈/패키지 구조
- PoC Vertical Slice 구현 계획
- 필요한 DB 테이블 또는 합성 데이터 구조
- `AnalysisPlan` 초안
- 첫 번째 Data Tool 인터페이스와 구현 후보
- 테스트 계획
- 사용자 확인이 필요한 핵심 질문 3~5개

---

## 16. 장기 확장 방향

장기적으로는 아래 흐름을 지원할 수 있어야 한다.

```text
마이데이터 수집
    ↓
Feature / Signal 탐색
    ↓
Opportunity Score
    ↓
Next Best Action
    ↓
CRM / APP / 상담원
    ↓
고객 동의 및 승인
    ↓
금융행위 실행
    ↓
실행 결과·고객 편익·매출 측정
```

최종적으로 Signal별 성과를 아래처럼 측정할 수 있어야 한다.

- 대상 고객 수
- 노출 수
- 클릭 수
- 상담 또는 신청 수
- 가입·실행 수
- 고객 예상·실현 혜택
- 회사 매출 또는 비용절감
- 추천 거절·민원·오탐 비율

AI 활용의 성과는 모델 사용량이 아니라 **고객 편익, 사업 성과, 신규 Use Case 개발시간 단축, 안전성과 설명 가능성**으로 평가한다.

---

## 17. 참고 문서

- 금융위원회, `AI 시대 국민의 일상과 생업을 잇는 마이데이터 발전방안(안)`, 2026.8

> 주의: 참고 문서의 법령·시행령 개정 일정은 변동될 수 있으므로 실제 서비스 적용 시 최신 법령, 금융위원회·금융감독원 가이드, 사내 준법·정보보호 검토를 다시 확인한다.
