package org.alliance.core;

import com.stendahls.XUI.XUIElement;
import com.stendahls.ui.JHtmlLabel;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeSet;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;

/**
 *
 * @author Bastvera
 */
public class Language {

    private static TreeSet<String> AVAILABLE_LANGUAGES;
    private static ResourceBundle LANGUAGE_BUNDLE;
    private static final String LANGUAGE_PATH = "language/";
    private static final String PACKAGES_HEAD = "org.alliance.";
    private static final String BUNDLE_FILE = "alliance_";
    private static final String MISSING_TEXT = "%Missing translation!%";

    public Language(String language) throws MalformedURLException {
        URL[] url = {new File(LANGUAGE_PATH).toURI().toURL()};
        Locale locale = null;
        for (Locale l : Locale.getAvailableLocales()) {
            if (language.endsWith(l.toString())) {
                locale = l;
                break;
            }
        }
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        LANGUAGE_BUNDLE = ResourceBundle.getBundle("alliance", locale, new URLClassLoader(url));

        loadAvailableLanguages();
    }

    public static void loadAvailableLanguages() {
        AVAILABLE_LANGUAGES = new TreeSet<String>();
        File languageDir = new File(LANGUAGE_PATH);
        for (String filename : languageDir.list()) {
            if (filename.startsWith(BUNDLE_FILE) && filename.contains("_")) {
                String id = filename.substring(filename.indexOf("_") + 1, filename.lastIndexOf("."));
                Locale l = new Locale(id);
                if (!l.getDisplayLanguage().equalsIgnoreCase(id)) {
                    AVAILABLE_LANGUAGES.add(l.getDisplayLanguage() + " - " + id);
                }
            }
        }
    }

    public static TreeSet<String> getAllLanguages() {
        return AVAILABLE_LANGUAGES;
    }

    public static String getLocalizedString(Class<?> c, String key) {
        return getResource(c, key);
    }

    public static String getLocalizedString(Class<?> c, String key, String... params) {
        StringBuilder paramsString = new StringBuilder(getResource(c, key));
        for (String param : params) {
            int paramStart = paramsString.indexOf("<%");
            int paramEnd = paramsString.indexOf("%>") + 2;
            if (paramStart != -1 && paramEnd != -1) {
                paramsString = paramsString.replace(paramStart, paramEnd, param);
            }
        }
        return paramsString.toString();
    }

    public static void getLocalizedXUIToolbar(Class<?> c, ArrayList<JButton> buttons) {
        for (JButton b : buttons) {
            String tooltipText = b.getToolTipText();
            tooltipText = tooltipText.replace("%DES%", getResource(c, "toolbar", b.getActionCommand(), "description"));
            tooltipText = tooltipText.replace("%NAME%", getResource(c, "toolbar", b.getActionCommand(), "name"));
            b.setToolTipText(tooltipText);
        }
    }

    public static void translateXUIElements(Class<?> c, Collection coll) {
        for (Object o : coll) {
            XUIElement element = (XUIElement) o;
            JComponent comp = (JComponent) element.getComponent();
            if (comp.getToolTipText() != null && !comp.getToolTipText().trim().isEmpty()) {
                String text = getResource(c, "xui", element.getId(), "tooltip");
                if (text.indexOf("[/") != -1) {
                    text = text.replace("[br]", "<br>&nbsp;");
                    text = text.replace('[', '<');
                    text = text.replace(']', '>');
                    text = "<html>&nbsp;" + text + "&nbsp;</html>";
                }
                comp.setToolTipText(text);
            }
            if (comp.getBorder() instanceof CompoundBorder) {
                CompoundBorder b = (CompoundBorder) comp.getBorder();
                if (b.getOutsideBorder() instanceof TitledBorder) {
                    TitledBorder border = (TitledBorder) b.getOutsideBorder();
                    if (border != null && !border.getTitle().trim().isEmpty()) {
                        border.setTitle(getResource(c, "xui", element.getId(), "border"));
                    }
                }
            } else if (comp.getBorder() instanceof TitledBorder) {
                TitledBorder border = (TitledBorder) comp.getBorder();
                if (border != null && !border.getTitle().trim().isEmpty()) {
                    border.setTitle(getResource(c, "xui", element.getId(), "border"));
                }
            }
            if (comp instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) comp;
                if (button.getText() != null && !button.getText().trim().isEmpty()) {
                    button.setText(getResource(c, "xui", element.getId()));
                }
                continue;
            }
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getText() != null && !label.getText().trim().isEmpty()) {
                    label.setText(getResource(c, "xui", element.getId()));
                }
                continue;
            }
            if (comp instanceof JHtmlLabel) {
                JHtmlLabel label = (JHtmlLabel) comp;
                if (label.getText() != null && !label.getText().trim().isEmpty()) {
                    label.setText(getResource(c, "xui", element.getId()));
                }
                continue;
            }
            if (comp instanceof JTextArea) {
                JTextArea area = (JTextArea) comp;
                if (area.getText() != null && !area.getText().trim().isEmpty()) {
                    area.setText(getResource(c, "xui", element.getId()));
                }
                continue;
            }
        }
    }

    private static String getResource(Class<?> c, String... strings) {
        StringBuilder sb = new StringBuilder();
        sb.append(getKeyHeader(c));
        sb.append(".");
        for (String s : strings) {
            sb.append(s);
            sb.append(".");
        }
        sb.deleteCharAt(sb.length() - 1);
        String localized = null;
        try {
            try {
                localized = new String(LANGUAGE_BUNDLE.getString(sb.toString()).getBytes("ISO-8859-1"), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                localized = LANGUAGE_BUNDLE.getString(sb.toString());
            }
        } catch (MissingResourceException e) {
            //Test if getClass() is invoked by inherited methods from extended class that belongs to alliance package
            if (c.getSuperclass() != null && c.getSuperclass().getName().startsWith(PACKAGES_HEAD)) {
                localized = getResource(c.getSuperclass(), strings);
            }
        }
        if (localized == null) {
            // System.out.println(sb.toString());
            return MISSING_TEXT;
        } else {
            return localized;
        }
    }

    private static String getKeyHeader(Class<?> c) {
        String className = c.getCanonicalName();
        if (className == null) {
            className = c.getName().replaceAll("\\$\\d*", "");
        }
        return className.substring(PACKAGES_HEAD.length());
    }
}
