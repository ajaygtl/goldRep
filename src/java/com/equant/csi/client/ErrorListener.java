package com.equant.csi.client;

import org.apache.log4j.Category;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ErrorListener {
    private static String labelPrefix = "Number of Error Messages: ";
    public static int numClicks = 0;
    JLabel label;
    private static final Category logger = Category.getInstance(ErrorListener.class.getName());

    public Component createComponents() {
        label = new JLabel(labelPrefix + "   " + getCount());
        JButton button = new JButton("SwingButton");
        button.setMnemonic(KeyEvent.VK_I);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                numClicks++;
                label.setText(labelPrefix + numClicks);
            }
        });
        label.setLabelFor(button);

        /*
         * An easy way to put space between a top-level container
         * and its contents is to put the contents in a JPanel
         * that has an "empty" border.
         */
        JPanel pane = new JPanel();
        pane.setBorder(BorderFactory.createEmptyBorder(
                30, //top
                30, //left
                10, //bottom
                30) //right
        );
        pane.setLayout(new GridLayout(0, 1));
        pane.add(button);
        pane.add(label);

        return pane;
    }

    public ErrorListener() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }

        //Create the top-level container and add contents to it.
        JFrame frame = new JFrame("ErrorMessageListener");
        Component contents = this.createComponents();
        frame.getContentPane().add(contents, BorderLayout.CENTER);

        //Finish setting up the frame, and show it.
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.pack();
        frame.setVisible(true);
    }

    public void setCount(int numClicks) {
        this.numClicks = numClicks;
        setLabelText();

    }

    public int getCount() {
        return numClicks;
    }

    public void setLabelText() {

        label.setText(labelPrefix + "   " + getCount() + 1);
    }


}