package com.paperpiper.client;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setUndecorated(true);
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
            int screenW = frame.getWidth();
            int screenH = frame.getHeight();
            if (screenW <= 0 || screenH <= 0) {
                return;
            }
            BufferedImage scaled = new BufferedImage(screenW, screenH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, 0, 0, screenW, screenH, null);
            g.dispose();
            imageLabel.setIcon(new ImageIcon(scaled));
            if (!frame.isVisible()) {
                frame.setVisible(true);
            }
        });
    }

    private static BufferedImage toBufferedImage(FrameData payload) {
        int width = payload.width();
        int height = payload.height();
        byte[] data = payload.payload();
        String format = payload.pixelFormat();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        if ("GRAY8".equals(format)) {
            // Single-channel grayscale — expand to R=G=B=gray, A=255.
            // Compressed with ZLIB + grayscale to cut UDP payload size.
            // Grayscale uses ITU-R BT.601 luma on the server side.
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int gray = data[index++] & 0xFF;
                    int argb = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
                    image.setRGB(x, y, argb);
                }
            }
        } else {
            // RGBA8 (legacy / fallback)
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int r = data[index] & 0xFF;
                    int g = data[index + 1] & 0xFF;
                    int b = data[index + 2] & 0xFF;
                    int a = data[index + 3] & 0xFF;
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    image.setRGB(x, y, argb);
                    index += 4;
                }
            }
        }

        return image;
    }
}
