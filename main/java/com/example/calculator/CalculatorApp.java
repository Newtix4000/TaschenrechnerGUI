package com.example.calculator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class CalculatorApp extends JFrame {

    private final JTextField display = new JTextField("0");

    private BigDecimal accumulator = BigDecimal.ZERO;
    private String pendingOp = null;
    private boolean startNewNumber = true;
    private final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    public CalculatorApp() {
        super("Taschenrechner – Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(340, 460));
        setLocationByPlatform(true);

        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setEditable(false);
        display.setFont(display.getFont().deriveFont(Font.PLAIN, 28f));
        display.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        display.setBackground(Color.WHITE);

        JPanel buttons = new JPanel(new GridLayout(5, 4, 8, 8));
        buttons.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] labels = {
                "C", "←", "÷", "×",
                "7", "8", "9", "−",
                "4", "5", "6", "+",
                "1", "2", "3", "=",
                "0", "0", ".", "=" // trick: we will merge "0" cells below
        };

        // Add buttons with actions
        for (String label : labels) {
            JButton b = new JButton(label);
            b.setFont(b.getFont().deriveFont(Font.PLAIN, 20f));
            b.setFocusable(false);
            b.addActionListener(this::onButton);
            buttons.add(b);
        }

        // Replace the last row with custom layout for 0  .  =
        buttons.remove(16); buttons.remove(16); buttons.remove(16); buttons.remove(16);
        JPanel lastRow = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0,0,0,0);
        c.gridy = 0; c.fill = GridBagConstraints.BOTH; c.weighty = 1.0;

        JButton zero = mkBtn("0");
        c.gridx = 0; c.weightx = 2.0; c.gridwidth = 2;
        lastRow.add(zero, c);

        JButton dot = mkBtn(".");
        c.gridx = 2; c.weightx = 1.0; c.gridwidth = 1;
        lastRow.add(dot, c);

        JButton eq = mkBtn("=");
        c.gridx = 3; c.weightx = 1.0;
        lastRow.add(eq, c);

        buttons.add(lastRow);

        // Keyboard shortcuts
        addKeyBindings();

        // Layout
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.add(display, BorderLayout.NORTH);
        root.add(buttons, BorderLayout.CENTER);
        setContentPane(root);
        pack();
    }

    private JButton mkBtn(String label) {
        JButton b = new JButton(label);
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 20f));
        b.setFocusable(false);
        b.addActionListener(this::onButton);
        return b;
    }

    private void addKeyBindings() {
        // Key listener on frame root pane to catch digits/operators
        JComponent root = getRootPane();
        root.setFocusable(true);
        root.requestFocusInWindow();
        root.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char ch = e.getKeyChar();
                if (Character.isDigit(ch)) {
                    appendDigit(String.valueOf(ch));
                } else if (ch == '.') {
                    appendDot();
                } else if (ch == '+' || ch == '-' || ch == '*' || ch == '/') {
                    String op = String.valueOf(ch);
                    if (op.equals("*")) op = "×";
                    if (op.equals("/")) op = "÷";
                    applyOperator(op);
                } else if (ch == '\n' || ch == '=') {
                    calculateEquals();
                } else if (ch == 27) { // ESC
                    clearAll();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    backspace();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    calculateEquals();
                }
            }
        });
    }

    private void onButton(ActionEvent e) {
        String label = ((JButton) e.getSource()).getText();
        switch (label) {
            case "C" -> clearAll();
            case "←" -> backspace();
            case "+" -> applyOperator("+");
            case "−" -> applyOperator("−");
            case "×" -> applyOperator("×");
            case "÷" -> applyOperator("÷");
            case "=" -> calculateEquals();
            case "." -> appendDot();
            default -> {
                if (label.chars().allMatch(Character::isDigit)) {
                    appendDigit(label);
                }
            }
        }
    }

    private void clearAll() {
        accumulator = BigDecimal.ZERO;
        pendingOp = null;
        startNewNumber = true;
        display.setText("0");
    }

    private void backspace() {
        if (startNewNumber) return;
        String txt = display.getText();
        if (txt.length() > 1) {
            display.setText(txt.substring(0, txt.length() - 1));
        } else {
            display.setText("0");
            startNewNumber = true;
        }
    }

    private void appendDigit(String d) {
        if (startNewNumber || display.getText().equals("0")) {
            display.setText(d);
        } else {
            display.setText(display.getText() + d);
        }
        startNewNumber = false;
    }

    private void appendDot() {
        if (startNewNumber) {
            display.setText("0.");
            startNewNumber = false;
            return;
        }
        if (!display.getText().contains(".")) {
            display.setText(display.getText() + ".");
        }
    }

    private BigDecimal currentValue() {
        try {
            return new BigDecimal(display.getText());
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private void applyOperator(String op) {
        if (pendingOp == null) {
            accumulator = currentValue();
        } else if (!startNewNumber) {
            // chain operation: compute previous pending, then set new op
            accumulator = compute(accumulator, currentValue(), pendingOp);
            display.setText(accumulator.stripTrailingZeros().toPlainString());
        }
        pendingOp = op;
        startNewNumber = true;
    }

    private void calculateEquals() {
        if (pendingOp == null) return;
        BigDecimal result = compute(accumulator, currentValue(), pendingOp);
        display.setText(result.stripTrailingZeros().toPlainString());
        accumulator = result;
        pendingOp = null;
        startNewNumber = true;
    }

    private BigDecimal compute(BigDecimal a, BigDecimal b, String op) {
        try {
            return switch (op) {
                case "+" -> a.add(b, MC);
                case "−" -> a.subtract(b, MC);
                case "×" -> a.multiply(b, MC);
                case "÷" -> {
                    if (b.compareTo(BigDecimal.ZERO) == 0) {
                        JOptionPane.showMessageDialog(this, "Division durch 0 ist nicht erlaubt.", "Fehler", JOptionPane.ERROR_MESSAGE);
                        yield a;
                    }
                    yield a.divide(b, 12, RoundingMode.HALF_UP);
                }
                default -> b;
            };
        } catch (ArithmeticException ex) {
            JOptionPane.showMessageDialog(this, "Rechenfehler: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            return a;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new CalculatorApp().setVisible(true);
        });
    }
}
