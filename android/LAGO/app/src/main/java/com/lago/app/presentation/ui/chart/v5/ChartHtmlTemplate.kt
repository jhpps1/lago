package com.lago.app.presentation.ui.chart.v5

/**
 * Lightweight Charts v5 HTML template for realtime updates.
 * - Loads v5 standalone bundle
 * - Exposes: setInitialData(candlesJson, volsJson), updateBar(barJson), updateVolume(volJson)
 * - Notifies Android via Android.onChartReady()
 */
object ChartHtmlTemplate {
    fun get(): String = """
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1"/>
  <title>LAGO Chart</title>
  <link rel="icon" href="data:,">
  <style>
    html,body,#chart{height:100%;margin:0;padding:0;background:#ffffff}
  </style>
  <script src="https://unpkg.com/lightweight-charts@5.0.8/dist/lightweight-charts.standalone.production.js"></script>
</head>
<body>
  <div id="chart"></div>
  <script>
    let chart, series = {};

    function initChart(){
      if(!window.LightweightCharts){ setTimeout(initChart, 50); return; }
      const { createChart, CandlestickSeries, HistogramSeries, CrosshairMode } = LightweightCharts;
      chart = createChart(document.getElementById('chart'), {
        autoSize:true,
        layout:{ background:{type:'solid', color:'#ffffff'}, textColor:'#333', fontSize:12 },
        grid:{ vertLines:{color:'#f0f0f0'}, horzLines:{color:'#f0f0f0'} },
        crosshair:{ mode: CrosshairMode.Normal },
        rightPriceScale:{ borderColor:'#e1e1e1' },
        timeScale:{ borderColor:'#e1e1e1', timeVisible:true, secondsVisible:false }
      });

      series.main = chart.addSeries(CandlestickSeries, {
        upColor:'#FF99C5', downColor:'#42A6FF', borderVisible:false,
        wickUpColor:'#FF99C5', wickDownColor:'#42A6FF',
        priceFormat:{ type:'price', precision:0, minMove:1 }
      });

      series.volume = chart.addSeries(HistogramSeries, {
        priceFormat:{ type:'volume' }, priceScaleId:'',
        scaleMargins:{ top:0.8, bottom:0 }
      });

      window.__chartReady = true;
      if(window.Android && Android.onChartReady){ Android.onChartReady(); }
      console.log('Chart ready');
    }

    window.setInitialData = function(candlesJson, volsJson){
      try{
        const candles = JSON.parse(candlesJson||'[]');
        const vols    = JSON.parse(volsJson||'[]');
        series.main.setData(candles);
        if(vols.length) series.volume.setData(vols);
        chart.timeScale().fitContent();
      }catch(e){ console.error(e); }
    }

    window.updateBar = function(barJson){
      try{
        const bar = JSON.parse(barJson); // {time,open,high,low,close}
        series.main.update(bar);
      }catch(e){ console.error(e); }
    }

    window.updateVolume = function(volJson){
      try{
        const v = JSON.parse(volJson); // {time,value}
        series.volume.update(v);
      }catch(e){ console.error(e); }
    }

    document.addEventListener('DOMContentLoaded', initChart);
  </script>
</body>
</html>
    """.trimIndent()
}