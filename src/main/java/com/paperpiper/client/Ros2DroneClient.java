package com.paperpiper.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paperpiper.ross.FrameData;
import com.paperpiper.ross.RossTopics;
import com.paperpiper.ross.TelemetryData;

import id.jros2client.JRos2Client;
import id.jros2client.JRos2ClientConfiguration;
import id.jros2client.JRos2ClientFactory;
import id.jros2messages.geometry_msgs.PoseStampedMessage;
import id.jros2messages.sensor_msgs.ImageMessage;
import id.jrosclient.TopicSubscriber;

/**
 * ROS2-based drone client that subscribes to telemetry and camera image topics
 * via DDS (jros2client) instead of the custom TCP/UDP ROSS protocol.
 *
 * <p>
 * Subscribes to:
 * <ul>
 * <li>{@code /drone/telemetry} — {@code geometry_msgs/PoseStamped}</li>
 * <li>{@code /drone/camera/image_raw} — {@code sensor_msgs/Image}</li>
 * </ul>
 */
public class Ros2DroneClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Ros2DroneClient.class);

    private final List<Consumer<TelemetryData>> telemetryListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<FrameData>> frameListeners = new CopyOnWriteArrayList<>();
    private final FrameStreamBuffer frameStreamBuffer = new FrameStreamBuffer();

    private JRos2Client ros2Client;
    private volatile boolean connected;
    private String droneId;

    public void connect(String droneId) throws Exception {
        this.droneId = droneId;
        var config = new JRos2ClientConfiguration.Builder().build();
        ros2Client = new JRos2ClientFactory().createClient(config);
        connected = true;

        ros2Client.subscribe(new TopicSubscriber<>(PoseStampedMessage.class, RossTopics.TELEMETRY) {
            @Override
            public void onNext(PoseStampedMessage item) {
                TelemetryData data = fromPoseStamped(droneId, item);
                telemetryListeners.forEach(l -> l.accept(data));
                getSubscription().ifPresent(s -> s.request(1));
            }
        });

        ros2Client.subscribe(new TopicSubscriber<>(ImageMessage.class, RossTopics.CAMERA_IMAGE) {
            @Override
            public void onNext(ImageMessage item) {
                FrameData data = fromImageMessage(droneId, item);
                frameStreamBuffer.acceptFrame(data);
                frameListeners.forEach(l -> l.accept(data));
                getSubscription().ifPresent(s -> s.request(1));
            }
        });

        logger.info("ROS2 drone client connected for drone {}", droneId);
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() {
        connected = false;
        if (ros2Client != null) {
            try {
                ros2Client.close();
            } catch (Exception ex) {
                logger.debug("Error closing ROS2 client", ex);
            }
            ros2Client = null;
        }
        logger.info("ROS2 drone client disconnected");
    }

    public void addTelemetryListener(Consumer<TelemetryData> listener) {
        telemetryListeners.add(listener);
    }

    public void removeTelemetryListener(Consumer<TelemetryData> listener) {
        telemetryListeners.remove(listener);
    }

    public void addFrameListener(Consumer<FrameData> listener) {
        frameListeners.add(listener);
    }

    public void removeFrameListener(Consumer<FrameData> listener) {
        frameListeners.remove(listener);
    }

    public FrameStreamBuffer getFrameStreamBuffer() {
        return frameStreamBuffer;
    }

    private static TelemetryData fromPoseStamped(String droneId, PoseStampedMessage msg) {
        var pos = msg.pose.position;
        return new TelemetryData(
                droneId,
                (float) pos.x, (float) pos.y, (float) pos.z,
                0f, 0f, 0f,
                System.currentTimeMillis());
    }

    private static FrameData fromImageMessage(String droneId, ImageMessage msg) {
        String encoding = msg.encoding != null ? msg.encoding.data : "RGBA8";
        return new FrameData(
                droneId,
                msg.width,
                msg.height,
                encoding,
                msg.data,
                System.currentTimeMillis());
    }
}
