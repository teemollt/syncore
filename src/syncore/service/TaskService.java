package syncore.service;

import syncore.domain.Project;
import syncore.domain.Task;
import syncore.domain.User;
import syncore.enums.ProjectStatus;
import syncore.enums.Role;
import syncore.enums.TaskStatus;
import syncore.exception.AuthorizationException;
import syncore.exception.BusinessException;
import syncore.exception.ConcurrencyException;

import java.time.LocalDate;

/**
 * Task 생성·배정·상태 전이·완료 요청·완료 승인 책임.
 *
 * 허용된 상태 전이 그래프:
 *   TODO → IN_PROGRESS
 *   IN_PROGRESS → TODO | PENDING_APPROVAL (requestCompletion 경유)
 *   PENDING_APPROVAL → IN_PROGRESS | DONE (approveCompletion 경유)
 *   DONE → (종단, 이후 전이 불가)
 */
public class TaskService {

    private int idSeq = 1;

    // ─── Task 생성 ────────────────────────────────────────────────
    public Task createTask(User actor, Project project,
                           String title, String completionCriteria, LocalDate deadline) {
        requireRole(actor, Role.PM, "Task를 생성");
        requireActiveProject(project);

        String id   = "TASK-" + idSeq++;
        Task   task = new Task(id, title, completionCriteria, deadline);
        project.addTask(task);

        System.out.printf("[Task 생성] \"%s\" (ID: %s)%n", title, id);
        System.out.printf("  완료 기준: %s%n", completionCriteria);
        System.out.printf("  마감일  : %s%n", deadline);
        return task;
    }

    // ─── Task 배정 ────────────────────────────────────────────────
    public void assignTask(User actor, Project project, Task task, User assignee) {
        requireRole(actor, Role.PM, "Task를 배정");
        requireActiveProject(project);

        if (!project.hasMember(assignee)) {
            throw new BusinessException(
                    "[비즈니스 오류] " + assignee.getName() + "은(는) 프로젝트 멤버가 아닙니다.");
        }
        if (assignee.getRole() != Role.TEAM_MEMBER) {
            throw new AuthorizationException(
                    "[권한 오류] Task는 팀 멤버(TM)에게만 배정할 수 있습니다."
                    + " " + assignee + "은(는) " + assignee.getRole().getDisplayName() + "입니다.");
        }

        task.setAssignee(assignee);
        System.out.printf("[Task 배정] \"%s\" → %s%n", task.getTitle(), assignee.getName());
    }

    // ─── 상태 변경 (PM 또는 TM 자신의 Task, PENDING_APPROVAL·DONE 제외) ──
    public void changeStatus(User actor, Task task, TaskStatus newStatus, int expectedVersion) {
        checkVersion(task.getVersion(), expectedVersion, "Task");

        if (actor.getRole() == Role.VIEWER) {
            throw new AuthorizationException("[권한 오류] VIEWER는 Task 상태를 변경할 수 없습니다.");
        }
        if (actor.getRole() == Role.TEAM_MEMBER) {
            requireAssignee(actor, task);
            // TM은 완료 관련 상태로 직접 전이 불가 — 전용 메서드를 사용해야 함
            if (newStatus == TaskStatus.PENDING_APPROVAL || newStatus == TaskStatus.DONE) {
                throw new AuthorizationException(
                        "[권한 오류] " + newStatus.getDisplayName()
                        + " 전이는 requestCompletion / approveCompletion을 사용하세요.");
            }
        }

        validateTransition(task.getStatus(), newStatus);

        TaskStatus prev = task.getStatus();
        task.applyStatus(newStatus);
        System.out.printf("[상태 변경] \"%s\": %s → %s%n",
                task.getTitle(), prev.getDisplayName(), newStatus.getDisplayName());
    }

    // ─── 완료 기준 충족 등록 ─────────────────────────────────────
    public void fulfillCriteria(User actor, Task task) {
        if (actor.getRole() != Role.TEAM_MEMBER) {
            throw new AuthorizationException("[권한 오류] 완료 기준 충족 등록은 팀 멤버(TM)만 가능합니다.");
        }
        requireAssignee(actor, task);

        task.markCriteriaFulfilled();
        System.out.printf("[완료기준 충족] \"%s\" ← %s%n", task.getCompletionCriteria(), actor.getName());
    }

