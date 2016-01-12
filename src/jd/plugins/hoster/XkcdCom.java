//    jDownloader - Downloadmanager
//    Copyright (C) 2016  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.net.URL;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision: 25467 $", interfaceVersion = 3, names = { "xkcd.com" }, urls = { "http://(?:www\\.)?xkcd\\.com/(\\d+)/" }, flags = { 2 })
public class XkcdCom extends PluginForHost {

    public XkcdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://xkcd.com";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String dllink = null;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        // lets set a linkid
        downloadLink.setLinkID(getHost() + "://" + new Regex(downloadLink.getDownloadURL(), this.getSupportedLinks()).getMatch(0));
        // get the page source
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        // find the desired 0:dllink, 1:long filename, 2:short filename
        final String regex = "<div id=\"comic\">\\s*<img src=\"([^\"]+)\" title=\"([^\"]+)\" alt=\"([^\"]+)\"";
        String filename = br.getRegex(regex).getMatch(1);
        // find the img tag
        dllink = br.getRegex(regex).getMatch(0);
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // bug in correcturl
        dllink = dllink.startsWith("//") ? "http:" + dllink : dllink;
        // set filename
        filename = filename.trim();
        final String tempfilename = getFileNameFromURL(new URL(dllink));
        String ext = tempfilename.substring(tempfilename.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            ext = ".png";
        }
        // cleanup extra . at teh end of filename
        while (filename.endsWith(".")) {
            filename = filename.substring(0, filename.length() - 1);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);

        // do a request to find out the filesize? this can slow down tasks as its another request.
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (con.isOK()) {
                downloadLink.setVerifiedFileSize(con.getLongContentLength());
                // incase redirect after advertised img link.
                dllink = br.getURL();
                return AvailableStatus.TRUE;
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } catch (final Throwable e) {
            return AvailableStatus.UNCHECKABLE;
        } finally {
            try {
                /* make sure we close connection */
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}