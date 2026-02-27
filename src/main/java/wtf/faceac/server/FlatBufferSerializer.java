


package wtf.faceac.server;
import com.google.flatbuffers.FlatBufferBuilder;
import wtf.faceac.data.TickData;
import wtf.faceac.flatbuffers.FBTickData;
import wtf.faceac.flatbuffers.FBTickDataSequence;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
public class FlatBufferSerializer {
    private static final ThreadLocal<FlatBufferBuilder> BUILDER =
        ThreadLocal.withInitial(() -> new FlatBufferBuilder(4096));
    public static byte[] serialize(List<TickData> ticks) {
        return serializeMatrix(ticks, ticks.size());
    }
    public static byte[] serializeMatrix(List<TickData> ticks, int rows) {
        int safeRows = Math.max(1, rows);
        ByteBuffer buffer = ByteBuffer.allocate(safeRows * 8 * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < safeRows; i++) {
            TickData tick = i < ticks.size() ? ticks.get(i) : null;
            if (tick == null) {
                for (int f = 0; f < 8; f++) {
                    buffer.putFloat(0.0f);
                }
                continue;
            }
            buffer.putFloat(tick.deltaYaw);
            buffer.putFloat(tick.deltaPitch);
            buffer.putFloat(tick.accelYaw);
            buffer.putFloat(tick.accelPitch);
            buffer.putFloat(tick.jerkYaw);
            buffer.putFloat(tick.jerkPitch);
            buffer.putFloat(tick.gcdErrorYaw);
            buffer.putFloat(tick.gcdErrorPitch);
        }
        return buffer.array();
    }
    public static byte[] serializeFlatBuffer(List<TickData> ticks) {
        FlatBufferBuilder builder = BUILDER.get();
        builder.clear();
        int[] tickOffsets = new int[ticks.size()];
        for (int i = ticks.size() - 1; i >= 0; i--) {
            TickData tick = ticks.get(i);
            FBTickData.startFBTickData(builder);
            FBTickData.addDeltaYaw(builder, tick.deltaYaw);
            FBTickData.addDeltaPitch(builder, tick.deltaPitch);
            FBTickData.addAccelYaw(builder, tick.accelYaw);
            FBTickData.addAccelPitch(builder, tick.accelPitch);
            FBTickData.addJerkPitch(builder, tick.jerkPitch);
            FBTickData.addJerkYaw(builder, tick.jerkYaw);
            FBTickData.addGcdErrorYaw(builder, tick.gcdErrorYaw);
            FBTickData.addGcdErrorPitch(builder, tick.gcdErrorPitch);
            tickOffsets[i] = FBTickData.endFBTickData(builder);
        }
        int ticksVector = FBTickDataSequence.createTicksVector(builder, tickOffsets);
        FBTickDataSequence.startFBTickDataSequence(builder);
        FBTickDataSequence.addTicks(builder, ticksVector);
        int sequenceOffset = FBTickDataSequence.endFBTickDataSequence(builder);
        builder.finish(sequenceOffset);
        ByteBuffer buf = builder.dataBuffer();
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return bytes;
    }

    /**
     * Serialize tick data into a JSON 2D array for the FACE Dashboard backend.
     * Returns UTF-8 bytes of: [[dy,dp,ay,ap,jy,jp,gy,gp], ...]
     * This is used as the "features" field in the /api/checks/submit request body.
     */
    public static byte[] serializeJsonFeatures(List<TickData> ticks, int rows) {
        int safeRows = Math.max(1, rows);
        StringBuilder sb = new StringBuilder(safeRows * 80);
        sb.append('[');
        for (int i = 0; i < safeRows; i++) {
            if (i > 0) sb.append(',');
            TickData tick = i < ticks.size() ? ticks.get(i) : null;
            sb.append('[');
            if (tick == null) {
                sb.append("0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0");
            } else {
                sb.append(tick.deltaYaw).append(',');
                sb.append(tick.deltaPitch).append(',');
                sb.append(tick.accelYaw).append(',');
                sb.append(tick.accelPitch).append(',');
                sb.append(tick.jerkYaw).append(',');
                sb.append(tick.jerkPitch).append(',');
                sb.append(tick.gcdErrorYaw).append(',');
                sb.append(tick.gcdErrorPitch);
            }
            sb.append(']');
        }
        sb.append(']');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
