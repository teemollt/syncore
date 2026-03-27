package syncore.enums;

public enum Role {
    PM("프로젝트 매니저"),
    TEAM_MEMBER("팀 멤버"),
    VIEWER("뷰어");

    private final String displayName;

    Role(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
