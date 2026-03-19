package com.paperpiper.client;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.paperpiper.ross.FrameData;

/**
 * Client-side window that displays the camera frame stream received from the
 * ROSS server. Essentially a video feed of the drone's camera.
 */
public class ClientDisplayWindow {

    private final JFrame frame;
    private final JLabel imageLabel;
    private final AtomicBoolean open = new AtomicBoolean(true);

    public ClientDisplayWindow(String title) {
        frame = new JFrame(title);
        imageLabel = new JLabel();

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(imageLabel);
        frame.setSize(800, 480);
        frame.setLocationByPlatform(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                open.set(false);
            }

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                open.set(false);
            }
        });
    }

    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public boolean isOpen() {
        return open.get();
    }

    public void close() {
        open.set(false);
        SwingUtilities.invokeLater(frame::dispose);
    }

    public void render(FrameData framePayload) {
        if (framePayload == null) {
            return;
        }

        BufferedImage image = toBufferedImage(framePayload);

        SwingUtilities.invokeLater(() -> {
            imageLabel.setIcon(new ImageIcon(image));
            frame.pack();
            if (!frame.isVisible()) {
                frame.setVisible(true);
            }
        });
    }

    private static BufferedImage toBufferedImage(FrameData payload) {
        int width = payload.width();
        int height = payload.height();
        byte[] rgba = payload.payload();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = rgba[index] & 0xFF;
                int g = rgba[index + 1] & 0xFF;
                int b = rgba[index + 2] & 0xFF;
                int a = rgba[index + 3] & 0xFF;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, argb);
                index += 4;
            }
        }

        return image;
    }
}
