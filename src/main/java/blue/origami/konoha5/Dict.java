package blue.origami.konoha5;

import java.util.Map;
import java.util.TreeMap;

import blue.origami.konoha5.Func.FuncStrObj;
import blue.origami.util.StringCombinator;

public class Dict extends TreeMap<String, Object> implements FuncStrObj, StringCombinator {
	private static final long serialVersionUID = -827646422601520488L;
	private boolean isMutable;

	public Dict() {
		this(false);
	}

	public Dict(boolean isMutable) {
		this.isMutable = true;
	}

	@Override
	public Object apply(String key) {
		return this.get(key);
	}

	@Override
	public String toString() {
		return StringCombinator.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		int cnt = 0;
		sb.append(this.isMutable ? "{" : "[");
		for (Map.Entry<String, Object> e : this.entrySet()) {
			if (cnt > 0) {
				sb.append(", ");
			}
			StringCombinator.appendQuoted(sb, e.getKey());
			sb.append(": ");
			StringCombinator.append(sb, e.getValue());
			cnt++;
		}
		sb.append(this.isMutable ? "}" : "]");
	}

	public Object geti(String key) {
		return this.getOrDefault(key, null);
	}

	public void seti(String key, Object value) {
		this.put(key, value);
	}

}