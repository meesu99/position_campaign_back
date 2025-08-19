-- 관리자 계정 생성 (비밀번호: admin123)
INSERT INTO app_users (email, password_hash, business_no, company_name, points, role, created_at) 
VALUES ('admin@example.com', '$2a$10$CwTycUXWue0Thq9StjUM0uJ6wGqe9fkNEKzBhV3.5EIxkTHqHcLpC', '123-45-67890', 'Admin Company', 100000, 'ADMIN', NOW());

-- 일반 사용자 계정 생성 (비밀번호: user123)  
INSERT INTO app_users (email, password_hash, business_no, company_name, points, role, created_at) 
VALUES ('user@example.com', '$2a$10$7eqOqFuBiNh.uokj9H.kLuJ5G1HNJa.UvLLi.8OkNu5l9SRX7VDHC', '987-65-43210', 'User Company', 50000, 'USER', NOW());

-- 수도권 더미 고객 데이터 생성
INSERT INTO customers (name, gender, birth_year, phone, road_address, detail_address, postal_code, sido, sigungu, lat, lng, geom, created_at) VALUES
('김민수', 'M', 1985, '010-1234-5678', '서울특별시 중구 세종대로 110', '시청 앞', '04524', '서울특별시', '중구', 37.5665, 126.9780, ST_SetSRID(ST_MakePoint(126.9780, 37.5665), 4326)::geography, NOW()),
('박지영', 'F', 1990, '010-2345-6789', '서울특별시 강남구 테헤란로 152', '강남역 근처', '06236', '서울특별시', '강남구', 37.4979, 127.0276, ST_SetSRID(ST_MakePoint(127.0276, 37.4979), 4326)::geography, NOW()),
('이철수', 'M', 1978, '010-3456-7890', '서울특별시 마포구 홍익로 94', '홍대입구역', '04039', '서울특별시', '마포구', 37.5511, 126.9230, ST_SetSRID(ST_MakePoint(126.9230, 37.5511), 4326)::geography, NOW()),
('최영희', 'F', 1982, '010-4567-8901', '서울특별시 송파구 올림픽로 300', '잠실역 근처', '05551', '서울특별시', '송파구', 37.5133, 127.1028, ST_SetSRID(ST_MakePoint(127.1028, 37.5133), 4326)::geography, NOW()),
('장동건', 'M', 1995, '010-5678-9012', '경기도 성남시 분당구 판교역로 235', '판교테크노밸리', '13494', '경기도', '성남시 분당구', 37.3925, 127.1107, ST_SetSRID(ST_MakePoint(127.1107, 37.3925), 4326)::geography, NOW()),
('김영미', 'F', 1987, '010-6789-0123', '경기도 고양시 일산동구 중앙로 1036', '일산신도시', '10380', '경기도', '고양시 일산동구', 37.6566, 126.7695, ST_SetSRID(ST_MakePoint(126.7695, 37.6566), 4326)::geography, NOW()),
('이상호', 'M', 1993, '010-7890-1234', '서울특별시 영등포구 여의도동 11', '여의도역', '07328', '서울특별시', '영등포구', 37.5219, 126.9245, ST_SetSRID(ST_MakePoint(126.9245, 37.5219), 4326)::geography, NOW()),
('박현정', 'F', 1980, '010-8901-2345', '서울특별시 서초구 강남대로 200', '교대역', '06526', '서울특별시', '서초구', 37.4949, 127.0144, ST_SetSRID(ST_MakePoint(127.0144, 37.4949), 4326)::geography, NOW()),
('정민석', 'M', 1991, '010-9012-3456', '인천광역시 연수구 송도과학로 123', '송도국제도시', '21984', '인천광역시', '연수구', 37.3891, 126.6453, ST_SetSRID(ST_MakePoint(126.6453, 37.3891), 4326)::geography, NOW()),
('김수연', 'F', 1988, '010-0123-4567', '서울특별시 종로구 종로 69', '종각역', '03155', '서울특별시', '종로구', 37.5700, 126.9830, ST_SetSRID(ST_MakePoint(126.9830, 37.5700), 4326)::geography, NOW()),
('이성민', 'M', 1986, '010-1357-2468', '서울특별시 강서구 공항대로 260', '김포공항', '07505', '서울특별시', '강서구', 37.5585, 126.7942, ST_SetSRID(ST_MakePoint(126.7942, 37.5585), 4326)::geography, NOW()),
('박나영', 'F', 1992, '010-2468-1357', '경기도 수원시 영통구 월드컵로 206', '수원월드컵경기장', '16419', '경기도', '수원시 영통구', 37.2859, 127.0369, ST_SetSRID(ST_MakePoint(127.0369, 37.2859), 4326)::geography, NOW()),
('최준혁', 'M', 1989, '010-3691-4825', '서울특별시 관악구 신림로 1', '신림역', '08826', '서울특별시', '관악구', 37.4842, 126.9296, ST_SetSRID(ST_MakePoint(126.9296, 37.4842), 4326)::geography, NOW()),
('윤소정', 'F', 1994, '010-4825-3691', '서울특별시 동대문구 청량리로 588', '청량리역', '02618', '서울특별시', '동대문구', 37.5802, 127.0474, ST_SetSRID(ST_MakePoint(127.0474, 37.5802), 4326)::geography, NOW()),
('강태현', 'M', 1983, '010-5947-2816', '경기도 안양시 만안구 안양로 119', '안양시청', '14041', '경기도', '안양시 만안구', 37.3943, 126.9568, ST_SetSRID(ST_MakePoint(126.9568, 37.3943), 4326)::geography, NOW()),
('황민지', 'F', 1996, '010-6173-8425', '서울특별시 금천구 가산디지털1로 168', '가산디지털단지', '08506', '서울특별시', '금천구', 37.4817, 126.8819, ST_SetSRID(ST_MakePoint(126.8819, 37.4817), 4326)::geography, NOW()),
('조현우', 'M', 1977, '010-7284-9163', '서울특별시 노원구 노원로 437', '노원역', '01695', '서울특별시', '노원구', 37.6541, 127.0615, ST_SetSRID(ST_MakePoint(127.0615, 37.6541), 4326)::geography, NOW()),
('신예린', 'F', 1985, '010-8395-6274', '경기도 부천시 원미구 길주로 210', '부천시청', '14662', '경기도', '부천시 원미구', 37.5058, 126.7659, ST_SetSRID(ST_MakePoint(126.7659, 37.5058), 4326)::geography, NOW()),
('임동호', 'M', 1990, '010-9406-5183', '서울특별시 구로구 구로중앙로 235', '구로디지털단지', '08391', '서울특별시', '구로구', 37.4855, 126.9018, ST_SetSRID(ST_MakePoint(126.9018, 37.4855), 4326)::geography, NOW()),
('오세영', 'F', 1981, '010-0517-4829', '서울특별시 강북구 수유로 47', '수유역', '01134', '서울특별시', '강북구', 37.6384, 127.0253, ST_SetSRID(ST_MakePoint(127.0253, 37.6384), 4326)::geography, NOW());

