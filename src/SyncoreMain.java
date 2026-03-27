import syncore.domain.Issue;
import syncore.domain.Project;
import syncore.domain.Task;
import syncore.domain.User;
import syncore.enums.Role;
import syncore.enums.TaskStatus;
import syncore.service.IssueService;
import syncore.service.ProjectService;
import syncore.service.TaskService;

import java.time.LocalDate;

/**
 * Syncore 협업 관리 시스템 — 시나리오 실행 진입점
 *
 * 정상 흐름 5단계 + 대안 흐름(예외) 5단계를 순차 실행한다.
 */
public class SyncoreMain {

    static final String SEP  = "=".repeat(62);
    static final String LINE = "-".repeat(62);

    public static void main(String[] args) {

        ProjectService projectSvc = new ProjectService();
        TaskService    taskSvc    = new TaskService();
        IssueService   issueSvc   = new IssueService();

        banner("SYNCORE 협업 관리 시스템 — 시나리오 실행");

        // ════════════════════════════════════════════════════════════
        //  시나리오 1 : 계정/프로젝트 생성
        // ════════════════════════════════════════════════════════════
        section("시나리오 1 : 계정/프로젝트 생성");

        User pm     = new User("U-001", "김팀장",  Role.PM);
        User tm1    = new User("U-002", "이개발",  Role.TEAM_MEMBER);
        User tm2    = new User("U-003", "박개발",  Role.TEAM_MEMBER);
        User viewer = new User("U-004", "최관찰",  Role.VIEWER);

        System.out.println("[참여자 등록]");
        System.out.println("  PM     : " + pm);
        System.out.println("  TM1    : " + tm1);
        System.out.println("  TM2    : " + tm2);
        System.out.println("  Viewer : " + viewer);
        System.out.println();

        Project project = projectSvc.createProject(pm, "Syncore v1.0 개발 프로젝트");
        projectSvc.addMember(pm, project, tm1);
        projectSvc.addMember(pm, project, tm2);
        projectSvc.addMember(pm, project, viewer);

        // ════════════════════════════════════════════════════════════
        //  시나리오 2 : 과업 등록/배정
        // ════════════════════════════════════════════════════════════
        section("시나리오 2 : 과업 등록/배정");

        Task task1 = taskSvc.createTask(
                pm, project,
                "백엔드 API 설계",
                "REST API 문서 작성 완료 및 단위 테스트 전체 통과",
                LocalDate.of(2026, 4, 15));
        System.out.println();

        Task task2 = taskSvc.createTask(
                pm, project,
                "프론트엔드 UI 구현",
                "UI 컴포넌트 구현 완료 및 E2E 시나리오 테스트 통과",
                LocalDate.of(2026, 4, 30));
        System.out.println();

        taskSvc.assignTask(pm, project, task1, tm1);
        taskSvc.assignTask(pm, project, task2, tm2);

        // ════════════════════════════════════════════════════════════
        //  시나리오 3 : 상태 전이 / 이슈 처리
        // ════════════════════════════════════════════════════════════
        section("시나리오 3 : 상태 전이 / 이슈 처리");

        // TM1: task1을 진행 중으로 변경
        taskSvc.changeStatus(tm1, task1, TaskStatus.IN_PROGRESS, task1.getVersion());

        // TM1: 이슈 등록
        Issue issue1 = issueSvc.registerIssue(tm1, task1, "API 인증 토큰 만료 오류 발생");

        // PM: 이슈 해결 처리
        issueSvc.resolveIssue(pm, issue1, issue1.getVersion());

        // ════════════════════════════════════════════════════════════
        //  시나리오 4 : 과업 완료 검증 (TM2 → task2)
        // ════════════════════════════════════════════════════════════
        section("시나리오 4 : 과업 완료 검증");

        // TM2: task2를 진행 중으로 변경
        taskSvc.changeStatus(tm2, task2, TaskStatus.IN_PROGRESS, task2.getVersion());

        // TM2: 완료 기준 충족 처리
        taskSvc.fulfillCriteria(tm2, task2);

        // TM2: 완료 요청 (PENDING_APPROVAL)
        taskSvc.requestCompletion(tm2, task2, task2.getVersion());

        // PM: 최종 승인 (DONE)
        taskSvc.approveCompletion(pm, project, task2, task2.getVersion());

        // ════════════════════════════════════════════════════════════
        //  시나리오 5 : 전체 프로젝트 대시보드 조회 (Viewer)
        // ════════════════════════════════════════════════════════════
        section("시나리오 5 : 전체 프로젝트 대시보드 조회");
        projectSvc.printDashboard(viewer, project);


        // ════════════════════════════════════════════════════════════
        //  대안 흐름 : 예외 처리 검증 (5가지)
        // ════════════════════════════════════════════════════════════
        banner("대안 흐름 — 예외 처리 검증 (시스템이 거부하는 5가지 케이스)");

        // ── 대안 흐름 1 : 권한 부족 ──────────────────────────────
        //    VIEWER가 Task 생성을 시도한다.
        alt("대안 흐름 1", "VIEWER가 Task 생성 시도 → 권한 부족 예외");
        tryExpectingFailure(() ->
            taskSvc.createTask(viewer, project, "무단 Task", "조건 없음", LocalDate.now())
        );

        // ── 대안 흐름 2 : 조건 미충족 완료 요청 ─────────────────
        //    task1은 IN_PROGRESS이지만 완료 기준(criteriaFulfilled)이 미충족 상태다.
        alt("대안 흐름 2", "완료 기준 미충족 상태에서 완료 요청 → BusinessException");
        System.out.println("  (task1 완료 기준 충족 여부: " + task1.isCriteriaFulfilled() + ")");
        tryExpectingFailure(() ->
            taskSvc.requestCompletion(tm1, task1, task1.getVersion())
        );

        // ── 대안 흐름 3 : 프로젝트 종료 제한 ────────────────────
        //    task1이 아직 완료되지 않은 상태에서 PM이 프로젝트를 종료하려 한다.
        alt("대안 흐름 3", "미완료 Task 존재 시 프로젝트 종료 시도 → BusinessException");
        System.out.println("  (미완료 Task 수: " + project.countPendingTasks() + "개)");
        tryExpectingFailure(() ->
            projectSvc.closeProject(pm, project, project.getVersion())
        );

        // ── 대안 흐름 4 : 잘못된 상태 전이 ──────────────────────
        //    task2는 이미 DONE 상태다. DONE → IN_PROGRESS 전이는 허용되지 않는다.
        alt("대안 흐름 4", "DONE 상태 Task를 IN_PROGRESS로 되돌리기 → 잘못된 전이 예외");
        System.out.println("  (task2 현재 상태: " + task2.getStatus().getDisplayName() + ")");
        tryExpectingFailure(() ->
            taskSvc.changeStatus(pm, task2, TaskStatus.IN_PROGRESS, task2.getVersion())
        );

        // ── 대안 흐름 5 : 동시성 충돌 ───────────────────────────
        //    task1은 시나리오 3에서 이미 상태가 변경되어 버전이 올라갔다.
        //    구버전(0)으로 상태 변경을 시도하면 낙관적 잠금 충돌이 발생한다.
        int staleVersion = 0;
        alt("대안 흐름 5", "구버전(v" + staleVersion + ")으로 Task 상태 변경 시도 → 낙관적 잠금 충돌");
        System.out.println("  (task1 현재 버전: " + task1.getVersion()
                + ", 시도 버전: " + staleVersion + ")");
        tryExpectingFailure(() ->
            taskSvc.changeStatus(tm1, task1, TaskStatus.PENDING_APPROVAL, staleVersion)
        );

        banner("모든 시나리오 실행 완료");
    }

    // ─── 출력 헬퍼 ───────────────────────────────────────────────
    static void banner(String title) {
        System.out.println();
        System.out.println(SEP);
        System.out.println("  " + title);
        System.out.println(SEP);
    }

    static void section(String title) {
        System.out.println();
        System.out.println(LINE);
        System.out.println("▶ " + title);
        System.out.println(LINE);
    }

    static void alt(String label, String desc) {
        System.out.println();
        System.out.println("  [" + label + "] " + desc);
        System.out.println("  " + "-".repeat(56));
    }

    /**
     * 반드시 예외가 발생해야 하는 코드 블록을 실행한다.
     * 예외 메시지를 출력하고, 예외가 발생하지 않으면 경고를 출력한다.
     */
    static void tryExpectingFailure(Runnable action) {
        try {
            action.run();
            System.out.println("  [⚠ 경고] 예상된 예외가 발생하지 않았습니다!");
        } catch (Exception e) {
            System.out.println("  → " + e.getMessage());
            System.out.println("  → 시스템이 정상적으로 요청을 거부했습니다. (" + e.getClass().getSimpleName() + ")");
        }
    }
}
