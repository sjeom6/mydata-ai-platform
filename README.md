# mydata-ai-platform

마이데이터 사업 아이디어를 검증된 `AnalysisPlan`과 허용된 Data Tool 조합으로 실행하는 Java 21 / Spring Boot PoC입니다. 현재 예제는 최근 3개월 카페 또는 여행 소비 고객을 선별하고 판매 가능한 카드의 예상 연간 혜택을 계산해 추천합니다.

## 설계 원칙

- LLM은 자연어를 구조화된 Plan으로 변환하며 DB나 SQL을 직접 실행하지 않습니다.
- 모든 Plan은 JSON Schema, Tool allowlist, 기간·금액·정책 범위를 통과해야 실행됩니다.
- 금액과 추천 순위는 결정론적인 Java Tool이 계산합니다.
- Audit에는 원본 거래나 자연어 Prompt 대신 실행 메타데이터와 건수 요약만 저장합니다.
- `poc` 프로필은 합성 데이터와 Mock LLM만 사용하며 실제 고객 데이터나 외부 AI API를 호출하지 않습니다.

## 실행

요구 사항은 Java 21입니다.

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun --args='--spring.profiles.active=poc'
```

기본 주소는 `http://localhost:8080`입니다.

### 자연어 요청 실행

```powershell
$headers = @{
  'X-Requester-Id' = 'business-user'
  'X-Business-Purpose' = 'CARD_RECOMMENDATION'
  'X-Data-As-Of' = '2026-09-03'
}
$body = @{ request = '최근 3개월 카페 월평균 10만원 이상 고객에게 카드를 추천해줘' } | ConvertTo-Json
Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/v1/business-analysis/execute' `
  -Headers $headers -ContentType 'application/json' -Body $body
```

검증된 Plan JSON을 직접 실행하려면 같은 헤더와 함께 `POST /api/v1/analysis-plans/execute`를 사용합니다. 선택 헤더 `X-Max-Result-Count`의 허용 범위는 1~1000이며 기본값은 100입니다.

## 주요 패키지

- `analysis`: Plan 도메인, Schema 검증, 저장, 실행 API
- `ai`: 모델 독립적인 `LlmClient` 포트와 자연어 실행 흐름
- `tool`: 소비 집계, 고객군 필터, 상품 검색, 혜택 계산, 추천 순위
- `audit`: 요청부터 Tool 실행 결과까지의 최소 감사 기록
- `support`: PoC 합성 데이터, Mock LLM, Spring 설정

## 확장성 검증

- 카페 카드 추천과 해외여행 카드 추천이 동일한 5개 Data Tool 순서를 사용합니다.
- 두 번째 Use Case는 신규 업무 Tool이나 전용 계산 로직 없이 카테고리 allowlist, 합성 데이터, Plan 설정만 확장했습니다.
- 여행/해외 키워드가 포함된 자연어 요청은 `TRAVEL` Plan으로 변환되며 나머지는 PoC 기본 `CAFE` Plan으로 처리됩니다.

## PoC 한계

- 인증 서버 대신 `X-Requester-Id` 헤더를 사용하므로 운영 환경에서는 인증 주체로 교체해야 합니다.
- 저장소와 Audit은 인메모리이며 재시작하면 초기화됩니다.
- Mock LLM은 카페 카드 추천 Plan을 고정 반환합니다. 실제 Private LLM 연동 시 `LlmClient` Adapter만 교체합니다.
- 실제 메시지 발송, 카드 발급 등 금융 Action은 수행하지 않습니다.
- 법률·보안·개인정보 검토와 운영 DB 연결은 별도 단계입니다.
