package com.prometis.plugin.api;

import org.pf4j.ExtensionPoint;

/**
 * 워크플로우 플러그인 노드가 실행하는 플러그인 진입점.
 *
 * <p>플러그인 애플리케이션은 이 인터페이스를 구현하고 클래스에 {@code @org.pf4j.Extension} 을 붙인다.
 * 호스트(admin-builder)는 워크플로우의 PLUGIN 노드가 실행될 때 노드에 설정한 pluginPath 와 요청메시지를 넘겨 이 메소드를 부른다.
 * 플러그인 하나가 여러 pluginPath 를 받을 수 있으므로, 구현체는 {@code pluginPath} 로 요청을 구분(라우팅)해 처리한다.</p>
 *
 * <p>플러그인 jar 에는 이 API(plugin-api)와 pf4j 를 넣지 않는다(compileOnly). 실행 시에는 호스트에 있는 것을 쓰며,
 * 함께 넣으면 클래스가 두 벌이 되어 호스트가 구현체를 찾지 못한다.</p>
 */
public interface WorkflowNodePlugin extends ExtensionPoint {

    /**
     * @param pluginPath 콘솔에서 이 플러그인에 등록한 pluginPath 중 노드가 지정한 값 (요청을 구분하는 라우팅 키)
     * @param request    노드로 들어온 요청메시지와 호출 정보
     * @param context    호스트가 플러그인에 열어주는 기능 (등록된 데이터소스 등). 이 실행이 속한 애플리케이션 범위로 제한된다
     * @return 실행 결과. {@link PluginResult#getContents()} 가 노드의 응답메시지가 된다
     */
    PluginResult execute(String pluginPath, PluginRequest request, PluginContext context);
}
