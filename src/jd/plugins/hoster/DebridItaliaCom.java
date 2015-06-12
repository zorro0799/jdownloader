//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debriditalia.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOMasdfasdfsadfsdgfd32423" }, flags = { 2 })
public class DebridItaliaCom extends antiDDoSForHost {

    public DebridItaliaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.debriditalia.com/premium.php");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://www.debriditalia.com/premium.php";
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        /**
         * Note: Rest of this class makes use of the field "maxPrem" (see comment of controlPrem()) which was formerly returned here. But it
         * simply didn't work. It led to a strange behavior where only a single file was downloaded via DebridItalia premium, all other
         * links got started as non-premium. Once it got analyzed what causes this behavior, either remove all the "maxPrem" code below
         * (currently it works also without!) or reintroduce it here!
         */
        return super.getMaxSimultanDownload(link, account);
    }

    private static final String                            NICE_HOST                        = "debriditalia.com";
    private static final String                            NICE_HOSTproperty                = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NOCHUNKS                         = "NOCHUNKS";
    private static final String                            MAX_RETRIES_UNAVAILABLE_PROPERTY = "MAX_RETRIES_UNAVAILABLE";
    private static final int                               DEFAULT_MAX_RETRIES_UNAVAILABLE  = 30;
    private static final String                            MAX_RETRIES_DL_ERROR_PROPERTY    = "MAX_RETRIES_DL_ERROR";
    private static final int                               DEFAULT_MAX_RETRIES_DL_ERROR     = 50;

    // note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20]
    private static AtomicInteger                           totalMaxSimultanFreeDownload     = new AtomicInteger(20);
    // don't touch the following!
    private static AtomicInteger                           maxPrem                          = new AtomicInteger(1);
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap               = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currAcc                          = null;
    private DownloadLink                                   currDownloadLink                 = null;

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    private void prepBR() {
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        /* 401 can happen when user enters invalid logindata */
        br.setAllowedResponseCodes(401);
        br.getHeaders().put("User-Agent", "JDownloader");
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        if (account.getUser().equals("") || account.getPass().equals("")) {
            /* Server returns 401 if you send empty fields (logindata) */
            accountInvalid();
        }
        final AccountInfo ac = new AccountInfo();
        prepBR();
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        ac.setUnlimitedTraffic();
        if (!loginAPI(account)) {
            if (br.containsHTML("<status>expired</status>")) {
                ac.setStatus("Account is expired!");
                ac.setExpired(true);
                account.setValid(false);
                return ac;
            }
            accountInvalid();
        }
        final String expire = br.getRegex("<expiration>(\\d+)</expiration>").getMatch(0);
        if (expire == null) {
            ac.setStatus("Account is invalid. Invalid or unsupported accounttype!");
            accountInvalid();
        }
        ac.setValidUntil(Long.parseLong(expire) * 1000l);

        // now let's get a list of all supported hosts:
        super.br.getPage("https://debriditalia.com/api.php?hosts");
        hosts = br.getRegex("\"([^<>\"]*?)\"").getColumn(0);
        final List<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
        addUnreportedHostsToList(supportedHosts);
        ac.setMultiHostSupport(this, supportedHosts);
        ac.setStatus("Premium account");
        return ac;
    }

    private void addUnreportedHostsToList(List<String> supportedHosts) {
        // DebridItalia supports more hosts than returned by API call 'https://debriditalia.com/api.php?hosts'
        // As long as they don't get reported correctly, add them here ...
        supportedHosts.add("filesmonster.com");
    }

    private void accountInvalid() throws PluginException {
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBR();
        setConstants(account, link);

        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }

        showMessage(link, "Generating link");
        String dllink = checkDirectLink(link, "debriditaliadirectlink");
        if (dllink == null) {
            String host_downloadlink = link.getDownloadURL();
            /* Workaround for serverside debriditalia bug. */
            if (link.getHost().equals("share-online.biz") && host_downloadlink.contains("https://")) {
                host_downloadlink = host_downloadlink.replace("https://", "http://");
            }
            final String encodedLink = Encoding.urlEncode(host_downloadlink);
            super.br.getPage("https://debriditalia.com/api.php?generate=on&u=" + Encoding.urlEncode(account.getUser()) + "&p=" + Encoding.urlEncode(account.getPass()) + "&link=" + encodedLink);
            /* Either server error or the host is broken (we have to find out by retrying) */
            if (br.containsHTML("ERROR: not_available")) {
                int timesFailed = link.getIntegerProperty("timesfaileddebriditalia_not_available", 0);
                int maxRetriesIfUnavailable = getPluginConfig().getIntegerProperty(MAX_RETRIES_UNAVAILABLE_PROPERTY, DEFAULT_MAX_RETRIES_UNAVAILABLE);
                if (timesFailed <= maxRetriesIfUnavailable) {
                    timesFailed++;
                    logger.fine("Hoster unavailable! Retry attempt " + timesFailed + " of " + maxRetriesIfUnavailable);
                    link.setProperty("timesfaileddebriditalia_not_available", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
                } else {
                    logger.fine("Hoster unavailable! Max. retry attempts reached!");
                    link.setProperty("timesfaileddebriditalia_not_available", Property.NULL);
                    tempUnavailableHoster(15 * 60 * 1000l);
                }

            } else if (br.containsHTML("ERROR: not_supported")) {
                logger.info("Current host is not supported");
                tempUnavailableHoster(3 * 60 * 60 * 1000l);
            }
            dllink = br.getRegex("(https?://(\\w+\\.)?debriditalia\\.com/dl/.+)").getMatch(0);
            if (dllink == null) {
                logger.info("debriditalia.com: Unknown error - final downloadlink is missing");
                int timesFailed = link.getIntegerProperty("timesfaileddebriditalia_unknownerror_dllink_missing", 0);
                if (timesFailed <= 20) {
                    timesFailed++;
                    link.setProperty("timesfaileddebriditalia_unknownerror_dllink_missing", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
                } else {
                    logger.info("debriditalia.com: Unknown error - final downloadlink is missing -> Disabling current host");
                    link.setProperty("timesfaileddebriditalia_unknownerror_dllink_missing", Property.NULL);
                    tempUnavailableHoster(60 * 60 * 1000l);
                }
            }
        }

        int chunks = 0;
        if (link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false)) {
            chunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink.trim()), true, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            int maxRetriesOnDownloadError = getPluginConfig().getIntegerProperty(MAX_RETRIES_DL_ERROR_PROPERTY, DEFAULT_MAX_RETRIES_DL_ERROR);
            if (br.containsHTML("No htmlCode read")) {
                logger.info("debriditalia.com: Unknown download error");
                handleErrorRetries("unknowndlerror", maxRetriesOnDownloadError, 5 * 60 * 1000l);
            }
            logger.info("debriditalia.com: Unknown download error 2" + br.toString());
            handleErrorRetries("unknowndlerror2", maxRetriesOnDownloadError, 1 * 60 * 60 * 1000l);
        }
        // Directlinks can be used for up to 2 days
        link.setProperty("debriditaliadirectlink", dllink);
        try {
            // add a download slot
            controlPrem(+1);
            // start the dl

            try {
                if (!this.dl.startDownload()) {
                    try {
                        if (dl.externalDownloadStop()) {
                            return;
                        }
                    } catch (final Throwable e) {
                    }
                    /* unknown error, we disable multiple chunks */
                    if (link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false) == false) {
                        link.setProperty(DebridItaliaCom.NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            } catch (final PluginException e) {
                // New V2 chunk errorhandling
                /* unknown error, we disable multiple chunks */
                if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false) == false) {
                    link.setProperty(DebridItaliaCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }

                throw e;
            }
        } finally {
            // remove download slot
            controlPrem(-1);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private boolean loginAPI(final Account acc) throws IOException {
        super.br.getPage("https://debriditalia.com/api.php?check=on&u=" + Encoding.urlEncode(acc.getUser()) + "&p=" + Encoding.urlEncode(acc.getPass()));
        if (!br.containsHTML("<status>valid</status>") || br.getHttpConnection().getResponseCode() == 401) {
            return false;
        }
        return true;
    }

    private void tempUnavailableHoster(long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(this.currAcc);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(this.currAcc, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
        return true;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @SuppressWarnings("deprecation")
    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (downloadLink.getDownloadURL().contains("clz.to/")) {
            dllink = downloadLink.getDownloadURL();
        }
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetries(final String error, final int maxRetries, final long timeout) throws PluginException {
        int timesFailed = this.currDownloadLink.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        this.currDownloadLink.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            logger.fine("Unknown download error! Retry attempt " + timesFailed + " of " + maxRetries);
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            logger.fine("Unknown download error! Max. retry attempts reached!");
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(1 * 60 * 60 * 1000l);
        }
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlFree
     *            (+1|-1)
     */
    public synchronized void controlPrem(final int num) {
        logger.info("maxFree was = " + maxPrem.get());
        maxPrem.set(Math.min(Math.max(1, maxPrem.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxPrem.get());
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    protected void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), MAX_RETRIES_UNAVAILABLE_PROPERTY, JDL.L("plugins.hoster.debriditaliacom.maxRetriesUnavailable", "Maximum Retries If Unavailable")).setDefaultValue(DEFAULT_MAX_RETRIES_UNAVAILABLE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), MAX_RETRIES_DL_ERROR_PROPERTY, JDL.L("plugins.hoster.debriditaliacom.maxRetriesDlError", "Maximum Retries On Download Error")).setDefaultValue(DEFAULT_MAX_RETRIES_DL_ERROR));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }
}