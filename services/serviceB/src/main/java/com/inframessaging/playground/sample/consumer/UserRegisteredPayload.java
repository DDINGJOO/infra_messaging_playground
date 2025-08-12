package com.inframessaging.playground.sample.consumer;

import lombok.Data;

/**
 * (type="UserRegisteredEvent", version=1) 의 payload VO 예시
 * - 역직렬화 시 필요한 필드: userId, email, userNumber, regCode
 */
@Data
public class UserRegisteredPayload {
    private String userId;
    private String email;
    private Long userNumber;
    private Long regCode; // 요청 명세의 RegCode
}
