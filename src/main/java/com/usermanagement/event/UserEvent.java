package com.usermanagement.event;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType;
    private Long userId;
    private String email;
    private LocalDateTime timestamp;

    public enum EventType {
        USER_REGISTERED,
        USER_LOGGED_IN
    }
}
