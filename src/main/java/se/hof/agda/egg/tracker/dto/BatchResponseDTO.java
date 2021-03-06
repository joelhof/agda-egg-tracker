package se.hof.agda.egg.tracker.dto;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;

public class BatchResponseDTO {

    private int eggsReported;

    private List<LocalDate> failed;

    public static BatchResponseDTO from(List<DiaryEntryDTO> entries,
                                        int[] result) {
        BatchResponseDTO dto = new BatchResponseDTO();

        AtomicInteger counter = new AtomicInteger(0);
        dto.eggsReported = entries.stream()
                                  .filter(entry -> {
                                      int res = result[counter.getAndIncrement()];
                                      return res != PreparedStatement.EXECUTE_FAILED;
                                  })
                                  .mapToInt(entry -> entry.getEggs())
                                  .sum();

        counter.set(0);
        dto.failed = entries.stream()
               .filter(entry -> {
                   int res = result[counter.getAndIncrement()];
                   return res == PreparedStatement.EXECUTE_FAILED;
               }).map(entry -> entry.getTimestamp())
               .map(millis -> toDate(millis))
               .collect(Collectors.toList());
        return dto;
    }

    private static LocalDate toDate(Long millis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), UTC)
                            .toLocalDate();
    }

    public int getEggsReported() {
        return eggsReported;
    }

    public void setEggsReported(int eggsReported) {
        this.eggsReported = eggsReported;
    }

    public List<LocalDate> getFailed() {
        return failed;
    }

    public void setFailed(List<LocalDate> failed) {
        this.failed = failed;
    }
}
