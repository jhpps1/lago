"""
Date   : 2024-01-13
Author : Zetra Team
Detect Triangle Patterns - Ascending, Descending and Symmetrical 
"""

import numpy as np
import pandas as pd 
import logging

from .pivot_points import find_all_pivot_points
from .plotting import display_chart_pattern
from scipy.stats import linregress
from tqdm import tqdm

def get_triangle_details(ohlc: pd.DataFrame):
    """
    감지된 삼각형 패턴의 상세 정보를 추출합니다.
    """
    pattern_info = ohlc[ohlc['chart_type'] == 'triangle']
    if pattern_info.empty:
        return {}

    last_pattern = pattern_info.iloc[-1]
    
    high_idx = last_pattern['triangle_high_idx']
    low_idx = last_pattern['triangle_low_idx']
    
    start_date = ohlc.loc[min(high_idx[0], low_idx[0]), 'date'].strftime('%Y-%m-%d')
    end_date = ohlc.loc[max(high_idx[-1], low_idx[-1]), 'date'].strftime('%Y-%m-%d')

    return {
        "dates": [start_date, end_date],
        "slmax": last_pattern['triangle_slmax'],
        "slmin": last_pattern['triangle_slmin'],
        "rmax": last_pattern['triangle_r2_high'],
        "rmin": last_pattern['triangle_r2_low']
    }

def find_triangle_pattern(ohlc: pd.DataFrame, lookback: int = 25, min_points: int = 3, rlimit: int = 0.9, 
                          slmax_limit: float = 0.00001, slmin_limit: float = 0.00001,
                          triangle_type: str = "ascending", progress: bool = False ) -> pd.DataFrame:
    """
    Find the specified triangle pattern 
    
    :params ohlc is the OHLC dataframe 
    :type :pd.DataFrame
    
    :params lookback is the number of periods to use for back candles
    :type :int 

    :params min_points is the minimum of pivot points to use to detect a flag pattern
    :type :int
    
    :params rlimit is the R-squared fit lower limit for the pivot points
    :type :float
    
    :params slmax_limit is the limit for the slope of the pivot highs
    :type :float
    
    :params slmin_limit is the limit for the slope of the pivot lows
    :type :float
    
    :params triangle_type is the type of triangle pattern to detect. Options - ["ascending", "descending", "symmetrical"]
    :type :str 
    
    :params progress bar to be displayed or not
    :type :bool
    
    :return (pd.DataFrame)
    """
    
    if "chart_type" not in ohlc.columns:
        ohlc["chart_type"]            = ""
    ohlc["triangle_type"]         = ""
    ohlc["triangle_slmax"]        = np.nan
    ohlc["triangle_slmin"]        = np.nan
    ohlc["triangle_intercmin"]    = np.nan
    ohlc["triangle_intercmax"]    = np.nan
    ohlc["triangle_high_idx"]     = [np.array([]) for _ in range(len(ohlc)) ]
    ohlc["triangle_low_idx"]      = [np.array([]) for _ in range(len(ohlc)) ]
    ohlc["triangle_point"]        = np.nan
    
    candle_iter = reversed(range(lookback, len(ohlc)))
    
    for candle_idx in candle_iter:
        
        maxim = np.array([])
        minim = np.array([])
        xxmin = np.array([])
        xxmax = np.array([])

        for i in range(candle_idx - lookback, candle_idx+1):
            if ohlc.loc[i,"pivot"] == 1:
                minim = np.append(minim, ohlc.loc[i, "low"])
                xxmin = np.append(xxmin, i) 
            if ohlc.loc[i,"pivot"] == 2:
                maxim = np.append(maxim, ohlc.loc[i,"high"])
                xxmax = np.append(xxmax, i)

        if xxmax.size < min_points or xxmin.size < min_points:
            continue

        # To prevent RuntimeWarning from linregress, check if all y-values are the same.
        if len(np.unique(minim)) < 2 or len(np.unique(maxim)) < 2:
            continue

        slmin, intercmin, rmin, _, _ = linregress(xxmin, minim)
        slmax, intercmax, rmax, _, _ = linregress(xxmax, maxim)

        def set_pattern_data(pattern_type):
            ohlc.loc[candle_idx, "chart_type"]            = "triangle"
            ohlc.loc[candle_idx, "triangle_type"]         = pattern_type
            ohlc.loc[candle_idx, "triangle_slmax"]        = slmax
            ohlc.loc[candle_idx, "triangle_slmin"]        = slmin
            ohlc.loc[candle_idx, "triangle_intercmin"]    = intercmin
            ohlc.loc[candle_idx, "triangle_intercmax"]    = intercmax
            ohlc.at[candle_idx,  "triangle_high_idx"]     = xxmax
            ohlc.at[candle_idx,  "triangle_low_idx"]      = xxmin
            ohlc.loc[candle_idx, "triangle_point"]        = candle_idx

            # === 판단 근거 출력 ===
            logging.debug(f"\n=== {pattern_type.capitalize()} Triangle Detected ===")
            logging.debug(f"candle_idx: {candle_idx}")
            logging.debug(f"pivot_high_count: {len(xxmax)}")
            logging.debug(f"pivot_low_count: {len(xxmin)}")
            logging.debug(f"slope_high: {slmax:.6f}")
            logging.debug(f"slope_low: {slmin:.6f}")
            logging.debug(f"r2_high: {rmax:.4f}")
            logging.debug(f"r2_low: {rmin:.4f}")
            logging.debug(f"lookback: {lookback}")
            logging.debug("==============================\n")

            # 추가로 판단 근거를 df에 컬럼으로 저장
            ohlc.loc[candle_idx, "triangle_pivot_high_count"] = len(xxmax)
            ohlc.loc[candle_idx, "triangle_pivot_low_count"] = len(xxmin)
            ohlc.loc[candle_idx, "triangle_r2_high"] = rmax
            ohlc.loc[candle_idx, "triangle_r2_low"] = rmin

        pattern_found = False

        if triangle_type == "symmetrical":
            if abs(rmax)>=rlimit and abs(rmin)>=rlimit and slmin>=slmin_limit and slmax<=-1*slmax_limit:
                set_pattern_data("symmetrical")
                pattern_found = True

        elif triangle_type == "ascending":
            if abs(rmax)>=rlimit and abs(rmin)>=rlimit and slmin>=slmin_limit and (slmax>=-1*slmax_limit and slmax <= slmax_limit):
                set_pattern_data("ascending")
                pattern_found = True
    
        elif triangle_type == "descending":
            if abs(rmax)>=rlimit and abs(rmin)>=rlimit and slmax<=-1*slmax_limit and (slmin>=-1*slmin_limit and slmin <= slmin_limit):
                set_pattern_data("descending")
                pattern_found = True
        
        if pattern_found:
            break

    # After the loop, check if a pattern was found and extract details
    if ohlc["chart_type"].str.contains("triangle").any():
        details = get_triangle_details(ohlc)
    else:
        details = {}

    return ohlc, details