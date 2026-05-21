# Liveclass BE-B 크리에이터 정산 API

## 프로젝트 개요

크리에이터가 판매한 강의 매출을 기준으로 플랫폼 수수료와 취소/환불 금액을 반영하여 정산 금액을 계산하는 Spring Boot 백엔드 API입니다.

주요 기능은 다음과 같습니다.

- 판매 내역 등록 및 조회
- 취소/환불 내역 등록
- 크리에이터별 월별 정산 계산
- 운영자용 기간별 전체 정산 집계
- 정산 생성 및 상태 관리
- 동일 기간 중복 정산 방지
- 저장된 정산 내역 XLSX 다운로드

샘플 데이터는 `src/main/resources/data.sql`을 통해 애플리케이션 시작 시 자동 삽입됩니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.14
- Spring Web
- Spring Data JPA
- Bean Validation
- H2 Database
- Gradle
- Lombok
- Apache POI
- JUnit 5 / Mockito / MockMvc

## 실행 방법

### 로컬 실행

```bash
./gradlew bootRun
```

Windows 환경에서는 다음 명령어를 사용할 수 있습니다.

```bash
gradlew.bat bootRun
```

서버 실행 후 기본 주소는 다음과 같습니다.

```text
http://localhost:8080
```

H2 Console:

```text
http://localhost:8080/h2-console
```

기본 H2 접속 정보:

```text
JDBC URL: jdbc:h2:mem:settlement
Username: sa
Password:
```

### Docker 실행

Docker Desktop 실행 후 다음 명령어를 사용합니다.

```bash
docker build -t liveclass-be-b:local .
docker run -p 8080:8080 liveclass-be-b:local
```

## 요구사항 해석 및 가정

크리에이터는 강의를 판매하고, 플랫폼은 판매 금액에서 수수료와 환불 금액을 차감하여 정산 금액을 계산합니다.

정산은 판매 1건마다 확정되는 방식이 아니라, **크리에이터와 정산 연월 단위로 집계되는 월별 정산**으로 해석했습니다. 판매/취소는 각각 개별 이벤트로 저장하고, 정산 계산 시 각 건의 발생 일시를 기준으로 해당 기간 포함 여부를 판단합니다.

기간 기준은 다음과 같습니다.

- 판매 금액 반영 기준: `SaleRecord.paidAt`
- 취소/환불 금액 반영 기준: `CancelRecord.canceledAt`
- 월 경계 기준: KST
- 구현 방식: 시작 시각 이상, 다음 달 시작 시각 미만
    - 예: 2025-03 정산
    - `2025-03-01T00:00:00+09:00 <= paidAt < 2025-04-01T00:00:00+09:00`

따라서 1월에 판매되고 2월에 취소된 경우, 판매 금액은 1월 정산에 반영되고 환불 금액은 2월 정산에 반영됩니다.

환불만 존재하는 기간에는 순 판매 금액과 정산 예정 금액이 음수가 될 수 있습니다. 이는 취소 일시 기준으로 환불분을 차감한다는 요구사항에 따른 결과로 보았습니다.

## 설계 결정과 이유

### 실시간 계산과 정산 스냅샷 분리

월별 정산 조회와 운영자 기간 집계는 판매/취소 원천 데이터를 기준으로 실시간 계산합니다.

반면 운영자가 정산을 생성하면 계산 결과를 `Settlement` 엔티티에 저장합니다. 저장된 정산은 당시 계산 결과의 스냅샷이며, 이후 원천 데이터나 수수료율 설정이 변경되어도 기존 저장 정산은 자동 변경되지 않습니다.

### 정산 상태 관리

선택 구현 항목인 정산 상태 관리를 위해 다음 상태 전이를 구현했습니다.

```text
PENDING -> CONFIRMED -> PAID
```

- `PENDING`: 정산 계산 결과가 생성된 상태
- `CONFIRMED`: 운영자가 정산 금액을 확정한 상태
- `PAID`: 지급 완료 상태

상태 전이 규칙은 `Settlement` 엔티티 내부 메서드에서 검증합니다.

### 중복 정산 방지

동일 크리에이터와 정산 연월 조합은 중복 생성할 수 없도록 했습니다.

- 서비스 레벨에서 사전 검증
- DB unique 제약으로 동시 요청 상황 최종 방어

```text
unique creator_id + settlement_month
```

### 수수료율 설계

현재 수수료율은 기본 20%이며 설정값으로 변경할 수 있습니다.

```properties
SETTLEMENT_PLATFORM_FEE_RATE=20
```

수수료 계산은 `PlatformFeePolicy` 인터페이스로 분리했습니다. 또한 `Settlement` 생성 시 적용된 `feeRate`, `platformFeeAmount`, `settlementAmount`를 저장하여 과거 정산 스냅샷이 이후 설정 변경에 영향받지 않도록 했습니다.

### 엑셀 다운로드

엑셀 다운로드는 실시간 집계 결과가 아니라 저장된 `Settlement` 정산 스냅샷을 기준으로 합니다. 따라서 다운로드 대상 데이터를 만들려면 먼저 정산 생성 API를 통해 정산 내역을 저장해야 합니다.

