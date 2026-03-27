package syncore.domain;

import syncore.enums.IssueStatus;

/**
 * 이슈 엔티티.
 * version 필드로 낙관적 잠금(Optimistic Locking)을 시뮬레이션한다.
 */
public class Issue {
    private final String id;
    private final String description;
    private IssueStatus status;
    private final User reporter;
    private int version;

    public Issue(String id, String description, User reporter) {
        this.id          = id;
        this.description = description;
        this.reporter    = reporter;
        this.status      = IssueStatus.OPEN;
        this.version     = 0;
    }

    public String      getId()          { return id; }
    public String      getDescription() { return description; }
    public IssueStatus getStatus()      { return status; }
    public User        getReporter()    { return reporter; }
    public int         getVersion()     { return version; }

    /** 상태를 RESOLVED로 전환하고 버전을 증가시킨다. */
    public void resolve() {
        this.status = IssueStatus.RESOLVED;
        this.version++;
    }

    @Override
    public String toString() {
        return "  [이슈:" + id + "] " + description
                + " (" + status.getDisplayName() + ") - 등록자: " + reporter.getName();
    }
}
