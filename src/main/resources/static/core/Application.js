/**
 * 현재 화면의 애플리케이션ID.
 * 콘솔(/{applicationId}/console/...)과 생성 화면(/{applicationId}/, /{applicationId}/pages/...)은 모두
 * /{applicationId} 아래에서 열리므로 URL 의 첫 경로에서 읽는다. 서버 요청은 이 값을 앞에 붙여서 보낸다.
 *   예) `/${APPLICATION_ID}/console/api/workflow`, `/${APPLICATION_ID}/workflow`
 */
window.APPLICATION_ID = decodeURIComponent(window.location.pathname.split('/')[1] || '');
