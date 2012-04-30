package at.markmei.xeneo.plugin;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xeneo.core.activity.Activity;
import org.xeneo.core.activity.Object;
import org.xeneo.core.plugin.AbstractActivityPlugin;
import org.xeneo.core.plugin.PluginConfiguration;

/*
 * @author Markus Meingassner
 *
 */
public class DropboxActivityPlugin extends AbstractActivityPlugin {

    private String folder;
    private String url;
    private String activityUri;

    
    public void init() {

        PluginConfiguration pc = this.getPluginConfiguration();
        Properties ps = pc.getProperties();

        ps.setProperty("url", "C:/Users/XENEO/Documents/NetBeansProjects/DropboxActivityPlugin/src/test/resources/testDropboxActivities.xml");

        if (ps.containsKey("folder")) {
            folder = ps.getProperty("folder");
        }

        if (ps.containsKey("url")) {
                url = ps.getProperty("url");
        }

    }

   
    public void run() {
        try {
            List<Activity> acts = getActivities();

            Iterator<Activity> it = acts.iterator();
            while (it.hasNext()) {
                Activity a = it.next();
                if (!true) {
                    addActivity(a);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DropboxActivityPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(DropboxActivityPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FeedException ex) {
            Logger.getLogger(DropboxActivityPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DropboxActivityPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public List<Activity> getActivities() throws FileNotFoundException, IllegalArgumentException, FeedException, IOException {

        List<Activity> acts = new ArrayList<Activity>();

        FileInputStream fis = new FileInputStream(url);

        SyndFeedInput input = new SyndFeedInput();
        SyndFeed sf = input.build(new XmlReader(fis));
        List entries = sf.getEntries();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            SyndEntry entry = (SyndEntry) it.next();
            assembleActivity(entry);
        }

        return acts;

    }

    public Activity assembleActivity(SyndEntry se) {
        Activity a = new Activity();
        a.setActivityURI(se.getUri());
        a.setCreationDate(se.getUpdatedDate());
        String document = se.getDescription().getValue().substring(1);
        String input = document;
        String action = "";
        String actor = "";
        String object = "";
        String objectURI = "http://www.drobox.com";
        String objectType = "";
        String target = "";
        String targetURI = "http://www.dropbox.com";
        String targetType = "Folder";
        if (document.startsWith("In") == true) {
            //set targetURI
            String input_new = document.substring(12);
            int targetendindex = input_new.indexOf('\'');
            targetURI = targetURI.concat(input_new.substring(0, targetendindex));

            //set target
            int targetbeginindex = targetendindex + 2;
            String input_target = input_new.substring(targetbeginindex);
            targetendindex = input_target.indexOf("/a") - 1;
            target = input_target.substring(0, targetendindex);

            //set actor
            document = input_target.substring(input_target.indexOf(',') + 2);
            input_target = document;
            targetendindex = 0;
            while ((document.startsWith("deleted") | document.startsWith("added") | document.startsWith("renamed") | document.startsWith("removed") | document.startsWith("moved")) == false) {
                targetendindex++;
                document = input_target.substring(targetendindex);
            }
            actor = input_target.substring(0, targetendindex - 1);

            //set actor
            if (document.startsWith("deleted") | document.startsWith("removed")) {
                action = "DELETE";
            }
            if (document.startsWith("added")) {
                action = "ADD";
            }
            if (document.startsWith("moved") | document.startsWith("renamed")) {
                action = "UPDATE";
            }

            //set object uri and object type
            input_target = document.substring(document.indexOf('\'') + 1);
            objectURI = objectURI.concat(input_target.substring(0, input_target.indexOf('\'')));
            if (objectURI.startsWith("http://www.drobox.comhttps")) {
                objectURI = objectURI.substring(21);
                objectType = "Document";
            } else {
                objectType = "Folder";
            }

            //set ONE object
            document = input_target.substring(input_target.indexOf("&#47;") + 5);
            input_target = document.substring(document.indexOf("&#47;") + 5);
            targetendindex = input_target.indexOf('\'');
            object = input_target.substring(0, targetendindex);

            //set more than one Object
            if (input_target.indexOf("&#47;") > -1) {
                objectType = objectType.concat("s");
                targetendindex = input_target.indexOf('\'');
                object = input_target.substring(input_target.indexOf("&#47;") + 5, targetendindex);
                document = input_target.substring(input_target.indexOf("a href='") + 9);
                targetendindex = document.indexOf('\'');
                objectURI = objectURI.concat(" and http://www.dropbox.com/" + document.substring(0, targetendindex));
            }
            //end if - join folder or invite to folder or leave folder
        } else {
            //set Actor and Verb
            int index = 0;
            while ((document.startsWith("left") | document.startsWith("joined") | document.startsWith("invited")) == false) {
                index++;
                document = input.substring(index);
            }
            actor = input.substring(1, index - 1);
            if (document.startsWith("invited") == true) {
                //set action
                action = "INVITE";

                //set object, objectURI, objectType
                object = document.substring(8, document.indexOf("to the shared folder") - 1);
                objectURI = object;
                objectType = "email";

                //set targetURI
                input = document.substring(document.indexOf('"') + 1);
                targetURI = targetURI.concat(input.substring(0, input.indexOf('"')));

                //set target
                target = input.substring(input.indexOf('"') + 2, input.indexOf('\'') - 4);

                //set target type
                targetType = "folder";

            } else {
                //set action JOIN or LEAVE
                if (document.startsWith("joined")) {
                    action = "JOIN";
                }
                if (document.startsWith("left")) {
                    action = "LEAVE";
                }

                //set ObjectURI
                input = document.substring(document.indexOf('"') + 1);
                objectURI = objectURI.concat(input.substring(0, input.indexOf('"')));

                //set Object
                object = input.substring(input.indexOf('"') + 2, input.indexOf('\'') - 4);

                //no target
                target = "not available";
                targetURI = "not available";
                targetType = "not available";
            }
        }
        Object o = new Object();
        Object t = new Object();
        o.setObjectName(object);
        o.setObjectTypeURI(objectType);
        o.setObjectURI(objectURI);
        t.setObjectName(target);
        t.setObjectTypeURI(targetType);
        t.setObjectURI(targetURI);
        a.setActionURI(action);
        a.setActorURI(actor);
        a.setDescription(actor + " " + action + " " + object +" "+" " +target);
        a.setObject(o);
        a.setTarget(t);

        
        return a;
    }
}
