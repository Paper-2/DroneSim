package com.paperpiper.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paperpiper.hardware.DroneHardwareApi;
import com.paperpiper.hardware.DroneTelemetrySample;
import com.paperpiper.hardware.HardwareVector3;
import com.paperpiper.hardware.SimulationHardwareApi;
import com.paperpiper.ross.FrameData;
import com.paperpiper.ross.RossTopics;

import id.jros2client.JRos2Client;
import id.jros2client.JRos2ClientConfiguration;
import id.jros2client.JRos2ClientFactory;
import id.jros2messages.geometry_msgs.PoseStampedMessage;
import id.jros2messages.sensor_msgs.ImageEncoding;
import id.jros2messages.sensor_msgs.ImageMessage;
import id.jros2messages.std_msgs.HeaderMessage;
import id.jrosclient.TopicSubmissionPublisher;
import id.jrosmessages.geometry_msgs.PointMessage;
import id.jrosmessages.geometry_msgs.PoseMessage;
import id.jrosmessages.geometry_msgs.QuaternionMessage;
import id.jrosmessages.primitives.Time;

/**
 * Publishes drone telemetry and camera frames to ROS2 topics via DDS
 * (jros2client).
 *
 * <p>
 * Topics published:
 * <ul>
 * <li>{@code /drone/telemetry} {@code geometry_msgs/PoseStamped}</li>
 * <li>{@code /drone/camera/image_raw} {@code sensor_msgs/Image}</li>
 * </ul>
 */
public class Ros2Bridge implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Ros2Bridge.class);

    private final SimulationHardwareApi hardwareApi;
    private final ServerCamera camera;
    private final int telemetryRateHz;
    private final int frameRateHz;

    private JRos2Client ros2Client;
    private TopicSubmissionPublisher<PoseStampedMessage> telemetryPublisher;
    private TopicSubmissionPublisher<ImageMessage> imagePublisher;
    private ScheduledExecutorService scheduler;
    private volatile boolean running;
    private long frameTick;

    public Ros2Bridge(SimulationHardwareApi hardwareApi, ServerCamera camera,
            int telemetryRateHz, int frameRateHz) {
        this.hardwareApi = hardwareApi;
        this.camera = camera;
        this.telemetryRateHz = telemetryRateHz;
        this.frameRateHz = frameRateHz;
    }

    public synchronized void start() throws Exception {
        if (running) {
            return;
        }

        var config = new JRos2ClientConfiguration.Builder().build();
        ros2Client = new JRos2ClientFactory().createClient(config);

        telemetryPublisher = new TopicSubmissionPublisher<>(PoseStampedMessage.class, RossTopics.TELEMETRY);
        imagePublisher = new TopicSubmissionPublisher<>(ImageMessage.class, RossTopics.CAMERA_IMAGE);

        ros2Client.publish(telemetryPublisher);
        ros2Client.publish(imagePublisher);

        running = true;

        scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::publishTelemetrySafe, 0,
                Math.max(1, 1000 / telemetryRateHz), TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::publishFrameSafe, 0,
                Math.max(1, 1000 / frameRateHz), TimeUnit.MILLISECONDS);

        logger.info("ROS2 bridge started  publishing telemetry@{}Hz, images@{}Hz",
                telemetryRateHz, frameRateHz);
    }

    @Override
    public synchronized void close() {
        running = false;

        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        if (ros2Client != null) {
            try {
                ros2Client.close();
            } catch (Exception ex) {
                logger.debug("Error closing ROS2 client", ex);
            }
            ros2Client = null;
        }

        logger.info("ROS2 bridge stopped");
    }

    public boolean isRunning() {
        return running;
    }

    private void publishTelemetrySafe() {
        try {
            publishTelemetry();
        } catch (Exception ex) {
            logger.warn("ROS2 telemetry publish failed", ex);
        }
    }

    private void publishFrameSafe() {
        try {
            publishFrame();
        } catch (Exception ex) {
            logger.warn("ROS2 image publish failed", ex);
        }
    }

    private void publishTelemetry() {
        var droneIds = hardwareApi.listDroneIds();
        if (droneIds.isEmpty()) {
            return;
        }

        String droneId = droneIds.get(0);
        hardwareApi.getDrone(droneId).ifPresent(droneApi -> {
            DroneTelemetrySample sample = droneApi.readTelemetry();
            PoseStampedMessage msg = toPoseStamped(sample);
            telemetryPublisher.submit(msg);
        });
    }

    private void publishFrame() {
        var droneIds = hardwareApi.listDroneIds();
        if (droneIds.isEmpty()) {
            return;
        }

        String droneId = droneIds.get(0);
        var telemetry = hardwareApi.getDrone(droneId)
                .map(DroneHardwareApi::readTelemetry)
                .orElse(null);
        FrameData frame = camera.capture(droneId, telemetry, frameTick++);
        ImageMessage msg = toImageMessage(frame);
        imagePublisher.submit(msg);
    }

    private static PoseStampedMessage toPoseStamped(DroneTelemetrySample sample) {
        HardwareVector3 pos = sample.position();
        var orientation = sample.orientation();

        var header = new HeaderMessage()
                .withStamp(Time.now())
                .withFrameId("map");

        var point = new PointMessage(pos.x(), pos.y(), pos.z());
        var quat = new QuaternionMessage(
                orientation.x(), orientation.y(), orientation.z(), orientation.w());
        var pose = new PoseMessage().withPosition(point).withQuaternion(quat);

        return new PoseStampedMessage().withHeader(header).withPose(pose);
    }

    private static ImageMessage toImageMessage(FrameData frame) {
        var header = new HeaderMessage()
                .withStamp(Time.now())
                .withFrameId("camera");

        return new ImageMessage()
                .withHeader(header)
                .withHeight(frame.height())
                .withWidth(frame.width())
                .withEncoding(ImageEncoding.RGBA8)
                .withIsBigendian((byte) 0)
                .withStep(frame.width() * 4)
                .withData(frame.payload());
    }
}
