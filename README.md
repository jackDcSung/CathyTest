# CoinDesk 幣別匯率 API

國泰世華 Java engineer 線上作業。以 Spring Boot 建立一組 REST API：維護「幣別 / 中文名稱」對應表，
並呼叫 CoinDesk API 取得比特幣報價，結合對應表轉換成新的 API 下行內容。

## Tech Stack

| 項目 | 選用 |
| --- | --- |
| Build Tool | Maven |
| JDK | 8 |
| Framework | Spring Boot 2.7.18（最後一個支援 Java 8 的版本） |
| ORM | Spring Data JPA (Hibernate) |
| Database | H2（in-memory） |
| Test | JUnit 5、Mockito、MockMvc、MockRestServiceServer |

## Project Structure

```
com.cathay.coindesk
├── client        CoinDesk HTTP 溝通與其 response contract
├── config        RestTemplate（含 timeout）設定
├── controller    HTTP 進入點，只做參數綁定、validation、回傳
├── dto           對外 API contract（request / response / error）
├── entity        JPA entity
├── exception     語意化例外 + @RestControllerAdvice
├── repository    persistence
└── service       use case、交易邊界、資料轉換
```

資料流：

```
Controller → CurrencyService → CurrencyRepository → H2
Controller → CoinDeskService → CoinDeskClient → CoinDesk API
                            → CurrencyRepository（一次取得中文名稱）
                            → CoinDeskResponseMapper（純轉換邏輯）
```

## How to Run

```bash
mvn clean package
mvn spring-boot:run
```

服務啟動於 `http://localhost:8080`。

> 需以 JDK 8 執行。若本機預設為其他版本：
> `JAVA_HOME=/path/to/jdk8 mvn spring-boot:run`

## How to Test

```bash
mvn clean test
```

共 34 個測試，全部不依賴外部網路。

| 測試類別 | 對應作業要求 |
| --- | --- |
| `CoinDeskResponseMapperTest` | 資料轉換邏輯單元測試（不啟動 Spring Context） |
| `CoinDeskServiceTest` | 轉換流程單元測試，mock Client 與 Repository |
| `CoinDeskClientTest` | 呼叫 CoinDesk API（`MockRestServiceServer`），含 timeout / 5xx / 格式錯誤 / 錯誤訊息不外洩上游內容 |
| `CurrencyControllerTest` | 幣別 CRUD API 端到端測試，並輸出回應內容 |
| `CoinDeskControllerTest` | 原始 API 與轉換後 API 端到端測試，並輸出回應內容 |

## H2 Console

啟動後開啟 <http://localhost:8080/h2-console>

| 欄位 | 值 |
| --- | --- |
| JDBC URL | `jdbc:h2:mem:coindesk` |
| User Name | `sa` |
| Password | （空白） |

## API Endpoints

### 幣別維護

| Method | Path | 說明 | 成功 | 失敗 |
| --- | --- | --- | --- | --- |
| GET | `/api/currencies` | 查詢全部（依代碼排序） | 200 | - |
| GET | `/api/currencies/{code}` | 查詢單筆 | 200 | 404 |
| POST | `/api/currencies` | 新增 | 201 | 400 / 409 |
| PUT | `/api/currencies/{code}` | 修改中文名稱 | 200 | 400 / 404 |
| DELETE | `/api/currencies/{code}` | 刪除 | 204 | 404 |

`{code}` 大小寫皆可，系統一律正規化為大寫。

### CoinDesk

| Method | Path | 說明 |
| --- | --- | --- |
| GET | `/api/coindesk/original` | 呼叫 CoinDesk API，回傳原始內容 |
| GET | `/api/coindesk/converted` | 呼叫 CoinDesk API 並轉換後回傳 |

## Request / Response Examples

新增幣別：

```bash
curl -i -X POST http://localhost:8080/api/currencies \
  -H "Content-Type: application/json" \
  -d '{"code":"aud","chineseName":"澳幣"}'
```

```
HTTP/1.1 201 Created
Location: /api/currencies/AUD

{"code":"AUD","chineseName":"澳幣","createdTime":"2026-09-10T21:30:11.24","updatedTime":"2026-09-10T21:30:11.24"}
```

