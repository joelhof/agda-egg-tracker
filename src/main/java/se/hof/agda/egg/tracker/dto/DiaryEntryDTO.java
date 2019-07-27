package se.hof.agda.egg.tracker.dto;

/**
 * Data class representing the data posted by clients to POST a new Diary Entry.
 */
public class DiaryEntryDTO {

    long timestamp;

    int eggs;

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

}
