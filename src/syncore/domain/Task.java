package syncore.domain;

import syncore.enums.IssueStatus;
import syncore.enums.TaskStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 과업(Task) 엔티티.
 * version 필드로 낙관적 잠금을 시뮬레이션한다.
 * criteriaFulfilled 플래그로 완료 기준 충족 여부를 추적한다.
 */
public class Task {
    private final String    id;
    private final String    title;
    private final String    completionCriteria; // 완료 기준
    private final LocalDate deadline;           // 마감일
    private TaskStatus      status;
    private User            assignee;
    private boolean         criteriaFulfilled;
    private final List<Issue> issues;
    private int             version;

    public Task(String id, String title, String completionCriteria, LocalDate deadline) {
        this.id                 = id;
        this.title              = title;
        this.completionCriteria = completionCriteria;
        this.deadline           = deadline;
        this.status             = TaskStatus.TODO;
        this.criteriaFulfilled  = false;
        this.issues             = new ArrayList<>();
        this.version            = 0;
    }

    public String    getId()                 { return id; }
    public String    getTitle()              { return title; }
    public String    getCompletionCriteria() { return completionCriteria; }
    public LocalDate getDeadline()           { return deadline; }
    public TaskStatus getStatus()            { return status; }
    public User      getAssignee()           { return assignee; }
    public boolean   isCriteriaFulfilled()   { return criteriaFulfilled; }
    public List<Issue> getIssues()           { return issues; }
    public int       getVersion()            { return version; }

    public void setAssignee(User assignee)   { this.assignee = assignee; }

    /** 상태를 변경하고 버전을 증가시킨다 (낙관적 잠금 갱신). */
    public void applyStatus(TaskStatus newStatus) {
        this.status = newStatus;
        this.version++;
    }

    public void markCriteriaFulfilled() {
        this.criteriaFulfilled = true;
    }

    public void addIssue(Issue issue) {
        this.issues.add(issue);
    }

    public long countOpenIssues() {
        return issues.stream().filter(i -> i.getStatus() == IssueStatus.OPEN).count();
    }

    @Override
    public String toString() {
        String assigneeName = assignee != null ? assignee.getName() : "미배정";
        return "[Task:" + id + "] " + title
                + " | 상태: " + status.getDisplayName()
                + " | 담당: " + assigneeName
                + " | 마감: " + deadline
                + " | 이슈: " + issues.size() + "건(미해결 " + countOpenIssues() + "건)";
    }
}
