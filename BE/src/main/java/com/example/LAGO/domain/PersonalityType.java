package com.example.LAGO.domain;

import lombok.Getter;

/**
 * 투자 성향 enum
 * 캐릭터명과 설명을 포함하여 중앙 집중 관리
 */
@Getter
public enum PersonalityType {
    CONSERVATIVE("조심이", "안정추구형", "안정적인 투자를 선호하며 리스크를 최소화"),
    BALANCED("균형이", "위험중립형", "안정성과 수익성의 균형을 추구"),
    ACTIVE("적극이", "적극투자형", "적극적인 투자로 높은 수익을 추구"),
    AGGRESSIVE("화끈이", "공격투자형", "고위험 고수익 투자를 선호하는 공격적 성향");

    private final String characterName;    // 캐릭터 이름 (조심이, 균형이, 적극이, 화끝이)
    private final String description;      // 성향 설명 (안정추구형, 위험중립형, etc)
    private final String detail;          // 상세 설명

    PersonalityType(String characterName, String description, String detail) {
        this.characterName = characterName;
        this.description = description;
        this.detail = detail;
    }

    /**
     * 캐릭터명으로 PersonalityType 조회
     */
    public static PersonalityType fromCharacterName(String characterName) {
        for (PersonalityType type : PersonalityType.values()) {
            if (type.getCharacterName().equals(characterName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("알 수 없는 성향: " + characterName);
    }

    /**
     * 설명으로 PersonalityType 조회 (기존 TradingConstants와 호환)
     */
    public static PersonalityType fromDescription(String description) {
        for (PersonalityType type : PersonalityType.values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        throw new IllegalArgumentException("알 수 없는 성향: " + description);
    }

    /**
     * enum name으로 PersonalityType 조회 (안전한 valueOf)
     */
    public static PersonalityType fromName(String name) {
        try {
            return PersonalityType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 성향: " + name);
        }
    }
}