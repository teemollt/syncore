package syncore.enums;

public enum TaskStatus {
    TODO("할 일"),
    IN_PROGRESS("진행 중"),
    PENDING_APPROVAL("승인 대기"),
    DONE("완료");

    private final String displayName;

    TaskStatus(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
