"""
Date   : 2024-07-08
Author : Zetra Team
Function used to detect Pennant patterns
"""

import numpy as np
import pandas as pd 
import logging

from scipy.stats import linregress

def get_pennant_details(ohlc: pd.DataFrame):
    """
    감지된 페넌트 패턴의 상세 정보를 추출합니다.
    """
    pattern_info = ohlc[ohlc['chart_type'] == 'pennant']
    if pattern_info.empty:
        return {}

    last_pattern = pattern_info.iloc[-1]
    
    # 페넌트 방향 결정 (Bullish or Bearish)
    # Note: 페넌트 패턴의 방향은 주로 이전 추세에 따라 결정됩니다. 
    # 여기서는 단순하게 기울기로만 판단하지만, 개선의 여지가 있습니다.
    direction = "bullish" if last_pattern['pennant_slmin'] > 0 else "bearish"
    
    high_idx = last_pattern['pennant_highs_idx']
    low_idx = last_pattern['pennant_lows_idx']
    
    start_date = ohlc.loc[min(high_idx[0], low_idx[0]), 'date'].strftime('%Y-%m-%d')
    end_date = ohlc.loc[max(high_idx[-1], low_idx[-1]), 'date'].strftime('%Y-%m-%d')

    return {
        "dates": [start_date, end_date],
        "direction": direction
    }

def find_pennant(ohlc: pd.DataFrame, lookback: int = 20, min_points: int = 3,
                r_max: float = 0.9, r_min: float = 0.9, slope_max: float = -0.0001, slope_min: float = 0.0001, 
                 lower_ratio_slope: float = 0.95, upper_ratio_slope: float = 1,
                 progress: bool = False) -> pd.DataFrame:
    """
    Find the pennant pattern point
    
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
    
    ohlc["chart_type"]        = ""
    ohlc["pennant_point"]        = np.nan 
    ohlc["pennant_highs_idx"]    = [np.array([]) for _ in range(len(ohlc)) ]
    ohlc["pennant_lows_idx"]     = [np.array([]) for _ in range(len(ohlc)) ]
    ohlc["pennant_highs"]        = [np.array([]) for _ in range(len(ohlc)) ]
    ohlc["pennant_lows"]         = [np.array([]) for _ in range(len(ohlc)) ]
    ohlc["pennant_slmax"]        = np.nan 
    ohlc["pennant_slmin"]        = np.nan 
    ohlc["pennant_intercmin"]    = np.nan
    ohlc["pennant_intercmax"]    = np.nan
    
    candle_iter = reversed(range(lookback, len(ohlc)))
        
    for candle_idx in candle_iter:
    
        maxim = np.array([])
        minim = np.array([])
        xxmin = np.array([])
        xxmax = np.array([])

        for i in range(candle_idx-lookback, candle_idx+1):
            if ohlc.loc[i,"pivot"] == 1:
                minim = np.append(minim, ohlc.loc[i, "low"])
                xxmin = np.append(xxmin, i) 
            if ohlc.loc[i,"pivot"] == 2:
                maxim = np.append(maxim, ohlc.loc[i,"high"])
                xxmax = np.append(xxmax, i)
        
        # Check the correct number of pivot points have been found
        if xxmax.size < min_points or xxmin.size < min_points:
            continue

        # To prevent RuntimeWarning from linregress, check if all y-values are the same.
        if len(np.unique(minim)) < 2 or len(np.unique(maxim)) < 2:
            continue

        # Run the regress to get the slope, intercepts and r-squared
        slmin, intercmin, rmin, pmin, semin = linregress(xxmin, minim)
        slmax, intercmax, rmax, pmax, semax = linregress(xxmax, maxim)
        
        if abs(rmax)>=r_max and abs(rmin)>=r_min and slmin>=slope_min  and slmax<= slope_max  and abs(slmax/slmin) > lower_ratio_slope and abs(slmax/slmin) < upper_ratio_slope:
                ohlc.loc[candle_idx,"chart_type"]          = "pennant"
                ohlc.loc[candle_idx, "pennant_point"]         = candle_idx
                ohlc.at[candle_idx, "pennant_highs"]          = maxim
                ohlc.at[candle_idx, "pennant_lows"]           = minim
                ohlc.at[candle_idx, "pennant_highs_idx"]      = xxmax
                ohlc.at[candle_idx, "pennant_lows_idx"]       = xxmin
                ohlc.loc[candle_idx, "pennant_slmax"]         = slmax
                ohlc.loc[candle_idx, "pennant_slmin"]         = slmin 
                ohlc.loc[candle_idx, "pennant_intercmin"]     = intercmin
                ohlc.loc[candle_idx, "pennant_intercmax"]     = intercmax
                
                # === 판단 근거 출력 ===
                logging.debug("\n=== Pennant Triangle Detected ===")
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
                ohlc.loc[candle_idx, "pennant_pivot_high_count"] = len(xxmax)
                ohlc.loc[candle_idx, "pennant_pivot_low_count"] = len(xxmin)
                ohlc.loc[candle_idx, "pennant_r2_high"] = rmax
                ohlc.loc[candle_idx, "pennant_r2_low"] = rmin

                break
    
    return ohlc, {}