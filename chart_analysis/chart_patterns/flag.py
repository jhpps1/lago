"""
Date  : 2023-12-26
Author: Zetra Team
Function used to detect the Flag pattern
"""

import numpy as np
import pandas as pd 
import logging

from scipy.stats import linregress

def get_flag_details(ohlc: pd.DataFrame):
    """
    감지된 플래그 패턴의 상세 정보를 추출합니다.
    """
    pattern_info = ohlc[ohlc['chart_type'] == 'flag']
    if pattern_info.empty:
        return {}

    last_pattern = pattern_info.iloc[-1]
    
    # 플래그 방향 결정 (Bullish or Bearish)
    direction = "bullish" if last_pattern['flag_slmax'] > 0 else "bearish"
    
    high_idx = last_pattern['flag_highs_idx']
    low_idx = last_pattern['flag_lows_idx']
    
    start_date = ohlc.loc[min(high_idx[0], low_idx[0]), 'date'].strftime('%Y-%m-%d')
    end_date = ohlc.loc[max(high_idx[-1], low_idx[-1]), 'date'].strftime('%Y-%m-%d')

    return {
        "dates": [start_date, end_date],
        "direction": direction,
        "rmax": last_pattern['flag_r2_high'],
        "rmin": last_pattern['flag_r2_low']
    }

def find_flag_pattern(ohlc: pd.DataFrame, lookback: int = 25, min_points: int = 3,
                      r_max: float = 0.9, r_min: float = 0.9, slope_max: float = 0, slope_min: float = 0, 
                      lower_ratio_slope: float = 0.9, upper_ratio_slope: float = 1.05,
                      progress: bool = False) -> pd.DataFrame:
    """
    Find the flag pattern 
    
    :params ohlc is the OHLC dataframe
    :type :pd.DataFrame 
    
    :params lookback is the number of periods to use for back candles
    :type :int 
    
    :params min_points is the minimum of pivot points to use to detect a flag pattern
    :type :int
    
    :params r_max is the R-sqaured fit for the high pivot points
    :type :float
    
    :params r_min is the R-sqaured fit for the low pivot points
    :type :float    
    
    :params slope_max is the slope for the high pivot points
    :type :float    
    
    :params slope_min is the slope for the low pivot points
    :type :float    
    
    :params lower_ratio_slope is the lower limit for the ratio of the slope min to slope max
    :type :float    
    
    :params upper_ratio_slope is the upper limit for the ratio of the slope min to slope max
    :type :float   
    
    :params progress bar to be displayed or not 
    :type :bool
    
    :return (pd.DataFrame)
    """
    if "chart_type" not in ohlc.columns:
        ohlc["chart_type"]        = ""
    ohlc["flag_point"]        = np.nan 
    ohlc["flag_highs_idx"]    = [np.array([]) for _ in range(len(ohlc)) ]
    ohlc["flag_lows_idx"]     = [np.array([]) for _ in range(len(ohlc)) ]
    ohlc["flag_highs"]        = [np.array([]) for _ in range(len(ohlc)) ]
    ohlc["flag_lows"]         = [np.array([]) for _ in range(len(ohlc)) ]
    ohlc["flag_slmax"]        = np.nan 
    ohlc["flag_slmin"]        = np.nan 
    ohlc["flag_intercmin"]    = np.nan
    ohlc["flag_intercmax"]    = np.nan
    
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
        
        # Check if the correct number of pivot points have been found
        if xxmax.size < min_points or xxmin.size < min_points:
            continue
        
        # To prevent RuntimeWarning from linregress, check if all y-values are the same.
        if len(np.unique(minim)) < 2 or len(np.unique(maxim)) < 2:
            continue

        # Check the order condition of the pivot points is met
        if (np.any(np.diff(minim) < 0)) or (np.any(np.diff(maxim) < 0)):
               continue
            
        # Run the regress to get the slope, intercepts and r-squared   
        slmin, intercmin, rmin, _, _ = linregress(xxmin, minim)
        slmax, intercmax, rmax, _, _ = linregress(xxmax, maxim)
  
        # Check if the lines are parallel 
        if abs(rmax)>=r_max and abs(rmin)>=r_min and (slmin > slope_min and slmax > slope_max ) or (slmin < slope_min and slmax < slope_max):
            if (slmin/slmax > lower_ratio_slope and slmin/slmax < upper_ratio_slope):
                ohlc.loc[candle_idx,"chart_type"]          = "flag"
                ohlc.loc[candle_idx, "flag_point"]         = candle_idx
                ohlc.at[candle_idx, "flag_highs"]          = maxim
                ohlc.at[candle_idx, "flag_lows"]           = minim
                ohlc.at[candle_idx, "flag_highs_idx"]      = xxmax
                ohlc.at[candle_idx, "flag_lows_idx"]       = xxmin
                ohlc.loc[candle_idx, "flag_slmax"]         = slmax
                ohlc.loc[candle_idx, "flag_slmin"]         = slmin 
                ohlc.loc[candle_idx, "flag_intercmin"]     = intercmin
                ohlc.loc[candle_idx, "flag_intercmax"]     = intercmax

                # === 판단 근거 출력 ===
                logging.debug("\n=== Flag Detected ===")
                logging.debug(f"candle_idx: {candle_idx}")
                logging.debug(f"flag_highs: {maxim}")
                logging.debug(f"flag_lows: {minim}")
                logging.debug(f"pivot_high_count: {len(xxmax)}")
                logging.debug(f"pivot_low_count: {len(xxmin)}")
                logging.debug(f"slope_high: {slmax:.6f}")
                logging.debug(f"slope_low: {slmin:.6f}")
                logging.debug(f"r2_high: {rmax:.4f}")
                logging.debug(f"r2_low: {rmin:.4f}")
                logging.debug(f"lookback: {lookback}")
                logging.debug("==============================\n")

                # 추가로 판단 근거를 df에 컬럼으로 저장
                ohlc.loc[candle_idx, "flag_pivot_high_count"] = len(xxmax)
                ohlc.loc[candle_idx, "flag_pivot_low_count"] = len(xxmin)
                ohlc.loc[candle_idx, "flag_r2_high"] = rmax
                ohlc.loc[candle_idx, "flag_r2_low"] = rmin

                break
                            
    return ohlc, {}