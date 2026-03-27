package syncore.enums;

public enum IssueStatus {
    OPEN("미해결"),
    RESOLVED("해결됨");

    private final String displayName;

    IssueStatus(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
