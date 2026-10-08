-- 완전한 테이블 스키마 재생성 스크립트

-- 기존 테이블 모두 삭제 (대소문자 구분)
DROP TABLE IF EXISTS "STOCK_DAY" CASCADE;
DROP TABLE IF EXISTS "STOCK_WEEK" CASCADE;
DROP TABLE IF EXISTS "STOCK_MONTH" CASCADE;
DROP TABLE IF EXISTS "STOCK_YEAR" CASCADE;

DROP MATERIALIZED VIEW IF EXISTS ticks_day CASCADE;
DROP MATERIALIZED VIEW IF EXISTS ticks_week CASCADE;
DROP MATERIALIZED VIEW IF EXISTS ticks_month CASCADE;

-- 시계열/업무 테이블 전부 드롭 (존재하면)
DROP TABLE IF EXISTS "TICKS" CASCADE;
DROP TABLE IF EXISTS "ACCOUNT_TRANSACTION" CASCADE;
DROP TABLE IF EXISTS "MOCK_TRADE" CASCADE;
DROP TABLE IF EXISTS "STOCK_LOTS" CASCADE;
DROP TABLE IF EXISTS "STOCK_HOLDING" CASCADE;
DROP TABLE IF EXISTS "INTEREST" CASCADE;
DROP TABLE IF EXISTS "STOCK_INFO" CASCADE;
DROP TABLE IF EXISTS "ACCOUNTS" CASCADE;
DROP TABLE IF EXISTS "USER_BEHAVIOR" CASCADE;
DROP TABLE IF EXISTS "USER_PERSONALITY" CASCADE;
DROP TABLE IF EXISTS "PERSONALITY_QUESTION" CASCADE;
DROP TABLE IF EXISTS "USER_TOKEN" CASCADE;
DROP TABLE IF EXISTS "USERS" CASCADE;
DROP TABLE IF EXISTS "RECAP" CASCADE;
DROP TABLE IF EXISTS "HISTORY_CHALLENGE_NEWS" CASCADE;
DROP TABLE IF EXISTS "HISTORY_CHALLENGE_DATA" CASCADE;
DROP TABLE IF EXISTS "HISTORY_CHALLENGE" CASCADE;
DROP TABLE IF EXISTS "AI_STRATEGY" CASCADE;
DROP TABLE IF EXISTS "CHART_PATTERN" CASCADE;
DROP TABLE IF EXISTS "INVESTMENT_TERM" CASCADE;
DROP TABLE IF EXISTS "KNOW_TERM" CASCADE;
DROP TABLE IF EXISTS "QUIZ" CASCADE;
DROP TABLE IF EXISTS "DAILY_SOLVED" CASCADE;
DROP TABLE IF EXISTS "PUSH_NOTIFICATION" CASCADE;
DROP TABLE IF EXISTS "NEWS" CASCADE;
DROP TABLE IF EXISTS "FRAME" CASCADE;
DROP TABLE IF EXISTS "HAVE_FRAME" CASCADE;
DROP TABLE IF EXISTS "SHOP" CASCADE;
DROP TABLE IF EXISTS "HAVE_ITEM" CASCADE;
DROP TABLE IF EXISTS "DAILY_QUIZ_SCHEDULE" CASCADE;

-- 기존 테이블/뷰 삭제 (소문자)
DROP TABLE IF EXISTS stock_day CASCADE;
DROP TABLE IF EXISTS stock_week CASCADE;
DROP TABLE IF EXISTS stock_month CASCADE;
DROP TABLE IF EXISTS stock_year CASCADE;

DROP MATERIALIZED VIEW IF EXISTS ticks_day CASCADE;
DROP MATERIALIZED VIEW IF EXISTS ticks_week CASCADE;
DROP MATERIALIZED VIEW IF EXISTS ticks_month CASCADE;

