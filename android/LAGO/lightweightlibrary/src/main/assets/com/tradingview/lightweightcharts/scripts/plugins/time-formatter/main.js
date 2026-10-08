(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else if(typeof exports === 'object')
		exports["LightweightCharts"] = factory();
	else
		root["LightweightCharts"] = factory();
})(this, () => {
return /******/ (() => { // webpackBootstrap
var __webpack_exports__ = {};
window.timeFormatter = (formatterParams) => {
    function toLocaleString(date) {
        const dateTimeFormat = formatterParams.dateTimeFormat
        const locale = formatterParams.locale
        switch (dateTimeFormat) {
            case "DATE":
                return date.toLocaleDateString(locale)
            case "TIME":
                return date.toLocaleTimeString(locale)
            case "DATE_TIME":
                return date.toLocaleString(locale)
        }
    }

    return (businessDayOrTimestamp) => {
        if (typeof businessDayOrTimestamp == 'number') {
            return toLocaleString(new Date(businessDayOrTimestamp * 1000))
        }

        const date = `${businessDayOrTimestamp.year}-${businessDayOrTimestamp.month}-${businessDayOrTimestamp.day}`
        return toLocaleString(new Date(Date.parse(date)))
    }
}
/******/ 	return __webpack_exports__;
/******/ })()
;
});