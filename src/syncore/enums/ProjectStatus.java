package syncore.enums;

public enum ProjectStatus {
    ACTIVE("진행 중"),
    CLOSED("종료됨");

    private final String displayName;

    ProjectStatus(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
