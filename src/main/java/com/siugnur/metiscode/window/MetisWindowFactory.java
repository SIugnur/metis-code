package com.siugnur.metiscode.window;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MetisWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建主面板
        MetisMainForm form = new MetisMainForm();
        JPanel mainPanel = form.getMain();

        // 设置 ToolWindow 内容
        toolWindow.getContentManager().addContent(toolWindow.getContentManager().getFactory().createContent(mainPanel, "", false));
    }
}
