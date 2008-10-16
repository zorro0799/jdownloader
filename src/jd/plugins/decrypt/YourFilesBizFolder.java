//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class YourFilesBizFolder extends PluginForDecrypt {

    public YourFilesBizFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("Ordner Passwort")) {
            String password = getUserInput(null, param);
            br.postPage(parameter, "act=login&password=" + password + "&login=Einloggen");
        }

        String links[] = br.getRegex("href='(http://yourfiles\\.biz/\\?d=.*?)'").getColumn(0);
        progress.setRange(links.length);
        for (String link : links) {
            decryptedLinks.add(createDownloadlink(link));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}