-- 필수 확장
CREATE EXTENSION IF NOT EXISTS postgis;

-- 사용자 계정
CREATE TABLE IF NOT EXISTS app_users (
  id               BIGSERIAL PRIMARY KEY,
  email            VARCHAR(255) NOT NULL UNIQUE,
  password_hash    VARCHAR(255) NOT NULL,
  business_no      VARCHAR(64)  NOT NULL,
  company_name     VARCHAR(255) NOT NULL,
  points           BIGINT       NOT NULL DEFAULT 0,
  role             TEXT         NOT NULL DEFAULT 'USER', -- USER | ADMIN
  created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 수신 대상 고객
CREATE TABLE IF NOT EXISTS customers (
  id              BIGSERIAL PRIMARY KEY,
  name            VARCHAR(100),
  gender          VARCHAR(16),         -- index
  birth_year      INT,                 -- index
  phone           VARCHAR(50),
  road_address    VARCHAR(255),
  detail_address  VARCHAR(255),
  postal_code     VARCHAR(20),
  sido            VARCHAR(50),         -- composite index(sido,sigungu)
  sigungu         VARCHAR(80),         -- composite index(sido,sigungu)
  lat             DOUBLE PRECISION,
  lng             DOUBLE PRECISION,
  geom            GEOGRAPHY(POINT,4326) NOT NULL, -- 반경 필터 필수 사용
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 캠페인 정의
CREATE TABLE IF NOT EXISTS campaigns (
  id                   BIGSERIAL PRIMARY KEY,
  user_id              BIGINT NOT NULL REFERENCES app_users(id),
  title                VARCHAR(200),
  message_text         TEXT,
  link                 VARCHAR(500),
  filters              JSONB,
  price_per_recipient  INT,
  estimated_cost       BIGINT,
  final_cost           BIGINT,
  recipients_count     INT,
  status               TEXT,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 캠페인 대상 스냅샷
CREATE TABLE IF NOT EXISTS campaign_targets (
  id               BIGSERIAL PRIMARY KEY,
  campaign_id      BIGINT NOT NULL REFERENCES campaigns(id),
  customer_id      BIGINT NOT NULL REFERENCES customers(id),
  delivery_status  TEXT,
  sent_at          TIMESTAMPTZ,
  read_at          TIMESTAMPTZ,
  click_at         TIMESTAMPTZ
);

-- 지갑 거래 원장
CREATE TABLE IF NOT EXISTS wallet_transactions (
  id             BIGSERIAL PRIMARY KEY,
  user_id        BIGINT NOT NULL REFERENCES app_users(id),
  type           TEXT, -- CHARGE | DEBIT_CAMPAIGN | REFUND
  amount         BIGINT,
  balance_after  BIGINT,
  meta           JSONB,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 인앱 알림 로그
CREATE TABLE IF NOT EXISTS chat_messages (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES app_users(id),
  from_admin   BOOLEAN DEFAULT TRUE,
  campaign_id  BIGINT REFERENCES campaigns(id),
  text         TEXT,
  link         VARCHAR(500),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_customers_gender       ON customers(gender);
CREATE INDEX IF NOT EXISTS idx_customers_birth_year   ON customers(birth_year);
CREATE INDEX IF NOT EXISTS idx_customers_region       ON customers(sido, sigungu);
CREATE INDEX IF NOT EXISTS idx_customers_geom_gist    ON customers USING GIST(geom);

CREATE INDEX IF NOT EXISTS idx_ct_campaign_id         ON campaign_targets(campaign_id);
CREATE INDEX IF NOT EXISTS idx_ct_delivery_status     ON campaign_targets(delivery_status);
