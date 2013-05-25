package org.alliance.ui.windows.options;

import com.stendahls.XUI.XUI;
import com.stendahls.XUI.XUIDialog;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuListener;

/**
 *
 * @author Bastvera
 */
public class GeneralTab extends XUIDialog implements TabHelper {

    private JPanel tab;
    private UISubsystem ui;
    private JComboBox language;
    private JComboBox globalfont;
    private JComboBox chatfont;
    private final static String FONT_SIZES[] = {"9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "20",
        "22", "24", "26", "28", "30", "32", "34", "36", "38", "40", "44", "48", "56", "64", "72"};
    private final static String[] OPTIONS = {
        "my.nickname", "internal.guiskin", "internal.language",
        "internal.enablesupportfornonenglishcharacters", "internal.showpublicchatmessagesintray",
        "internal.showprivatechatmessagesintray", "internal.showsystemmessagesintray",
        "internal.globalfont", "internal.chatfont", "internal.globalsize", "internal.chatsize"};

    public GeneralTab(String loading) {
        tab = new JPanel();
        tab.add(new JLabel(loading));
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));
    }

    public GeneralTab(final UISubsystem ui) throws Exception {
        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionstabs/generaltab.xui.xml"));
        this.ui = ui;

        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        tab = (JPanel) xui.getComponent("generaltab");
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));

        PopupMenuListener fontPopup = new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                fillFonts(e.getSource());
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        };

        globalfont = (JComboBox) xui.getComponent("internal.globalfont");
        chatfont = (JComboBox) xui.getComponent("internal.chatfont");
        globalfont.addPopupMenuListener(fontPopup);
        chatfont.addPopupMenuListener(fontPopup);

        language = (JComboBox) xui.getComponent("internal.language");

        fillLanguage();
        fillFontSizes();
    }

    private void fillLanguage() {
        for (String lang : Language.getAllLanguages()) {
            language.addItem(lang);
            if (lang.endsWith("en")) {
                language.setSelectedItem(lang);
            }
        }
    }

    private void fillFonts(Object source) {
        final JComboBox fontBox = (JComboBox) source;
        if (fontBox.getItemCount() == 1) {
            fontBox.addItem(Language.getLocalizedString(getClass(), "fontload"));
            final Object selectedGlobal = globalfont.getSelectedItem();
            final Object selectedChat = chatfont.getSelectedItem();
            fontBox.removeItemAt(0);
            fontBox.setSelectedIndex(0);
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    chatfont.removeAllItems();
                    globalfont.removeAllItems();
                    GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    Font[] fonts = e.getAllFonts();
                    for (Font font : fonts) {
                        globalfont.addItem(font.getFontName());
                        chatfont.addItem(font.getFontName());
                    }
                    globalfont.setSelectedItem(selectedGlobal);
                    chatfont.setSelectedItem(selectedChat);
                    fontBox.hidePopup();
                    fontBox.showPopup();
                }
            });
        }
    }

    private void fillFontSizes() {
        JComboBox globalsize = (JComboBox) xui.getComponent("internal.globalsize");
        JComboBox chatsize = (JComboBox) xui.getComponent("internal.chatsize");
        for (int i = 0; i < FONT_SIZES.length; i++) {
            globalsize.addItem(FONT_SIZES[i]);
            chatsize.addItem(FONT_SIZES[i]);
        }
    }

    @Override
    public JPanel getTab() {
        return tab;
    }

    @Override
    public String[] getOptions() {
        return OPTIONS;
    }

    @Override
    public XUI getXUI() {
        return super.getXUI();
    }

    @Override
    public boolean isAllowedEmpty(String option) {
        return false;
    }

    @Override
    public String getOverridedSettingValue(String option, String value) {
        if (option.equals("internal.language")) {
            for (int i = 0; i < language.getItemCount(); i++) {
                if (language.getItemAt(i).toString().endsWith(value)) {
                    return Integer.toString(i);
                }
            }
        }
        if (option.equals("internal.globalfont")) {
            if (value.isEmpty()) {
                globalfont.addItem(tab.getFont().getFontName());
                return tab.getFont().getFontName();
            }
            globalfont.addItem(value);
        }
        if (option.equals("internal.chatfont")) {
            if (value.isEmpty()) {
                chatfont.addItem(tab.getFont().getFontName());
                return tab.getFont().getFontName();
            }
            chatfont.addItem(value);
        }
        if (option.equals("internal.globalsize") || option.equals("internal.chatsize")) {
            int i = 0;
            while (i < FONT_SIZES.length) {
                if (FONT_SIZES[i].equals(value)) {
                    return String.valueOf(i);
                }
                i++;
            }
        }
        return value;
    }

    @Override
    public Object getOverridedComponentValue(String option, Object value) {
        if (value == null || value.toString().isEmpty()) {
            if (!isAllowedEmpty(option)) {
                return null;
            }
        }
        if (option.equals("internal.language")) {
            String lang = ((JComboBox) xui.getXUIComponent(option).getComponent()).getSelectedItem().toString();
            lang = lang.substring(lang.lastIndexOf("-") + 2);
            return lang;
        }
        if (option.equals("internal.guiskin") || option.equals("internal.globalfont") || option.equals("internal.chatfont")) {
            return (((JComboBox) xui.getXUIComponent(option).getComponent()).getSelectedItem());
        }
        if (option.equals("internal.globalsize") || option.equals("internal.chatsize")) {
            return FONT_SIZES[Integer.parseInt(value.toString())];
        }
        if (option.equals("my.nickname")) {
            String nickname = value.toString();
            nickname = nickname.replace("<", "").replace(">", "");
            if (nickname.trim().isEmpty()) {
                return null;
            }
            ui.getCore().getFriendManager().getMe().setNickname(nickname);
            if (ui.getNodeTreeModel(false) != null) {
                ui.getNodeTreeModel(false).signalNodeChanged(
                        ui.getCore().getFriendManager().getMe());
            }
        }
        return value;
    }

    @Override
    public void postOperation() {
    }
}
