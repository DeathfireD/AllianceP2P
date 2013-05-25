package org.alliance.ui.windows.options;

import com.stendahls.XUI.XUI;
import javax.swing.JPanel;

/**
 *
 * @author Bastvera
 */
public interface TabHelper {   

    public JPanel getTab();

    public String[] getOptions();

    public XUI getXUI();

    public boolean isAllowedEmpty(String option);

    public String getOverridedSettingValue(String option, String value);

    public Object getOverridedComponentValue(String option, Object value);

    public void postOperation();
}
