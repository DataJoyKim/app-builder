package com.prometis.appbuilder.console.app.dashboard;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 콘솔 첫 화면의 "애플리케이션 사용현황" 지표. 모두 지금 DB에 있는 데이터로 계산한 값이다.
 */
public record AppDashboardResponse(
        Users users,
        Membership membership,
        Organization organization,
        Resources resources
) {
    /**
     * @param total        애플리케이션 사용자(AppUser) 수
     * @param admins       그 중 관리자(APPLICATION_ADMIN)
     * @param members      그 중 일반 사용자(APPLICATION_USER)
     * @param loggedIn     한 번이라도 이 애플리케이션에 접속한 사용자 (AppSession 또는 lastAccessAt 이 있는 사용자)
     * @param neverLoggedIn 가입만 하고 아직 접속하지 않은 사용자
     * @param withoutCompany 회사가 지정되지 않은 사용자 (회사가 하나라도 있을 때만 의미가 있다)
     * @param activeLast7Days  최근 7일 안에 접속한 사용자 (AppUser.lastAccessAt)
     * @param activeLast30Days 최근 30일 안에 접속한 사용자
     * @param joinedLast30Days 최근 30일 안에 가입한 사용자 (AppUser.joinedAt, 가입 경로 무관)
     */
    public record Users(long total, long admins, long members, long loggedIn, long neverLoggedIn, long withoutCompany,
                        long activeLast7Days, long activeLast30Days, long joinedLast30Days) {
    }

    /**
     * @param joinPolicy          가입 방식 (INVITE_ONLY/APPROVAL/OPEN)
     * @param pendingRequests     승인을 기다리는 가입 신청
     * @param invitationsPending  수락 전이고 아직 유효한 초대 (진행중)
     * @param invitationsExpired  수락하지 않은 채 만료된 초대
     * @param invitationsAccepted 수락해서 가입한 초대
     * @param acceptedLast30Days  최근 30일 동안 수락된 초대
     * @param recentJoins         최근 가입한 사용자 (AppUser.joinedAt 최신순 최대 5건, 가입 시각이 없는 예전 사용자는 빠진다)
     */
    public record Membership(String joinPolicy, long pendingRequests,
                             long invitationsPending, long invitationsExpired, long invitationsAccepted,
                             long acceptedLast30Days, List<RecentJoin> recentJoins) {
    }

    public record RecentJoin(String userName, String email, String authority, LocalDateTime joinedAt) {
    }

    public record Organization(long companies, long userGroups) {
    }

    public record Resources(long workflows, long viewObjects, long menus, long restApis, long schedulerJobs, long databases) {
    }
}
