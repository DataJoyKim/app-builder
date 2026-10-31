# Low-Code 플랫폼 / App Builder
## Overview
어드민, 백오피스, 업무 시스템을 구축하는 Low-code 플랫폼입니다.

``가치있는 일에 집중 할 수 있는 환경을 만듭니다.``

개발자가 아니어도 웹 콘솔에서 엔티티, 화면, 워크플로우, 데이터 소스를 정의하면 런타임이 그 정의를 해석해 실제 CRUD/어드민 화면과 API를 제공합니다. 화면마다 컨트롤러와 쿼리를 손으로 짜지 않아도 됩니다.

## 효과
- **반복 개발 제거** — 표준 CRUD/어드민 화면을 코드 없이 콘솔에서 조립합니다.
- **기존 시스템 연동** — 외부 DB(MySQL/MariaDB/MSSQL/Oracle), REST 서버, 파일 스토리지를 등록해 그대로 관리 대상으로 씁니다.
- **업무 로직의 시각화** — 요청 처리 흐름을 워크플로우 캔버스에서 노드와 연결선으로 설계하고, 조건 분기·오류 응답까지 화면에서 정의합니다.
- **멀티 애플리케이션** — 하나의 플랫폼에서 여러 애플리케이션을 만들고, 애플리케이션별로 사용자·권한·회사·가입 정책을 따로 운영합니다.
- **메타데이터 버전 관리** — 애플리케이션 메타데이터를 JSON DSL로 내보내 GitHub 저장소에 커밋합니다.

## 주요 기능

| 영역 | 내용 |
|------|------|
| 콘솔 (설계) | `/{applicationId}/console` — 엔티티, 쿼리, 화면(View Builder / 파일 화면), 메뉴, 레이아웃, 공통코드, 이미지 관리 |
| 런타임 (서비스) | `/pages/{objectCode}` — 콘솔에서 정의한 화면을 렌더링, 화면 단위 인증/권한 검사 |
| 워크플로우 | 화면이 호출하는 요청 처리 단위. 노드 종류: `ENTITY`, `SQL`, `REST_CLIENT`, `MESSAGE_PROCESSOR`(JS 스크립트), `NOTIFICATION`, `FILE`, `CONDITION`, `ERROR_MESSAGE`. 파일 업로드(multipart) 지원 |
| REST API | 워크플로우를 `/{applicationId}/rest/**` REST 엔드포인트로 공개. 파라미터 스키마 검증, 응답 매핑, API 문서/테스트 호출 |
| 스케줄러 | Quartz 기반. cron 주기로 워크플로우 실행, 실행 이력 관리 |
| 데이터 소스 | 외부 DB, REST 서버, 파일 스토리지(LOCAL), 알림(SMTP) 연결 등록 |
| 보안 | JWT 쿠키 인증, 권한 코드 기반 인가(콘솔/화면/워크플로우), 워크플로우별 IP·도메인 접근 제어 |
| 가입/초대 | 이메일 인증코드 기반 공개 가입, 관리자 초대, 애플리케이션 가입 정책(초대 전용/승인/공개) |
| 배포 | 메타데이터 → JSON DSL 변환·검증 → GitHub 커밋 (Git Data API 사용) |

## Tool
- Java 17, Spring Boot 3.3.4 (Web, Data JPA, Validation, Mail, Quartz)
- Thymeleaf + AdminLTE, ag-Grid (별도 프론트엔드 빌드 없음)
- GraalVM Polyglot JS (워크플로우 스크립트 샌드박스 실행)
- MariaDB (메타데이터 DB) / H2 (로컬 개발)
- JWT (jjwt), jBCrypt

## 시작하기

### 로컬 개발 (H2)
`dev` 프로필은 임베디드 H2(`C:\admin-builder\data\admin-builder`)를 쓰고, 템플릿/정적 리소스를 소스 폴더에서 바로 읽습니다.

```bash
# Windows
gradlew.bat bootRun --args="--spring.profiles.active=dev"
# macOS / Linux
./gradlew bootRun --args="--spring.profiles.active=dev"
```

- 접속: http://localhost:8080
- H2 콘솔: http://localhost:8080/h2-console
- 최초 기동 시 플랫폼 관리자 계정이 생성됩니다 (`platform.sysadmin` 설정, 기본 `sysadmin` / `sysadmin1!`). 운영에서는 반드시 비밀번호를 바꾸세요.

### 기본 프로필 (MariaDB)
기본 설정은 MariaDB 메타데이터 DB(`app-builder-metadatadb:3306/metadatadb`)에 연결하며 아래 환경변수가 필요합니다.

| 환경변수 | 용도 |
|----------|------|
| `DB_USERNAME`, `DB_PASSWORD` | 메타데이터 DB 계정 |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | 가입 인증코드·초대 메일 발송 (`spring.mail.host`가 없으면 로그로 대체) |
| `DEPLOY_TOKEN_SECRET_KEY` | GitHub 토큰 암호화 키 (운영 필수, 변경 시 저장된 토큰 재입력 필요) |

### 빌드 / 테스트
```bash
./gradlew build
./gradlew test
./gradlew test --tests "com.prometis.appbuilder.app.workflow.WorkflowServiceTest"
```

## 프로젝트 구조

```
src/main/java/com/prometis
├── appbuilder
│   ├── app          # 런타임: workflow, node, entity, query, view, restapi, scheduler,
│   │                #         datasource, executor(sql/rest/script/file/notification), image ...
│   ├── console      # 콘솔(설계 화면) 컨트롤러와 REST API, deploy(DSL/GitHub)
│   ├── platform     # 홈, 애플리케이션 관리, 가입/초대, 사용자
│   └── security     # JWT 인증, 토큰, 보안 필터
└── core             # 공통 유틸: 암호화, 예외, util
src/main/resources
├── templates        # console / pages / template / login / home / error
└── static/core      # HttpClient.js, Grid.js, Popup.js, Util.js 등 공용 클라이언트 모듈
docs                 # 기능 기획(PRD), 가이드 문서
```

## License

Apache License 2.0 — 자세한 내용은 [LICENSE](LICENSE) 파일을 참고하세요.