DROP TABLE IF EXISTS ticks CASCADE;
DROP TABLE IF EXISTS account_transaction CASCADE;
DROP TABLE IF EXISTS mock_trade CASCADE;
DROP TABLE IF EXISTS stock_lots CASCADE;
DROP TABLE IF EXISTS stock_holding CASCADE;
DROP TABLE IF EXISTS interest CASCADE;
DROP TABLE IF EXISTS stock_info CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS user_behavior CASCADE;
DROP TABLE IF EXISTS user_personality CASCADE;
DROP TABLE IF EXISTS personality_question CASCADE;
DROP TABLE IF EXISTS user_token CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS recap CASCADE;
DROP TABLE IF EXISTS history_challenge_news CASCADE;
DROP TABLE IF EXISTS history_challenge_data CASCADE;
DROP TABLE IF EXISTS history_challenge CASCADE;
DROP TABLE IF EXISTS ai_strategy CASCADE;
DROP TABLE IF EXISTS chart_pattern CASCADE;
DROP TABLE IF EXISTS investment_term CASCADE;
DROP TABLE IF EXISTS know_term CASCADE;
DROP TABLE IF EXISTS quiz CASCADE;
DROP TABLE IF EXISTS daily_solved CASCADE;
DROP TABLE IF EXISTS push_notification CASCADE;
DROP TABLE IF EXISTS news CASCADE;
DROP TABLE IF EXISTS frame CASCADE;
DROP TABLE IF EXISTS have_frame CASCADE;
DROP TABLE IF EXISTS shop CASCADE;
DROP TABLE IF EXISTS have_item CASCADE;
DROP TABLE IF EXISTS daily_quiz_schedule CASCADE;

-- USERS
CREATE TABLE users (
  user_id bigint PRIMARY KEY,
  email text,
  social_login_id text,
  login_type text,
  nickname text,
  personality text,
  created_at timestamp,
  frame_id integer,
  deleted_at timestamp,
  is_ai boolean NOT NULL DEFAULT FALSE,
  ai_id integer
);

-- USER_TOKEN
CREATE TABLE user_token (
  token_id integer PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(user_id),
  refresh_token text,
  expired_at timestamp,
  created_at timestamp NOT NULL
);

-- USER_PERSONALITY
CREATE TABLE user_personality (
  test_id integer PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(user_id),
  personality text,
  tested_at timestamp NOT NULL
);

-- STOCK_INFO
CREATE TABLE stock_info (
  stock_info_id integer PRIMARY KEY,
  code text,
  name text,
  market text
);

-- ACCOUNTS
CREATE TABLE accounts (
  account_id bigint PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(user_id),
  balance integer,
  total_asset integer,
  profit integer,
  profit_rate double precision,
  type integer NOT NULL
);

-- INTEREST
CREATE TABLE interest (
  interest_id integer PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(user_id),
  stock_info_id integer NOT NULL REFERENCES stock_info(stock_info_id)
);

-- STOCK_HOLDING
CREATE TABLE stock_holding (
  holding_id integer PRIMARY KEY,
  account_id bigint NOT NULL REFERENCES accounts(account_id),
  stock_info_id integer NOT NULL REFERENCES stock_info(stock_info_id),
  quantity integer,
  total_price integer
);

-- MOCK_TRADE
CREATE TABLE mock_trade (
  trade_id integer PRIMARY KEY,
  account_id bigint NOT NULL REFERENCES accounts(account_id),
  stock_id integer REFERENCES stock_info(stock_info_id),
  buy_sell text, -- BUY/SELL
  quantity integer,
  price integer NOT NULL,
  trade_at timestamp NOT NULL,
  is_quiz boolean
);

-- HISTORY_CHALLENGE
CREATE TABLE history_challenge (
  challenge_id integer PRIMARY KEY,
  theme text,
  stock_name text,
  stock_code text,
  start_date timestamp,
  end_date timestamp,
  origin_date timestamp
);

-- HISTORY_CHALLENGE_DATA
CREATE TABLE history_challenge_data (
  challenge_data_id integer PRIMARY KEY,
  challenge_id integer NOT NULL REFERENCES history_challenge(challenge_id),
  event_date_time timestamp NOT NULL,
  origin_date_time timestamp NOT NULL,
  open_price integer,
  high_price integer,
  low_price integer,
  close_price integer,
  volume integer
);

