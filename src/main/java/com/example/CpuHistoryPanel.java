package com.example;

import oshi.hardware.CentralProcessor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Mostra o histórico de utilização de cada CPU lógica
 * durante os últimos 30 segundos.
 */
public class CpuHistoryPanel extends JPanel {

    private static final int HISTORY_SECONDS = 30;
    private static final int UPDATE_INTERVAL = 1000;

    private final CentralProcessor processor;

    private long[][] previousTicks;

    private final List<double[]> history =
            new ArrayList<>();

    private final JLabel informationLabel =
            new JLabel("CPU");

    private final JLabel currentLabel =
            new JLabel("Current: --%");

    private final HistoryGraph graph =
            new HistoryGraph();

    public CpuHistoryPanel(
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
                new Color(
                        15,
                        17,
                        23
                )
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

        initialize();

        startUpdater();
    }

    // =========================================================
    // INTERFACE
    // =========================================================

    private void createInterface() {

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

        header.setOpaque(false);

        JLabel title =
                new JLabel(
                        "CPU History"
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

        informationLabel.setForeground(
                new Color(
                        150,
                        158,
                        175
                )
        );

        informationLabel.setFont(
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
                informationLabel,
                BorderLayout.SOUTH
        );

        add(
                header,
                BorderLayout.NORTH
        );

        // -----------------------------------------------------
        // GRAPH
        // -----------------------------------------------------

        add(
                graph,
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

        JPanel footer =
                new JPanel(
                        new BorderLayout(
                                20,
                                0
                        )
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

        JLabel historyLabel =
                new JLabel(
                        "History: 30 seconds"
                );

        historyLabel.setForeground(
                new Color(
                        150,
                        158,
                        175
                )
        );

        historyLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        currentLabel.setForeground(
                Color.WHITE
        );

        currentLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        14
                )
        );

        footer.add(
                historyLabel,
                BorderLayout.WEST
        );

        footer.add(
                currentLabel,
                BorderLayout.EAST
        );

        return footer;
    }

    // =========================================================
    // INITIALIZATION
    // =========================================================

    private void initialize() {

        int physicalCores =
                processor.getPhysicalProcessorCount();

        int logicalProcessors =
                processor.getLogicalProcessorCount();

        informationLabel.setText(
                physicalCores
                        + " physical cores  •  "
                        + logicalProcessors
                        + " logical processors"
        );

        /*
         * Primeira leitura.
         *
         * Precisamos de duas leituras do CPU para
         * calcular a utilização.
         */
        previousTicks =
                processor.getProcessorCpuLoadTicks();
    }

    // =========================================================
    // TIMER
    // =========================================================

    private void startUpdater() {

        Timer timer =
                new Timer(
                        UPDATE_INTERVAL,
                        event -> updateCpu()
                );

        timer.setCoalesce(true);

        timer.start();
    }

    // =========================================================
    // CPU UPDATE
    // =========================================================

    private void updateCpu() {

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

                            addHistory(
                                    usage
                            );

                        } catch (Exception ignored) {
                        }
                    }
                };

        worker.execute();
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void addHistory(
            double[] usage
    ) {

        if (usage == null) {
            return;
        }

        double[] percentages =
                new double[
                        usage.length
                ];

        double total = 0;

        for (
                int i = 0;
                i < usage.length;
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

            percentages[i] =
                    percentage;

            total += percentage;
        }

        history.add(
                percentages
        );

        /*
         * Mantém somente os últimos
         * 30 segundos.
         */
        while (
                history.size()
                        > HISTORY_SECONDS
        ) {

            history.remove(0);
        }

        double average =
                total / usage.length;

        currentLabel.setText(
                String.format(
                        "Current: %.1f%%",
                        average
                )
        );

        graph.setHistory(
                history
        );
    }

    // =========================================================
    // GRAPH
    // =========================================================

    private static class HistoryGraph
            extends JPanel {

        private List<double[]> history =
                new ArrayList<>();

        /*
         * Cores das threads.
         *
         * Até 8 threads ficam visualmente
         * distintas.
         */
        private final Color[] threadColors = {

                new Color(
                        80,
                        170,
                        255
                ),

                new Color(
                        50,
                        220,
                        140
                ),

                new Color(
                        255,
                        190,
                        60
                ),

                new Color(
                        255,
                        75,
                        75
                ),

                new Color(
                        180,
                        110,
                        255
                ),

                new Color(
                        50,
                        210,
                        220
                ),

                new Color(
                        255,
                        110,
                        180
                ),

                new Color(
                        160,
                        210,
                        80
                )
        };

        public HistoryGraph() {

            setOpaque(false);

            setPreferredSize(
                    new Dimension(
                            600,
                            400
                    )
            );
        }

        public void setHistory(
                List<double[]> history
        ) {

            /*
             * Criamos uma cópia para evitar
             * alterações enquanto o Swing
             * está a desenhar.
             */
            this.history =
                    new ArrayList<>(
                            history
                    );

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

            // -------------------------------------------------
            // BACKGROUND
            // -------------------------------------------------

            g.setColor(
                    new Color(
                            22,
                            26,
                            34
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
            // GRAPH MARGINS
            // -------------------------------------------------

            int left = 55;
            int right = 20;
            int top = 25;
            int bottom = 40;

            int graphWidth =
                    width
                            - left
                            - right;

            int graphHeight =
                    height
                            - top
                            - bottom;

            // -------------------------------------------------
            // GRID
            // -------------------------------------------------

            drawGrid(
                    g,
                    left,
                    top,
                    graphWidth,
                    graphHeight
            );

            if (history.isEmpty()) {

                drawNoData(
                        g,
                        width,
                        height
                );

                g.dispose();

                return;
            }

            // -------------------------------------------------
            // THREAD COUNT
            // -------------------------------------------------

            int threadCount =
                    history
                            .get(0)
                            .length;

            // -------------------------------------------------
            // DRAW THREAD LINES
            // -------------------------------------------------

            for (
                    int thread = 0;
                    thread < threadCount;
                    thread++
            ) {

                drawThread(
                        g,
                        thread,
                        left,
                        top,
                        graphWidth,
                        graphHeight
                );
            }

            // -------------------------------------------------
            // X AXIS
            // -------------------------------------------------

            drawXAxis(
                    g,
                    left,
                    top,
                    graphWidth,
                    graphHeight
            );

            // -------------------------------------------------
            // LEGEND
            // -------------------------------------------------

            drawLegend(
                    g,
                    threadCount,
                    left,
                    top
            );

            g.dispose();
        }

        // =====================================================
        // GRID
        // =====================================================

        private void drawGrid(
                Graphics2D g,
                int left,
                int top,
                int width,
                int height
        ) {

            g.setStroke(
                    new BasicStroke(
                            1
                    )
            );

            g.setFont(
                    new Font(
                            "SansSerif",
                            Font.PLAIN,
                            10
                    )
            );

            for (
                    int value = 0;
                    value <= 100;
                    value += 25
            ) {

                int y =
                        top
                                + height
                                - (int)
                                (
                                        height
                                                * value
                                                / 100.0
                                );

                g.setColor(
                        new Color(
                                55,
                                61,
                                74
                        )
                );

                g.drawLine(
                        left,
                        y,
                        left + width,
                        y
                );

                g.setColor(
                        new Color(
                                120,
                                128,
                                145
                        )
                );

                String label =
                        value + "%";

                g.drawString(
                        label,
                        15,
                        y + 4
                );
            }
        }

        // =====================================================
        // THREAD LINE
        // =====================================================

        private void drawThread(
                Graphics2D g,
                int thread,
                int left,
                int top,
                int width,
                int height
        ) {

            Path2D path =
                    new Path2D.Double();

            boolean started = false;

            int count =
                    history.size();

            for (
                    int i = 0;
                    i < count;
                    i++
            ) {

                double[] values =
                        history.get(i);

                if (
                        thread
                                >= values.length
                ) {

                    continue;
                }

                double usage =
                        values[thread];

                double x;

                if (count <= 1) {

                    x = left;

                } else {

                    x =
                            left
                                    + (
                                    i
                                            / (double)
                                            (HISTORY_SECONDS - 1)
                            )
                                    * width;
                }

                double y =
                        top
                                + height
                                - (
                                usage
                                        / 100.0
                                        * height
                        );

                if (!started) {

                    path.moveTo(
                            x,
                            y
                    );

                    started = true;

                } else {

                    path.lineTo(
                            x,
                            y
                    );
                }
            }

            Color color =
                    threadColors[
                            thread
                                    % threadColors.length
                    ];

            g.setColor(color);

            g.setStroke(
                    new BasicStroke(
                            2.5f,
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );

            g.draw(path);
        }

        // =====================================================
        // X AXIS
        // =====================================================

        private void drawXAxis(
                Graphics2D g,
                int left,
                int top,
                int width,
                int height
        ) {

            g.setFont(
                    new Font(
                            "SansSerif",
                            Font.PLAIN,
                            10
                    )
            );

            g.setColor(
                    new Color(
                            120,
                            128,
                            145
                    )
            );

            int bottom =
                    top + height;

            g.drawString(
                    "30s",
                    left,
                    bottom + 25
            );

            String now =
                    "Now";

            int nowWidth =
                    g.getFontMetrics()
                            .stringWidth(
                                    now
                            );

            g.drawString(
                    now,
                    left
                            + width
                            - nowWidth,
                    bottom + 25
            );
        }

        // =====================================================
        // LEGEND
        // =====================================================

        private void drawLegend(
                Graphics2D g,
                int threadCount,
                int left,
                int top
        ) {

            int x =
                    left;

            int y =
                    top + 10;

            /*
             * A legenda fica no topo do gráfico.
             */
            for (
                    int thread = 0;
                    thread < threadCount;
                    thread++
            ) {

                String text =
                        "Thread "
                                + (thread + 1);

                Color color =
                        threadColors[
                                thread
                                        % threadColors.length
                        ];

                g.setColor(color);

                g.fillOval(
                        x,
                        y - 8,
                        8,
                        8
                );

                g.setColor(
                        new Color(
                                180,
                                185,
                                195
                        )
                );

                g.setFont(
                        new Font(
                                "SansSerif",
                                Font.PLAIN,
                                10
                        )
                );

                g.drawString(
                        text,
                        x + 13,
                        y
                );

                x += 75;
            }
        }

        // =====================================================
        // NO DATA
        // =====================================================

        private void drawNoData(
                Graphics2D g,
                int width,
                int height
        ) {

            String text =
                    "Collecting CPU data...";

            g.setFont(
                    new Font(
                            "SansSerif",
                            Font.PLAIN,
                            14
                    )
            );

            g.setColor(
                    new Color(
                            120,
                            128,
                            145
                    )
            );

            FontMetrics metrics =
                    g.getFontMetrics();

            g.drawString(
                    text,
                    width / 2
                            - metrics.stringWidth(
                            text
                    ) / 2,
                    height / 2
            );
        }
    }
}
