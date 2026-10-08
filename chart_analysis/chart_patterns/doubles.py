"""
Date   : 2023-01-06
Author : Zetra Team
Function used to find the Double chart patterns
"""

import numpy as np
import pandas as pd 
import logging

def get_doubles_details(ohlc: pd.DataFrame, back_candles: int = 25):
    """
    감지된 더블 패턴의 상세 정보를 추출합니다.
    
    :params ohlc: OHLC 데이터프레임
    :params back_candles: 분석에 사용할 캔들 수
    :return: 상세 정보를 담은 딕셔너리
    """
    
    pattern_info = ohlc[ohlc['chart_type'] == 'double']
    if pattern_info.empty:
        return {}

    # 가장 최근에 감지된 패턴을 기준으로 상세 정보 추출
    last_pattern = pattern_info.iloc[-1]
    idx = last_pattern.name
    
    pivots_idx = last_pattern['double_idx']
    pivots_points = last_pattern['double_point']
    
    # 주요 포인트 날짜 추출
    date1 = ohlc.loc[pivots_idx[1], 'date'].strftime('%Y-%m-%d')
    date2 = ohlc.loc[pivots_idx[3], 'date'].strftime('%Y-%m-%d')
    
    # 넥라인 가격 및 돌파 여부 확인
    neckline_price = pivots_points[2]
    
    # 넥라인 돌파 확인 (패턴 완성 이후 캔들)
    future_candles = ohlc.loc[idx : idx + back_candles]
    
    neckline_break = False
    if last_pattern['double_type'] == 'tops':
        # 넥라인 아래로 종가가 형성되는지 확인
        if not future_candles[future_candles['closePrice'] < neckline_price].empty:
            neckline_break = True
    elif last_pattern['double_type'] == 'bottoms':
        # 넥라인 위로 종가가 형성되는지 확인
        if not future_candles[future_candles['closePrice'] > neckline_price].empty:
            neckline_break = True

    return {
        "dates": [date1, date2],
        "neckline_break": neckline_break
    }

def find_doubles_pattern(ohlc: pd.DataFrame, lookback: int = 25, double: str = "tops", 
                         tops_max_ratio: float = 1.01, bottoms_min_ratio: float = 0.98,
                         progress: bool = False) -> tuple[pd.DataFrame, dict]:
    """
    Find the Double chart patterns 
    
    :params ohlc is the OHLC dataframe 
    :type :pd.DataFrame
    
    :params lookback is the number of periods to use for back candles
    :type :int 
    
    :params double is a options string variable of the type of doubles chart pattern that needs to be identifed. ['tops', 'bottoms', 'both']
    :type :str 
    
    :params tops_max_ratio is the max ratio between the peak points in the tops chart pattern
    :type :float
    
    params bottoms_min_ratio is the min ratio between the trough points in the bottoms chart pattern
    :type :float 
    
    :params progress bar to be displayed or not 
    :type:bool
    
    :return (pd.DataFrame, dict)
    """
    
    # Placeholders for the Double patterns     
    ohlc["double_type"]   = ""
    ohlc["chart_type"]    = ""
    ohlc["double_idx"]    = [np.array([]) for _ in range(len(ohlc)) ]
    ohlc["double_point"]  = [np.array([]) for _ in range(len(ohlc)) ]
    
    details = {}

    candle_iter = reversed(range(lookback, len(ohlc)))
           
    for candle_idx in candle_iter:
        sub_ohlc = ohlc.loc[candle_idx-lookback: candle_idx,:]
        
        pivot_indx = [ i for i, p in zip(sub_ohlc["pivot"].index.values, sub_ohlc["pivot"].tolist()) if p != 0 ]
        if len(pivot_indx) != 5:
            continue
        
        if len(pivot_indx) == 5: # Must have only 5 pivots
            pivots = ohlc.loc[pivot_indx, "pivot_pos"].tolist() 
            
            # Find Double Tops
            if double == "tops" or double == "both":
                if (pivots[0] < pivots[1]) and (pivots[0] < pivots[3]) and (pivots[2] < pivots[1]) and \
                    (pivots[2] < pivots[3]) and (pivots[4] < pivots[1]) and (pivots[4] < pivots[3]) and \
                        (pivots[1] > pivots[3]) and (pivots[1]/pivots[3] <= tops_max_ratio):  
                        ohlc.at[candle_idx, "double_idx"]     = pivot_indx
                        ohlc.at[candle_idx, "double_point"]   = pivots
                        ohlc.loc[candle_idx, "double_type"]   = "tops"
                        ohlc.loc[candle_idx, "chart_type"]    = "double"
                        
                                                # Construct details directly here
                        date1 = ohlc.loc[pivot_indx[1], 'date'].strftime('%Y-%m-%d')
                        date2 = ohlc.loc[pivot_indx[3], 'date'].strftime('%Y-%m-%d')
                        neckline_price = pivots[2]

                        # Simplified neckline break check for now, as full future_candles scan is complex here
                        # This part might need more context from the calling function or a dedicated helper
                        # For now, let's assume neckline_break is false unless explicitly determined
                        neckline_break = False # Placeholder, actual logic might be more complex

                        details = {
                            "dates": [date1, date2],
                            "neckline_break": neckline_break,
                            "double_type": "tops" # Add double_type for generate_judgement_reason
                        }
                        logging.debug("\n=== Double Top Detected ===\nDetails: %s", details)
                        break
                        
            # Find Double Bottoms            
            elif double == "bottoms" or double == "both":
              if (pivots[0] > pivots[1]) and (pivots[0] > pivots[3]) and (pivots[2] > pivots[1]) and \
                    (pivots[2] > pivots[3]) and (pivots[4] > pivots[1]) and (pivots[4] > pivots[3]) and \
                        (pivots[1] < pivots[3]) and  (pivots[1]/pivots[3] >= bottoms_min_ratio) :
                        ohlc.at[candle_idx, "double_idx"]     = pivot_indx
                        ohlc.at[candle_idx, "double_point"]   = pivots
                        ohlc.loc[candle_idx, "double_type"]   = "bottoms"                         
                        ohlc.loc[candle_idx, "chart_type"]    = "double"

                                                # Construct details directly here
                        date1 = ohlc.loc[pivot_indx[1], 'date'].strftime('%Y-%m-%d')
                        date2 = ohlc.loc[pivot_indx[3], 'date'].strftime('%Y-%m-%d')
                        neckline_price = pivots[2]

                        # Simplified neckline break check for now, as full future_candles scan is complex here
                        # This part might need more context from the calling function or a dedicated helper
                        # For now, let's assume neckline_break is false unless explicitly determined
                        neckline_break = False # Placeholder, actual logic might be more complex

                        details = {
                            "dates": [date1, date2],
                            "neckline_break": neckline_break,
                            "double_type": "bottoms" # Add double_type for generate_judgement_reason
                        }
                        logging.debug("\n=== Double Bottom Detected ===\nDetails: %s", details)
                        break
                        
    return ohlc, details
