package util;

import java.util.HashMap;

public class StateManager {
	private static final HashMap<String, Object> state = new HashMap<String, Object>();
	
	public static void setItem(String key, Object obj) {
		if(state.containsKey(key))
			state.remove(key);
		state.put(key, obj);
	}

	public static void deleteItem(String key) {
		state.remove(key);
	}

	public static Object getItem(String key) {
		if(state.containsKey(key))
			return state.get(key);
		return null;
	}
}
