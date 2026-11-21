package com.jaspergameengine.editorprototype;

/** One named JavaScript behaviour attached to a prototype scene object. */
final class SceneScript {
    String name;
    String source;
    SceneScript(String name, String source) {
        this.name = name == null ? "Script.js" : name;
        this.source = source == null ? "" : source;
    }
}
