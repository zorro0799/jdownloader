package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class Xirror extends PluginForDecrypt {

    final static String host             = "xirror.com";

    private String      version          = "2.0.0.0";

    private Pattern     patternSupported = getSupportPattern("http://[*]xirror.com/spread/[+]/[*]");

    public Xirror() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
    }

    @Override
    public String getCoder() {
        return "Botzi";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Xirror.com-2.0.0.";
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			int count = 0;
    			
    			// Anzahl der Links zählen
    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true)) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_UPLOADED",true)) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_NETLOAD",true)) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_FILEFACTORY",true)) {
    				count++;
    			}
    			progress.setRange( count);
    			
    			// Links auslesen und umdrehen
    			Vector<Vector<String>> g = getAllSimpleMatches(reqinfo.getHtmlCode(), "popup(\"°\", \"°\")");
    			// Rapidshare Link speichern
    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true)) {
    				for( int i=0; i<g.size();i++){
    				    if(g.get(i).get(1).equalsIgnoreCase("rapidshare"))
    				    {
    				        decryptedLinks.add(rotate(g.get(i).get(0)));
        				progress.increase(1);
    				    }
    				}	
    			}
    			// Uploaded Link speichern
    			if((Boolean) this.getProperties().getProperty("USE_UPLOADED",true)) {
                    for( int i=0; i<g.size();i++){
                        if(g.get(i).get(1).equalsIgnoreCase("gulli"))
                        {
                            decryptedLinks.add(rotate(g.get(i).get(0)));
        				progress.increase(1);
                        }
                    }
    			}
    			// Netload Link speichern
    			if((Boolean) this.getProperties().getProperty("USE_NETLOAD",true)) {
                    for( int i=0; i<g.size();i++){
                        if(g.get(i).get(1).equalsIgnoreCase("netload"))
                        {
                            decryptedLinks.add(rotate(g.get(i).get(0)));
        				progress.increase(1);
                        }
                    }
    			}
    			// Filefactory Link speichern
    			if((Boolean) this.getProperties().getProperty("USE_FILEFACTORY",true)) {
                    for( int i=0; i<g.size();i++){
                        if(g.get(i).get(1).equalsIgnoreCase("filefactory"))
                        {
                            decryptedLinks.add(rotate(g.get(i).get(0)));
        				progress.increase(1);
                        }
                    }
    			}
    			    			
    			// Decrypt abschliessen
    			
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			 e.printStackTrace();
    		}
    	}
    	return null;
    }

    private void setConfigEelements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_UPLOADED", "Uploaded.to"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_NETLOAD", "Netload.in"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_FILEFACTORY", "Filefactory.com"));
        cfg.setDefaultValue(false);
    }

    private String rotate(String code) {
        String result = "";
        try {
            String url = URLDecoder.decode(code, "UTF-8");
            for (int i = 0; i < url.length(); i++) {
                result = result + url.charAt(url.length() - 1 - i);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}