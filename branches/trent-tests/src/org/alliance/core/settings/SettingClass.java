package org.alliance.core.settings;

import com.stendahls.util.TextUtils;

import java.lang.reflect.Method;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-22
 * Time: 09:51:47
 */
public class SettingClass {

    public Object getValue(String k) throws Exception {
        Class c = getClass();
        Method m = c.getMethod("get" + TextUtils.upperCaseFirstLetter(k));
        return m.invoke(this);
    }

    public void setValue(String name, Object val) throws Exception {
        Class c = getClass();
        Method m;
        try {
            m = c.getMethod("set" + TextUtils.upperCaseFirstLetter(name), val.getClass());
        } catch (NoSuchMethodException e) {
            val = Integer.parseInt(val.toString());
            m = c.getMethod("set" + TextUtils.upperCaseFirstLetter(name), val.getClass());
        }
        m.invoke(this, val);
    }
}
