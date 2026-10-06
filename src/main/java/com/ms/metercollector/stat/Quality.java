package com.ms.metercollector.stat;

// 통계 구간의 데이터 품질. 여러 조건에 걸리면 위에 있는 것이 우선한다.
public enum Quality {
    MISSING("없음"),  // 표본이 하나도 없음
    FAULT("이상"),    // 계량기 이상 표본이 있거나, 누적값이 줄어듦(계량기 교체/리셋)
    MAINT("점검"),    // 점검 중 표본이 있음
    PARTIAL("일부"),  // 표본이 기대 개수보다 적음
    OK("정상");

    private final String label;

    Quality(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
