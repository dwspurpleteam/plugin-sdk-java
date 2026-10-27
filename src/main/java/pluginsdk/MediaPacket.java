package pluginsdk;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Objects;

@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class MediaPacket {
  private static final int RTP_EXTENSION_HEADER_BYTES = 4;
  private final TrackInfo track;
  private final byte[] rtp;
  private final Instant receivedAt;
  private final int payloadOffset;
  private final int payloadEnd;

  public MediaPacket(TrackInfo track, byte[] rtp, Instant receivedAt) {
    this.track = Objects.requireNonNull(track, "track");
    this.rtp = Objects.requireNonNull(rtp, "rtp").clone();
    this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
    if (rtp.length < 12 || ((rtp[0] & 0xc0) >>> 6) != 2) {
      throw new IllegalArgumentException("invalid RTP header");
    }
    int offset = 12 + (rtp[0] & 0x0f) * 4;
    if (offset > rtp.length) throw new IllegalArgumentException("invalid RTP CSRC list");
    if ((rtp[0] & 0x10) != 0) {
      if (rtp.length - offset < RTP_EXTENSION_HEADER_BYTES)
        throw new IllegalArgumentException("invalid RTP extension");
      int words = ((rtp[offset + 2] & 255) << 8) | (rtp[offset + 3] & 255);
      offset += 4 + words * 4;
      if (offset > rtp.length) throw new IllegalArgumentException("invalid RTP extension length");
    }
    int end = rtp.length;
    if ((rtp[0] & 0x20) != 0) {
      int padding = rtp[rtp.length - 1] & 255;
      if (padding == 0 || padding > end - offset)
        throw new IllegalArgumentException("invalid RTP padding");
      end -= padding;
    }
    payloadOffset = offset;
    payloadEnd = end;
  }

  public TrackInfo track() {
    return track;
  }

  public Instant receivedAt() {
    return receivedAt;
  }

  public ByteBuffer rtp() {
    return ByteBuffer.wrap(rtp).asReadOnlyBuffer();
  }

  public ByteBuffer payload() {
    return ByteBuffer.wrap(rtp, payloadOffset, payloadEnd - payloadOffset)
        .slice()
        .asReadOnlyBuffer();
  }

  public int sequenceNumber() {
    return ((rtp[2] & 255) << 8) | (rtp[3] & 255);
  }

  public long timestamp() {
    return Integer.toUnsignedLong(ByteBuffer.wrap(rtp, 4, 4).getInt());
  }

  public long ssrc() {
    return Integer.toUnsignedLong(ByteBuffer.wrap(rtp, 8, 4).getInt());
  }

  public boolean marker() {
    return (rtp[1] & 0x80) != 0;
  }

  public int payloadType() {
    return rtp[1] & 0x7f;
  }
}
