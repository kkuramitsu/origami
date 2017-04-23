
	// Tree Construction

	public static class KeyValueTree {
		public String key;
		public Object value;

		KeyValueTree(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			this.strOut(sb);
			return sb.toString();
		}

		private void strOut(StringBuilder sb) {
			sb.append("[#");
			sb.append(this.key == null ? "" : this.key);
			if (this.value instanceof KeyValueTree[]) {
				KeyValueTree[] sub = (KeyValueTree[]) this.value;
				for (KeyValueTree child : sub) {
					sb.append(" ");
					if (child.key != null) {
						sb.append("$" + child.key + "=");
					}
					((KeyValueTree) child.value).strOut(sb);
				}
			} else {
				sb.append(" '");
				sb.append(this.value);
				sb.append("'");
			}
			sb.append("]");
		}
	}

	static String readInputs(String[] a) {
		StringBuilder sb = new StringBuilder();
		if (a.length > 0) {
			sb.append(a[0]);
		} else {
			Scanner console = new Scanner(System.in);
			String s = console.nextLine();
			while (s != null) {
				sb.append(s);
				s = console.nextLine();
			}
			console.close();
		}
		return sb.toString();
	}

	final static int MEMOSIZE = 0;
	
	static NezParserContext<KeyValueTree> getSampleParserContext(String[] a) {
		NewFunc<KeyValueTree> f = (String tag, byte[] inputs, int pos, int len, int size, String value) -> {
			if (size == 0) {
				return new KeyValueTree(tag, value != null ? value : new String(inputs, pos, len));
			}
			return new KeyValueTree(tag, new KeyValueTree[size]);
		};
		SetFunc<KeyValueTree> f2 = (KeyValueTree parent, int n, String label, KeyValueTree child) -> {
			KeyValueTree[] childs = (KeyValueTree[]) parent.value;
			childs[n] = new KeyValueTree(label, child);
			return parent;
		};
		return new NezParserContext<>(readInputs(a), MEMOSIZE, f, f2);
	}
	
	public final static void main(String[] args) {
		NezParserContext<KeyValueTree> px = getSampleParserContext(args);
		e0(px);  // start point
		System.out.println(px.tree);
	}

	public static <T> T parse(String inputs, NewFunc<T> newFunc, SetFunc<T> setFunc) {
		NezParserContext<T> px = new NezParserContext<>(inputs, MEMOSIZE, newFunc, setFunc);
		e0(px);
		return px.tree;
	}

