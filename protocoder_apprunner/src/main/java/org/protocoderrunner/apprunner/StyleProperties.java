package org.protocoderrunner.apprunner; /**
 *
 */

import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author
 *
 */
public class StyleProperties implements Scriptable, Map<String,Object> {

    private static final java.lang.String TAG = StyleProperties.class.getSimpleName();
    private HashMap<String, Object> values = new HashMap<>();
    private OnChangeListener changeListener;
    public boolean eventOnChange = true;

    public StyleProperties() {

    }

    @Override
    public String getClassName() {
        return "Object";
    }

    @Override
    public Object get(String name, Scriptable start) {
        // MLog.d(TAG, "get 1: " + name + " " + values.get(name));

        return values.get(name);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        // MLog.d(TAG, "put 1: " + name + " : " + value + " " + changeListener);
        values.put(name, value);

        if (changeListener != null && eventOnChange) changeListener.event(name, value);
    }

    @Override
    public Object get(int index, Scriptable start) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean has(String name, Scriptable start) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean has(int index, Scriptable start) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(int index) {
        // TODO Auto-generated method stub

    }

    @Override
    public Scriptable getPrototype() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        // TODO Auto-generated method stub

    }

    @Override
    public Scriptable getParentScope() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setParentScope(Scriptable parent) {
        // TODO Auto-generated method stub

    }

    @Override
    public Object[] getIds() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values.containsKey(value);
    }

    @Override
    public Object get(Object key) {
        // MLog.d(TAG, "get 2: " + key + " " + values.get(key));
        return values.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        // MLog.d(TAG, "put 2: " + key + " " + values.get(key));
        values.put(key, value);
        
        return value;
    }

    @Override
    public Object remove(Object key) {
        return values.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        values.putAll(m);
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public Set<String> keySet() {
        return values.keySet();
    }

    @Override
    public Collection<Object> values() {
        return values.values();
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

    private static Scriptable globalPrototype;

    public static void finishInit(Scriptable scope, FunctionObject constructor, Scriptable prototype) {
        System.out.println("finishInit is called.");
        globalPrototype = prototype;
    }

    public void onChange(OnChangeListener listener) {
        changeListener = listener;
    }

    public interface OnChangeListener {
        void event(String name, Object value);
    }
}