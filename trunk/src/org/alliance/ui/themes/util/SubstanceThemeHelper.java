package org.alliance.ui.themes.util;

import com.stendahls.XUI.elements.button;
import com.stendahls.XUI.elements.panel;
import com.stendahls.nif.ui.mdi.infonodemdi.theme.InfoDefaultTheme;
import org.alliance.ui.themes.AllianceTheme;
import org.pushingpixels.substance.api.DecorationAreaType;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;

import java.awt.Color;
import java.util.Collection;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import sun.swing.SwingUtilities2;

/**
 *
 * @author Bastvera
 */
public class SubstanceThemeHelper {

    private static boolean CUSTOM_SKIN = false;
    private static boolean SUBSTANCE_IN_USE = false;

    private SubstanceThemeHelper() {
    }

    public static void setComponentToGeneralArea(JComponent comp) {
        if (!SUBSTANCE_IN_USE) {
            return;
        }
        if (CUSTOM_SKIN) {
            SubstanceLookAndFeel.setDecorationType(comp, DecorationAreaType.GENERAL);
        }
        setColorization(comp, new Double(0.0));
    }

    public static void setComponentToToolbarArea(JComponent comp) {
        if (!SUBSTANCE_IN_USE) {
            return;
        }
        if (CUSTOM_SKIN) {
            SubstanceLookAndFeel.setDecorationType(comp, DecorationAreaType.TOOLBAR);
        }
        setColorization(comp, new Double(0.0));
    }

    public static void setPanelsToFooterArea(Collection c) {
        if (!SUBSTANCE_IN_USE) {
            return;
        }
        for (Object o : c) {
            if (o instanceof panel) {
                panel p = (panel) o;
                JComponent comp = (JComponent) p.getComponent();
                if (CUSTOM_SKIN) {
                    SubstanceLookAndFeel.setDecorationType(comp, DecorationAreaType.FOOTER);
                }
                setColorization(comp, new Double(0.0));
            }
        }
    }

    public static void setButtonsToGeneralArea(Collection c) {
        if (!SUBSTANCE_IN_USE) {
            return;
        }
        for (Object o : c) {
            if (o instanceof button) {
                button b = (button) o;
                JComponent comp = (JComponent) b.getComponent();
                if (CUSTOM_SKIN) {
                    SubstanceLookAndFeel.setDecorationType(comp, DecorationAreaType.GENERAL);
                }
                setColorization(comp, new Double(0.0));
            }
        }
    }

    public static void setColorization(Object o, double factor) {
        if (!SUBSTANCE_IN_USE) {
            return;
        }
        if (o instanceof JComponent) {
            ((JComponent) o).putClientProperty(SubstanceLookAndFeel.COLORIZATION_FACTOR, factor);
        }
    }

    public static void flatButton(JComponent comp) {
        if (!SUBSTANCE_IN_USE) {
            return;
        }
        ((JButton) comp).putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, true);
    }

    public static void setSubstanceTheme(String theme) throws ClassNotFoundException {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        //Dirty hack to support ClearType in InfoNodeMdi and TitledBorders
        final boolean lafCond = SwingUtilities2.isLocalDisplay();
        Object aaTextInfo = SwingUtilities2.AATextInfo.getAATextInfo(lafCond);
        UIManager.getDefaults().put(SwingUtilities2.AA_TEXT_PROPERTY_KEY, aaTextInfo);
        if (theme.equals("Alliance")) {
            CUSTOM_SKIN = true;
            SubstanceLookAndFeel.setSkin(new AllianceTheme());
            InfoDefaultTheme.setLightColor(new Color(227, 226, 230));
            InfoDefaultTheme.setDarkColor(new Color(220, 219, 224));
            InfoDefaultTheme.setBGColor(new Color(253, 253, 253));
            InfoDefaultTheme.setBorderColor(new Color(250, 250, 250));
        } else {
            theme = theme.replace(" ", "") + "Skin";
            Class.forName("org.pushingpixels.substance.api.skin." + theme);
            SubstanceLookAndFeel.setSkin("org.pushingpixels.substance.api.skin." + theme);
        }
        SUBSTANCE_IN_USE = true;
    }

    public static boolean isSubstanceInUse() {
        return SUBSTANCE_IN_USE;
    }
}
