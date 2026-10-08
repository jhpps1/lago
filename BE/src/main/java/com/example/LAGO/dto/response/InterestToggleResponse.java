package com.example.LAGO.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;


// 관심 종목 추가/삭제 응답
@Getter
@AllArgsConstructor
public class InterestToggleResponse {
    private boolean isAdded;    // true : 추가됨, false : 삭제됨
    private boolean success;    // 작업 성공 여부
}