轉換後 API：

```bash
curl http://localhost:8080/api/coindesk/converted
```

```json
{
  "updateTime": "2024/09/02 07:07:20",
  "currencies": [
    { "code": "EUR", "chineseName": "歐元", "rate": 52243.2865 },
    { "code": "GBP", "chineseName": "英鎊", "rate": 43984.0203 },
    { "code": "USD", "chineseName": "美元", "rate": 57756.2984 }
  ]
}
```

錯誤格式（所有錯誤一致）：

```json
{
  "timestamp": "2026-09-10T21:31:02.118",
  "status": 404,
  "error": "Not Found",
  "message": "查無幣別資料: XXX",
  "path": "/api/currencies/XXX"
}
```

| 情境 | HTTP Status |
| --- | --- |
| 查無幣別 | 404 Not Found |
| 幣別已存在 | 409 Conflict |
| 欄位驗證失敗 / JSON 無法解析 | 400 Bad Request |
| CoinDesk 無法取得或格式異常 | 502 Bad Gateway |
| 其他未預期錯誤 | 500 Internal Server Error |

## Database

Schema 由 `src/main/resources/schema.sql` 建立，測試資料由 `data.sql` 匯入，
`spring.jpa.hibernate.ddl-auto=none` 讓 Hibernate 不再自行產生 schema，避免兩份定義互相衝突。

```sql
CREATE TABLE currency (
    code         VARCHAR(3)  NOT NULL,
    chinese_name VARCHAR(50) NOT NULL,
    created_time TIMESTAMP   NOT NULL,
    updated_time TIMESTAMP   NOT NULL,
    CONSTRAINT pk_currency PRIMARY KEY (code)
);
```

初始資料包含 CoinDesk sample 會用到的 `USD` / `GBP` / `EUR`，另加 `TWD`、`JPY`。

## Design Decisions / Trade-offs

**Entity 與 DTO 分離**
Entity 是資料表結構，DTO 是 API contract，兩者變動原因不同。直接回傳 Entity 會讓欄位改名或新增欄位
立刻影響對外契約，也容易把不該外露的資料一併吐出去。

**CoinDesk 呼叫獨立成 Client**
`CoinDeskClient` 是唯一知道 URL 與 HTTP 細節的地方，Service 只依賴「取得 CoinDesk 資料」這件事。
好處是換掉 RestTemplate 或改走內部 gateway 時不影響 Service，測試也能直接以 `@MockBean` 取代。
URL 與 timeout 都放在 `application.yml`，不寫死在程式碼。

**測試中 mock 外部 API**
測試若真的打 CoinDesk，結果會取決於 GitHub Pages 當下是否可用，變成不可重現的 flaky test。
因此以 `MockRestServiceServer`（驗證 HTTP 層）與 `@MockBean`（驗證應用流程）取代真實呼叫，
反而能穩定測到 timeout、5xx、回應格式錯誤這些難以在真實環境重現的情境。

**幣別中文名稱一次查詢**
`CoinDeskService` 先蒐集本次需要的幣別代碼，用 `findByCodeIn(codes)` 一次取回，再於記憶體中 lookup，
避免在轉換迴圈裡逐筆 `findById` 造成 N+1。雖然目前只有 3 種幣別，但這是會隨資料量放大的成本。

**幣別代碼作為主鍵**
ISO 4217 代碼本身穩定且唯一，不需要再加一個代理主鍵；主鍵即滿足唯一性限制。
代價是日後若代碼需要變更會比較麻煩，但幣別代碼在實務上不會改。

**刪除不存在的資料回 404**
DELETE 在 REST 上可設計為冪等（回 204），這裡選擇與 GET / PUT 一致回 404，
讓呼叫端能明確知道「這筆資料本來就不存在」，維護工具的操作紀錄比較清楚。

**錯誤處理集中在 `@RestControllerAdvice`**
Controller 內不寫 try/catch。例外都是有語意的型別（`CurrencyNotFoundException`、
`CurrencyAlreadyExistsException`、`CoinDeskClientException`），由 `GlobalExceptionHandler` 統一
對應 HTTP status 與回應格式。特別把外部相依失敗對應到 502，與本系統自身的 500 區隔。
