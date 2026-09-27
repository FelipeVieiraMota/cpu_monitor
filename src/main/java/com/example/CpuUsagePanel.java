package com.example;

import oshi.hardware.CentralProcessor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class CpuUsagePanel extends JPanel {

    private final CentralProcessor processor;

    private JPanel coresPanel;


    private final JLabel totalUsageLabel =
            new JLabel("--%");

    private final JLabel coreInfoLabel =
            new JLabel("CPU");

    private long[][] previousTicks;

    private CoreUsageGauge[] gauges;

    public CpuUsagePanel(
            CentralProcessor processor
    ) {

        this.processor = processor;

        setLayout(
                new BorderLayout(
                        0,
                        15
                )
        );

        setBackground(
                new Color(15, 17, 23)
        );

        setBorder(
                new EmptyBorder(
                        25,
                        30,
                        25,
                        30
                )
        );

        createInterface();

        initializeCpu();

        startUpdater();
    }

    // =========================================================
    // INTERFACE
    // =========================================================

    private void createInterface() {

        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        JPanel header = new JPanel(
                new BorderLayout()
        );

        header.setOpaque(false);

        JLabel title = new JLabel(
                "CPU Usage"
        );

        title.setForeground(
                Color.WHITE
        );

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        27
                )
        );

        coreInfoLabel.setForeground(
                new Color(
                        150,
                        158,
                        175
                )
        );

        coreInfoLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );

        header.add(
                title,
                BorderLayout.NORTH
        );

        header.add(
                coreInfoLabel,
                BorderLayout.SOUTH
        );

        add(
                header,
                BorderLayout.NORTH
        );

        // -----------------------------------------------------
        // CORE GRID
        // -----------------------------------------------------

        coresPanel = new JPanel(
                new GridLayout(
                        0,
                        2,
                        20,
                        20
                )
        );

        coresPanel.setOpaque(false);

        JScrollPane scrollPane =
                new JScrollPane(
                        coresPanel
                );

        scrollPane.setBorder(null);

        scrollPane.setOpaque(false);

        scrollPane.getViewport()
                .setOpaque(false);

        scrollPane.getVerticalScrollBar()
                .setUnitIncrement(16);

        add(
                scrollPane,
                BorderLayout.CENTER
        );

        // -----------------------------------------------------
        // FOOTER
        // -----------------------------------------------------

        JPanel footer =
                createFooter();

        add(
                footer,
                BorderLayout.SOUTH
        );
    }

    // =========================================================
    // FOOTER
    // =========================================================

    private JPanel createFooter() {

        JPanel footer = new JPanel(
                new BorderLayout()
        );

        footer.setBackground(
                new Color(
                        29,
                        33,
                        43
                )
        );

        footer.setBorder(
                new EmptyBorder(
                        12,
                        15,
                        12,
                        15
                )
        );

        JLabel totalTitle =
                new JLabel(
                        "TOTAL CPU"
                );

        totalTitle.setForeground(
                new Color(
                        120,
                        128,
                        145
                )
        );

        totalTitle.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        10
                )
        );

        totalUsageLabel.setForeground(
                Color.WHITE
        );

        totalUsageLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        18
                )
        );

        footer.add(
                totalTitle,
                BorderLayout.WEST
        );

        footer.add(
                totalUsageLabel,
                BorderLayout.EAST
        );

        return footer;
    }

    // =========================================================
    // INITIALIZE CPU
    // =========================================================

    private void initializeCpu() {

        int physicalCores =
                processor.getPhysicalProcessorCount();

        int logicalProcessors =
                processor.getLogicalProcessorCount();

        coreInfoLabel.setText(
                physicalCores
                        + " physical cores  •  "
                        + logicalProcessors
                        + " logical processors"
        );

        List<CentralProcessor.LogicalProcessor>
                logicalProcessorsList =
                processor.getLogicalProcessors();

        gauges =
                new CoreUsageGauge[
                        logicalProcessors
                ];

        for (
                int i = 0;
                i < logicalProcessors;
                i++
        ) {

            CentralProcessor.LogicalProcessor logical =
                    logicalProcessorsList.get(i);

            int physicalCore =
                    logical.getPhysicalProcessorNumber();

            int physicalPackage =
                    logical.getPhysicalPackageNumber();

            CoreUsageGauge gauge =
                    new CoreUsageGauge(
                            i,
                            physicalCore,
                            physicalPackage
                    );

            gauges[i] = gauge;

            coresPanel.add(gauge);
        }

        coresPanel.revalidate();
        coresPanel.repaint();

        /*
         * Primeira leitura.
         *
         * Esta leitura serve apenas como referência.
         * A próxima leitura permitirá calcular a percentagem.
         */
        previousTicks =
                processor.getProcessorCpuLoadTicks();
    }

    // =========================================================
    // CPU UPDATE
    // =========================================================

    private void startUpdater() {

        Timer timer =
                new Timer(
                        1000,
                        event -> updateUsage()
                );

        timer.setCoalesce(true);

        timer.setInitialDelay(1000);

        timer.start();
    }

    private void updateUsage() {

        SwingWorker<double[], Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected double[] doInBackground() {

                        long[][] newTicks =
                                processor
                                        .getProcessorCpuLoadTicks();

                        double[] usage =
                                processor
                                        .getProcessorCpuLoadBetweenTicks(
                                                previousTicks,
                                                newTicks
                                        );

                        previousTicks =
                                newTicks;

                        return usage;
                    }

                    @Override
                    protected void done() {

                        try {

                            double[] usage =
                                    get();

                            updateGauges(
                                    usage
                            );

                        } catch (Exception ignored) {
                        }
                    }
                };

        worker.execute();
    }

    // =========================================================
    // UPDATE GAUGES
    // =========================================================

    private void updateGauges(
            double[] usage
    ) {

        if (usage == null) {
            return;
        }

        double total = 0;

        int count =
                Math.min(
                        usage.length,
                        gauges.length
                );

        for (
                int i = 0;
                i < count;
                i++
        ) {

            double percentage =
                    usage[i] * 100.0;

            percentage =
                    Math.max(
                            0,
                            Math.min(
                                    100,
                                    percentage
                            )
                    );

            gauges[i].setUsage(
                    percentage
            );

            total += percentage;
        }

        if (count > 0) {

            double average =
                    total / count;

            totalUsageLabel.setText(
                    String.format(
                            "%.1f%%",
                            average
                    )
            );
        }
    }

    // =========================================================
    // CORE GAUGE
    // =========================================================

    private static class CoreUsageGauge
            extends JPanel {

        private final int logicalProcessor;

        private final int physicalCore;

        private final int physicalPackage;

        private double usage = 0;

        public CoreUsageGauge(
                int logicalProcessor,
                int physicalCore,
                int physicalPackage
        ) {

            this.logicalProcessor =
                    logicalProcessor;

            this.physicalCore =
                    physicalCore;

            this.physicalPackage =
                    physicalPackage;

            setOpaque(false);

            setPreferredSize(
                    new Dimension(
                            230,
                            230
                    )
            );
        }

        public void setUsage(
                double usage
        ) {

            this.usage = usage;

            repaint();
        }

        @Override
        protected void paintComponent(
                Graphics graphics
        ) {

            super.paintComponent(
                    graphics
            );

            Graphics2D g =
                    (Graphics2D)
                            graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int width =
                    getWidth();

            int height =
                    getHeight();

            int size =
                    Math.min(
                            width,
                            height
                    ) - 30;

            int x =
                    (width - size) / 2;

            int y =
                    (height - size) / 2;

            // -------------------------------------------------
            // CARD BACKGROUND
            // -------------------------------------------------

            g.setColor(
                    new Color(
                            29,
                            33,
                            43
                    )
            );

            g.fillRoundRect(
                    0,
                    0,
                    width,
                    height,
                    20,
                    20
            );

            // -------------------------------------------------
            // CIRCLE
            // -------------------------------------------------

            int margin = 25;

            int circleSize =
                    size - margin * 2;

            int circleX =
                    x + margin;

            int circleY =
                    y + margin;

            g.setColor(
                    new Color(
                            20,
                            24,
                            32
                    )
            );

            g.fill(
                    new Ellipse2D.Double(
                            circleX,
                            circleY,
                            circleSize,
                            circleSize
                    )
            );

            // -------------------------------------------------
            // BACKGROUND ARC
            // -------------------------------------------------

            int arcMargin = 10;

            Arc2D backgroundArc =
                    new Arc2D.Double(
                            circleX + arcMargin,
                            circleY + arcMargin,
                            circleSize - arcMargin * 2,
                            circleSize - arcMargin * 2,
                            90,
                            -360,
                            Arc2D.OPEN
                    );

            g.setStroke(
                    new BasicStroke(
                            12,
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

            g.draw(
                    backgroundArc
            );

            // -------------------------------------------------
            // USAGE ARC
            // -------------------------------------------------

            double angle =
                    -360 *
                            (usage / 100.0);

            Arc2D usageArc =
                    new Arc2D.Double(
                            circleX + arcMargin,
                            circleY + arcMargin,
                            circleSize - arcMargin * 2,
                            circleSize - arcMargin * 2,
                            90,
                            angle,
                            Arc2D.OPEN
                    );

            g.setColor(
                    getUsageColor(
                            usage
                    )
            );

            g.draw(
                    usageArc
            );

            // -------------------------------------------------
            // PERCENTAGE
            // -------------------------------------------------

            String usageText =
                    String.format(
                            "%.0f%%",
                            usage
                    );

            g.setFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            32
                    )
            );

            FontMetrics metrics =
                    g.getFontMetrics();

            int textX =
                    width / 2
                            - metrics.stringWidth(
                            usageText
                    ) / 2;

            int textY =
                    height / 2
                            + metrics.getAscent()
                            / 3;

            g.setColor(
                    Color.WHITE
            );

            g.drawString(
                    usageText,
                    textX,
                    textY
            );

            // -------------------------------------------------
            // LOGICAL CPU
            // -------------------------------------------------

            String logicalText =
                    "Thread "
                            + (logicalProcessor + 1);

            g.setFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            12
                    )
            );

            metrics =
                    g.getFontMetrics();

            g.setColor(
                    new Color(
                            150,
                            158,
                            175
                    )
            );

            g.drawString(
                    logicalText,
                    width / 2
                            - metrics.stringWidth(
                            logicalText
                    ) / 2,
                    textY + 27
            );

            // -------------------------------------------------
            // PHYSICAL CORE
            // -------------------------------------------------

            String physicalText =
                    "Core "
                            + (physicalCore + 1);

            g.setFont(
                    new Font(
                            "SansSerif",
                            Font.PLAIN,
                            11
                    )
            );

            metrics =
                    g.getFontMetrics();

            g.setColor(
                    new Color(
                            100,
                            108,
                            125
                    )
            );

            g.drawString(
                    physicalText,
                    width / 2
                            - metrics.stringWidth(
                            physicalText
                    ) / 2,
                    textY + 45
            );

            g.dispose();
        }

        // =====================================================
        // USAGE COLOR
        // =====================================================

        private Color getUsageColor(
                double usage
        ) {

            if (usage < 50) {

                return new Color(
                        50,
                        220,
                        140
                );
            }

            if (usage < 75) {

                return new Color(
                        80,
                        170,
                        255
                );
            }

            if (usage < 90) {

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
    }
}
