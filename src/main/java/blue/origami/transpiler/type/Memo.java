package blue.origami.transpiler.type;

import java.util.HashMap;

class Memo {
	static char NonChar = '%';
	static String NonStr = "%";

	static HashMap<String, Ty> memoMap = new HashMap<>();

	public static Ty t(String id) {
		return memoMap.get(id);
	}

	public static Ty memo(Ty ty) {
		String id = ty.keyMemo();
		if (id.indexOf(NonChar) >= 0) {
			return ty;
		}
		Ty ty2 = memoMap.get(id);
		if (ty2 == null) {
			int seq = memoMap.size() + 1;
			ty.typeId(seq);
			memoMap.put(id, ty);
			ty2 = ty;
		}
		return ty2;
	}
}
