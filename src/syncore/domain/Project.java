package syncore.domain;

import syncore.enums.ProjectStatus;
import syncore.enums.TaskStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 프로젝트 엔티티.
 * version 필드로 낙관적 잠금을 시뮬레이션한다.
 */
public class Project {
    private final String        id;
    private final String        name;
    private ProjectStatus       status;
    private final User          pm;
    private final List<User>    members;
    private final List<Task>    tasks;
    private int                 version;

    public Project(String id, String name, User pm) {
        this.id      = id;
        this.name    = name;
        this.pm      = pm;
        this.status  = ProjectStatus.ACTIVE;
        this.members = new ArrayList<>();
        this.members.add(pm);
        this.tasks   = new ArrayList<>();
        this.version = 0;
    }

    public String        getId()      { return id; }
    public String        getName()    { return name; }
    public ProjectStatus getStatus()  { return status; }
    public User          getPm()      { return pm; }
    public List<User>    getMembers() { return members; }
    public List<Task>    getTasks()   { return tasks; }
    public int           getVersion() { return version; }

    public void addMember(User user) {
        members.add(user);
        version++;
    }

    public void addTask(Task task) {
        tasks.add(task);
        version++;
    }

    /** 프로젝트를 종료 상태로 전환하고 버전을 증가시킨다. */
    public void close() {
        this.status = ProjectStatus.CLOSED;
        this.version++;
    }

    public boolean hasMember(User user) {
        return members.stream().anyMatch(m -> m.getId().equals(user.getId()));
    }

    public long countCompletedTasks() {
        return tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
    }

    public long countPendingTasks() {
        return tasks.stream().filter(t -> t.getStatus() != TaskStatus.DONE).count();
    }

    public long countAllIssues() {
        return tasks.stream().mapToLong(t -> t.getIssues().size()).sum();
    }

    public long countOpenIssues() {
        return tasks.stream().mapToLong(Task::countOpenIssues).sum();
    }

    public double getProgressRate() {
        if (tasks.isEmpty()) return 0.0;
        return (double) countCompletedTasks() / tasks.size() * 100.0;
    }
}
