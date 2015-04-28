package org.jdownloader.captcha.v2.solver.browser;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;

public interface BrowserCaptchaSolverConfig extends ChallengeSolverConfig {
    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isAutoClickEnabled();

    void setAutoClickEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isAutoOpenBrowserEnabled();

    void setAutoOpenBrowserEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Example: [ \"C:\\\\Program Files (x86)\\\\Google\\Chrome\\\\Application\\\\chrome.exe\", \"-app=%s\" ]")
    String[] getBrowserCommandline();

    void setBrowserCommandline(String[] cmd);

    @DefaultIntValue(24613)
    @AboutConfig
    int getLocalHttpPort();

    void setLocalHttpPort(int port);

}
