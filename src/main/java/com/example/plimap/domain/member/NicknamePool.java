package com.example.plimap.domain.member;

import java.util.List;

// 벌점으로 닉네임이 강제 치환될 때 쓰는 후보 목록(미사용 값 선택·중복 체크는 벌점 부여 기능에서 처리)
public final class NicknamePool {

    public static final List<String> CANDIDATES = List.of(
            "참새", "까치", "부엉이", "올빼미", "갈매기", "독수리",
            "앵무새", "딱따구리", "제비", "뻐꾸기", "황새", "두루미",
            "공작새", "솔개", "박새", "직박구리", "물총새", "곤줄박이",
            "동고비", "종달새"
    );

    private NicknamePool() {
    }
}
