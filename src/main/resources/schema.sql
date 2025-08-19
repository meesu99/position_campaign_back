-- PostGIS 확장은 이미 데이터베이스에 설치되어 있습니다.
-- JPA가 테이블을 생성한 후 추가 인덱스를 생성합니다.

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_customers_gender       ON customers(gender);
CREATE INDEX IF NOT EXISTS idx_customers_birth_year   ON customers(birth_year);
CREATE INDEX IF NOT EXISTS idx_customers_region       ON customers(sido, sigungu);
CREATE INDEX IF NOT EXISTS idx_customers_geom_gist    ON customers USING GIST(geom);

CREATE INDEX IF NOT EXISTS idx_ct_campaign_id         ON campaign_targets(campaign_id);
CREATE INDEX IF NOT EXISTS idx_ct_delivery_status     ON campaign_targets(delivery_status);