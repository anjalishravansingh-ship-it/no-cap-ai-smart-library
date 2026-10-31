package com.nocapai;

import com.nocapai.gui.LoginFrame;
import com.nocapai.service.LibraryService;

import javax.swing.*;

/**
 * No Cap AI — Smart Library Management System
 * Entry point: creates the shared backend (LibraryService) and shows the login screen.
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to default look and feel if the system one is unavailable
        }

        LibraryService service = new LibraryService();

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(service);
            loginFrame.setVisible(true);
        });
    }
}
