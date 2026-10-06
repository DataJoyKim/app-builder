package com.prometis.appbuilder.platform.join;

import com.prometis.appbuilder.platform.application.Application;
import com.prometis.appbuilder.platform.application.ApplicationRepository;
import com.prometis.appbuilder.platform.user.User;
import com.prometis.appbuilder.platform.user.UserRepository;
import com.prometis.core.crypto.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 메일 인증을 거치는 가입. 세 가지가 있다.
 * - 공개 가입(/signup) : 누구나 계정을 만든다. 가입한 사람은 자기 애플리케이션을 만들 수 있다 (남의 애플리케이션 권한은 없다)
 * - 초대 합류(/{applicationId}/console/join?token=...) : 애플리케이션 관리자가 초대한 이메일로만, 초대받은 권한(관리자/사용자)으로 합류한다.
 *   계정이 없으면 가입하면서 합류하고, 이미 계정이 있으면 로그인해서 수락한다.
 * - 앱 가입(/{applicationId}/signup) : 그 앱의 가입 방식(AppJoinSetting)대로 바로 사용자가 되거나 가입 신청을 남긴다. 초대만 받는 앱이면 쓸 수 없다.
 *   이미 계정이 있으면 로그인해서 가입한다 (AppMembershipService.applyForApplication).
 * 가입 순서: 가입 정보 → 인증코드 메일(유효시간 platform.join.verification-code-ttl) → 인증코드 확인 → User 생성.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(JoinProperties.class)
public class JoinService {
    static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_LENGTH = 100;
    private static final String SIGNUP_PURPOSE = "App Builder 가입";

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final AppMembershipService appMembershipService;
    private final JoinVerificationRepository joinVerificationRepository;
    private final AppInvitationRepository appInvitationRepository;
    private final AppInvitationService appInvitationService;
    private final PasswordEncoder passwordEncoder;
    private final JoinMailSender joinMailSender;
    private final JoinProperties properties;

    private final SecureRandom random = new SecureRandom();

    // ---- 공개 가입 ----

    @Transactional(rollbackFor = JoinException.class)
    public JoinRequestResponse requestSignupCode(JoinRequest request) throws JoinException {
        return requestCode(request, trim(request.getEmail()), JoinVerification.TYPE_SIGNUP, null, null, SIGNUP_PURPOSE);
    }

    @Transactional(noRollbackFor = JoinException.class)
    public User verifySignup(JoinVerifyRequest request) throws JoinException {
        return verify(JoinVerification.TYPE_SIGNUP, null, request);
    }

    // ---- 초대 합류 ----

    /** 초대받은 이메일로만 인증코드를 보낸다. 화면이 보낸 이메일은 쓰지 않는다. */
    @Transactional(rollbackFor = JoinException.class)
    public JoinRequestResponse requestInvitationCode(String applicationId, String token, JoinRequest request) throws JoinException {
        AppInvitation invitation = appInvitationService.usableInvitation(applicationId, token);
        String purpose = applicationNameOf(applicationId) + " " + AppInvitationService.roleLabelOf(invitation) + " 합류";

        return requestCode(request, invitation.getEmail(), JoinVerification.TYPE_INVITATION, applicationId, invitation, purpose);
    }

    @Transactional(noRollbackFor = JoinException.class)
    public User verifyInvitation(String applicationId, JoinVerifyRequest request) throws JoinException {
        return verify(JoinVerification.TYPE_INVITATION, applicationId, request);
    }

    /**
     * 이미 계정이 있는 사람이 로그인한 상태로 초대를 수락한다. 초대받은 이메일의 계정이어야 한다.
     * 관리자 초대면 관리자가 되고(사용자였으면 올라간다), 사용자 초대면 사용자가 된다 (이미 관리자면 그대로).
     */
    @Transactional(rollbackFor = JoinException.class)
    public void acceptInvitation(String applicationId, String token, Long userId) throws JoinException {
        AppInvitation invitation = appInvitationService.usableInvitation(applicationId, token);

        User user = Optional.ofNullable(userId).flatMap(userRepository::findById)
                .orElseThrow(() -> new JoinException("로그인이 필요합니다."));
        if(!invitation.isEmailOf(user.getEmail())) {
            throw new JoinException("초대받은 이메일(" + invitation.getEmail() + ")의 계정으로 로그인해야 수락할 수 있습니다.");
        }

        appMembershipService.join(applicationId, user.getId(), invitation.getAuthority(), invitation.getCompanyCode());
        invitation.accept(user.getId(), LocalDateTime.now());
        joinVerificationRepository.deleteAll(joinVerificationRepository.findByInvitationId(invitation.getId()));
    }

