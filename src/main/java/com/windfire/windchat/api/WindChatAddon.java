package com.windfire.windchat.api;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.YetAnotherConfigLib;

import java.util.List;
import java.util.Optional;


public interface WindChatAddon {


    void onInitialize();


    default Optional<ConfigCategory> buildConfigCategory() {
        return Optional.empty();
    }


    default List<AddonTab> getAddonTabs() {
        return List.of();
    }


    default void onConfigSaved() {}


    String getAddonId();
}