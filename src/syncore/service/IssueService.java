package syncore.service;

import syncore.domain.Issue;
import syncore.domain.Task;
import syncore.domain.User;
import syncore.enums.IssueStatus;
import syncore.enums.Role;
import syncore.exception.AuthorizationException;
import syncore.exception.BusinessException;
import syncore.exception.ConcurrencyException;

/**
 * 이슈 등록·해결 책임.
 */
public class IssueService {

    private int idSeq = 1;

    // ─── 이슈 등록 ────────────────────────────────────────────────
    public Issue registerIssue(User actor, Task task, String description) {
        if (actor.getRole() == Role.VIEWER) {
            throw new AuthorizationException("[권한 오류] VIEWER는 이슈를 등록할 수 없습니다.");
        }
        // TM은 자신에게 배정된 Task에만 이슈 등록 가능
        if (actor.getRole() == Role.TEAM_MEMBER) {
            if (task.getAssignee() == null || !task.getAssignee().getId().equals(actor.getId())) {
                throw new AuthorizationException(
                        "[권한 오류] " + actor.getName() + "은(는) 자신에게 배정된 Task에만 이슈를 등록할 수 있습니다.");
            }
        }

        String id    = "ISS-" + idSeq++;
        Issue  issue = new Issue(id, description, actor);
        task.addIssue(issue);

        System.out.printf("[이슈 등록] (ID: %s) \"%s\" — 등록자: %s%n", id, description, actor.getName());
        return issue;
    }

    // ─── 이슈 해결 ────────────────────────────────────────────────
    public void resolveIssue(User actor, Issue issue, int expectedVersion) {
        checkVersion(issue.getVersion(), expectedVersion, "이슈");

        if (actor.getRole() != Role.PM) {
            throw new AuthorizationException("[권한 오류] 이슈 해결 처리는 PM만 가능합니다.");
        }
        if (issue.getStatus() == IssueStatus.RESOLVED) {
            throw new BusinessException("[비즈니스 오류] 이미 해결된 이슈입니다. (ID: " + issue.getId() + ")");
        }

        issue.resolve();
        System.out.printf("[이슈 해결] \"%s\" — 해결자: %s%n", issue.getDescription(), actor.getName());
    }

    // ─── 내부 가드 ────────────────────────────────────────────────
    private void checkVersion(int current, int expected, String target) {
        if (current != expected) {
            throw new ConcurrencyException(
                    "[동시성 오류] " + target + " 정보가 다른 사용자에 의해 변경되었습니다."
                    + " 최신 정보를 다시 조회하세요."
                    + " (기대 버전: " + expected + ", 현재 버전: " + current + ")");
        }
    }
}
