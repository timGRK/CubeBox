package com.cube.event;

public enum EventEnum {

    /**
     * 连接验证
     */
    ONE((short) 1),
    /**
     * 注册版本号
     */
    TWO((short) 2),
    /**
     * 云->LED
     */
    THREE((short) 3),
    /**
     * LED->云
     */
    FOUR((short) 4),
    /**
     * 心跳
     */
    FIVE((short) 5),
    /**
     * OTA返回
     */
    SIX((short) 6),
    /**
     * Rom升级
     */
    SEVEN((short) 7);

    private short v;

    private EventEnum(short v) {

        this.v = v;
    }

    public short getVal() {
        return v;
    }

    public static EventEnum valuesOf(short e) {
        EventEnum[] vs = EventEnum.values();
        if (vs == null || vs.length == 0) {
            return null;
        }
        for (EventEnum event : vs) {
            if (event.getVal() == e) {
                return event;
            }
        }
        return null;
    }

}
