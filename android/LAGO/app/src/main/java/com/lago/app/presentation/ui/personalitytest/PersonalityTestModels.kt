package com.lago.app.presentation.ui.personalitytest

data class PersonalityQuestion(
    val id: Int,
    val question: String,
    val options: List<PersonalityOption>
)

data class PersonalityOption(
    val text: String,
    val score: Int
)

data class PersonalityResult(
    val type: PersonalityType,
    val score: Int,
    val characterRes: Int,
    val description: String,
    val nickname: String
)

enum class PersonalityType(val displayName: String, val characterName: String) {
    CAUTIOUS("안정추구형", "조심이"),
    BALANCED("위험중립형", "균형이"),
    ACTIVE("적극투자형", "적극이"),
    AGGRESSIVE("공격투자형", "화끈이")
}

object PersonalityTestData {
    val questions = listOf(
        PersonalityQuestion(
            id = 1,
            question = "친구가 \"요즘 주식 할까 봐\"라고 말했을 때 당신의 반응은?",
            options = listOf(
                PersonalityOption("에이 위험하잖아. 하지 마~", 1),
                PersonalityOption("음… 예적금이 더 마음 편하긴 한데?", 2),
                PersonalityOption("그래? 뭘 사볼까 같이 알아볼까?", 3),
                PersonalityOption("오 재밌겠다! 나도 관심 있는데 같이 해보자", 4),
                PersonalityOption("야 나 요즘 코인도 보고 있어ㅋㅋ 뭐 재밌는 거 없냐?", 5)
            )
        ),
        PersonalityQuestion(
            id = 2,
            question = "쇼핑할 때 당신의 소비 스타일은?",
            options = listOf(
                PersonalityOption("세일할 때만 산다. 고민 많이 하고 신중하게", 1),
                PersonalityOption("사고 싶은 건 장바구니에 담아두고 며칠 고민함", 2),
                PersonalityOption("실용성 따져보고 괜찮으면 산다", 3),
                PersonalityOption("예산 안 넘으면 그냥 산다", 4),
                PersonalityOption("느낌 오는 대로 바로 결제! 후회는 나중에!", 5)
            )
        ),
        PersonalityQuestion(
            id = 3,
            question = "갑자기 돈이 생겼다! 100만 원이면?",
            options = listOf(
                PersonalityOption("무조건 통장으로!", 1),
                PersonalityOption("반은 저축, 반은 생활비", 2),
                PersonalityOption("적금 좀 넣고 나머진 ETF나 간단한 투자", 3),
                PersonalityOption("단기 주식이나 펀드에 굴려볼까?", 4),
                PersonalityOption("한 번쯤은 고위험 고수익도 도전해야지!", 5)
            )
        ),
        PersonalityQuestion(
            id = 4,
            question = "새로운 앱이나 서비스 쓸 때 당신은?",
            options = listOf(
                PersonalityOption("사용 후기부터 다 찾아보고 가입해요", 1),
                PersonalityOption("익숙한 서비스가 더 편해서 잘 안 바꿔요", 2),
                PersonalityOption("남들이 좋다 하면 한번 써봐요", 3),
                PersonalityOption("신기하면 바로 깔고 해봐요", 4),
                PersonalityOption("베타 테스트 신청도 자주 해요. 신상 좋아요!", 5)
            )
        ),
        PersonalityQuestion(
            id = 5,
            question = "여행을 가게 된다면?",
            options = listOf(
                PersonalityOption("여행? 집이 제일 좋아요", 1),
                PersonalityOption("계획 꼼꼼히 세우고 경비도 최대한 아껴요", 2),
                PersonalityOption("자유여행! 일정은 대충, 분위기 따라~", 3),
                PersonalityOption("일단 가서 생각함! 즉흥 여행 좋아함", 4),
                PersonalityOption("해외 무계획 여행도 OK. 현지에서 알아보면 되지", 5)
            )
        ),
        PersonalityQuestion(
            id = 6,
            question = "투자를 할 때 가장 중요하게 생각하는 건?",
            options = listOf(
                PersonalityOption("무조건 안전! 원금 보장", 1),
                PersonalityOption("손해는 보기 싫어… 적당한 수익만", 2),
                PersonalityOption("너무 튀지 않는, 안정+성장 둘 다", 3),
                PersonalityOption("리스크 있어도 기회라면 잡아야지", 4),
                PersonalityOption("수익률이 깡패. 공격적 투자도 괜찮아", 5)
            )
        ),
        PersonalityQuestion(
            id = 7,
            question = "아래 중 가장 나와 닮은 말은?",
            options = listOf(
                PersonalityOption("천천히 가도 괜찮아, 안전하게!", 1),
                PersonalityOption("준비된 만큼만 움직이자", 2),
                PersonalityOption("가끔은 모험도 필요하지!", 3),
                PersonalityOption("리스크 없인 변화도 없어!", 4),
                PersonalityOption("기회는 준비된 사람의 것, 지금이야!", 5)
            )
        )
    )
    
    fun calculatePersonality(totalScore: Int): PersonalityType {
        return when (totalScore) {
            in 0..13 -> PersonalityType.CAUTIOUS
            in 14..21 -> PersonalityType.BALANCED
            in 22..28 -> PersonalityType.ACTIVE
            in 29..35 -> PersonalityType.AGGRESSIVE
            else -> PersonalityType.BALANCED
        }
    }
    
    fun getPersonalityDescription(type: PersonalityType): String {
        return when (type) {
            PersonalityType.AGGRESSIVE -> "위험? 수익? 기회가 보이면 질러버려요\n#상승장좋아 #단타의신 #하이리스크하이리턴"
            PersonalityType.ACTIVE -> "분석은 필수! 빠른 판단력으로 기회를 잡아요 위험도 수익도 챙기는 똑똑한 투자자\n#전략형투자 #리스크관리"
            PersonalityType.BALANCED -> "수익도 중요, 안정성도 중요! 분산 투자로 리스크를 줄이는 스마트 전략가\n#분산투자 #중장기관점 #위험조절"
            PersonalityType.CAUTIOUS -> "조금 늦어도 괜찮아. 안전한 자산으로 착실하게 불려나가요\n#예금좋아 #리스크회피형"
        }
    }
}