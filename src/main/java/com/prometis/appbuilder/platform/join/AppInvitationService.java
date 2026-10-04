package com.prometis.appbuilder.platform.join;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * 애플리케이션 초대. 애플리케이션 콘솔(사용자 화면)에서 이메일과 권한(관리자/사용자)을 지정해 만들고, 초대 링크를 메일로 보낸다.
 * 같은 이메일을 다시 초대하면 이전 링크는 쓸 수 없게 된다.
 */
@Service
@RequiredArgsConstructor
public class AppInvitationService {
    private final AppInvitationRepository appInvitationRepository;
    private final JoinVerificationRepository joinVerificationRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JoinMailSender joinMailSender;
    private final JoinProperties properties;

    private final SecureRandom random = new SecureRandom();

    /**
     * @param authority 합류하면 받는 권한 (APPLICATION_ADMIN / APPLICATION_USER, 비어 있으면 관리자)
     * @param baseUrl   링크 앞부분. platform.join.public-base-url 이 있으면 그 값을 쓴다
     */
    @Transactional(rollbackFor = JoinException.class)
    public AppInvitationResponse create(String applicationId, String email, String authority, Long inviterUserId, String baseUrl) throws JoinException {
        Application application = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new JoinException("존재하지 않는 애플리케이션입니다."));

        String invitedAuthority = authority == null || authority.isBlank() ? AppUser.AUTHORITY_APPLICATION_ADMIN : authority.trim();
        if(!AppUser.AUTHORITY_APPLICATION_ADMIN.equals(invitedAuthority) && !AppUser.AUTHORITY_APPLICATION_USER.equals(invitedAuthority)) {
            throw new JoinException("초대할 수 없는 권한입니다.");
        }

        String normalizedEmail = email == null ? "" : email.trim();
        if(normalizedEmail.isEmpty()) {
            throw new JoinException("초대할 이메일을 입력해주세요.");
        }
        if(normalizedEmail.length() > 100 || !JoinService.EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new JoinException("이메일 형식이 올바르지 않습니다.");
        }

        // 다시 초대하면 이전 링크(와 그 링크로 진행 중이던 가입)는 버린다
        for(AppInvitation previous : appInvitationRepository.findByApplicationIdAndEmailIgnoreCaseAndAcceptedAtIsNull(applicationId, normalizedEmail)) {
            discard(previous);
        }

        LocalDateTime now = LocalDateTime.now();
        AppInvitation invitation = appInvitationRepository.save(AppInvitation.builder()
                .token(newToken())
                .applicationId(applicationId)
                .email(normalizedEmail)
                .authority(invitedAuthority)
                .invitedBy(inviterUserId)
                .createdAt(now)
                .expiresAt(now.plus(properties.invitationTtl()))
                .build());

        String link = linkOf(invitation, baseUrl);
        String inviterName = inviterUserId == null ? null : userRepository.findById(inviterUserId).map(User::getUserName).orElse(null);
        boolean mailSent = joinMailSender.sendInvitation(normalizedEmail, application.getName(), roleLabelOf(invitation), inviterName, link, properties.invitationTtl());

        return AppInvitationResponse.of(invitation, link, now, mailSent);
    }

    @Transactional(readOnly = true)
    public List<AppInvitationResponse> getInvitations(String applicationId, String baseUrl) {
        LocalDateTime now = LocalDateTime.now();
        return appInvitationRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId).stream()
                .map(invitation -> AppInvitationResponse.of(invitation, linkOf(invitation, baseUrl), now, null))
                .toList();
    }

    /** 초대를 취소(삭제)한다. 그 링크로 진행 중이던 가입도 버린다. */
    @Transactional(rollbackFor = JoinException.class)
    public void revoke(String applicationId, Long invitationId) throws JoinException {
        AppInvitation invitation = appInvitationRepository.findById(invitationId)
                .filter(found -> applicationId.equals(found.getApplicationId()))
                .orElseThrow(() -> new JoinException("초대를 찾을 수 없습니다."));

        discard(invitation);
    }

    /** 링크의 초대가 지금 쓸 수 있는지 확인한다. 쓸 수 없으면 이유를 메시지로 던진다. */
    @Transactional(readOnly = true)
    public AppInvitation usableInvitation(String applicationId, String token) throws JoinException {
        if(token == null || token.isBlank()) {
            throw new JoinException("초대 링크가 올바르지 않습니다. 초대받은 링크로 다시 접속해주세요.");
        }

        AppInvitation invitation = appInvitationRepository.findByToken(token.trim())
                .filter(found -> found.getApplicationId().equals(applicationId))
                .orElseThrow(() -> new JoinException("유효하지 않은 초대 링크입니다. 초대한 관리자에게 다시 요청해주세요."));

        checkUsable(invitation);
        return invitation;
    }

    void checkUsable(AppInvitation invitation) throws JoinException {
        if(invitation.isAccepted()) {
            throw new JoinException("이미 사용된 초대 링크입니다.");
        }
        if(invitation.isExpired(LocalDateTime.now())) {
            throw new JoinException("만료된 초대 링크입니다. 초대한 관리자에게 다시 요청해주세요.");
        }
    }

    // 메일/화면에 쓰는 초대 권한 이름
    static String roleLabelOf(AppInvitation invitation) {
        return invitation.isAdminInvitation() ? "관리자" : "사용자";
    }

    private void discard(AppInvitation invitation) {
        joinVerificationRepository.deleteAll(joinVerificationRepository.findByInvitationId(invitation.getId()));
        appInvitationRepository.delete(invitation);
    }

    private String linkOf(AppInvitation invitation, String baseUrl) {
        String base = properties.publicBaseUrl().isEmpty() ? (baseUrl == null ? "" : baseUrl.replaceAll("/+$", "")) : properties.publicBaseUrl();
        return base + "/" + URLEncoder.encode(invitation.getApplicationId(), StandardCharsets.UTF_8)
                + "/console/join?token=" + URLEncoder.encode(invitation.getToken(), StandardCharsets.UTF_8);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
