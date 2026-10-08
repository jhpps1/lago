import pandas as pd
from flask import Flask, request, jsonify
from .config import HOST, PORT, DEBUG, LOG_LEVEL, LOG_FORMAT
import logging
import concurrent.futures

# Import pattern detection functions
from .doubles import find_doubles_pattern
from .flag import find_flag_pattern
from .head_and_shoulders import find_head_and_shoulders
from .inverse_head_and_shoulders import find_inverse_head_and_shoulders
from .pennant import find_pennant
from .triangles import find_triangle_pattern
from .plotting import display_chart_pattern
from .pivot_points import find_all_pivot_points

# --- Configuration ---

# 로깅 설정
logging.basicConfig(level=LOG_LEVEL, format=LOG_FORMAT)
logger = logging.getLogger(__name__)

# --- Result Check Functions ---
def check_flag_result(df):
    return not df[df["flag_point"].notna()].empty

def check_pennant_result(df):
    return not df[df["pennant_point"].notna()].empty

def check_triangle_result(df):
    return not df[df["triangle_point"].notna()].empty

def check_hs_result(df):
    return not df[df["hs_idx"].str.len() > 0].empty

def check_ihs_result(df):
    return not df[df["ihs_idx"].str.len() > 0].empty

def check_doubles_result(df):
    return not df[df["chart_type"] == "double"].empty

# --- Pattern Definitions ---
PATTERNS_CONFIG = [
    {
        "name": "더블 탑 패턴",
        "function": find_doubles_pattern,
        "params": {"double": "tops"},
        "result_check": check_doubles_result,
        "reason": "두 개의 고점이 형성되며 넥라인을 돌파하여 하락 반전을 암시하는 패턴입니다.",
        "display_options": {"pattern": "double", "save": True}
    },
    {
        "name": "더블 바텀 패턴",
        "function": find_doubles_pattern,
        "params": {"double": "bottoms"},
        "result_check": check_doubles_result,
        "reason": "두 개의 저점이 형성되며 넥라인을 돌파하여 상승 반전을 암시하는 패턴입니다.",
        "display_options": {"pattern": "double", "save": True}
    },
    {
        "name": "플래그 패턴",
        "function": find_flag_pattern,
        "params": {},
        "result_check": check_flag_result,
        "reason": "급등락 이후 평행한 추세선 사이에서 가격이 일시적으로 조정되는 패턴으로, 기존 추세의 지속 가능성을 시사합니다.",
        "display_options": {"pattern": "flag", "save": True}
    },
    {
        "name": "페넌트 패턴",
        "function": find_pennant,
        "params": {},
        "result_check": check_pennant_result,
        "reason": "급등락 후 고점과 저점이 수렴하는 삼각형 형태로, 강한 추세 후 휴식 구간을 나타내는 지속형 패턴입니다.",
        "display_options": {"pattern": "pennant", "save": True}
    },
    {
        "name": "상승 삼각형",
        "function": find_triangle_pattern,
        "params": {"triangle_type": "ascending"},
        "result_check": check_triangle_result,
        "reason": "고점이 일정하게 유지되고 저점이 점점 높아지는 구조로, 매수세가 점차 강해지는 패턴입니다.",
        "display_options": {"pattern": "triangle", "save": True}
    },
    {
        "name": "하락 삼각형",
        "function": find_triangle_pattern,
        "params": {"triangle_type": "descending"},
        "result_check": check_triangle_result,
        "reason": "저점이 일정하게 유지되고 고점이 점점 낮아지는 구조로, 매도세가 강해지는 패턴입니다.",
        "display_options": {"pattern": "triangle", "save": True}
    },
    {
        "name": "대칭 삼각형",
        "function": find_triangle_pattern,
        "params": {"triangle_type": "symmetrical"},
        "result_check": check_triangle_result,
        "reason": "고점과 저점이 점차 수렴하는 구조로, 향후 큰 방향성이 나올 가능성이 높은 패턴입니다.",
        "display_options": {"pattern": "triangle", "save": True}
    },
    {
        "name": "헤드 앤 숄더 패턴",
        "function": find_head_and_shoulders,
        "params": {},
        "result_check": check_hs_result,
        "reason": "고점이 세 번 형성되며 가운데 고점이 가장 높아 하락 반전을 암시하는 패턴입니다.",
        "display_options": {"pattern": "hs", "save": True}
    },
    {
        "name": "역 헤드 앤 숄더 패턴",
        "function": find_inverse_head_and_shoulders,
        "params": {},
        "result_check": check_ihs_result,
        "reason": "저점이 세 번 형성되며 가운데 저점이 가장 낮아 상승 반전을 암시하는 패턴입니다.",
        "display_options": {"pattern": "ihs", "save": True, "pivot_name": "short_pivot"}
    }
]

# --- Flask App ---
app = Flask(__name__)

def detect_single_pattern(pattern_config, ohlc_df):
    try:
        # 각 패턴 감지 전 원본 데이터프레임 복사
        ohlc_copy = ohlc_df.copy()
        
        # 패턴 감지 함수 실행
        ohlc_copy, details = pattern_config["function"](ohlc_copy, **pattern_config["params"])
        
        # 감지 결과 확인
        is_detected = pattern_config["result_check"](ohlc_copy)

        if is_detected:
            reason = generate_judgement_reason(pattern_config['name'], details)
            result = {
                "name": pattern_config["name"],
                "reason": reason
            }
            return (result, ohlc_copy, pattern_config["display_options"])
        return None
    except Exception as e:
        logging.error(f"Error detecting pattern {pattern_config['name']}: {e}", exc_info=True)
        return None

