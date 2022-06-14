package com.netease.cloud.lowcode.naslstorage.enums;

public enum ActionEnum {
    CREATE("create", "新建"),
    DELETE("delete", "删除"),
    UPDATE("update", "更新");

    private String action;
    private String note;

    ActionEnum(String action, String note) {
        this.action = action;
        this.note = note;
    }

    public static ActionEnum from(String action) {
        for (ActionEnum actionEnum : values()) {
            if (actionEnum.action.equalsIgnoreCase(action)) {
                return actionEnum;
            }
        }
        throw new RuntimeException();
    }

    public static Boolean validAction(String action) {
        for (ActionEnum actionEnum : values()) {
            if (actionEnum.action.equalsIgnoreCase(action)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
    }
}
