package syncore.service;

import syncore.domain.Project;
import syncore.domain.Task;
import syncore.domain.User;
import syncore.enums.ProjectStatus;
import syncore.enums.Role;
import syncore.exception.AuthorizationException;
import syncore.exception.BusinessException;
import syncore.exception.ConcurrencyException;

import java.util.HashMap;
import java.util.Map;

/**
 * 프로젝트 생성·멤버 관리·대시보드 조회 책임.
 */
public class ProjectService {

    private final Map<String, Project> store = new HashMap<>();
    private int idSeq = 1;

    // ─── 프로젝트 생성 ────────────────────────────────────────────
    public Project createProject(User actor, String name) {
        requireRole(actor, Role.PM, "프로젝트를 생성");

        String id = "PRJ-" + idSeq++;
        Project project = new Project(id, name, actor);
        store.put(id, project);

        System.out.printf("[프로젝트 생성] \"%s\" (ID: %s) | PM: %s%n", name, id, actor);
        return project;
    }

    // ─── 멤버 추가 ────────────────────────────────────────────────
    public void addMember(User actor, Project project, User newMember) {
        requireProjectPm(actor, project, "멤버를 추가");
        requireActiveProject(project);

        if (project.hasMember(newMember)) {
            throw new BusinessException(
                    "[비즈니스 오류] " + newMember.getName() + "은(는) 이미 프로젝트 멤버입니다.");
        }

        project.addMember(newMember);
        System.out.printf("[멤버 추가] %s → 프로젝트 \"%s\"%n", newMember, project.getName());
    }

    // ─── 프로젝트 종료 ────────────────────────────────────────────
    public void closeProject(User actor, Project project, int expectedVersion) {
        requireProjectPm(actor, project, "프로젝트를 종료");
        requireActiveProject(project);
        checkVersion(project.getVersion(), expectedVersion, "프로젝트");

        if (project.countPendingTasks() > 0) {
            throw new BusinessException(
                    "[비즈니스 오류] 미완료 Task가 " + project.countPendingTasks()
                    + "개 있어 프로젝트를 종료할 수 없습니다. 모든 Task를 완료한 후 종료하세요.");
        }

        project.close();
        System.out.printf("[프로젝트 종료] \"%s\" 종료 완료%n", project.getName());
    }

    // ─── 대시보드 조회 ────────────────────────────────────────────
    public void printDashboard(User actor, Project project) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.printf( "║  프로젝트 대시보드: %-38s║%n", project.getName());
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf( "║  조회자  : %-46s║%n", actor);
        System.out.printf( "║  상태    : %-46s║%n", project.getStatus().getDisplayName());
        System.out.printf( "║  진행률  : %5.1f%%  (%d / %d Task 완료)%-18s║%n",
                project.getProgressRate(),
                project.countCompletedTasks(),
                project.getTasks().size(), "");
        System.out.printf( "║  남은과업: %-2d개%-44s║%n", project.countPendingTasks(), "");
        System.out.printf( "║  이슈    : 전체 %d건  (미해결 %d건)%-26s║%n",
                project.countAllIssues(), project.countOpenIssues(), "");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  [Task 목록]                                             ║");
        for (Task task : project.getTasks()) {
            System.out.printf("║    %s%n", task);
            for (var issue : task.getIssues()) {
                System.out.printf("║   %s%n", issue);
            }
        }
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    // ─── 내부 가드 메서드 ─────────────────────────────────────────
    private void requireRole(User actor, Role required, String action) {
        if (actor.getRole() != required) {
            throw new AuthorizationException(
                    "[권한 오류] " + actor + "은(는) " + action + " 권한이 없습니다."
                    + " (" + required.getDisplayName() + "만 가능)");
        }
    }

    private void requireProjectPm(User actor, Project project, String action) {
        if (actor.getRole() != Role.PM || !project.getPm().getId().equals(actor.getId())) {
            throw new AuthorizationException(
                    "[권한 오류] " + actor + "은(는) " + action + " 권한이 없습니다."
                    + " 해당 프로젝트의 PM만 가능합니다.");
        }
    }

    private void requireActiveProject(Project project) {
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessException(
                    "[비즈니스 오류] 종료된 프로젝트(\"" + project.getName()
                    + "\")에는 해당 작업을 수행할 수 없습니다.");
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