def run_pattern_detection(ohlc_df):
    detected_patterns = []
    ohlc_df_with_pivots = find_all_pivot_points(ohlc_df.copy())

    with concurrent.futures.ProcessPoolExecutor() as executor:
        future_to_pattern = {executor.submit(detect_single_pattern, config, ohlc_df_with_pivots): config for config in PATTERNS_CONFIG}

        for future in concurrent.futures.as_completed(future_to_pattern):
            pattern_name = future_to_pattern[future]['name']
            try:
                result_tuple = future.result()
                if result_tuple:
                    result, ohlc_result_df, display_options = result_tuple
                    logger.debug(f"'{result['name']}' 패턴이 감지되었습니다!")
                    detected_patterns.append(result)

                    # 차트 이미지 생성
                    # display_chart_pattern(ohlc_result_df, **display_options)
                else:
                    logger.debug(f"'{pattern_name}' 패턴이 감지되지 않았습니다.")
            except Exception as exc:
                logger.error(f"'{pattern_name}' 패턴 감지 중 예외 발생: {exc}")

    return detected_patterns

@app.route('/', methods=['GET'])
def health_check():
    """서버 상태 확인용 엔드포인트"""
    return jsonify({
        "status": "healthy"
    })

@app.route('/detect-patterns', methods=['POST'])
def detect_patterns_endpoint():
    logger.info("패턴 감지 요청 수신")
    
    ohlc_data = request.get_json()
    if not ohlc_data:
        return jsonify({"error": "OHLC 데이터가 필요합니다."}), 400

    try:
        ohlc_df = pd.DataFrame(ohlc_data)
        ohlc_df['date'] = pd.to_datetime(ohlc_df['date'])
        ohlc_df.reset_index(drop=True, inplace=True)
    except Exception as e:
        logger.error(f"데이터프레임 변환 오류: {e}")
        return jsonify({"error": f"잘못된 형식의 OHLC 데이터입니다: {e}"}), 400

    logger.debug("데이터프레임 변환 완료")
    detected_patterns = run_pattern_detection(ohlc_df)

    logger.info(f"패턴 감지 완료! 총 {len(detected_patterns)}개 패턴 감지.")
    return jsonify(detected_patterns)

def generate_judgement_reason(pattern_name, details):
    if not details:
        return "패턴이 감지되었으나, 상세 정보를 생성할 수 없습니다."

    pattern_type = details.get('pattern_type')
    dates = details.get('dates', [])
    
    if pattern_name == "더블 탑 패턴":
        if len(dates) >= 2:
            reason = f"{dates[0]}와 {dates[1]}에 고점이 반복 형성되었으며, "
            if details.get('neckline_break'):
                reason += "넥라인을 돌파해 하락 반전 가능성이 높습니다."
            else:
                reason += "아직 넥라인 돌파는 발생하지 않았습니다."
            return reason
        else:
            return "두 개의 고점이 형성되어야 하지만 충분하지 않습니다."

    elif pattern_name == "더블 바텀 패턴":
        if len(dates) >= 2:
            reason = f"{dates[0]}와 {dates[1]}에 저점이 반복 형성되었으며, "
            if details.get('neckline_break'):
                reason += "넥라인을 돌파해 상승 반전 가능성이 높습니다."
            else:
                reason += "아직 넥라인 돌파는 발생하지 않았습니다."
            return reason
        else:
            return "두 개의 저점이 형성되어야 하지만 충분하지 않습니다."
    
    elif pattern_name == "플래그 패턴":
        direction = details.get('direction')
        trend = "상승 추세 후" if direction == "bullish" else "하락 추세 후"
        confidence = "높은 신뢰도로" if details.get('rmax', 0) > 0.9 else "다소 불확실한 흐름 속에서"
        return f"{trend} 깃발 형태의 조정 구간이 {confidence} 나타났습니다. 추세 지속 가능성이 있습니다."

    elif pattern_name == "헤드 앤 숄더 패턴":
        return f"좌우 어깨와 머리 형태로 고점이 점차 낮아지는 패턴입니다. 하락 반전 가능성이 있습니다. 주요 고점: {', '.join(dates)}"

    elif pattern_name == "역 헤드 앤 숄더 패턴":
        return f"역 헤드 앤 숄더 패턴으로 바닥 다지기 후 상승세로 전환될 가능성이 있습니다. 주요 저점: {', '.join(dates)}"
    
    elif pattern_name == "페넌트 패턴":
        direction = details.get('direction')
        trend = "상승세" if direction == "bullish" else "하락세"
        return f"급격한 {trend} 이후 삼각 수렴형 조정이 발생했습니다. 패턴은 {', '.join(dates)}에 형성되었습니다."

    elif pattern_name == "상승 삼각형":
        if details.get('rmax', 0) > 0.9 and abs(details.get('slmax', 0)) < 0.001:
            return f"저항선을 여러 차례 돌파 시도했으며, {', '.join(dates)}에 고점이 형성되었습니다. 상승 가능성이 높습니다."
        else:
            return "고점이 일정한 수평선을 이루며 매수세가 점차 강해지는 모습입니다."

    elif pattern_name == "하락 삼각형":
        if details.get('rmin', 0) > 0.9 and abs(details.get('slmin', 0)) < 0.001:
            return f"지지선을 여러 번 시험하는 하락형 패턴입니다. {', '.join(dates)}에 저점이 반복적으로 발생했습니다."
        else:
            return "저점이 수평선을 이루며 매도 압력이 강해지는 모습입니다."

    elif pattern_name == "대칭 삼각형":
        return f"수렴형 삼각형 패턴으로, 고점과 저점이 점점 좁아지고 있습니다. 변동성 확대가 예상됩니다. ({', '.join(dates)} 기준)"
    
    return "알 수 없는 패턴입니다."

if __name__ == "__main__":
    logger.info("Flask 서버 시작 중...")
    app.run(host=HOST, port=PORT, debug=DEBUG)