    // ---- 앱 가입 ----

    /** 초대만 받는 앱이면 인증코드를 보내지 않는다. */
    @Transactional(rollbackFor = JoinException.class)
    public JoinRequestResponse requestAppSignupCode(String applicationId, JoinRequest request) throws JoinException {
        String applicationName = applicationNameOf(applicationId);
        if(appMembershipService.policyOf(applicationId) == AppJoinPolicy.INVITE_ONLY) {
            throw new JoinException(AppMembershipService.INVITE_ONLY_MESSAGE);
        }

        return requestCode(request, trim(request.getEmail()), JoinVerification.TYPE_APP_SIGNUP, applicationId, null, applicationName + " 가입");
    }

    /**
     * 인증코드를 확인해 계정을 만들고, 앱의 가입 방식대로 가입(MEMBER)하거나 신청(PENDING)한다.
     * 메일을 기다리는 사이에 초대만 받도록 바뀌었으면 계정만 만들고 NONE 을 돌려준다.
     */
    @Transactional(noRollbackFor = JoinException.class)
    public AppSignupResult verifyAppSignup(String applicationId, JoinVerifyRequest request) throws JoinException {
        User user = verify(JoinVerification.TYPE_APP_SIGNUP, applicationId, request);

        // applyForApplication 이 예외를 던지면 이 트랜잭션까지 롤백 표시되므로(계정 생성도 취소), 초대만 받는 경우는 미리 가린다
        if(appMembershipService.policyOf(applicationId) == AppJoinPolicy.INVITE_ONLY) {
            return new AppSignupResult(user, AppJoinStatus.NONE);
        }

        return new AppSignupResult(user, appMembershipService.applyForApplication(applicationId, user.getId()));
    }

    public record AppSignupResult(User user, AppJoinStatus status) {
    }

    // ---- 공통 ----

