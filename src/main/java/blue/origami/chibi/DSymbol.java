package blue.origami.chibi;

import java.util.ArrayList;
import java.util.HashMap;

public class DSymbol {
	private static HashMap<String, DSymbol> tagIdMap = new HashMap<>();
	private static ArrayList<DSymbol> tagNameList = new ArrayList<>(64);

	public final static DSymbol unique(String s) {
		DSymbol tag = tagIdMap.get(s);
		if (tag == null) {
			tag = new DSymbol(tagIdMap.size(), s);
			tagIdMap.put(s, tag);
			tagNameList.add(tag);
		}
		return tag;
	}

	public final static DSymbol Null = unique("");

	public final static int id(String symbol) {
		return unique(symbol).id;
	}

	public final static DSymbol symbol(int id) {
		return tagNameList.get(id);
	}

	final int id;
	final String symbol;

	private DSymbol(int id, String symbol) {
		this.id = id;
		this.symbol = symbol;
	}

	@Override
	public final int hashCode() {
		return this.id;
	}

	@Override
	public final boolean equals(Object o) {
		return this == o;
	}

	public final int id() {
		return this.id;
	}

	@Override
	public String toString() {
		return this.symbol;
	}

}
