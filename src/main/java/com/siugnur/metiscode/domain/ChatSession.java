package com.siugnur.metiscode.domain;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChatSession {
    private String id;
    private int sessionId;
    private String topic;
    private String memoryPrompt;
    private String messages;
    private String stat;
    private long lastUpdate;
    private int lastSummarizeIndex;
    private String mask;
    private int clearContextIndex;
    private int userId;
    private boolean isDeleted;
    private List<Message> msgList = new ArrayList<>();
}