    /**
     * 가입 정보를 검사하고 인증코드를 메일로 보낸다.
     * 같은 로그인ID/이메일로 다시 요청하면(재전송) 이전 인증코드는 쓸 수 없게 된다. 메일을 보내지 못하면 요청도 저장하지 않는다.
     */
    private JoinRequestResponse requestCode(
            JoinRequest request,
            String email,
            String joinType,
            String applicationId,
            AppInvitation invitation,
            String purpose
    ) throws JoinException {
        String loginId = trim(request.getLoginId());
        String userName = trim(request.getUserName());
        String password = request.getPassword() == null ? "" : request.getPassword();

        requireText(loginId, "로그인ID를 입력해주세요.");
        requireText(userName, "이름을 입력해주세요.");
        requireText(email, "이메일을 입력해주세요.");
        if(!EMAIL_PATTERN.matcher(email).matches()) {
            throw new JoinException("이메일 형식이 올바르지 않습니다.");
        }
        if(password.isBlank()) {
            throw new JoinException("비밀번호를 입력해주세요.");
        }
        if(!password.equals(request.getCheckPassword())) {
            throw new JoinException("비밀번호와 비밀번호 확인이 다릅니다.");
        }
        if(userRepository.findByLoginId(loginId).isPresent()) {
            throw new JoinException("이미 사용 중인 로그인ID입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        checkResendCooldown(email, now);

        joinVerificationRepository.deleteAll(joinVerificationRepository.findByLoginId(loginId));
        joinVerificationRepository.deleteAll(joinVerificationRepository.findByEmailIgnoreCase(email));

        String code = newCode();
        JoinVerification verification = joinVerificationRepository.save(JoinVerification.builder()
                .verificationKey(UUID.randomUUID().toString())
                .joinType(joinType)
                .applicationId(applicationId)
                .invitationId(invitation == null ? null : invitation.getId())
                .loginId(loginId)
                .userName(userName)
                .email(email)
                .encodedPassword(passwordEncoder.encode(password))
                .encodedCode(passwordEncoder.encode(code))
                .expiresAt(now.plus(properties.verificationCodeTtl()))
                .failCount(0)
                .createdAt(now)
                .build());

        joinMailSender.sendVerificationCode(email, purpose, code, properties.verificationCodeTtl());

        return JoinRequestResponse.builder()
                .verificationKey(verification.getVerificationKey())
                .email(email)
                .expiresInSeconds(properties.verificationCodeTtl().toSeconds())
                .build();
    }

    // 같은 이메일로 너무 자주 메일을 보내지 않는다
    private void checkResendCooldown(String email, LocalDateTime now) throws JoinException {
        Duration cooldown = properties.resendCooldown();
        if(cooldown.isZero()) {
            return;
        }

        Optional<LocalDateTime> lastSentAt = joinVerificationRepository.findByEmailIgnoreCase(email).stream()
                .map(JoinVerification::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo);

        if(lastSentAt.isPresent() && now.isBefore(lastSentAt.get().plus(cooldown))) {
            long waitSeconds = Math.max(1, Duration.between(now, lastSentAt.get().plus(cooldown)).toSeconds());
            throw new JoinException("인증코드는 " + waitSeconds + "초 후에 다시 받을 수 있습니다.");
        }
    }

    /**
     * 인증코드를 확인하고 계정을 만든다. 가입 종류와 애플리케이션이 맞는 요청만 받는다 (공개 가입 요청을 초대 합류 API 로 인증할 수 없다).
     * 초대 합류면 초대받은 권한으로 등록하고 초대를 사용 처리한다.
     * 틀린 횟수, 만료/초과로 버린 요청은 실패해도 남아야 하므로 JoinException 으로는 롤백하지 않는다 (호출하는 쪽 @Transactional 참고).
     */
    private User verify(String joinType, String applicationId, JoinVerifyRequest request) throws JoinException {
        String verificationKey = trim(request.getVerificationKey());
        String code = trim(request.getCode());

        JoinVerification verification = joinVerificationRepository.findByVerificationKey(verificationKey)
                .filter(found -> joinType.equals(found.getJoinType()) && Objects.equals(applicationId, found.getApplicationId()))
                .orElseThrow(() -> new JoinException("가입 요청을 찾을 수 없습니다. 인증코드를 다시 받아주세요."));

        if(verification.isExpired(LocalDateTime.now())) {
            joinVerificationRepository.delete(verification);
            throw new JoinException("인증코드가 만료되었습니다. 인증코드를 다시 받아주세요.");
        }

        if(code.isEmpty() || !passwordEncoder.matches(code, verification.getEncodedCode())) {
            verification.increaseFailCount();

            int remaining = properties.maxVerifyAttempts() - verification.getFailCount();
            if(remaining <= 0) {
                joinVerificationRepository.delete(verification);
                throw new JoinException("인증코드를 " + properties.maxVerifyAttempts() + "회 잘못 입력했습니다. 인증코드를 다시 받아주세요.");
            }
            throw new JoinException("인증코드가 올바르지 않습니다. (남은 입력 횟수 " + remaining + "회)");
        }

        // 메일을 기다리는 사이에 초대가 취소/만료/사용되었을 수 있다
        AppInvitation invitation = null;
        if(verification.isInvitation()) {
            invitation = appInvitationRepository.findById(verification.getInvitationId()).orElse(null);
            if(invitation == null) {
                joinVerificationRepository.delete(verification);
                throw new JoinException("취소된 초대입니다. 초대한 관리자에게 다시 요청해주세요.");
            }
            try {
                appInvitationService.checkUsable(invitation);
            }
            catch (JoinException e) {
                joinVerificationRepository.delete(verification);
                throw e;
            }
        }

        // 메일을 기다리는 사이에 같은 로그인ID로 가입한 사람이 있을 수 있다
        if(userRepository.findByLoginId(verification.getLoginId()).isPresent()) {
            joinVerificationRepository.delete(verification);
            throw new JoinException("이미 사용 중인 로그인ID입니다. 다른 로그인ID로 다시 가입해주세요.");
        }

        User user = userRepository.save(User.builder()
                .loginId(verification.getLoginId())
                .userName(verification.getUserName())
                .email(verification.getEmail())
                .password(verification.getEncodedPassword())
                // 공개 가입(/signup)으로 만든 계정은 애플리케이션을 만들 수 있는 애플리케이션 관리자가 된다 (초대/앱 가입은 권한 없음)
                .authority(JoinVerification.TYPE_SIGNUP.equals(joinType) ? User.AUTHORITY_APPLICATION_ADMIN : null)
                .build());

        if(invitation != null) {
            appMembershipService.join(invitation.getApplicationId(), user.getId(), invitation.getAuthority(), invitation.getCompanyCode());
            invitation.accept(user.getId(), LocalDateTime.now());
        }

        joinVerificationRepository.delete(verification);

        return user;
    }

    String applicationNameOf(String applicationId) throws JoinException {
        return applicationRepository.findByApplicationId(applicationId)
                .map(Application::getName)
                .orElseThrow(() -> new JoinException("존재하지 않는 애플리케이션입니다."));
    }

    private String newCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private static void requireText(String value, String message) throws JoinException {
        if(value.isEmpty()) {
            throw new JoinException(message);
        }
        if(value.length() > MAX_LENGTH) {
            throw new JoinException("입력값은 " + MAX_LENGTH + "자를 넘을 수 없습니다.");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
