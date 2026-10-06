package com.ms.metercollector.message;

public enum MeterStatus {
    OK,     // 정상
    FAULT,  // 계량기 이상. 상세는 errorCodes
    MAINT   // 점검 중
}
