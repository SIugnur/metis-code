package com.siugnur.metiscode.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionSimple {
    private String id;
    private int sessionId;
    private String topic;
}
