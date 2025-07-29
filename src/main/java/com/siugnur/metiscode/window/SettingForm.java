package com.siugnur.metiscode.window;

import com.intellij.ide.util.PropertiesComponent;
import com.siugnur.metiscode.service.MetisServiceImpl;
import com.siugnur.metiscode.utils.Constant;
import lombok.Getter;
import javax.swing.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SettingForm {
    private JTextField inputUsername;
    private JTextField inputUserId;
    private JLabel labelUsername;
    private JLabel labelPwd;
    private JLabel labelUserId;
    private JPasswordField inputPwd;

    @Getter
    private JPanel form;
    private JButton loginBtn;

    public SettingForm() {
        inputUsername.setText(PropertiesComponent.getInstance().getValue(Constant.USERNAME));
        inputUserId.setText(PropertiesComponent.getInstance().getValue(Constant.USER_ID));

        loginBtn.addActionListener(e -> {
            if ("Login".equals(loginBtn.getName()) && !loginBtn.getText().equals("登录中...")) {
                // 设置 loading 状态
                loginBtn.setText("登录中...");
                loginBtn.setEnabled(false);

                CompletableFuture.runAsync(() -> {
                    try {
                        PropertiesComponent.getInstance().setValue(Constant.USERNAME, inputUsername.getText());
                        PropertiesComponent.getInstance().setValue(Constant.USER_ID, inputUserId.getText());
                        PropertiesComponent.getInstance().setValue(Constant.PASSWORD, Arrays.toString(inputPwd.getPassword()));
                        MetisServiceImpl service = new MetisServiceImpl();
                        service.login(inputUsername.getText(), new String(inputPwd.getPassword()));

                        MetisMainForm.instance.init();
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(loginBtn, "登录失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                            // 恢复按钮状态
                            loginBtn.setText("登录");
                            loginBtn.setEnabled(true);
                        });
                        return;
                    }

                    // 恢复按钮状态并切换为登出
                    SwingUtilities.invokeLater(() -> {
                        loginBtn.setText("登出");
                        loginBtn.setName("Logout");
                        loginBtn.setEnabled(true);
                    });
                });
            }

            if ("Logout".equals(loginBtn.getName())) {
                PropertiesComponent.getInstance().setValue(Constant.TOKEN, "");
                loginBtn.setText("登录");
                loginBtn.setName("Login");
            }
        });

        // 初始化按钮状态
        String token = PropertiesComponent.getInstance().getValue(Constant.TOKEN);
        if (token != null && !token.isEmpty()) {
            loginBtn.setName("Logout");
            loginBtn.setText("登出");
        } else {
            loginBtn.setName("Login");
            loginBtn.setText("登录");
        }
    }

}
