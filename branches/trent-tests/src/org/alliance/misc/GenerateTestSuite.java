package org.alliance.misc;

import com.stendahls.nif.util.xmlserializer.SXML;
import com.stendahls.nif.util.xmlserializer.XMLSerializer;
import org.alliance.core.settings.Friend;
import org.alliance.core.settings.Internal;
import org.alliance.core.settings.My;
import org.alliance.core.settings.Plugin;
import org.alliance.core.settings.Server;
import org.alliance.core.settings.Settings;
import org.alliance.core.settings.Share;
import org.alliance.launchers.testsuite.Main;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Use this to generate settings for the testsuite Main class.
 * For even more useful tests, generate the plugin tests with: ant plugin-test-jar
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 10:10:40
 */
public class GenerateTestSuite {

    public static final String DEFAULT_TEST_SHARE_DIR = Main.TEST_SUITE_DIRNAME + "-share";

    private ArrayList<Friend> users = new ArrayList<Friend>();
    private HashMap<Friend, Settings> settings = new HashMap<Friend, Settings>();
    private HashSet<Friend> usedFriends = new HashSet<Friend>();
    private HashMap<Friend, Friend> parentFor = new HashMap<Friend, Friend>();
    private static final int N_FRIENDS_FACTOR = 2;
    private String shareDirectory;
    private static final boolean LINK_MORE_THEN_JUST_TO_MACIEK = true;

    public GenerateTestSuite(String shareDirectory) throws Exception {
        this.shareDirectory = shareDirectory;
        File shareDir = new File(shareDirectory);
        if (!new File(shareDirectory).exists()) {
            System.err.println("Directory with fake share test data does not exist: " + shareDirectory + ".");
            System.exit(1);
        }

        System.out.println("Cleaning up old testsuite...");
        File settingsFile = new File(Main.TEST_SUITE_DIRNAME + File.separator + "settings");
        settingsFile.mkdirs();
        new File(Main.TEST_SUITE_DIRNAME + File.separator + "logs").mkdirs();

        for (File f : settingsFile.listFiles()) {
            f.delete();
        }

        System.out.println("Setting up maciek...");
        Friend maciek = new Friend("maciek", "localhost", 333, 10333);
        Settings maciekSettings = createSettings(maciek);
        settings.put(maciek, maciekSettings);

        System.out.println("Creating users...");
        int port = 100;
        Random r = new Random();
        for (String nick : usernames) {
            if (port == 135) {
                port += 10;
            }
            Friend f = new Friend(nick, "localhost", r.nextInt(), port++);
            users.add(f);
        }

        System.out.println("Creating settings for users...");
        for (Friend f : users) {
            Settings s = createSettings(f);
            settings.put(f, s);
        }

        System.out.println("Linking friends to maciek...");
        for (int i = 0; i < N_FRIENDS_FACTOR / 2; i++) {
            Friend f = getNewFriend();
            link(maciek, f);
        }

        if (LINK_MORE_THEN_JUST_TO_MACIEK) {
            System.out.println("Linking friends to maciek friends (recursive)...");
            for (int i = 0; i < maciekSettings.getFriendlist().size(); i++) {
                Friend f = maciekSettings.getFriendlist().get(i);
                addFriends(f, 1);
            }
        }

        saveSettings("settings" + File.separator, maciekSettings);

        int n = 0;
        System.out.println("Saving settings...");
        for (Friend f : usedFriends) {
            saveSettings("settings" + File.separator, settings.get(f));
            n++;
        }
        System.out.println("Saved " + n + " users excl maciek.");
    }

    private void addFriends(Friend friend, int level) {
        if (level > 3) {
            return;
        }
        System.out.println("Adding friends for " + friend);
        for (int i = 0; i < N_FRIENDS_FACTOR; i++) {
            if (Math.random() > 0.85) {
                Friend f = getUsedFriend();
                if (f != null) {
                    System.out.println("  adding USED friend: " + f);
                    link(friend, f);
                }
            } else if (Math.random() > 0.7 || getFriendFromParentOf(friend) == null) {
                Friend f = getNewFriend();
                System.out.println("  adding random friend: " + f);
                link(friend, f);
                addFriends(f, level + 1);
            } else {
                Friend f = getFriendFromParentOf(friend);
                System.out.println("  adding known friend: " + f);
                link(friend, f);
            }
        }
    }

