package com.netease.cloud.lowcode.naslstorage.enums;

public enum  ConceptEnum {
    Module("Module", "模块"),
    Process("Process", "流程"),
    ProcessElement("ProcessElement", "流程元素"),
    View("View", "视图"),
    Logic("Logic", "逻辑"),
    Interface("Interface", "接口"),
    Entity("Entity", "实体"),
    Structure("Structure", "结构体"),
    StructureProperty("StructureProperty", "结构体属性"),
    Enum("Enum", "枚举");

    private String concept;
    private String note;

    ConceptEnum(String concept, String note) {
        this.concept = concept;
        this.note = note;
    }

    public static ConceptEnum from(String concept) {
        for (ConceptEnum conceptEnum : values()) {
            if (conceptEnum.concept.equalsIgnoreCase(concept)) {
                return conceptEnum;
            }
        }
        throw new RuntimeException();
    }
}
