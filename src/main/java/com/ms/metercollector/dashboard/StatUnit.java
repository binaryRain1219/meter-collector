package com.ms.metercollector.dashboard;

// 장비 상세 화면에서 고르는 통계 단위. code 가 URL 의 unit 파라미터 값이다.
public enum StatUnit {
    M15("15m", "15분", 15),
    H1("1h", "1시간", 60);

    private final String code;
    private final String label;
    private final int minutes;

    StatUnit(String code, String label, int minutes) {
        this.code = code;
        this.label = label;
        this.minutes = minutes;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public int minutes() {
        return minutes;
    }

    public static StatUnit fromCode(String code) {
        for (StatUnit unit : values()) {
            if (unit.code.equals(code)) {
                return unit;
            }
        }
        return M15;
    }
}
