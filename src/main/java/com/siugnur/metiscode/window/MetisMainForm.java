package com.siugnur.metiscode.window;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.Messages;
import com.siugnur.metiscode.domain.ChatSession;
import com.siugnur.metiscode.domain.ChatSessionSimple;
import com.siugnur.metiscode.domain.Message;
import com.siugnur.metiscode.service.IMetisService;
import com.siugnur.metiscode.service.MetisServiceImpl;
import com.siugnur.metiscode.utils.CommonUtil;
import com.siugnur.metiscode.utils.Constant;
import okio.BufferedSource;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MetisMainForm {
    public static MetisMainForm instance = null;
    public static ChatSession currentSession = new ChatSession();

    private JPanel metisMain;
    private JComboBox<ChatSessionSimple> historyBox;
    private JButton send;
    private JTextArea inputArea;
    private JTabbedPane tabbedPane;
    private JScrollPane setting;
    private JScrollPane chat;
    private JPanel msgListPanel;

    public MetisMainForm() {
        send.addActionListener(actionEvent -> {
            String inputMsg = inputArea.getText();
            if (inputMsg.isBlank()) {
                return;
            }
            inputArea.setText("");
            inputArea.updateUI();

            Object item = historyBox.getSelectedItem();
            if (item == null || !((ChatSessionSimple) item).getId().equals(currentSession.getId())) {
                Messages.showWarningDialog("请重新选择当前会话", "提示");
                return;
            }
            List<Message> msgList = MetisMainForm.currentSession.getMsgList();
            Message sendMsg = new Message();
            sendMsg.setId(CommonUtil.generateRandomID());
            sendMsg.setRole("user");
            sendMsg.setContent(inputMsg);
            sendMsg.setDate(CommonUtil.currentDatetime());
            msgList.add(sendMsg);
            addMessageItemPanel(sendMsg);
            msgListPanel.revalidate();
            msgListPanel.repaint();
            scrollToBottom();

            CompletableFuture.runAsync(() -> sendMsg(inputMsg));
        });
        historyBox.addActionListener(actionEvent ->
                historyBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
                    DefaultListCellRenderer renderer = new DefaultListCellRenderer();
                    renderer.getListCellRendererComponent(list, value.getTopic(), index, isSelected, cellHasFocus);
                    return renderer;
                }));
        historyBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                ChatSessionSimple selected = (ChatSessionSimple) itemEvent.getItem();
                String histories = PropertiesComponent.getInstance().getValue(Constant.HISTORIES);
                List<ChatSession> historyList = new Gson().fromJson(histories, new TypeToken<List<ChatSession>>() {}.getType());
                assert historyList != null;
                currentSession = historyList.stream().filter(item -> item.getId().equals(selected.getId())).findFirst().get();
                currentSession.setMsgList(new Gson().fromJson(currentSession.getMessages(), new TypeToken<List<Message>>() {}.getType()));
                msgListPanel.removeAll();
                for (Message message : currentSession.getMsgList()) {
                    addMessageItemPanel(message);
                }
                JScrollBar bar = chat.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
                chat.repaint();
            }
        });
        setting.setViewportView(new SettingForm().getForm());

        // 消息列表
        // 使用垂直BoxLayout
        msgListPanel  = new JPanel();
        msgListPanel.setLayout(new BoxLayout(msgListPanel, BoxLayout.Y_AXIS));
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(msgListPanel, BorderLayout.NORTH);
        wrapperPanel.add(Box.createVerticalGlue()); // 把消息顶上去
        chat.setViewportView(wrapperPanel);

        instance = this;
        init();
    }

    public void init() {
        setHistoryBox();
    }

    public JPanel getMain() {
        return metisMain;
    }

    public void setHistoryBox() {
        IMetisService service = new MetisServiceImpl();
        List<ChatSession> histories = service.getHistories();
        for (ChatSession history : histories) {
            historyBox.addItem(new ChatSessionSimple(history.getId(), history.getSessionId(), history.getTopic()));
        }
    }

    public void sendMsg(String inputMsg) {
        IMetisService service = new MetisServiceImpl();
        BufferedSource bufferedSource = service.sendMsg(inputMsg);

        Message replyMsg = new Message();
        replyMsg.setId(CommonUtil.generateRandomID());
        replyMsg.setRole("assistant");
        replyMsg.setModel(Constant.MODEL);
        replyMsg.setContent("");
        currentSession.getMsgList().add(replyMsg);
        MessageItemPanel replyMsgItem = addMessageItemPanel(replyMsg);

        // 在新线程中处理流式响应
        CompletableFuture<Void> metisAnswer = CompletableFuture.runAsync(() -> {
            String line;
            while (true) {
                try {
                    if ((line = bufferedSource.readUtf8Line()) == null) break;
                } catch (IOException e) {
                    inputArea.setText(inputMsg);
                    Messages.showErrorDialog("Metis 回答发送错误，请联系开发者", "Error");
                    throw new RuntimeException(e);
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
                    replyMsg.setContent(replyMsg.getContent() + partMsg);
                    replyMsg.setDate(CommonUtil.currentDatetime());
                    // 更新 UI
                    SwingUtilities.invokeLater(() -> {
                        replyMsgItem.updateMessage();
                        scrollToBottom();
                    });
                }
            }
        });
        metisAnswer.thenRun(() -> {
            // 构建新的 session
            currentSession.setMessages(new Gson().toJson(currentSession.getMsgList()));
            JsonElement stat = JsonParser.parseString(currentSession.getStat());
            int charCount = currentSession.getMsgList()
                    .stream()
                    .filter(message -> "assistant".equals(message.getRole()))
                    .mapToInt(message -> message.getContent().length())
                    .sum();
            stat.getAsJsonObject().addProperty("charCount", charCount);
            currentSession.setStat(new Gson().toJson(stat));
            currentSession.setLastUpdate(System.currentTimeMillis());

            service.saveSession(currentSession);
        });
    }

    public MessageItemPanel addMessageItemPanel(Message message) {
        MessageItemPanel messageItemPanel = new MessageItemPanel(message);
        msgListPanel.add(messageItemPanel);
        msgListPanel.add(Box.createVerticalStrut(15));
        return messageItemPanel;
    }

    // 在添加消息后调用滚动方法
    public void scrollToBottom() {
        chat.revalidate();
        chat.repaint();
        Timer timer = new Timer(50, e -> {
            JScrollBar verticalBar = chat.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
        timer.setRepeats(false);
        timer.start();
    }
}