-- HISTORY_CHALLENGE_NEWS
CREATE TABLE history_challenge_news (
  challenge_news_id integer PRIMARY KEY,
  challenge_id integer NOT NULL REFERENCES history_challenge(challenge_id),
  title text,
  content text,
  published_at timestamp NOT NULL
);

-- AI_STRATEGY
CREATE TABLE ai_strategy (
  strategy_id integer PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(user_id),
  strategy text NOT NULL,
  prompt text,
  created_at timestamp NOT NULL
);

-- CHART_PATTERN
CREATE TABLE chart_pattern (
  pattern_id integer PRIMARY KEY,
  name text,
  description text,
  chart_img text
);

-- INVESTMENT_TERM
CREATE TABLE investment_term (
  term_id integer PRIMARY KEY,
  term text,
  definition text,
  description text
);

-- KNOW_TERM
CREATE TABLE know_term (
  know_id integer PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(user_id),
  term_id integer NOT NULL REFERENCES investment_term(term_id),
  correct boolean
);

-- QUIZ
CREATE TABLE quiz (
  quiz_id integer PRIMARY KEY,
  question text,
  answer boolean,
  daily_date timestamp,
  explanation text,
  term_id integer REFERENCES investment_term(term_id)
);

-- DAILY_SOLVED
CREATE TABLE daily_solved (
  solved_id integer PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(user_id),
  quiz_id integer NOT NULL REFERENCES quiz(quiz_id),
  score integer,
  bonus_amount integer DEFAULT 0,
  ranking integer,
  solved_time_seconds integer,
  solved_at date NOT NULL,
  streak integer
);

-- NEWS
CREATE TABLE news (
  news_id integer PRIMARY KEY,
  title text,
  content text,
  sentiment text,
  summary text,
  published_at timestamp,
  type text
);