    private Friend getUsedFriend() {
        List l = Arrays.asList(usedFriends.toArray());
        if (l.size() == 0) {
            return null;
        }
        return (Friend) l.get((int) (l.size() * Math.random()));
    }

    private Friend getFriendFromParentOf(Friend friend) {
        Settings parent = settings.get(parentFor.get(friend));
        ArrayList<Friend> al = parent.getFriendlist();
        boolean ok = false;
        for (Friend f : al) {
            if (!settings.get(friend).hasFriend(f) && f != friend) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            return null;
        }
        for (;;) {
            Friend f = al.get((int) (Math.random() * al.size()));
            if (!settings.get(friend).hasFriend(f) && f != friend) {
                return f;
            }
        }
    }

    private void link(Friend f1, Friend f2) {
        if (f1 == f2) {
            return;
        }
        Settings s1 = settings.get(f1);
        Settings s2 = settings.get(f2);
        s1.addFriend(f2);
        s2.addFriend(f1);
        parentFor.put(f2, f1);
    }

    private Friend getNewFriend() {
        if (usedFriends.size() == users.size()) {
            System.err.println("All friends used!");
            return null;
        }
        for (;;) {
            Friend f = users.get((int) (Math.random() * (users.size())));
            if (!usedFriends.contains(f)) {
                usedFriends.add(f);
                return f;
            }
        }
    }

    private void saveSettings(String path, Settings settings) throws Exception {
        XMLSerializer s = new XMLSerializer();
        Document doc = s.serialize(settings);
        FileOutputStream out = new FileOutputStream(new File(Main.TEST_SUITE_DIRNAME + File.separator + "" + path + settings.getMy().getNickname() + ".xml"));
        out.write(SXML.toString(doc).getBytes());
        out.flush();
        out.close();
    }

    private Settings createSettings(Friend user) {
        Settings s = new Settings();
        s.setInternal(new Internal(120));
        s.setMy(new My(user.getGuid(), user.getNickname()));
        s.setServer(new Server(user.getPort()));
        String path = Main.TEST_SUITE_DIRNAME + File.separator + "data" + File.separator + user.getNickname() + File.separator;
        s.getInternal().setDatabasefile(path + "alliancedb");
        s.getInternal().setDownloadquefile(path + "downloads.dat");
        s.getInternal().setCorestatefile(path + "core.dat");

        s.getInternal().setDownloadfolder(path + "downloads");
        s.getInternal().setCachefolder(path + "cache");
        s.getInternal().setKeystorefilename(path + "me.ks");

        // if available, add the test plugin
        File testJar = new File("../build/testsuite/alliance-test-plugin.jar");
        if (testJar.exists()) {
            Plugin testPlugin = new Plugin();
            try {
                testPlugin.init(testJar);
                s.getPluginlist().add(testPlugin);
            } catch (IOException e) {
                // complain, but continue without it
                System.err.println("A test plugin jar exists, but got the following error while loading, so we'll ignore it.");
                e.printStackTrace();
            }
        }

        if (Math.random() > 0.5) {
            s.getMy().createChecksumAndSetInvitations((int) (4 * Math.random()));
        } else {
            s.getMy().setInvitations(0);
        }
        s.addShare(getRandomShare());
        return s;
    }

    private Share getRandomShare() {
        File files[] = new File(shareDirectory).listFiles();
        return new Share(files[((int) (Math.random() * files.length))].getPath());
    }















    /**
     *
     * @param args #1 is the share file directory, which will be created with test files if it doesn't already exist;
     * if no arguments are supplied, it will use/create the DEFAULT_TEST_SHARE_DIR
     *
     * @see GenerateTestSuite#DEFAULT_TEST_SHARE_DIR
     */
    public static void main(String[] args) throws Exception {
        String testShareDirName = null;
        // check for the given directory, or use the default
        if (args.length > 0) {
            testShareDirName = args[0];
        } else {
            testShareDirName = DEFAULT_TEST_SHARE_DIR;
            System.out.println("No testing share directory name supplied.  Will use '" + testShareDirName + "'.");
        }
        // create that directory and some random files if it doesn't exist
        File testShareDir = new File(testShareDirName);
        if (!testShareDir.exists()) {
            System.out.println("Test share directory not found.  Will create it with random files and directories.");
            if (!testShareDir.mkdir()) {
                throw new IOException("Unable to create test share directory " + testShareDirName + ".");
            }
            for (int i = 0; i < 20; i++) {
                createRandomFileTree(testShareDir, i);
            }
        }

        new GenerateTestSuite(testShareDirName);
    }

