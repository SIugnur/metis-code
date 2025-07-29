package com.siugnur.metiscode.service;

import com.siugnur.metiscode.domain.ChatSession;
import okio.BufferedSource;

import java.util.List;

public interface IMetisService {
    public void login(String username, String password);
    public List<ChatSession> getHistories();
    public BufferedSource sendMsg(String msg);
    public void saveSession(ChatSession session);
}
