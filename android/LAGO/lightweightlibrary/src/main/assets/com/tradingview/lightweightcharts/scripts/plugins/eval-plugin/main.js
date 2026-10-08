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
window.evalPlugin = (evalParams) => {
    return new Function(`return(${evalParams.f})`)()
}
/******/ 	return __webpack_exports__;
/******/ })()
;
});