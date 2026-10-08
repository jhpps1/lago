package com.lago.app.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 관심종목 메모리 캐시
 * 빠른 조회와 실시간 UI 동기화를 위한 캐시 시스템
 */
@Singleton
class FavoriteCache @Inject constructor() {
    
    private val favoriteSet = mutableSetOf<String>()
    private val _favoriteFlow = MutableStateFlow<Set<String>>(emptySet())
    
    /**
     * 관심종목 상태 변경을 실시간으로 관찰할 수 있는 Flow
     */
    val favoriteFlow: StateFlow<Set<String>> = _favoriteFlow.asStateFlow()
    
    /**
     * 캐시 전체 업데이트 (초기 로드시 사용)
     */
    fun updateCache(favorites: Set<String>) {
        synchronized(favoriteSet) {
            favoriteSet.clear()
            favoriteSet.addAll(favorites)
            _favoriteFlow.value = favoriteSet.toSet()
        }
        android.util.Log.d("FavoriteCache", "💖 관심종목 캐시 업데이트: ${favorites.size}개")
    }
    
    /**
     * 특정 종목의 관심종목 여부 확인 (O(1) 조회)
     */
    fun isFavorite(stockCode: String): Boolean {
        return synchronized(favoriteSet) {
            stockCode in favoriteSet
        }
    }
    
    /**
     * 관심종목 토글 (Optimistic Update용)
     * @return true면 추가됨, false면 제거됨
     */
    fun toggle(stockCode: String): Boolean {
        return synchronized(favoriteSet) {
            val wasAdded = if (stockCode in favoriteSet) {
                favoriteSet.remove(stockCode)
                false
            } else {
                favoriteSet.add(stockCode)
                true
            }
            _favoriteFlow.value = favoriteSet.toSet()
            android.util.Log.d("FavoriteCache", "💖 관심종목 토글: $stockCode → ${if (wasAdded) "추가" else "제거"}")
            wasAdded
        }
    }
    
    /**
     * 관심종목 추가
     */
    fun addFavorite(stockCode: String) {
        synchronized(favoriteSet) {
            if (favoriteSet.add(stockCode)) {
                _favoriteFlow.value = favoriteSet.toSet()
                android.util.Log.d("FavoriteCache", "💖 관심종목 추가: $stockCode")
            }
        }
    }
    
    /**
     * 관심종목 제거
     */
    fun removeFavorite(stockCode: String) {
        synchronized(favoriteSet) {
            if (favoriteSet.remove(stockCode)) {
                _favoriteFlow.value = favoriteSet.toSet()
                android.util.Log.d("FavoriteCache", "💖 관심종목 제거: $stockCode")
            }
        }
    }
    
    /**
     * 현재 관심종목 목록 조회
     */
    fun getCurrentFavorites(): Set<String> {
        return synchronized(favoriteSet) {
            favoriteSet.toSet()
        }
    }
    
    /**
     * 캐시 초기화
     */
    fun clear() {
        synchronized(favoriteSet) {
            favoriteSet.clear()
            _favoriteFlow.value = emptySet()
        }
        android.util.Log.d("FavoriteCache", "💖 관심종목 캐시 초기화")
    }
}