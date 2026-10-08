package com.example.LAGO.realtime;

import com.example.LAGO.realtime.dto.TickData;
import com.github.luben.zstd.Zstd;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 틱 데이터를 배치로 수집하고 압축하는 클래스
 * 1000개의 틱 데이터를 16바이트 바이너리로 압축하여 Redis에 저장
 */
public final class TickChunk {
    private static final int TICK_BYTES = 16; // TickDataSerializer의 16B 포맷
    private final ByteBuffer buf;
    private final int maxTicks;
    private int count = 0;

    /**
     * TickChunk 생성자
     * @param maxTicks 최대 틱 데이터 개수 (권장: 1000)
     */
    public TickChunk(int maxTicks) {
        this.maxTicks = maxTicks;
        this.buf = ByteBuffer.allocate(maxTicks * TICK_BYTES).order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * 틱 데이터를 청크에 추가
     * @param tickData 추가할 틱 데이터
     * @param stockId 종목 ID (STOCK_INFO.stock_info_id)
     * @return 추가 성공 여부 (false: 청크가 가득참)
     */
    public boolean add16B(TickData tickData, int stockId) {
        if (count >= maxTicks) {
            return false; // 용량 초과
        }
        
        try {
            // TickDataSerializer를 사용하여 16바이트 바이너리로 변환
            TickDataSerializer.write16B(buf, tickData, stockId);
            count++;
            return true;
        } catch (Exception e) {
            System.err.println("Failed to add tick data to chunk: " + e.getMessage());
            return false;
        }
    }

    /**
     * 청크가 가득 찼는지 확인
     * @return 가득참 여부
     */
    public boolean isFull() {
        return count >= maxTicks;
    }

    /**
     * 현재 저장된 틱 데이터 개수
     * @return 틱 데이터 개수
     */
    public int count() {
        return count;
    }

    /**
     * 청크를 Zstd로 압축하여 바이트 배열로 반환
     * @param zstdLevel 압축 레벨 (1-22, 권장: 3)
     * @return 압축된 바이트 배열
     */
    public byte[] toCompressedBlob(int zstdLevel) {
        if (count == 0) {
            return new byte[0];
        }

        // 실제 사용된 크기만큼 바이트 배열 생성
        byte[] raw = new byte[count * TICK_BYTES];
        buf.rewind();
        buf.get(raw, 0, raw.length);

        try {
            // Zstd 압축
            return Zstd.compress(raw, zstdLevel);
        } catch (Exception e) {
            System.err.println("Failed to compress tick chunk: " + e.getMessage());
            return raw; // 압축 실패 시 원본 반환
        }
    }

    /**
     * 청크 초기화 (재사용을 위해)
     */
    public void reset() {
        buf.clear();
        count = 0;
    }

    /**
     * 압축률 계산
     * @param compressedSize 압축된 크기
     * @return 압축률 (백분율)
     */
    public double getCompressionRatio(int compressedSize) {
        if (count == 0) return 0.0;
        int originalSize = count * TICK_BYTES;
        return ((double) compressedSize / originalSize) * 100.0;
    }

    /**
     * 청크 상태 정보 반환
     * @return 상태 문자열
     */
    public String getStatus() {
        return String.format("TickChunk[count=%d/%d, usage=%.1f%%, bytes=%d]",
            count, maxTicks, (count * 100.0 / maxTicks), count * TICK_BYTES);
    }

    /**
     * 청크가 비어있는지 확인
     * @return 비어있음 여부
     */
    public boolean isEmpty() {
        return count == 0;
    }
}