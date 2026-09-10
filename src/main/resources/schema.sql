-- 幣別代碼與其對應中文名稱的維護表。
-- 每次啟動先 DROP 再建立：測試會啟動多個 Spring context 共用同一個 in-memory DB，
-- 不先清掉會在第二個 context 撞到 table already exists。
DROP TABLE IF EXISTS currency;

CREATE TABLE currency (
    code         VARCHAR(3)  NOT NULL,   -- ISO 4217 幣別代碼，一律以大寫儲存
    chinese_name VARCHAR(50) NOT NULL,
    created_time TIMESTAMP   NOT NULL,
    updated_time TIMESTAMP   NOT NULL,
    CONSTRAINT pk_currency PRIMARY KEY (code)
);
