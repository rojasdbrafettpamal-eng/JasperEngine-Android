package com.jaspergameengine.editorprototype;

import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/** Small, isolated JavaScript runner for the Android prototype's preview only. */
final class ScriptRuntime {
    private static final ContextFactory FACTORY = new ContextFactory() {
        @Override protected Context makeContext() {
            Context context = super.makeContext();
            context.setLanguageVersion(Context.VERSION_ES6);
            context.setOptimizationLevel(-1);
            context.setInstructionObserverThreshold(100000);
            context.setClassShutter(className -> false); // scripts cannot call Java/Android classes
            return context;
        }
        @Override protected void observeInstructionCount(Context context, int count) {
            throw new EvaluatorException("El script tardó demasiado y se detuvo por seguridad.");
        }
    };

    private final String source;
    private Scriptable scope;
    private boolean initialized;

    ScriptRuntime(String source) { this.source = source == null ? "" : source; }

    String update(String sceneJson, float deltaSeconds) {
        Context context = FACTORY.enterContext();
        try {
            if (scope == null) {
                scope = context.initStandardObjects();
                context.evaluateString(scope, source, "project-script.js", 1, null);
            }
            Scriptable scene = (Scriptable) context.evaluateString(scope,
                    "JSON.parse(" + JSONObject.quote(sceneJson) + ")", "scene-state.js", 1, null);
            ScriptableObject.putProperty(scope, "scene", scene);
            if (!initialized) {
                call(context, "init", scene, 0f);
                initialized = true;
            }
            call(context, "update", scene, deltaSeconds);
            Object output = context.evaluateString(scope, "JSON.stringify(scene)", "scene-output.js", 1, null);
            return Context.toString(output);
        } finally {
            Context.exit();
        }
    }

    private void call(Context context, String name, Scriptable scene, float dt) {
        Object candidate = ScriptableObject.getProperty(scope, name);
        if (!(candidate instanceof Function)) return;
        ((Function) candidate).call(context, scope, scope, new Object[]{scene, dt});
    }
}
