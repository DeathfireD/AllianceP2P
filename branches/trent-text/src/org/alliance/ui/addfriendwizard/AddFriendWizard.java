package org.alliance.ui.addfriendwizard;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.ui.JHtmlLabel;
import com.stendahls.ui.JWizard;
import org.alliance.core.node.Invitation;
import org.alliance.core.Language;
import org.alliance.ui.T;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.util.CutCopyPastePopup;
import org.alliance.ui.dialogs.OptionDialog;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-apr-13
 * Time: 14:54:04
 */
public class AddFriendWizard extends JWizard {

    public static final int STEP_INTRO = 0;
    public static final int STEP_INVITATION_LIMIT = 1;
    public static final int STEP_ENTER_INVITATION = 2;
    public static final int STEP_INCORRECT_INVITATION_CODE = 3;
    public static final int STEP_ATTEMPT_CONNECT = 4;
    public static final int STEP_CONNECTION_FAILED = 5;
    public static final int STEP_FORWARD_INVITATIONS = 6;
    public static final int STEP_FORWARD_INVITATIONS_COMPLETE = 7;
    public static final int STEP_CONNECTION_FAILED_FOR_FORWARD = 8;
    public static final int STEP_MANUAL_INVITE = 9;
    public static final int STEP_PORT_OPEN_TEST = 10;
    public static final int STEP_PORT_NOT_OPEN = 11;
    public static final int STEP_MANUAL_CONNECTION_OK = 12;
    private int selectedIntroButton;
    private int selectedLimitButton;
    private UISubsystem ui;
    private XUIDialog outerDialog;
    private JTextField dayLimit;
    private JTextField codeinput;
    private JTextArea invitationCode;
    private JScrollPane listScrollPane;
    private ArrayList<JRadioButton> radioButtons = new ArrayList<JRadioButton>();
    private Thread connectionThread;
    private ForwardInvitationNodesList forwardInvitationNodesList;
    private Integer invitationFromGuid;
    private static final String PORT_OPEN_TEST_URL = "http://maciek.tv/porttest?port=";

    public AddFriendWizard() throws Exception {
        setSuperTitle(Language.getLocalizedString(getClass(), "windowheader"));
    }

    @Override
    public void EVENT_cancel(ActionEvent e) throws Exception {
        Component c = getParent();
        while (!(c instanceof Window)) {
            c = c.getParent();
        }
        ((Window) c).setVisible(false);
        ((Window) c).dispose();
    }

