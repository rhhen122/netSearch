
package org.netSearch.ui;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class StartUI {

    private static final int THREAD_COUNT = 10;

    private static JTextArea consoleOutput;
    private static JLabel[] workerLabels = new JLabel[THREAD_COUNT];

    public void startui(String mode, String handlingDir, String startSite) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("netSearch - Crawler Monitor");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 750);
            frame.setLocationRelativeTo(null);

            // Parameter banner
            String paramText = String.format(
                    " Mode: %s  |  Directory: %s  |  Target: %s",
                    mode, handlingDir, startSite
            );

            JTextField paramBox = new JTextField(paramText);
            paramBox.setEditable(false);
            paramBox.setBorder(
                    BorderFactory.createTitledBorder("Active Parameters")
            );
            paramBox.setFont(new Font("SansSerif", Font.BOLD, 12));

            // Worker monitor
            JPanel workerPanel = new JPanel(new GridLayout(THREAD_COUNT, 1, 4, 4));
            workerPanel.setBorder(
                    BorderFactory.createTitledBorder("Thread Activity")
            );

            for (int i = 0; i < THREAD_COUNT; i++) {

                workerLabels[i] = new JLabel(
                        "Worker " + (i + 1) + " | Waiting to start..."
                );

                workerLabels[i].setFont(
                        new Font("Monospaced", Font.PLAIN, 12)
                );

                workerLabels[i].setBorder(
                        BorderFactory.createEmptyBorder(5, 8, 5, 8)
                );

                workerLabels[i].setOpaque(true);
                workerLabels[i].setBackground(new Color(235, 235, 235));

                workerPanel.add(workerLabels[i]);
            }

            // Console
            consoleOutput = new JTextArea();
            consoleOutput.setEditable(false);
            consoleOutput.setFont(
                    new Font("Monospaced", Font.PLAIN, 12)
            );

            JScrollPane consoleScroll = new JScrollPane(consoleOutput);
            consoleScroll.setBorder(
                    BorderFactory.createTitledBorder("Crawler Console")
            );

            // Split worker monitor and console
            JSplitPane splitPane = new JSplitPane(
                    JSplitPane.VERTICAL_SPLIT,
                    workerPanel,
                    consoleScroll
            );

            splitPane.setResizeWeight(0.45);
            splitPane.setDividerLocation(330);

            frame.add(paramBox, BorderLayout.NORTH);
            frame.add(splitPane, BorderLayout.CENTER);

            // Redirect System.out and System.err
            redirectSystemStreams();

            frame.setVisible(true);
        });
    }

    // Update an individual worker's status
    public static void updateWorkerStatus(int workerId, String status) {

        if (workerId < 1 || workerId > THREAD_COUNT) {
            return;
        }

        SwingUtilities.invokeLater(() -> {

            JLabel label = workerLabels[workerId - 1];

            if (label == null) {
                return;
            }

            label.setText("Worker " + workerId + " | " + status);
            label.setToolTipText(status);

            if (status.startsWith("Downloading")) {
                label.setBackground(new Color(255, 235, 180));
            } else if (status.startsWith("Parsing")) {
                label.setBackground(new Color(190, 220, 255));
            } else if (status.startsWith("Saving")) {
                label.setBackground(new Color(210, 235, 210));
            } else if (status.startsWith("Stopped")) {
                label.setBackground(new Color(220, 220, 220));
            } else if (status.startsWith("ERROR")) {
                label.setBackground(new Color(255, 190, 190));
            } else {
                label.setBackground(new Color(235, 235, 235));
            }
        });
    }

    // Redirect console output into the Swing text area
    private static void redirectSystemStreams() {

        OutputStream out = new OutputStream() {

            @Override
            public void write(int b) {
                write(new byte[]{(byte) b}, 0, 1);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) {

                String text = new String(
                        bytes, offset, length, StandardCharsets.UTF_8
                );

                SwingUtilities.invokeLater(() -> {

                    if (consoleOutput == null) {
                        return;
                    }

                    consoleOutput.append(text);
                    consoleOutput.setCaretPosition(
                            consoleOutput.getDocument().getLength()
                    );
                });
            }
        };

        PrintStream printStream = new PrintStream(out, true);

        System.setOut(printStream);
        System.setErr(printStream);
    }
}
