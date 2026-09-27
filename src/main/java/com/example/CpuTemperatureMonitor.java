package com.example;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

public class CpuTemperatureMonitor extends JFrame {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final Sensors sensors;

    private final JLabel temperatureLabel = new JLabel("--°C");
    private final JLabel statusLabel = new JLabel("Checking...");
    private final JLabel maximumLabel = new JLabel("Maximum: --°C");
    private final JLabel cpuNameLabel = new JLabel("CPU");

    private final TemperatureGauge temperatureGauge =
            new TemperatureGauge();

    private double maximumTemperature = Double.NaN;

    private final Color backgroundColor =
            new Color(15, 17, 23);

    public CpuTemperatureMonitor() {

        systemInfo = new SystemInfo();
        hardware = systemInfo.getHardware();
        sensors = hardware.getSensors();

        setupWindow();
        createInterface();

        updateTemperature();

        Timer timer = new Timer(
                2000,
                event -> updateTemperature()
        );

        timer.setCoalesce(true);
        timer.start();
    }

    // =========================================================
    // WINDOW
    // =========================================================

    private void setupWindow() {

        setTitle("CPU Monitor");

        setSize(720, 620);

        setMinimumSize(
                new Dimension(650, 560)
        );

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setLocationRelativeTo(null);

        try {

            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // INTERFACE
    // =========================================================

    private void createInterface() {

        JTabbedPane tabs = new JTabbedPane();

        tabs.setBackground(backgroundColor);
        tabs.setForeground(Color.WHITE);

        // -----------------------------------------------------
        // ABA 1 - TEMPERATURA
        // -----------------------------------------------------

        JPanel temperaturePanel =
                createTemperaturePanel();

        tabs.addTab(
                "🌡 Temperature",
                temperaturePanel
        );

        // -----------------------------------------------------
        // ABA 2 - CPU USAGE
        // -----------------------------------------------------

        CpuUsagePanel cpuUsagePanel =
                new CpuUsagePanel(
                        hardware.getProcessor()
                );

        tabs.addTab(
                "⚡ CPU Usage",
                cpuUsagePanel
        );

        add(tabs);
    }

    // =========================================================
    // TEMPERATURE INTERFACE
    // =========================================================

    private JPanel createTemperaturePanel() {

        JPanel mainPanel = new JPanel(
                new BorderLayout()
        );

        mainPanel.setBackground(
                backgroundColor
        );

        mainPanel.setBorder(
                new EmptyBorder(
                        25,
                        30,
                        25,
                        30
                )
        );

        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        JPanel header = new JPanel(
                new BorderLayout()
        );

        header.setOpaque(false);

        JLabel titleLabel = new JLabel(
                "CPU Temperature"
        );

        titleLabel.setForeground(Color.WHITE);

        titleLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        27
                )
        );

        cpuNameLabel.setForeground(
                new Color(150, 158, 175)
        );

        cpuNameLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );

        header.add(
                titleLabel,
                BorderLayout.NORTH
        );

        header.add(
                cpuNameLabel,
                BorderLayout.SOUTH
        );

        mainPanel.add(
                header,
                BorderLayout.NORTH
        );

        // -----------------------------------------------------
        // CENTER
        // -----------------------------------------------------

        JPanel centerPanel = new JPanel(
                new BorderLayout(
                        0,
                        20
                )
        );

        centerPanel.setOpaque(false);

        temperatureGauge.setPreferredSize(
                new Dimension(360, 300)
        );

        centerPanel.add(
                temperatureGauge,
                BorderLayout.CENTER
        );

        // -----------------------------------------------------
        // INFORMATION CARDS
        // -----------------------------------------------------

        JPanel informationPanel = new JPanel(
                new GridLayout(
                        1,
                        2,
                        15,
                        0
                )
        );

        informationPanel.setOpaque(false);

        JPanel statusCard =
                createCard(
                        "STATUS",
                        statusLabel
                );

        JPanel maximumCard =
                createCard(
                        "RECORD",
                        maximumLabel
                );

