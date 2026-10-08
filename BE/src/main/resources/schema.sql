-- News 테이블 생성 스크립트
CREATE TABLE IF NOT EXISTS news (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    summary TEXT,
    url VARCHAR(1000) UNIQUE,
    published_date TIMESTAMP,
    source VARCHAR(100),
    stock_code VARCHAR(10),
    stock_name VARCHAR(100),
    keywords VARCHAR(200),
    sentiment VARCHAR(20),
    sentiment_score DOUBLE,
    images JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_news_published_date ON news(published_date DESC);
CREATE INDEX IF NOT EXISTS idx_news_stock_code ON news(stock_code);
CREATE INDEX IF NOT EXISTS idx_news_sentiment ON news(sentiment);
CREATE INDEX IF NOT EXISTS idx_news_url ON news(url);