## 검증 시나리오

### 1. 부분 환불

`sale-4`는 결제 금액이 80,000원이고, `cancel-2`는 환불 금액이 30,000원입니다.

이 케이스를 통해 환불 금액이 원결제 금액보다 작은 경우에도 순 판매 금액에 정확히 반영되는지 검증했습니다.

```text
원 결제 금액: 80,000
환불 금액: 30,000
순 반영 금액: 50,000
```

### 2. 월 경계 취소

`sale-5`는 2025년 1월 31일 23:30에 결제되었고, `cancel-3`은 2025년 2월 1일 09:00에 취소되었습니다.

이 케이스를 통해 판매는 결제 완료 일시 기준으로 1월 정산에 포함되고, 취소는 취소 일시 기준으로 2월 정산에 포함되는지 검증했습니다.

```text
sale-5 -> 2025-01 판매 금액 반영
cancel-3 -> 2025-02 환불 금액 반영
```

### 3. 빈 월 조회

`creator-3`은 2025년 2월 판매 내역은 있지만 2025년 3월 판매/취소 내역은 없습니다.

이 케이스를 통해 정산 대상 데이터가 없는 월을 조회했을 때 예외가 아니라 0원 응답을 반환하도록 검증했습니다.

```text
총 판매 금액: 0
총 환불 금액: 0
순 판매 금액: 0
정산 예정 금액: 0
```

### 4. 동일 월 다수 판매 및 다수 취소

`creator-1`의 2025년 3월 데이터는 여러 판매 건과 여러 취소 건을 포함합니다.

이 케이스를 통해 단일 건이 아니라 여러 건을 합산했을 때 총 판매 금액, 환불 금액, 판매 건수, 취소 건수가 정확히 계산되는지 검증했습니다.

```text
판매 건수: 4
취소 건수: 2
총 판매 금액: 260,000
총 환불 금액: 110,000
```

### 5. 잘못된 연월 형식

월별 정산 조회와 정산 생성, 엑셀 다운로드에서 `yyyy-MM` 형식이 아닌 값이 들어오면 400 응답을 반환하도록 검증했습니다.

예시:

```text
2025/03
2025-3
invalid
```

검증 목적:

```text
잘못된 날짜 형식으로 인한 잘못된 정산 계산 방지
```

### 6. 잘못된 기간 요청

운영자 기간 집계와 엑셀 다운로드에서 시작일 또는 시작 월이 종료 값보다 늦은 경우 400 응답을 반환하도록 검증했습니다.

예시:

```text
startDate=2025-04-01&endDate=2025-03-31
startMonth=2025-04&endMonth=2025-03
```

검증 목적:

```text
의미 없는 역방향 기간 조회 방지
```

### 7. 환불 금액 초과 요청

취소/환불 등록 시 누적 환불 금액이 원결제 금액을 초과하면 등록할 수 없도록 검증했습니다.

검증 목적:

```text
부분 환불이 여러 번 발생하더라도 총 환불 금액이 결제 금액을 넘지 않도록 방어
```

### 8. 중복 정산 생성

같은 크리에이터와 같은 정산 연월에 대해 정산을 중복 생성할 수 없도록 검증했습니다.

검증 목적:

```text
동일 기간 중복 정산 방지
```

방어 방식:

```text
서비스 레벨 사전 검증 + DB unique 제약
```

### 9. 정산 상태 전이

저장된 정산 내역은 다음 상태 전이만 허용하도록 검증했습니다.

```text
PENDING -> CONFIRMED -> PAID
```

검증 목적:

```text
PENDING 상태에서 바로 PAID 처리하거나, 이미 확정/지급된 정산을 잘못 변경하는 것을 방지
```

### 10. 저장된 정산 내역 엑셀 다운로드

정산 생성 API로 저장된 `Settlement` 데이터를 기준으로 XLSX 파일이 생성되는지 검증했습니다.

검증 목적:

```text
실시간 집계 결과가 아니라 저장된 정산 스냅샷 기준으로 다운로드되는지 확인
```

## 미구현 / 제약사항

- 인증/인가는 구현하지 않았습니다.
- 실제 결제 시스템 연동은 없습니다.
- 수수료율 변경 이력 테이블은 구현하지 않았습니다.
- 정산 확정 이후 재계산, 지급 취소, 반려 상태는 구현하지 않았습니다.
- 엑셀 다운로드는 저장된 `Settlement` 기준이며, 실시간 집계 결과 다운로드는 제공하지 않습니다.

## AI 활용 범위

AI 도구는 다음 범위에서 활용했습니다.

- 요구사항 해석 및 구현 계획 검토
- ERD 및 패키지 구조 검토
- 리팩터링 방향 검토
- 테스트 케이스 설계 및 테스트 코드 작성 보조
- API 명세 및 README 문서화 보조
- 데모 화면 구성을 위한 프롬프트 작성
- 엑셀 다운로드 기능 구현 및 테스트 보조

