package blue.origami.transpiler.type;

import java.util.HashMap;

public class TypeMemo {
	static char NonMemo = '%';
	static HashMap<String, Ty> memoMap = new HashMap<>();

	public static Ty memo(Ty ty) {
		String id = ty.toString();
		if (id.indexOf(NonMemo) > 0) {
			return ty;
		}
		Ty ty2 = memoMap.get(id);
		if (ty2 == null) {
			int seq = memoMap.size();
			ty.typeId(seq);
			memoMap.put(id, ty);
			ty2 = ty;
		}
		return ty2;
	}
}
