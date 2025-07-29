package com.siugnur.metiscode.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.ui.Messages;
import com.siugnur.metiscode.domain.Message;
import com.siugnur.metiscode.service.IMetisService;
import com.siugnur.metiscode.service.MetisServiceImpl;
import com.siugnur.metiscode.utils.CommonUtil;
import com.siugnur.metiscode.utils.Constant;
import okhttp3.*;
import okio.BufferedSource;
import okio.Okio;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GenerateCode extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        String prompt = Messages.showInputDialog("请输入要生成的内容", "提示", null);
        ArrayList<Message> context = new ArrayList<>();
        context.add(new Message("system", "You are a code generation assistant. Generate ONLY code based on:\n" +
                "\n" +
                "- The specified file content and line number\n" +
                "- User requirements/description\n" +
                "- Selected code to be replaced (if provided)\n" +
                "\n" +
                "Key rules:\n" +
                "1. Output ONLY code - no explanations, no markdown, no extra text\n" +
                "2. If selected code is provided, generate replacement code\n" +
                "3. If no selected code, generate new code at specified location\n" +
                "4. Write comments in the same language as user requirements\n" +
                "5. Maintain consistent coding style and syntax correctness\n" +
                "6. Return only the generated code snippet\n"));
        context.add(new Message("system", "All code: " + document.getText()));
        // 获取当前代码所在文件的行数
        int lineCount = document.getLineCount();
        context.add(new Message("system",  "Generate code in Line " + lineCount));
        context.add(new Message("user", prompt));

        OkHttpClient client = CommonUtil.getHttpClient();

        // 构建请求
        Request.Builder builder = new Request.Builder()
                .url("https://ai-chat.wiltechs.com/api/deepseek/chat/completions")
                .header("Content-Type", "application/json");

        try {
            if (selectionModel.hasSelection()) {
                String code = selectionModel.getSelectedText();
                context.add(new Message("system", "Selected code: " + code));
            }
            RequestBody requestBody = getRequestBody(context);
            Request req = builder.post(requestBody).build();
            Response response = client.newCall(req).execute();
            assert response.body() != null;
            if (selectionModel.hasSelection()) {
                int start = selectionModel.getSelectionStart();
                int end = selectionModel.getSelectionEnd();
                WriteCommandAction.runWriteCommandAction(e.getProject(), () -> {
                    document.replaceString(start, end, "");
                });
            }

            CompletableFuture.runAsync(() -> {
                BufferedSource buffer = Okio.buffer(response.body().source());
                String line;
                while (true) {
                    try {
                        if ((line = buffer.readUtf8Line()) == null) break;
                    } catch (IOException e1) {
                        Messages.showErrorDialog("Metis 回答发送错误，请联系开发者", "Error");
                        return;
                    }
                    if (line.startsWith("data:")) {
                        String content = line.substring(5).trim();
                        JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
                        String partMsg = jsonObject
                                .getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("delta")
                                .get("content")
                                .getAsString();
                        WriteCommandAction.runWriteCommandAction(e.getProject(), () -> {
                            // 获取光标的位置
                            int caretPosition = editor.getCaretModel().getOffset();
                            document.insertString(caretPosition, partMsg);
                            // 更新光标位置
                            editor.getCaretModel().moveToOffset(caretPosition + partMsg.length());
                        });
                    }
                }
            });

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    public RequestBody getRequestBody(List<Message> context) {
        JsonArray msgArray = new JsonArray();
        for (Message message : context) {
            JsonObject jsonItem = new JsonObject();
            jsonItem.addProperty("role", message.getRole());
            jsonItem.addProperty("content", message.getContent());
            msgArray.add(jsonItem);
        }

        JsonObject requestBody = new JsonObject();
        requestBody.add("messages", msgArray);
        requestBody.addProperty("stream", true);
        requestBody.addProperty("model", Constant.MODEL); // 示例模型名称，可以根据需要修改
        requestBody.addProperty("temperature", 0.5);
        requestBody.addProperty("presence_penalty", 0);
        requestBody.addProperty("frequency_penalty", 0);
        requestBody.addProperty("top_p", 1);

        // 构建请求体
        return RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json")
        );
    }
}
