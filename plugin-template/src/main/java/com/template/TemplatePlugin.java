package com.template;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * 플러그인 수명주기 훅 (MANIFEST 의 Plugin-Class). 없어도 되지만, 시작/중지 때 자원을 열고 닫아야 하면 여기서 한다.
 * 콘솔에서 시작/중지/리로드를 누르면 호스트가 start()/stop() 을 부른다. 리로드는 stop → 클래스로더 폐기 → 새 jar 로 start 순이다.
 */
public class TemplatePlugin extends Plugin {

    public TemplatePlugin() {
        super();
    }

    // pf4j 버전에 따라 이 생성자로 만들기도 해서 함께 둔다
    @SuppressWarnings("deprecation")
    public TemplatePlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        log.info("[template-plugin] started");
    }

    @Override
    public void stop() {
        // 스레드, 스케줄러, 커넥션 등 플러그인이 직접 만든 자원은 여기서 반드시 정리한다 (안 하면 리로드 후에도 옛 클래스가 남는다)
        log.info("[template-plugin] stopped");
    }
}
