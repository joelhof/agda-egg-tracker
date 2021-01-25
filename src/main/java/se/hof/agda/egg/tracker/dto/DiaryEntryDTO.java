package se.hof.agda.egg.tracker.dto;

/**
 * Data class representing the data posted by clients to POST a new Diary Entry.
 * Note that default constructor and getters are required for JSON-B injection
 * of Object instances.
 */
public class DiaryEntryDTO {

    long timestamp;

    int eggs;

    String event;

    /**
     * Required for JSON-B auto-magic instantiation.
     */
    public DiaryEntryDTO() { }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getEggs() {
        return eggs;
    }

    public void setEggs(int eggs) {
        this.eggs = eggs;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    @Override
    public String toString() {
        return "DiaryEntryDTO{" +
                "timestamp=" + timestamp +
                ", eggs=" + eggs +
                ", event=" + event +
                '}';
    }
}
