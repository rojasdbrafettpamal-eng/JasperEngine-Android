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
    private final int targetIndex;
    private Scriptable scope;
    private boolean initialized;

    ScriptRuntime(String source, int targetIndex) {
        this.source = source == null ? "" : source;
        this.targetIndex = targetIndex;
    }

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
            Object objectsValue = ScriptableObject.getProperty(scene, "objetos");
            if (!(objectsValue instanceof Scriptable)) throw new EvaluatorException("No encontré la lista de objetos de la escena.");
            Object targetValue = ScriptableObject.getProperty((Scriptable) objectsValue, targetIndex);
            if (!(targetValue instanceof Scriptable)) throw new EvaluatorException("No encontré el objeto asociado a este código.");
            Scriptable target = (Scriptable) targetValue;
            ScriptableObject.putProperty(scope, "obj", target);
            if (!initialized) {
                call(context, "init", scene, target, 0f);
                initialized = true;
            }
            call(context, "update", scene, target, deltaSeconds);
            Object output = context.evaluateString(scope, "JSON.stringify(scene)", "scene-output.js", 1, null);
            return Context.toString(output);
        } finally {
            Context.exit();
        }
    }

    private void call(Context context, String name, Scriptable scene, Scriptable target, float dt) {
        Object candidate = ScriptableObject.getProperty(scope, name);
        if (!(candidate instanceof Function)) return;
        Object[] args = "init".equals(name)
                ? new Object[]{scene, target}
                : new Object[]{scene, dt, target};
        ((Function) candidate).call(context, scope, scope, args);
    }
}
