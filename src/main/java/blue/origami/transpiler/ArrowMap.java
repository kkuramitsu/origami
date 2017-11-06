package blue.origami.transpiler;

public class ArrowMap extends CodeMap {
	CodeMap[] maps;

	public ArrowMap(String key, CodeMap... maps) {
		super(max(maps), key, key, maps[maps.length - 1].getReturnType(), maps[0].getParamTypes());
		this.maps = maps;
	}

	static int max(CodeMap[] maps) {
		int max = 0;
		for (CodeMap cmap : maps) {
			int m = cmap.mapCost();
			if (m > max) {
				max = m;
			}
		}
		return max;
	}

}