package com.siugnur.metiscode.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class Message {
    private String id;
    private String date;
    private String role;
    private String content;
    private boolean streaming;
    private String model;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