AI가 제안한 내용은 직접 검토하고 프로젝트 구조에 맞게 수정하여 반영했습니다.

## API 목록 및 예시

### 판매 등록

```http
POST /api/v1/sale-record/purchase
Content-Type: application/json
```

```json
{
  "id": "sale-100",
  "courseId": "course-1",
  "studentId": "student-100",
  "amount": 50000,
  "paidAt": "2025-03-05T10:00:00+09:00"
}
```

### 판매 내역 조회

```http
GET /api/v1/creators/{creatorId}/sale-records?startDate=2025-03-01&endDate=2025-03-31
```

### 취소/환불 등록

```http
POST /api/v1/cancel-records
Content-Type: application/json
```

```json
{
  "id": "cancel-100",
  "saleRecordId": "sale-100",
  "refundAmount": 30000,
  "canceledAt": "2025-03-10T10:00:00+09:00"
}
```

### 크리에이터 월별 정산 조회

```http
GET /api/v1/settlements/creators/creator-1/monthly?date=2025-03
```

예상 응답:

```json
{
  "success": true,
  "data": {
    "totalSalesAmount": 260000,
    "totalRefundAmount": 110000,
    "netSalesAmount": 150000,
    "platformFeeAmount": 30000,
    "settlementAmount": 120000,
    "salesCount": 4,
    "cancelCount": 2
  }
}
```

### 운영자 기간별 정산 집계

```http
GET /api/v1/admin/settlements?startDate=2025-03-01&endDate=2025-03-31
```

### 월별 정산 생성

```http
POST /api/v1/admin/settlements/monthly
Content-Type: application/json
```

```json
{
  "creatorId": "creator-1",
  "settlementMonth": "2025-03"
}
```

생성 시 상태는 `PENDING`입니다.

### 정산 확정

```http
PATCH /api/v1/admin/settlements/{settlementId}/confirm
```

`PENDING` 상태만 확정할 수 있습니다.

### 지급 완료

```http
PATCH /api/v1/admin/settlements/{settlementId}/pay
```

`CONFIRMED` 상태만 지급 완료 처리할 수 있습니다.

### 정산 내역 엑셀 다운로드

```http
GET /api/v1/admin/settlements/excel?startMonth=2025-01&endMonth=2025-03
```

응답은 `.xlsx` 파일입니다.

```text
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="settlements_2025-01_2025-03.xlsx"
```

## 데이터 모델 설명

![liveclass_erd.jpg](/liveclass_erd.jpg)

### Creator

크리에이터 정보를 저장합니다.

- `id`: 크리에이터 ID
- `name`: 크리에이터 이름

### Course

강의 정보를 저장합니다.

- `id`: 강의 ID
- `creator_id`: 크리에이터 ID
- `title`: 강의 제목

하나의 크리에이터는 여러 강의를 가질 수 있습니다.

### SaleRecord

판매 내역을 저장합니다.

- `id`: 판매 내역 ID
- `course_id`: 강의 ID
- `student_id`: 수강생 ID
- `amount`: 결제 금액
- `paid_at`: 결제 일시

정산 시 판매 금액은 `paid_at` 기준으로 기간에 포함됩니다.

### CancelRecord

취소/환불 내역을 저장합니다.

- `id`: 취소 내역 ID
- `sale_record_id`: 원본 판매 내역 ID
- `amount`: 환불 금액
- `canceled_at`: 취소 일시

정산 시 환불 금액은 `canceled_at` 기준으로 기간에 포함됩니다.

### Settlement

운영자가 생성한 정산 스냅샷을 저장합니다.

- `id`: 정산 ID
- `creator_id`: 크리에이터 ID
- `settlement_month`: 정산 연월
- `total_sales_amount`: 총 판매 금액
- `total_refund_amount`: 총 환불 금액
- `net_sales_amount`: 순 판매 금액
- `fee_rate`: 적용 수수료율
- `platform_fee_amount`: 플랫폼 수수료
- `settlement_amount`: 정산 예정 금액
- `sales_count`: 판매 건수
- `cancel_count`: 취소 건수
- `status`: 정산 상태
- `calculated_at`: 정산 계산 일시
- `confirmed_at`: 정산 확정 일시
- `paid_at`: 지급 완료 일시

`creator_id`와 `settlement_month` 조합은 중복될 수 없습니다.

## 테스트 실행 방법

전체 테스트는 다음 명령어로 실행합니다.

```bash
./gradlew test
```

Windows 환경:

```bash
gradlew.bat test
```

테스트 범위:

- 판매 등록 서비스/컨트롤러 테스트
- 판매 내역 기간 조회 테스트
- 취소/환불 등록 테스트
- 환불 금액 초과 검증 테스트
- 월별 정산 계산 테스트
- 운영자 기간별 정산 집계 테스트
- KST 기준 월 경계 검증
- 정산 생성 및 중복 방지 테스트
- 정산 상태 전이 테스트
- XLSX 다운로드 서비스/컨트롤러 테스트
- XLSX 파일 생성 및 헤더/데이터 검증 테스트