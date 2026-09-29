package com.example;

import javax.swing.*;

public class Runner {
    // =========================================================
    // MAIN
    // =========================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            final var application = new CpuTemperatureMonitor();
            application.setVisible(true);
        });
    }
}
