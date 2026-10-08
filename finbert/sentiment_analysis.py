"""
감정분석 관련 함수들
"""
import torch
import logging
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from config import (
    MODEL_NAME, MAX_SEQUENCE_LENGTH, SENTIMENT_LABELS, KEYWORD_WEIGHT,
    BUY_THRESHOLD, WEAK_BUY_THRESHOLD, SELL_THRESHOLD, WEAK_SELL_THRESHOLD,
    CONFIDENCE_THRESHOLDS
)

logger = logging.getLogger(__name__)

# 전역 변수
tokenizer = None
model = None


def get_positive_keywords():
    """긍정 키워드 사전"""
    return {
        '상승': 3, '증가': 3, '호재': 5, '성장': 4, '개선': 3,
        '신고가': 5, '흑자': 4, '수익': 3, '호조': 4, '강세': 4,
        '돌파': 3, '회복': 3, '상향': 3, '매수': 2, '추천': 2,
        '긍정': 3, '호전': 3, '상승세': 4, '급등': 5, '대박': 4
    }


def get_negative_keywords():
    """부정 키워드 사전"""
    return {
        '하락': -3, '감소': -3, '악재': -5, '위기': -4, '악화': -3,
        '신저가': -5, '적자': -4, '손실': -3, '부진': -4, '약세': -4,
        '붕괴': -5, '폭락': -5, '하향': -3, '매도': -2, '우려': -2,
        '부정': -3, '악영향': -4, '하락세': -4, '급락': -5, '최악': -5
    }


def load_model():
    """모델과 토크나이저를 로드하는 함수"""
    global tokenizer, model

    try:
        logger.info("ko-FinBERT 모델 로딩 중...")

        logger.info(f"토크나이저 로딩 중: {MODEL_NAME}")
        tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)

        logger.info(f"모델 로딩 중: {MODEL_NAME}")
        model = AutoModelForSequenceClassification.from_pretrained(MODEL_NAME)

        # 모델을 평가 모드로 설정
        model.eval()

        logger.info("모델 로딩 완료!")

        # 간단한 테스트
        test_text = "테스트 문장입니다"
        test_inputs = tokenizer(test_text, return_tensors="pt")
        logger.info("모델 테스트 성공!")

        return True

    except Exception as e:
        logger.error(f"모델 로딩 실패: {e}")
        import traceback
        logger.error(f"상세 오류: {traceback.format_exc()}")
        return False


def calculate_refined_score(probabilities, predicted_class):
    """
    세밀한 감정 점수 계산
    -100 ~ +100 범위의 점수를 반환
    """
    neg_prob = probabilities[0][0].item()
    neu_prob = probabilities[0][1].item()
    pos_prob = probabilities[0][2].item()

    # 기본 점수 계산: positive와 negative의 차이
    base_score = (pos_prob - neg_prob) * 100

    # 중립 확률을 고려한 조정
    neutral_weight = neu_prob * 0.5
    adjusted_score = base_score * (1 - neutral_weight)

    # 확률 차이에 따른 추가 조정
    prob_diff = abs(pos_prob - neg_prob)

    if prob_diff < 0.1:  # 매우 애매한 경우
        adjusted_score *= 0.3
    elif prob_diff < 0.2:  # 애매한 경우
        adjusted_score *= 0.5
    elif prob_diff < 0.3:  # 약간 애매한 경우
        adjusted_score *= 0.7
    elif prob_diff < 0.5:  # 어느정도 명확한 경우
        adjusted_score *= 0.85

    # 최종 점수 범위 보정 (-100 ~ +100)
    final_score = max(-100, min(100, adjusted_score))

    # 미세 조정: 너무 극단적인 값 완화
    if abs(final_score) > 80:
        final_score = final_score * 0.9

    return round(final_score, 2)


def get_confidence_level(probabilities, predicted_class):
    """예측의 신뢰도 수준 계산"""
    max_prob = probabilities[0][predicted_class].item()

    # 다른 클래스와의 확률 차이
    probs = [probabilities[0][i].item() for i in range(3)]
    sorted_probs = sorted(probs, reverse=True)
    prob_gap = sorted_probs[0] - sorted_probs[1]

    # 신뢰도 계산
    for level in ['very_high', 'high', 'medium', 'low']:
        threshold = CONFIDENCE_THRESHOLDS[level]
        if max_prob > threshold['min_prob'] and prob_gap > threshold['min_gap']:
            return level

    return "very_low"


def analyze_keywords_impact(text):
    """키워드 기반 추가 감정 점수 조정"""
    positive_keywords = get_positive_keywords()
    negative_keywords = get_negative_keywords()

    keyword_score = 0
    text_lower = text.lower()

    for keyword, score in positive_keywords.items():
        count = text_lower.count(keyword)
        keyword_score += count * score

    for keyword, score in negative_keywords.items():
        count = text_lower.count(keyword)
        keyword_score += count * score

    # 점수 정규화 (-20 ~ +20 범위로 제한)
    keyword_score = max(-20, min(20, keyword_score))

    return keyword_score


def generate_trading_signal(final_score, confidence_level):
    """거래 신호 생성"""
    if final_score > BUY_THRESHOLD and confidence_level in ["high", "very_high"]:
        return "BUY"
    elif final_score > WEAK_BUY_THRESHOLD and confidence_level in ["medium", "high", "very_high"]:
        return "WEAK_BUY"
    elif final_score < SELL_THRESHOLD and confidence_level in ["high", "very_high"]:
        return "SELL"
    elif final_score < WEAK_SELL_THRESHOLD and confidence_level in ["medium", "high", "very_high"]:
        return "WEAK_SELL"
    else:
        return "HOLD"


def analyze_sentiment(text):
    """감정분석 메인 함수"""
    if tokenizer is None or model is None:
        raise RuntimeError("모델이 로드되지 않았습니다")

    # 토크나이저 실행
    inputs = tokenizer(text, return_tensors="pt", truncation=True, max_length=MAX_SEQUENCE_LENGTH, padding=True)

    # 모델 추론
    with torch.no_grad():
        outputs = model(**inputs)

    # 결과 처리
    probabilities = torch.nn.functional.softmax(outputs.logits, dim=-1)
    predicted_class = torch.argmax(probabilities, dim=-1).item()
    predicted_label = SENTIMENT_LABELS[predicted_class]

    # 세밀한 점수 계산
    sentiment_score = calculate_refined_score(probabilities, predicted_class)
    keyword_adjustment = analyze_keywords_impact(text)

    # 최종 점수 (키워드 조정 포함)
    final_score = sentiment_score + keyword_adjustment * KEYWORD_WEIGHT
    final_score = max(-100, min(100, final_score))
    final_score = round(final_score, 2)

    # 신뢰도 수준
    confidence_level = get_confidence_level(probabilities, predicted_class)

    # 거래 신호 생성
    trading_signal = generate_trading_signal(final_score, confidence_level)

    return {
        "label": predicted_label,
        "score": final_score,
        "raw_score": sentiment_score,
        "keyword_adjustment": round(keyword_adjustment * KEYWORD_WEIGHT, 2),
        "confidence": probabilities[0][predicted_class].item(),
        "confidence_level": confidence_level,
        "probabilities": {
            "negative": round(probabilities[0][0].item(), 4),
            "neutral": round(probabilities[0][1].item(), 4),
            "positive": round(probabilities[0][2].item(), 4)
        },
        "trading_signal": trading_signal,
        "signal_strength": abs(final_score)
    }


def is_model_loaded():
    """모델 로딩 상태 확인"""
    return tokenizer is not None and model is not None