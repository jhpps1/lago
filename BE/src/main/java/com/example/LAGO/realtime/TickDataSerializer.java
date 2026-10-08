package com.example.LAGO.realtime;

import com.example.LAGO.realtime.dto.TickData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.*;
import java.util.Objects;

public final class TickDataSerializer {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // TickData → 16B 바이너리(LE)
    public static void write16B(ByteBuffer buf, TickData td, int stockId) {
        Objects.requireNonNull(td, "td");

        // 1) 가격/거래량만 사용 (OHLC 계산은 Timescale CAGG)
        int price  = td.getClosePrice(); // 체결가로만 운영
        int volume = td.getVolume();

        // 2) KST 기준 msOfDay 만들기
        LocalDateTime ldt = td.getParsedDateTime(); // 파일에 이미 있는 파서 사용
        if (ldt == null) throw new IllegalArgumentException("invalid TickData datetime");
        LocalTime lt = ldt.toLocalTime();
        int msOfDay = lt.toSecondOfDay() * 1000 + (ldt.getNano() / 1_000_000);

        // 3) 16B 레이아웃으로 쓰기
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(stockId);
        buf.putInt(msOfDay);
        buf.putInt(price);
        buf.putInt(volume);
    }

    // 16B → 복원(배치 해제 시 사용; 필요하면)
    public static Decoded16B read16B(ByteBuffer buf, LocalDate baseDateKst) {

        //////시작에 잔여바이트 가드

        // ✅ 잔여 16바이트 보장
        if (buf.remaining() < 16) {
            throw new java.nio.BufferUnderflowException();
        }

        //////
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int stockId = buf.getInt();
        int msOfDay = buf.getInt();
        int price   = buf.getInt();
        int volume  = buf.getInt();

        // TimescaleDB ts 만들기(KST → Instant)
        LocalTime lt = LocalTime.ofNanoOfDay(msOfDay * 1_000_000L);
        ZonedDateTime zdt = ZonedDateTime.of(LocalDateTime.of(baseDateKst, lt), KST);
        Instant ts = zdt.toInstant();

        return new Decoded16B(stockId, ts, price, volume);
    }

    public record Decoded16B(int stockId, Instant ts, int price, int volume) {}
}
