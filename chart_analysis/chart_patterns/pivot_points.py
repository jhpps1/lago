"""
Date  : 2023-12-25
Author: Zetra Team

"""

import numpy as np
import pandas as pd 
import logging

from .utils import check_ohlc_names
from .plotting import display_pivot_points
from tqdm import tqdm
from typing import Union



def find_all_pivot_points(ohlc: pd.DataFrame, left_count:int = 3, right_count:int = 3, name_pivot: Union[None, str] = None, 
                          progress: bool = False ) -> pd.DataFrame:
    """
    Find the all the pivot points for the given OHLC dataframe

    :params ohlc is a dataframe with Open, High, Low, Close data
    :type :pd.DataFrame
    
    :params left_count is the number of candles to the left to consider
    :type :int 
    
    :params right_count is the number of candles to right to consider 
    :type :int 
    
    :params progress bar to be displayed or not 
    :type :bool 
     
    :return (pd.DataFrame)
    """
    check_ohlc_names(ohlc)

    pivot_col_name = name_pivot if name_pivot else "pivot"
    pivot_pos_col_name = f"{pivot_col_name}_pos"

    # Initialize pivot column
    ohlc[pivot_col_name] = 0

    window_size = left_count + right_count + 1
    
    # Calculate rolling min and max
    rolling_min_low = ohlc['low'].rolling(window=window_size, center=True, min_periods=window_size).min()
    rolling_max_high = ohlc['high'].rolling(window=window_size, center=True, min_periods=window_size).max()

    # Identify potential pivot lows and highs
    is_pivot_low = (ohlc['low'] == rolling_min_low)
    is_pivot_high = (ohlc['high'] == rolling_max_high)

    # Assign pivot types based on the original logic
    # Assign 1 for pivot lows (where it's a low and not a high)
    ohlc.loc[is_pivot_low & ~is_pivot_high, pivot_col_name] = 1

    # Assign 2 for pivot highs (where it's a high and not a low)
    ohlc.loc[is_pivot_high & ~is_pivot_low, pivot_col_name] = 2

    # Assign 3 for points that are both pivot low and pivot high
    ohlc.loc[is_pivot_low & is_pivot_high, pivot_col_name] = 3

    # Calculate pivot positions
    ohlc[pivot_pos_col_name] = np.nan
    ohlc.loc[ohlc[pivot_col_name] == 1, pivot_pos_col_name] = ohlc['low'] - 1e-3
    ohlc.loc[ohlc[pivot_col_name] == 2, pivot_pos_col_name] = ohlc['high'] + 1e-3
    ohlc.loc[ohlc[pivot_col_name] == 3, pivot_pos_col_name] = ohlc['low'] - 1e-3 # Or high + 1e-3, depending on priority for type 3

    return ohlc

def find_pivot_point_position(row: pd.Series) -> float:
    """
    Get the Pivot Point position and assign the Low or High value.  

    :params row to assign the pivot point position value if applicable. There must be a 'pivot' value
    :type :pd.Series 
    
    :return (float)
    """
   
   
    try:
        if row['pivot']==1:
            return row['low']-1e-3
        elif row['pivot']==2:
            return row['high']+1e-3
        else:
            return np.nan

    except Exception as e:
        logging.error(f"Error: {e}")
        return np.nan
    
if __name__ == "__main__":
    import os
    # print(os.path.realpath('').split("\patterns")[0])
    data = pd.read_csv(os.path.join(os.path.realpath('').split(r"\patterns")[0],"data","eurusd-4h.csv"))
    
    # print(find_all_pivot_points(data))
    data = find_all_pivot_points(data)
    # display_pivot_points(data)
    