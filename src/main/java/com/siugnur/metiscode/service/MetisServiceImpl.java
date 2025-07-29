package com.siugnur.metiscode.service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.Messages;
import com.siugnur.metiscode.domain.ChatSession;
import com.siugnur.metiscode.domain.Message;
import com.siugnur.metiscode.utils.CommonUtil;
import com.siugnur.metiscode.utils.Constant;
import com.siugnur.metiscode.window.MetisMainForm;
import okhttp3.*;
import okio.BufferedSource;
import okio.Okio;
import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MetisServiceImpl implements IMetisService{
    @Override
    public void login(String username, String password) {
        OkHttpClient client = CommonUtil.getHttpClient();

        RequestBody body = new FormBody.Builder()
                .addEncoded("grant_type", "password")
                .addEncoded("client_id", "bced28a0-04cc-49b4-95ee-f0f0d8a4875c")
                .addEncoded("client_secret", "37b88cd3-7b59-494c-8de1-703a308888da")
                .addEncoded("username", username)
                .addEncoded("password", password)
                .addEncoded("scope", "createdway profile openid offline_access phone email ome-account-api  fullname  avatar  account birthday  address")
                .build();

        // 构建请求
        Request request = new Request.Builder()
                .url("https://ome-account.omenow.com/connect/token")
                .post(body)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try {
            Response execute = client.newCall(request).execute();
            assert execute.body() != null;
            String resp = execute.body().string();
            JsonObject jsonObject = JsonParser.parseString(resp).getAsJsonObject();
            String accessToken = jsonObject.get("access_token").getAsString();
            PropertiesComponent.getInstance().setValue(Constant.TOKEN, accessToken);
            SwingUtilities.invokeLater(() -> Messages.showMessageDialog("登录成功!", "提示", Messages.getInformationIcon()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ChatSession> getHistories() {
        OkHttpClient client = CommonUtil.getHttpClient();
        String token = PropertiesComponent.getInstance().getValue(Constant.TOKEN);
        Request request = new Request.Builder()
                .url("https://smarties.yamimeal.ca/api/v1/histories") // 替换为实际 URL
                .get()
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray dataArray = jsonObject.getAsJsonArray("data");

                // 需要存储历史记录
                PropertiesComponent.getInstance().setValue(Constant.HISTORIES, dataArray.toString());
                return new Gson().fromJson(dataArray, new TypeToken<List<ChatSession>>(){}.getType());
            } else {
                throw new Exception("错误：" + response.code() + ", " + Messages.getErrorIcon());
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> Messages.showMessageDialog("获取聊天历史请求异常: " + e.getMessage(), "错误", Messages.getErrorIcon()));
        }

        return new ArrayList<>();
    }

    @Override
    public BufferedSource sendMsg(String msg) {
        OkHttpClient client = CommonUtil.getHttpClient();

        List<Message> msgList = MetisMainForm.currentSession.getMsgList();

        // 上下文取 5 条消息
        int startIndex = Math.max(0, msgList.size() - 5);
        List<Message> lastFourItems = msgList.subList(startIndex, msgList.size());

        JsonArray msgArray = new JsonArray();
        for (Message message : lastFourItems) {
            JsonObject jsonItem = new JsonObject();
            jsonItem.addProperty("role", message.getRole());
            jsonItem.addProperty("content", message.getContent());
            msgArray.add(jsonItem);
        }

        JsonObject requestBody = new JsonObject();
        requestBody.add("messages", msgArray);
        requestBody.addProperty("stream", true);
        requestBody.addProperty("model", "metis-chat"); // 示例模型名称，可以根据需要修改
        requestBody.addProperty("temperature", 0.5);
        requestBody.addProperty("presence_penalty", 0);
        requestBody.addProperty("frequency_penalty", 0);
        requestBody.addProperty("top_p", 1);

        // 构建请求体
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json")
        );

        // 构建请求
        Request request = new Request.Builder()
                .url("https://ai-chat.wiltechs.com/api/deepseek/chat/completions")
                .post(body)
                .header("Content-Type", "application/json")
                .build();

        try {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            return Okio.buffer(response.body().source());
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> Messages.showMessageDialog("发送失败: " + e.getMessage(), "错误", Messages.getErrorIcon()));
            return null;
        }
    }

    @Override
    public void saveSession(ChatSession session) {
        OkHttpClient client = CommonUtil.getHttpClient();
        String token = PropertiesComponent.getInstance().getValue(Constant.TOKEN);
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("sessionId", session.getSessionId());
        requestBody.addProperty("id", session.getId());
        requestBody.addProperty("topic", session.getTopic());
        requestBody.addProperty("memoryPrompt", session.getMemoryPrompt());
        requestBody.addProperty("messages", session.getMessages());
        requestBody.addProperty("stat", session.getStat());
        requestBody.addProperty("lastUpdate", session.getLastUpdate());
        requestBody.addProperty("lastSummarizeIndex", 0);
        requestBody.addProperty("clearContextIndex", 0);
        requestBody.addProperty("mask", session.getMask());
        requestBody.addProperty("isDeleted", false);

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json")
        );
        Request request = new Request.Builder()
                .url("https://smarties.yamimeal.ca/api/v1/history/addOrUpdate")
                .post(body)
                .header("Content-Type", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            JsonObject data = JsonParser.parseString(response.body().string()).getAsJsonObject().getAsJsonObject("data");
            ChatSession chatSession = new Gson().fromJson(data, ChatSession.class);
            String historiesJson = PropertiesComponent.getInstance().getValue(Constant.HISTORIES);
            List<ChatSession> histories = new Gson().fromJson(historiesJson, new TypeToken<List<ChatSession>>() {}.getType());
            assert histories != null;
            for (int i = 0; i < histories.size(); i++) {
                if (histories.get(i).getId().equals(chatSession.getId())) {
                    histories.set(i, chatSession);
                    break;
                }
            }
            PropertiesComponent.getInstance().setValue(Constant.HISTORIES, new Gson().toJson(histories));
            if (!response.isSuccessful()) {
                throw new Exception("错误：" + response.code() + ", " + Messages.getErrorIcon());
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> Messages.showMessageDialog("保存会话异常: " + e.getMessage(), "错误", Messages.getErrorIcon()));
        }
    }
}