    // ─── 완료 요청 (TM → PENDING_APPROVAL) ──────────────────────
    public void requestCompletion(User actor, Task task, int expectedVersion) {
        checkVersion(task.getVersion(), expectedVersion, "Task");

        if (actor.getRole() != Role.TEAM_MEMBER) {
            throw new AuthorizationException("[권한 오류] 완료 요청은 팀 멤버(TM)만 가능합니다.");
        }
        requireAssignee(actor, task);

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new BusinessException(
                    "[비즈니스 오류] 진행 중(IN_PROGRESS) 상태에서만 완료 요청할 수 있습니다."
                    + " 현재 상태: " + task.getStatus().getDisplayName());
        }
        if (!task.isCriteriaFulfilled()) {
            throw new BusinessException(
                    "[비즈니스 오류] 완료 기준이 충족되지 않아 완료 요청이 거부되었습니다.\n"
                    + "  완료 기준: \"" + task.getCompletionCriteria() + "\"");
        }
        if (task.countOpenIssues() > 0) {
            throw new BusinessException(
                    "[비즈니스 오류] 미해결 이슈가 " + task.countOpenIssues()
                    + "건 남아 있습니다. 이슈를 모두 해결한 후 완료 요청하세요.");
        }

        task.applyStatus(TaskStatus.PENDING_APPROVAL);
        System.out.printf("[완료 요청] \"%s\" — PM의 최종 승인 대기 중%n", task.getTitle());
    }

    // ─── 완료 승인 (PM → DONE) ───────────────────────────────────
    public void approveCompletion(User actor, Project project, Task task, int expectedVersion) {
        checkVersion(task.getVersion(), expectedVersion, "Task");

        if (actor.getRole() != Role.PM || !project.getPm().getId().equals(actor.getId())) {
            throw new AuthorizationException("[권한 오류] Task 완료 승인은 해당 프로젝트의 PM만 가능합니다.");
        }
        if (task.getStatus() != TaskStatus.PENDING_APPROVAL) {
            throw new BusinessException(
                    "[비즈니스 오류] 승인 대기(PENDING_APPROVAL) 상태의 Task만 완료 승인할 수 있습니다."
                    + " 현재 상태: " + task.getStatus().getDisplayName());
        }

        task.applyStatus(TaskStatus.DONE);
        System.out.printf("[완료 승인] \"%s\" — PM이 최종 완료 처리%n", task.getTitle());
    }

    // ─── 상태 전이 유효성 검증 ────────────────────────────────────
    private void validateTransition(TaskStatus from, TaskStatus to) {
        boolean valid = switch (from) {
            case TODO             -> to == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS      -> to == TaskStatus.TODO || to == TaskStatus.PENDING_APPROVAL;
            case PENDING_APPROVAL -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.DONE;
            case DONE             -> false; // 종단 상태, 이후 전이 없음
        };
        if (!valid) {
            throw new BusinessException(
                    "[비즈니스 오류] 잘못된 상태 전이입니다: "
                    + from.getDisplayName() + " → " + to.getDisplayName());
        }
    }

    // ─── 내부 가드 메서드 ─────────────────────────────────────────
    private void requireRole(User actor, Role required, String action) {
        if (actor.getRole() != required) {
            throw new AuthorizationException(
                    "[권한 오류] " + actor + "은(는) " + action + " 권한이 없습니다."
                    + " (" + required.getDisplayName() + "만 가능)");
        }
    }

    private void requireAssignee(User actor, Task task) {
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(actor.getId())) {
            throw new AuthorizationException(
                    "[권한 오류] " + actor.getName() + "은(는) 자신에게 배정된 Task만 조작할 수 있습니다.");
        }
    }

    private void requireActiveProject(Project project) {
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessException("[비즈니스 오류] 종료된 프로젝트의 Task는 수정할 수 없습니다.");
        }
    }

    private void checkVersion(int current, int expected, String target) {
        if (current != expected) {
            throw new ConcurrencyException(
                    "[동시성 오류] " + target + " 정보가 다른 사용자에 의해 변경되었습니다."
                    + " 최신 정보를 다시 조회하세요."
                    + " (기대 버전: " + expected + ", 현재 버전: " + current + ")");
        }
    }
}