-- 샘플 캠페인 데이터
INSERT INTO campaigns (user_id, title, message_text, link, filters, price_per_recipient, estimated_cost, final_cost, recipients_count, status, created_at) 
VALUES 
(2, '갤럭시 폴드7 최저가 판매', '🔥 갤럭시 폴드7 최저가 특가! 지금 바로 확인하세요!', 'https://shop.kt.com/', '{"gender": "F", "ageRange": [25, 35], "region": {"sido": "서울특별시"}}', 70, 7000, 7000, 10, 'COMPLETED', NOW()),
(2, 'KT 5G 요금제 이벤트', 'KT 5G 무제한 요금제로 갈아타고 혜택 받아가세요!', 'https://shop.kt.com/5g', '{"gender": "M", "ageRange": [30, 40]}', 50, 5000, 5000, 10, 'DRAFT', NOW());

-- 샘플 캠페인 타겟 데이터
INSERT INTO campaign_targets (campaign_id, customer_id, delivery_status, sent_at, read_at, click_at) VALUES
(1, 2, 'DELIVERED', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '30 minutes'),
(1, 4, 'DELIVERED', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour 30 minutes', NULL),
(1, 8, 'DELIVERED', NOW() - INTERVAL '2 hours', NULL, NULL),
(1, 10, 'DELIVERED', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '45 minutes', NULL),
(1, 12, 'DELIVERED', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour 15 minutes', NOW() - INTERVAL '20 minutes'),
(1, 14, 'DELIVERED', NOW() - INTERVAL '2 hours', NULL, NULL),
(1, 16, 'DELIVERED', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '30 minutes', NULL),
(1, 18, 'DELIVERED', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour 45 minutes'),
(1, 20, 'DELIVERED', NOW() - INTERVAL '2 hours', NULL, NULL);

-- 지갑 거래 내역
INSERT INTO wallet_transactions (user_id, type, amount, balance_after, meta, created_at) VALUES
(2, 'CHARGE', 50000, 50000, '{"method": "credit_card", "card_last4": "1234"}', NOW()),
(2, 'DEBIT_CAMPAIGN', -7000, 43000, '{"campaign_id": 1, "recipients": 10, "unit_price": 70}', NOW());

-- 채팅 메시지 (인앱 알림)
INSERT INTO chat_messages (user_id, from_admin, campaign_id, text, link, created_at) VALUES
(2, true, 1, '갤럭시 폴드7 최저가 판매 캠페인이 성공적으로 발송되었습니다.', NULL, NOW()),
(2, true, NULL, '🎉 KT 쇼핑몰에서 새로운 이벤트가 시작되었습니다!', 'https://shop.kt.com/events', NOW());