-- DAILY_QUIZ_SCHEDULE
CREATE TABLE daily_quiz_schedule (
  schedule_id int PRIMARY KEY,
  quiz_date date NOT NULL,
  quiz_id int NOT NULL,
  start_time timestamp NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(quiz_date)
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_interest_user ON interest(user_id);
CREATE INDEX IF NOT EXISTS idx_interest_sid ON interest(stock_info_id);
CREATE INDEX IF NOT EXISTS idx_mock_trade_acc ON mock_trade(account_id);

-- TICKS
CREATE TABLE ticks (
  stock_info_id integer NOT NULL REFERENCES stock_info(stock_info_id),
  ts timestamptz NOT NULL,
  open_price integer,
  high_price integer,
  low_price integer,
  close_price integer,
  volume bigint,
  PRIMARY KEY(stock_info_id, ts)
);

-- 하이퍼테이블로 변환 (종목별 파티셔닝)
SELECT create_hypertable('ticks', 'ts',
         partitioning_column => 'stock_info_id',
         number_partitions  => 2,
         if_not_exists      => TRUE);

-- 쿼리 패턴 인덱스
CREATE INDEX IF NOT EXISTS idx_ticks_sid_ts_desc ON ticks(stock_info_id, ts DESC);

-- Day
-- MATERIALIZED VIEW: ticks_day / ticks_week / ticks_month
CREATE MATERIALIZED VIEW ticks_day
WITH (timescaledb.continuous) AS
SELECT
  stock_info_id,
  time_bucket('1 day', ts) AS date,
  first(open_price, ts) AS open_price,
  max(high_price) AS high_price,
  min(low_price) AS low_price,
  last(close_price, ts) AS close_price,
  sum(volume) AS volume
FROM ticks
GROUP BY stock_info_id, time_bucket('1 day', ts);

-- Week
CREATE MATERIALIZED VIEW ticks_week
WITH (timescaledb.continuous) AS
SELECT
  stock_info_id,
  time_bucket('1 week', ts) AS date,
  first(open_price, ts) AS open_price,
  max(high_price) AS high_price,
  min(low_price) AS low_price,
  last(close_price, ts) AS close_price,
  sum(volume) AS volume
FROM ticks
GROUP BY stock_info_id, time_bucket('1 week', ts);

-- Month
CREATE MATERIALIZED VIEW ticks_month
WITH (timescaledb.continuous) AS
SELECT
  stock_info_id,
  time_bucket('1 month', ts) AS date,
  first(open_price, ts) AS open_price,
  max(high_price) AS high_price,
  min(low_price) AS low_price,
  last(close_price, ts) AS close_price,
  sum(volume) AS volume
FROM ticks
GROUP BY stock_info_id, time_bucket('1 month', ts);

-- === 기존 테이블명 호환용 READ-ONLY VIEW들 ===
-- stock_*_id는 뷰에서 의미가 없으니 가상 ID 제공: row_number()

-- STOCK_DAY / STOCK_WEEK / STOCK_MONTH / STOCK_YEAR VIEW
CREATE OR REPLACE VIEW stock_day AS
SELECT
  row_number() OVER (ORDER BY stock_info_id, date)::int AS stock_day_id,
  stock_info_id,
  date::date AS date,
  open_price, high_price, low_price, close_price,
  NULL::double precision AS fluctuation_rate,
  volume::integer
FROM ticks_day;

CREATE OR REPLACE VIEW stock_week AS
SELECT
  row_number() OVER (ORDER BY stock_info_id, date)::int AS stock_week_id,
  stock_info_id,
  extract(epoch FROM date)::int AS date,
  open_price, high_price, low_price, close_price,
  NULL::double precision AS fluctuation_rate,
  volume::integer
FROM ticks_week;

CREATE OR REPLACE VIEW stock_month AS
SELECT
  row_number() OVER (ORDER BY stock_info_id, date)::int AS stock_mon_id,
  stock_info_id,
  (extract(year from date)::int * 100 + extract(month from date)::int) AS date,
  open_price, high_price, low_price, close_price,
  NULL::double precision AS fluctuation_rate,
  volume::integer
FROM ticks_month;

CREATE OR REPLACE VIEW stock_year AS
SELECT
  row_number() OVER (ORDER BY stock_info_id, date)::int AS stock_year_id,
  stock_info_id,
  extract(year from date)::int AS date,
  open_price, high_price, low_price, close_price,
  NULL::double precision AS fluctuation_rate,
  sum(volume)::bigint AS volume
FROM ticks_month
GROUP BY stock_info_id, extract(year from date), open_price, high_price, low_price, close_price;

-- 단일 PK 컬럼에 IDENTITY 추가(AUTO_INCREMENT)
ALTER TABLE users ALTER COLUMN user_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE user_token ALTER COLUMN token_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE user_personality ALTER COLUMN test_id ADD GENERATED BY DEFAULT AS IDENTITY;

ALTER TABLE stock_info ALTER COLUMN stock_info_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE accounts ALTER COLUMN account_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE interest ALTER COLUMN interest_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE stock_holding ALTER COLUMN holding_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE mock_trade ALTER COLUMN trade_id ADD GENERATED BY DEFAULT AS IDENTITY;

ALTER TABLE history_challenge ALTER COLUMN challenge_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE history_challenge_data ALTER COLUMN challenge_data_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE history_challenge_news ALTER COLUMN challenge_news_id ADD GENERATED BY DEFAULT AS IDENTITY;

ALTER TABLE ai_strategy ALTER COLUMN strategy_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE chart_pattern ALTER COLUMN pattern_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE investment_term ALTER COLUMN term_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE know_term ALTER COLUMN know_id ADD GENERATED BY DEFAULT AS IDENTITY;

ALTER TABLE quiz ALTER COLUMN quiz_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE daily_solved ALTER COLUMN solved_id ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE news ALTER COLUMN news_id ADD GENERATED BY DEFAULT AS IDENTITY;

ALTER TABLE daily_quiz_schedule ALTER COLUMN schedule_id ADD GENERATED BY DEFAULT AS IDENTITY;