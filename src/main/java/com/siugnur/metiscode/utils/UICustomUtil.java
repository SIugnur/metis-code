package com.siugnur.metiscode.utils;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

public class UICustomUtil {
    public static void showSimpleTip(JComponent component, String message) {
        SwingUtilities.invokeLater(() -> {
            /*Point location = component.getLocationOnScreen();
            int x = (int) (location.getX() + component.getWidth() / 2 - 50);
            int y = (int) (location.getY() - 30);

            JWindow tipWindow = new JWindow();
            JLabel tipLabel = new JLabel(message);
            tipLabel.setOpaque(true);
            tipLabel.setBackground(JBColor.WHITE);
            tipLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(JBColor.GRAY, 1),
                    BorderFactory.createEmptyBorder(3, 6, 3, 6)
            ));

            tipWindow.add(tipLabel);
            tipWindow.pack();
            tipWindow.setLocation(x, y);
            tipWindow.setVisible(true);

            // 1.5秒后自动消失
            Timer timer = new Timer(1500, evt -> {
                tipWindow.dispose();
            });
            timer.setRepeats(false);
            timer.start();*/
        });
    }
}
