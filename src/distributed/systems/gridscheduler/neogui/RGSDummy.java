package distributed.systems.gridscheduler.neogui;

/**
 * DS:distributed.systems.gridscheduler.neogui.RGSDummy
 * Written by Glenn. Created on 2017-12-06 at 19:00.
 */
public class RGSDummy implements RGSStatusFrameData {

    private String name;

    public RGSDummy(String name) {
        this.name = name;
    }

    @Override
    public String getNameValue() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
