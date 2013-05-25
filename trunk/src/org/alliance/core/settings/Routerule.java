package org.alliance.core.settings;

import java.io.Serializable;

public class Routerule implements Serializable {

    public final static Integer ALLOW = 1;
    public final static Integer DENY = 0;
    //Weather it is a allow or a deny rule
    private Integer ruletype;
    private Integer address;
    private String humanreadable;
    //Subnet mask for rule
    private Integer mask;

    public Routerule() {
    }

    public Routerule(String human) throws Exception {
        this.humanreadable = human;
        parseInput();
    }

    //Package methods done on purpose, to prevent it being written to xml file
    Integer getRuleType() {
        return this.ruletype;
    }

    Integer getAddress() {
        return this.address;
    }

    Integer getMask() {
        return this.mask;
    }

    public String getHumanreadable() {
        return humanreadable;
    }

    public void setHumanreadable(String human) throws Exception {
        humanreadable = human;
        parseInput();
    }

    @Override
    public String toString() {
        return humanreadable;
    }

    private void parseInput() throws Exception {
        byte address[] = new byte[4];

        if (humanreadable.charAt(0) == 'A') {
            ruletype = Routerule.ALLOW;
        } else {
            ruletype = Routerule.DENY;
        }
        String human_copy = humanreadable.substring(humanreadable.lastIndexOf(' ') + 1);
        Integer temp;
        for (int i = 0; i < 4; i++) {
            int divider = human_copy.indexOf('.');
            if (divider == -1) {
                divider = human_copy.indexOf('/');
            }
            try {
                temp = Integer.parseInt(human_copy.substring(0, divider));
                if (temp < 0 || temp > 255) {
                    throw new Exception("Invalid IP entered");
                } else {
                    address[i] = temp.byteValue();
                }
                human_copy = human_copy.substring(divider + 1);
            } catch (NumberFormatException e) {
                return;
            }
        }
        mask = Integer.parseInt(human_copy);
        if (mask > 32 || mask < 0) {
            throw new Exception("Invalid Subnet Mask");
        }
        makeArrayInt(address);

    }

    private void makeArrayInt(byte[] addr) {
        int value = 0;
        int shiftval = 0;
        for (int i = addr.length - 1; i >= 0; i--) {
            value = value + (addr[i] << shiftval);
            shiftval += 8;
        }
        address = value;
    }
}
