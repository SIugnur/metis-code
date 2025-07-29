package com.siugnur.metiscode.window;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.siugnur.metiscode.domain.Message;
import com.siugnur.metiscode.utils.UICustomUtil;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public class MessageItemPanel extends JPanel {
    private final JPanel contentArea;
    private final Message message;

    public MessageItemPanel(Message message) {
        this.message = message;
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel usernameLabel = new JLabel(message.getRole());
        headerPanel.setBorder(JBUI.Borders.empty(5, 15));
        JButton copyButton = new JButton(AllIcons.Actions.Copy);
        copyButton.setToolTipText("复制到剪贴板");
        copyButton.setPreferredSize(new Dimension(AllIcons.Actions.Copy.getIconWidth(), AllIcons.Actions.Copy.getIconHeight()));
        copyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        copyButton.addActionListener(e -> {
            // 实现复制功能
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(message.getContent());
            clipboard.setContents(selection, null);
            JOptionPane.showMessageDialog(this, "已复制到剪贴板", "提示", JOptionPane.INFORMATION_MESSAGE);
        });
        headerPanel.add(usernameLabel, BorderLayout.WEST);
        headerPanel.add(copyButton, BorderLayout.EAST);

        // 创建内容区域
        contentArea = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                // 如果有父容器，使用父容器的宽度
                Container parent = getParent();
                if (parent != null) {
                    size.width = parent.getWidth() - 20; // 减去边距
                }
                return size;
            }
        };
        contentArea.setLayout(new BoxLayout(contentArea, BoxLayout.Y_AXIS));
        setContent(message.getContent());
        JScrollPane contentScrollPane = new JBScrollPane(contentArea);
        contentScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        contentScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contentScrollPane.setWheelScrollingEnabled(false);

        // 创建尾部面板
        JPanel footerPanel = new JPanel();

        // 将各部分添加到主面板
        add(headerPanel, BorderLayout.NORTH);
        add(contentScrollPane, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);

        setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(JBColor.GRAY, 1, 15), // 圆角边框，10为圆角半径
                BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));

        // 设置最大尺寸等于首选尺寸，防止被拉伸
        //setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();

        // 计算内容区域所需的高度
        if (contentArea != null) {
            // 获取当前面板宽度减去边距和滚动面板边框
            int contentWidth = Math.max(100, getWidth() - 20);

            // 临时设置宽度以计算高度
            contentArea.setSize(contentWidth, Integer.MAX_VALUE);
            Dimension contentPrefSize = contentArea.getPreferredSize();

            // 更新首选尺寸的高度
            preferredSize.height = headerPanelHeight() + contentPrefSize.height + footerPanelHeight() + 20; // 20为边距
        }

        return preferredSize;
    }

    private int headerPanelHeight() {
        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel && ((JPanel) comp).getLayout() instanceof BorderLayout) {
                return comp.getPreferredSize().height;
            }
        }
        return 30; // 默认高度
    }

    private int footerPanelHeight() {
        return 20; // 默认尾部高度
    }

    public void updateMessage() {
        // 更新文本内容
        setContent(message.getContent());
        repaint();
        // 获取父容器并触发重新布局
        SwingUtilities.invokeLater(() -> {
            Container parent = getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        });
    }

    private void setContent(String markdownContent) {
        // 清空现有的内容 contentArea.setText("");
        // 解析 markdown 内容，按代码块分割
        String[] parts = markdownContent.split("```");
        // 创建主容器面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // flexmark 处理 markdown 的配置
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                TaskListExtension.create()
        ));
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // 交替处理普通文本和代码块
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (i % 2 == 0) {
                // 普通文本部分
                if (!part.trim().isEmpty()) {
                    String html = renderer.render(parser.parse(part));
                    // 添加自定义 CSS 样式
                    String styledHtml = "<html><head><style>" +
                            "body { " +
                            "word-wrap: break-word;" +
                            "white-space: pre-wrap;" +
                            "word-break: break-word; " +
                            "width: 100%; " +
                            "}" +
                            "</style></head><body>" + html + "</body></html>";

                    JTextPane textPane = new JTextPane();
                    textPane.setEditable(false);
                    textPane.setContentType("text/html");
                    textPane.setText(styledHtml);
                    textPane.setLayout(new BorderLayout());
                    mainPanel.add(textPane, BorderLayout.NORTH);
                }
            } else {
                // 代码块部分
                JPanel codePanel = createCodeBlockPanel(part);
                mainPanel.add(codePanel);
            }
        }

        // 将内容添加到主内容区域
        contentArea.removeAll();
        contentArea.setLayout(new BorderLayout());
        contentArea.add(mainPanel, BorderLayout.NORTH);
        contentArea.revalidate();
        contentArea.repaint();
    }

    private JPanel createCodeBlockPanel(String codeContent) {
        String language = "";
        String actualCodeContent;

        // 查找第一个换行符，分离语言标识和代码内容
        int firstNewLineIndex = codeContent.indexOf("\n");
        if (firstNewLineIndex > 0) {
            language = codeContent.substring(0, firstNewLineIndex).trim();
            actualCodeContent = codeContent.substring(firstNewLineIndex + 1);
        } else {
            actualCodeContent = codeContent;
        }

        JPanel panel = new JPanel(new BorderLayout());

        // 创建按钮面板
        JPanel coderHeader = new JPanel(new BorderLayout());
        coderHeader.setBackground(Color.decode("#2b2b2b"));
        coderHeader.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel codeType = new JLabel(language);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyButton = new JButton(AllIcons.Actions.Copy);
        copyButton.setToolTipText("复制代码");
        copyButton.setPreferredSize(new Dimension(AllIcons.Actions.Copy.getIconWidth(), AllIcons.Actions.Copy.getIconHeight()));
        copyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JButton insertButton = new JButton(AllIcons.Actions.AddList);
        insertButton.setToolTipText("插入代码");
        insertButton.setPreferredSize(new Dimension(AllIcons.Actions.Copy.getIconWidth(), AllIcons.Actions.Copy.getIconHeight()));
        insertButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        copyButton.addActionListener(e -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(actualCodeContent);
            clipboard.setContents(selection, null);
            UICustomUtil.showSimpleTip(copyButton, "复制成功");
        });
        insertButton.addActionListener(e -> {
            Editor editor = EditorFactory.getInstance().getAllEditors()[0];
            if (editor == null) return;
            Document document = editor.getDocument();
            SelectionModel selectionModel = editor.getSelectionModel();
            if (selectionModel.hasSelection()) {
                int start = selectionModel.getSelectionStart();
                int end = selectionModel.getSelectionEnd();
                WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                    document.replaceString(start, end, actualCodeContent);
                });
            } else {
                WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                    document.insertString(selectionModel.getSelectionEnd(), actualCodeContent);
                });
            }
        });

        coderHeader.add(codeType, BorderLayout.WEST);
        buttonPanel.add(copyButton);
        buttonPanel.add(insertButton);
        coderHeader.add(buttonPanel, BorderLayout.EAST);
        // 设置细底线
        coderHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new JBColor(Color.magenta, new Color(168, 166, 169))));
        JTextPane codeTextPane = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return false;
            }
        };
        codeTextPane.setEditable(false);
        codeTextPane.setText(actualCodeContent);
        codeTextPane.setBackground(Color.decode("#2b2b2b"));
        codeTextPane.setForeground(Color.WHITE);

        JScrollPane codeScrollPane = new JBScrollPane(codeTextPane);
        codeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        codeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        codeScrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(coderHeader, BorderLayout.NORTH);
        panel.add(codeScrollPane, BorderLayout.CENTER);

        return panel;
    }
}