        informationPanel.add(statusCard);
        informationPanel.add(maximumCard);

        centerPanel.add(
                informationPanel,
                BorderLayout.SOUTH
        );

        mainPanel.add(
                centerPanel,
                BorderLayout.CENTER
        );

        // -----------------------------------------------------
        // FOOTER
        // -----------------------------------------------------

        JLabel footerLabel = new JLabel(
                "Automatic refresh every 2 seconds",
                SwingConstants.CENTER
        );

        footerLabel.setForeground(
                new Color(100, 108, 125)
        );

        footerLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        11
                )
        );

        mainPanel.add(
                footerLabel,
                BorderLayout.SOUTH
        );

        return mainPanel;
    }

    // =========================================================
    // CARD
    // =========================================================

    private JPanel createCard(
            String title,
            JLabel valueLabel
    ) {

        JPanel card = new JPanel(
                new BorderLayout(
                        5,
                        5
                )
        );

        card.setBackground(
                new Color(29, 33, 43)
        );

        card.setBorder(
                new EmptyBorder(
                        12,
                        15,
                        12,
                        15
                )
        );

        JLabel titleLabel = new JLabel(title);

        titleLabel.setForeground(
                new Color(120, 128, 145)
        );

        titleLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        10
                )
        );

        valueLabel.setForeground(Color.WHITE);

        valueLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        16
                )
        );

        card.add(
                titleLabel,
                BorderLayout.NORTH
        );

        card.add(
                valueLabel,
                BorderLayout.CENTER
        );

        return card;
    }

    // =========================================================
    // TEMPERATURE UPDATE
    // =========================================================

    private void updateTemperature() {

        SwingWorker<Double, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Double doInBackground() {

                        return sensors.getCpuTemperature();
                    }

                    @Override
                    protected void done() {

                        try {

                            double temperature = get();

                            if (Double.isNaN(temperature)
                                    || temperature <= 0) {

                                showSensorUnavailable();
                                return;
                            }

                            updateInterface(
                                    temperature
                            );

                        } catch (Exception exception) {

                            showSensorUnavailable();
                        }
                    }
                };

        worker.execute();
    }

    // =========================================================
    // TEMPERATURE UI UPDATE
    // =========================================================

    private void updateInterface(
            double temperature
    ) {

        if (Double.isNaN(maximumTemperature)
                || temperature > maximumTemperature) {

            maximumTemperature = temperature;
        }

        temperatureLabel.setText(
                String.format(
                        "%.1f°C",
                        temperature
                )
        );

        maximumLabel.setText(
                String.format(
                        "Maximum: %.1f°C",
                        maximumTemperature
                )
        );

        statusLabel.setText(
                getTemperatureStatus(
                        temperature
                )
        );

        String processorName =
                hardware
                        .getProcessor()
                        .getProcessorIdentifier()
                        .getName();

        cpuNameLabel.setText(
                processorName
        );

        temperatureGauge.setTemperature(
                temperature
        );

        statusLabel.setForeground(
                getTemperatureColor(
                        temperature
                )
        );
    }

    private void showSensorUnavailable() {

        temperatureLabel.setText(
                "--°C"
        );

        statusLabel.setText(
                "Sensor unavailable"
        );

        statusLabel.setForeground(
                new Color(255, 180, 70)
        );

        temperatureGauge.setTemperature(
                Double.NaN
        );
    }

    // =========================================================
    // TEMPERATURE STATUS
    // =========================================================

    private String getTemperatureStatus(
            double temperature
    ) {

        if (temperature < 50) {
            return "Normal";
        }

        if (temperature < 70) {
            return "Moderate";
        }

        if (temperature < 85) {
            return "High";
        }

        return "Critical";
    }

    // =========================================================
    // TEMPERATURE COLORS
    // =========================================================

    private Color getTemperatureColor(
            double temperature
    ) {

        if (temperature < 50) {

            return new Color(
                    50,
                    220,
                    140
            );
        }

        if (temperature < 70) {

            return new Color(
                    80,
                    170,
                    255
            );
        }

        if (temperature < 85) {

            return new Color(
                    255,
                    190,
                    60
            );
        }

        return new Color(
                255,
                75,
                75
        );
    }

    // =========================================================
    // TEMPERATURE GAUGE
    // =========================================================

    private class TemperatureGauge extends JPanel {

        private double temperature = Double.NaN;

        public TemperatureGauge() {

            setOpaque(false);
        }

        public void setTemperature(
                double temperature
        ) {

            this.temperature = temperature;

            repaint();
        }

        @Override
        protected void paintComponent(
                Graphics graphics
        ) {

            super.paintComponent(graphics);

            Graphics2D g =
                    (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int width = getWidth();
            int height = getHeight();

            int size =
                    Math.min(width, height) - 40;

            int x =
                    (width - size) / 2;

            int y =
                    (height - size) / 2 - 5;

            // -------------------------------------------------
            // BACKGROUND CIRCLE
            // -------------------------------------------------

            g.setColor(
                    new Color(
                            25,
                            29,
                            38
                    )
            );

            g.fill(
                    new Ellipse2D.Double(
                            x,
                            y,
                            size,
                            size
                    )
            );

            // -------------------------------------------------
            // BACKGROUND ARC
            // -------------------------------------------------

            int margin = 22;

            Arc2D backgroundArc =
                    new Arc2D.Double(
                            x + margin,
                            y + margin,
                            size - margin * 2,
                            size - margin * 2,
                            135,
                            -270,
                            Arc2D.OPEN
                    );

            g.setStroke(
                    new BasicStroke(
                            15,
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );

            g.setColor(
                    new Color(
                            55,
                            61,
                            74
                    )
            );

            g.draw(backgroundArc);

            // -------------------------------------------------
            // TEMPERATURE ARC
            // -------------------------------------------------

            if (!Double.isNaN(temperature)) {

                double percentage =
                        Math.max(
                                0,
                                Math.min(
                                        100,
                                        temperature
                                )
                        );

                double angle =
                        -270 *
                                (percentage / 100.0);

                Arc2D temperatureArc =
                        new Arc2D.Double(
                                x + margin,
                                y + margin,
                                size - margin * 2,
                                size - margin * 2,
                                135,
                                angle,
                                Arc2D.OPEN
                        );

                g.setColor(
                        getTemperatureColor(
                                temperature
                        )
                );

                g.draw(
                        temperatureArc
                );
            }

            // -------------------------------------------------
            // TEMPERATURE TEXT
            // -------------------------------------------------

            String temperatureText;

            if (Double.isNaN(temperature)) {

                temperatureText = "--°C";

            } else {

                temperatureText =
                        String.format(
                                "%.1f°C",
                                temperature
                        );
            }

            g.setFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            42
                    )
            );

            FontMetrics metrics =
                    g.getFontMetrics();

            int textX =
                    width / 2
                            - metrics.stringWidth(
                            temperatureText
                    ) / 2;

            int textY =
                    height / 2
                            + metrics.getAscent()
                            / 3;

            g.setColor(Color.WHITE);

            g.drawString(
                    temperatureText,
                    textX,
                    textY
            );

            // -------------------------------------------------
            // CPU LABEL
            // -------------------------------------------------

            String cpuText = "CPU";

            g.setFont(
                    new Font(
                            "SansSerif",
                            Font.PLAIN,
                            13
                    )
            );

            metrics = g.getFontMetrics();

            g.setColor(
                    new Color(
                            130,
                            138,
                            155
                    )
            );

            g.drawString(
                    cpuText,
                    width / 2
                            - metrics.stringWidth(
                            cpuText
                    ) / 2,
                    textY + 30
            );

            g.dispose();
        }
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(
            String[] args
    ) {

        SwingUtilities.invokeLater(() -> {

            CpuTemperatureMonitor application =
                    new CpuTemperatureMonitor();

            application.setVisible(true);
        });
    }
}
