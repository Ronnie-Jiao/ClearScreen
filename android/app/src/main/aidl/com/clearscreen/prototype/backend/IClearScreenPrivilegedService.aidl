package com.clearscreen.prototype.backend;

interface IClearScreenPrivilegedService {
    void destroy() = 16777114;
    String inspectTargets() = 1;
    String readTarget(String id) = 2;
    String changeTarget(String id, String expected, String desired) = 3;
    String scanChannels() = 4;
    String readSetting(String namespace, String key) = 5;
    String writeSetting(String namespace, String key, String value) = 6;
    String blockedNotifications() = 7;
    String setAppNotification(String packageName, boolean enabled) = 8;
}
