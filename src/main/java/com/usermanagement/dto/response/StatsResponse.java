package com.usermanagement.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatsResponse {

    private long totalUsers;
    private LocalDateTime lastLoginTimestamp;
}