    /**
     * Create a random file under some random number of directories (all with fileNum in the name).
     *
     * Note that it will create at least one directory, because the share paths
     * in these settings are assumed to be directories within the main testsuite
     * share directory.
     *
     * @param dir directory under which to create the file
     * @param fileNum any number, used to create the file and directory name(s)
     */
    private static void createRandomFileTree(File dir, int fileNum) throws java.io.IOException {
        // make a directory
        File subDir = new File(dir.getPath() + File.separator + fileNum + File.separator);
        subDir.mkdir();
        
        // now either make a file here or recurse
        int depth = dir.getPath().split(File.separator).length;
        if (Math.random() * 15 < depth) {
            File thisFile = new File(dir.getPath() + File.separator +  fileNum + ".txt");
            thisFile.createNewFile();
            FileWriter writer = new FileWriter(thisFile);
            for (int i = 0; i < fileNum; i++) {
                writer.write(65 + i);
            }
            writer.close();
        } else {
            createRandomFileTree(subDir, fileNum);
        }
    }


    private String[] usernames = {
        "Abacuss", "Abbe", "Abbis", "Abbo", "Abbot", "Abby", "Absa", "Absy", "Ace", "Acka", "Acke", "Actra",
        "Addie", "Adian", "Adelie", "Adina", "Adonis", "Affa", "Affe", "Affi", "Africa", "Afrodite",
        "Agazzi", "Agnes", "Aikie", "Ajax", "Ajja", "Akeem", "Akela", "Akilles", "Akita", "Akka",
        "Bilbo", "Bill", "Billow", "Billy", "Bimba", "Bimbo", "Bimma", "Bimmer", "Bina", "Bingo",
        "Birka", "Biscaya", "Bison", "Bisse", "Bistro", "Bita", "Bittan", "Bitte", "Bizzy",
        "Blaise", "Blaisy", "Blenda", "Blissi", "Blitz", "Blizzard", "Blixten", "Blondie",
        "Blues", "Bluey", "Blumchen", "Blunder", "Blura", "Bobby", "Bobo", "Bodmin", "Bodo",
        "Bombax", "Bombibitt", "Bonbon", "Bonita", "Bonker", "Bonnie", "Bonzo", "Bonzai", "Boomer",
        "Boris", "Bosco", "Boss", "Bosse", "Bossman", "Bosso", "Bossy", "Botjena", "Botvid", "Boy",
        "Brahe", "Branco", "Brando", "Brandy", "Braska", "Brasse", "Bravo", "Brick", "Bricko",
        "Bronco", "Bronson", "Bruno", "Brusko", "Brutus", "Bubba", "Bubben", "Buddy", "Buggoo",
        "Bull", "Bulldoozer", "Bullen", "Bullet", "Bulten", "Bumbi", "Bunni", "Busbus", "Busen",
        "Dirre", "Disa", "Ditte", "Diva", "Divo", "Dixie", "Dixon", "Diza", "Dizzie", "Django", "Djingis",
        "Dodge", "Dogge", "Dogglas", "Dojjan", "Dojje", "Dolares", "Dollar", "Dolle", "Dolly", "Dominga",
        "Domino", "Don", "Donald", "Don", "Juan", "de", "Marco", "Don-Qijote", "Donna", "Donny",
        "Doris", "Dozer", "Draco", "Drago", "Droopy", "Drulle", "Drutten", "Duchess", "Duff", "Dumbo",
        "Effie", "Ego", "Egon", "Eiro", "Eisy", "Eja", "Ekstra", "Elektra", "Elga", "Elin", "Eliza", "Elke", "Ella",
        "Elli", "Elliot", "Elroy", "Elsa", "Elton", "Elva", "Elvira", "Elvis", "Embla", "Emil", "Emilia", "Emelie",
        "Emma", "Empress", "Enigma", "Enok", "Enzo", "Enya", "Epsylon", "Ernie", "Eroll", "Eros", "Esco", "Eski",
        "Esset", "Esso", "Essy", "Ester", "Ettan", "Evelina", "Evita", "Ewert", "Exa", "Exigor", "Exxet", "Exxon",
        "Fancy", "Fandango", "Fang", "Fangera", "Fanny", "Fanta", "Farao", "Fargo", "Fando", "Farris",
        "Fatima", "Fattzi", "Fatzer", "Faust", "Faxe", "Fazeth", "Faxxa", "Fazita", "Fedor", "Felicia",
        "Femman", "Fender", "Fenix", "Fernando", "Ferossa", "Ferox", "Ferrari", "Ferro", "Ferus",
        "Fetzo", "Fia", "Fidde", "Fiddeli", "Fido", "Fidoman", "Fiffi", "Figaro", "Fighter", "Filippa",
        "Filur", "Fimla", "Fina", "Findus", "Fingal", "Fining", "Finn", "Fiona", "Fira", "Fisan", "Fisen",
        "Fizzy", "Fjodor", "Flagan", "Flash", "Flax", "Flaxa", "Flickan", "Flingan", "Flinta", "Flipp",
        "Flora", "Flex", "Flipper", "Florry", "Floyd", "Flubber", "Fluff", "Fluffy", "Flux", "Flying-Flip",
        "Focus", "Folke", "Fonda", "Fonz", "Fonzie", "Foppa", "Fotti", "Fox", "Foxy", "Fozzie",
        "Franzesco", "Frasse", "Fredde", "Freddy", "Frej", "Freja", "Frejser", "Freud", "Frida",
        "Fritiof", "Fritte", "Fritz", "Frodo", "Frost", "Froy", "Froyd", "Froding", "Fuling", "Fuska",
        "F", "Fabbe", "Fabian", "Fabiola", "Faija", "Fairy", "Fajjen", "Fajt", "Falco", "Falcon", "Falstaff",
        "Lambada", "Lamborghini", "Lana", "Lapp-lisa", "Lara", "Larionov", "Larizza", "Larry",
        "Lassemaja", "Lassie", "Lasso", "Lattjo", "Lavinia", "Lazio", "Lazy", "Layla", "Lea",
        "Lego", "Leia", "Lejki", "Lenita", "Lennox", "Lenny", "Lenzi", "Leo", "Leon", "Leonard", "Lera",
        "Lester", "Leva", "Lewis", "Li", "Lia", "Libro", "Lilja", "Lillan", "Lillen", "Lilleman",
        "Lillis", "Lilly", "Limbo", "Lime", "Limpan", "Lina", "Lingo", "Lingon", "Linn", "Linne" +
        "", "Linus", "Linux",
        "Lipton", "Lisa", "Lisen", "Lista", "LitoLiz", "Liza", "Lize", "Lizton", "Lizze", "Lizzy", "Lobo",
        "Loke", "Lola", "Lolla", "Lollipop", "Lolita", "Lona", "Loppan", "Loranga", "Lord", "Lorry", "Lottie",
        "Louie", "Lova", "Love", "Lovis", "Lovisa", "Lowe", "Lucia", "Lucifer", "Lucinda", "Lucky", "Ludde",
        "Ludors", "Ludwig", "Luffis-Dino", "Lufsen", "Lufsi", "Luke", "Lukas", "Luki", "Luna", "Luni",
        "Lurvas", "Lurven", "Lussan", "Lusse", "Lusta", "Lutzie", "Luzette", "Lydia", "Lyssa", "L\u00e4ckra",
        "Zamba", "Zambexius", "Zambezi", "Zam-bo", "Zamira", "Zanderega", "Zandor", "Zane", "Zanja",
        "Zap", "Zappa", "Zaron", "Zarro", "Zax", "Zazza", "Zazzy", "Zeb", "Zefalus", "Zeina", "Zeke", "Zelda", "Zenta",
        "Zenthi", "Zephyr", "Zepp", "Zeppo", "Zera", "Zero", "Zesco", "Zeus", "Zhaan", "Ziara", "Zicki", "Zico",
        "Zilla", "Zimba", "Zingo", "Zinko", "Zinou", "Zip", "Zippa", "Zipper", "Ziri", "Zirro", "Zita", "Zockan",
        "Zombie", "Zombra", "Zoom", "Zorayah", "Zorba", "Zorina", "Zoritha", "Zorrie", "Zoy", "Zoya", "Zorro", "Zumo"
    };
}
