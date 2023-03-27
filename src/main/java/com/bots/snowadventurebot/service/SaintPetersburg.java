package com.bots.snowadventurebot.service;

public enum SaintPetersburg {
    RED_LIKE("Красное озеро"),
    IGORA("Игора"),
    GOLDEN_VALLEY("Золотая долина"),
    SNEGNIY("Снежный"),
    PUKHTOLOVA_GORA("Пухтолова гора"),
    KAVGOLOV("Кавголово"),
    OHTA_PARK("Охта парк"),
    UKKI("Юкки"),
    TUUATARI_PARK("Туутари парк"),
    SEVERNIY_SKLON("Северный склон"),
    AZURE_LAKES("Лазурные озера");


    String resort;
    SaintPetersburg(String resort) {
        this.resort = resort;
    }

    @Override
    public String toString() {
        return resort;
    }
}