    public void XUILayoutComplete(final UISubsystem ui, XUIDialog outerDialog) {
        this.ui = ui;
        this.outerDialog = outerDialog;
        innerXUI.setEventHandler(this);
        next.setEnabled(false);

        Language.translateXUIElements(getClass(), innerXUI.getXUIComponents());
        invitationCode = (JTextArea) innerXUI.getComponent("code");
        new CutCopyPastePopup(invitationCode);

        codeinput = (JTextField) innerXUI.getComponent("codeinput");
        new CutCopyPastePopup(codeinput);

        dayLimit = (JTextField) innerXUI.getComponent("daylimit");
        dayLimit.addKeyListener(new KeyAdapter() {

            String lastText;

            @Override
            public void keyReleased(KeyEvent e) {
                if (dayLimit.getText().isEmpty()) {
                    return;
                }
                try {
                    Integer.parseInt(dayLimit.getText());
                } catch (NumberFormatException ex) {
                    dayLimit.setText("7");
                }
            }
        });

        listScrollPane = (JScrollPane) innerXUI.getComponent("scrollpanel");

        radioButtons.add((JRadioButton) innerXUI.getComponent("radio1_1"));
        radioButtons.add((JRadioButton) innerXUI.getComponent("radio1_2"));
        radioButtons.add((JRadioButton) innerXUI.getComponent("radio1_3"));
        radioButtons.add((JRadioButton) innerXUI.getComponent("radio1_4"));
        radioButtons.add((JRadioButton) innerXUI.getComponent("radio2_1"));
        radioButtons.add((JRadioButton) innerXUI.getComponent("radio2_2"));
        radioButtons.add((JRadioButton) innerXUI.getComponent("radio2_3"));

        JHtmlLabel portclosed = (JHtmlLabel) innerXUI.getComponent("portclosed");
        portclosed.setText(Language.getLocalizedString(getClass(), "xui.portclosed",
                Integer.toString(ui.getCore().getSettings().getServer().getPort()),
                "[a href=http://www.portforward.com]http://www.portforward.com[/a]"));
        portclosed.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    ui.openURL(e.getDescription());
                }
            }
        });

        //disable looking for friends in secondary connections if we have no friends
        //or if we have no friends to forward invitations to  
        if (new ForwardInvitationNodesList.ForwardInvitationListModel(ui.getCore()).getSize() == 0
                || ui.getCore().getFriendManager().friends().isEmpty()) {
            innerXUI.getComponent("radio1_3").setEnabled(false);
        }

        final JHtmlLabel newcode = (JHtmlLabel) innerXUI.getComponent("newcode");
        newcode.setText(Language.getLocalizedString(getClass(), "xui.newcode",
                "[a href=.]" + Language.getLocalizedString(getClass(), "xui.newcodegen") + "[/a]"));
        newcode.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    EVENT_createnew(null);
                }
            }
        });
    }

    public static AddFriendWizard open(UISubsystem ui, int startAtStep) throws Exception {
        XUIDialog f = new XUIDialog(ui.getRl(), ui.getRl().getResourceStream("xui/addfriendwizard.xui.xml"), ui.getMainWindow());
        final AddFriendWizard wizard = (AddFriendWizard) f.getXUI().getComponent("wizard");
        SubstanceThemeHelper.setButtonsToGeneralArea(wizard.getXUIComponents());
        wizard.XUILayoutComplete(ui, f);
        if (startAtStep == STEP_FORWARD_INVITATIONS) {
            wizard.goToForwardInvitations();
        } else if (startAtStep == STEP_ATTEMPT_CONNECT) {
            wizard.goToAttemptConnect();
        } else if (startAtStep == STEP_PORT_OPEN_TEST) {
            wizard.goToPortTest();
        } else if (startAtStep != STEP_INTRO) {
            throw new Exception("No support for starting at step " + startAtStep);
        }
        return wizard;
    }

    private void goToEnterInvitation() {
        setStep(STEP_ENTER_INVITATION);
        codeinput.requestFocus();
    }

    private void goToInvitationLimit() {
        setStep(STEP_INVITATION_LIMIT);
        prev.setEnabled(true);
        next.setEnabled(false);
        cancel.setEnabled(true);
    }

    private void goToManualInvite() {
        setStep(STEP_MANUAL_INVITE);
        prev.setEnabled(true);
        next.setEnabled(false);
        cancel.setEnabled(true);
        cancel.setText(Language.getLocalizedString(getClass(), "finish"));
    }

    public void goToPortTest() {
        if (selectedIntroButton == 3 || ui.getCore().getSettings().getInternal().getSkipportcheck() > 0) {
            //Invitation will be made for LAN or user don't want to check port
            goToCreateInvitation();
            return;
        }
        setStep(STEP_PORT_OPEN_TEST);
        prev.setEnabled(false);
        next.setEnabled(false);
        cancel.setEnabled(false);

        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String result = getResponseFromURL(PORT_OPEN_TEST_URL + ui.getCore().getSettings().getServer().getPort());
                    if (T.t) {
                        T.info("Result from port test: " + result);
                    }
                    if ("OPEN".equals(result)) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                goToCreateInvitation();
                            }
                        });
                    } else if ("CLOSED".equals(result)) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                prev.setEnabled(true);
                                next.setEnabled(false);
                                cancel.setEnabled(true);
                                cancel.setText(Language.getLocalizedString(getClass(), "finish"));
                                setStep(STEP_PORT_NOT_OPEN);
                            }
                        });
                    } else {
                        if (T.t) {
                            T.error("Could not test if port is open: " + result);
                        }
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                goToCreateInvitation();
                            }
                        });
                    }
                } catch (Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
        t.start();
    }

    private String getResponseFromURL(String url) throws IOException {
        URLConnection c = new URL(url).openConnection();
        InputStream in = c.getInputStream();
        StringBuilder result = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = r.readLine()) != null) {
            result.append(line);
        }

        line = result.toString();
        return line;
    }

    private void goToCreateInvitation() {
        EVENT_createnew(null);
        goToManualInvite();
    }

    private void goToConnectionFailed() {
        next.setEnabled(false);
        if (invitationFromGuid != null) {
            setStep(STEP_CONNECTION_FAILED_FOR_FORWARD);
            next.setEnabled(false);
            prev.setEnabled(false);
            cancel.setText(Language.getLocalizedString(getClass(), "finish"));
        } else {
            setStep(STEP_CONNECTION_FAILED);
        }
    }

    private void goToConnectionOk() {
        setStep(STEP_MANUAL_CONNECTION_OK);
        next.setEnabled(false);
        prev.setEnabled(false);
    }

    @Override
    public void setStep(int i) {
        super.setStep(i);
        if (getSteptitle() != null && !getSteptitle().isEmpty()) {
            super.changeTitle(Language.getLocalizedString(getClass(), "xui." + getSteptitle().replace("%", "")));
        }
        resetAllRadioButtons();
    }

    private void resetAllRadioButtons() {
        for (JRadioButton b : radioButtons) {
            if (b != null) {
                b.setSelected(false);
            }
        }
    }

    @Override
    public void nextStep() {
        if (getStep() == STEP_INTRO) {
            if (selectedIntroButton == 0) {
                goToEnterInvitation();
            } else if (selectedIntroButton == 1 || selectedIntroButton == 3) {
                goToInvitationLimit();
            } else {
                goToForwardInvitations();
            }
        } else if (getStep() == STEP_INVITATION_LIMIT) {
            goToPortTest();
        } else if (getStep() == STEP_ENTER_INVITATION) {
            handleInvitationCode();
        } else if (getStep() == STEP_CONNECTION_FAILED) {
            goToPortTest();
        } else if (getStep() == STEP_FORWARD_INVITATIONS) {
            if (forwardInvitationNodesList != null) {
                forwardInvitationNodesList.forwardSelectedInvitations();
            }
            setStep(STEP_FORWARD_INVITATIONS_COMPLETE);
            next.setEnabled(false);
            cancel.setText(Language.getLocalizedString(getClass(), "finish"));
        } else {
            if (T.t) {
                T.ass(false, "Reached step in wizard that was unexcpected (" + getStep() + ")");
            }
        }
    }

    public void connectionWasSuccessful() {
        if (connectionThread != null) {
            connectionThread.interrupt();
        }
    }

    public void goToForwardInvitations() {
        connectionWasSuccessful();
        listScrollPane.setViewportView(forwardInvitationNodesList = new ForwardInvitationNodesList(ui, this));
        setStep(STEP_FORWARD_INVITATIONS);
        next.setEnabled(false);
        if (forwardInvitationNodesList.getModel().getSize() == 0) {
            getOuterDialog().dispose(); //we're done. Nothing to forward. Just close the wizard.
        }
    }

    private void handleInvitationCode() {
        String invitation = codeinput.getText().trim();
        if (invitation.length() == 0) {
            OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "nocode"));
        } else {
            try {
                ui.getCore().getInvitationManager().attemptToBecomeFriendWith(invitation.trim(), null);
                goToAttemptConnect();
            } catch (EOFException ex) {
                OptionDialog.showErrorDialog(ui.getMainWindow(), Language.getLocalizedString(getClass(), "shortcode"));
                goToEnterInvitation();
            } catch (Exception e) {
                ui.handleErrorInEventLoop(e);
                goToConnectionFailed();
            }
        }
    }

    public void goToAttemptConnect() {
        connectionThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(1000 * 20);
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            goToConnectionFailed();
                        }
                    });
                } catch (InterruptedException e) {
                    if (T.t) {
                        T.info("Looks like we connected succesfully.");
                    }
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            goToConnectionOk();
                        }
                    });
                }
            }
        });
        connectionThread.start();
        setStep(STEP_ATTEMPT_CONNECT);
        next.setEnabled(false);
        prev.setEnabled(false);
    }

    @Override
    public void prevStep() {
        if (getStep() == STEP_FORWARD_INVITATIONS) {
            setStep(STEP_INTRO);
        } else if (getStep() == STEP_ENTER_INVITATION) {
            setStep(STEP_INTRO);
        } else if (getStep() == STEP_CONNECTION_FAILED) {
            setStep(STEP_ENTER_INVITATION);
        } else if (getStep() == STEP_MANUAL_INVITE) {
            setStep(STEP_INTRO);
        } else if (getStep() == STEP_PORT_NOT_OPEN) {
            setStep(STEP_INTRO);
        } else if (getStep() == STEP_PORT_OPEN_TEST) {
            setStep(STEP_INTRO);
        } else {
            super.prevStep();
        }
    }

    public void EVENT_radio1_1(ActionEvent e) {
        selectedIntroButton = 0;
        next.setEnabled(true);
    }

    public void EVENT_radio1_2(ActionEvent e) {
        selectedIntroButton = 1;
        next.setEnabled(true);
    }

    public void EVENT_radio1_3(ActionEvent e) {
        selectedIntroButton = 2;
        next.setEnabled(true);
    }

    public void EVENT_radio1_4(ActionEvent e) {
        selectedIntroButton = 3;
        next.setEnabled(true);
    }

    public void EVENT_radio2_1(ActionEvent e) {
        selectedLimitButton = 1;
        next.setEnabled(true);
    }

    public void EVENT_radio2_2(ActionEvent e) {
        selectedLimitButton = 2;
        next.setEnabled(true);
    }

    public void EVENT_radio2_3(ActionEvent e) {
        selectedLimitButton = 3;
        next.setEnabled(true);
    }

    public void EVENT_createnew(ActionEvent e) {
        invitationCode.setText(Language.getLocalizedString(getClass(), "generatecode"));
        invitationCode.revalidate();
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    boolean forLan = selectedIntroButton == 3 ? true : false;
                    long timeValid = 0;
                    if (selectedLimitButton == 1) {
                        timeValid = -1;
                    } else if (selectedLimitButton == 2) {
                        timeValid = Integer.MAX_VALUE;
                        timeValid *= 1000;
                    } else if (selectedLimitButton == 3) {
                        if (dayLimit.getText().isEmpty()) {
                            timeValid = 7 * 24 * 60 * 60 * 1000;
                        } else {
                            timeValid = Long.parseLong(dayLimit.getText()) * 24 * 60 * 60 * 1000;
                        }
                    }

                    final Invitation i = ui.getCore().getInvitationManager().createInvitation(forLan, timeValid);
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            invitationCode.setText("");
                            invitationCode.append(Language.getLocalizedString(getClass(), "invline1"));
                            invitationCode.append("\n\n");
                            invitationCode.append(Language.getLocalizedString(getClass(), "invline2"));
                            invitationCode.append("\n");
                            invitationCode.append("http://www.alliancep2p.com/download\n\n");
                            invitationCode.append(Language.getLocalizedString(getClass(), "invline3"));
                            invitationCode.append("\n");
                            invitationCode.append(i.getCompleteInvitaitonString());
                            invitationCode.requestFocus();
                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    invitationCode.selectAll();
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
        t.start();
    }

    public void EVENT_selectall(ActionEvent e) {
        forwardInvitationNodesList.selectAll();
    }

    public void EVENT_selecttrusted(ActionEvent e) {
        forwardInvitationNodesList.selectTrusted();
    }

    public void EVENT_selectnone(ActionEvent e) {
        forwardInvitationNodesList.selectNone();
    }

    public XUIDialog getOuterDialog() {
        return outerDialog;
    }

    public void enableNext() {
        next.setEnabled(true);
    }

    public void setInvitationFromGuid(Integer invitationFromGuid) {
        this.invitationFromGuid = invitationFromGuid;
    }
}
