	// simple tree representation 

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

	static String readInputs(String[] a) throws IOException {
		StringBuilder sb = new StringBuilder();
		if (a.length > 0) {
			File file = new File(a[0]);
			if(file.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String str = br.readLine();
				while(str != null){
					sb.append(str);
					sb.append("\n");
					str = br.readLine();
				}
				br.close();
			}
			else {
				sb.append(a[0]);
			}
		} else {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String str = br.readLine();
			while(str != null){
				sb.append(str);
				sb.append("\n");
				str = br.readLine();
			}
		}
		return sb.toString();
	}
	
	public final static void main(String[] args) throws IOException {
		System.out.println(parse(readInputs(args)));
	}
	
	public static KeyValueTree parse(String s) {
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
		NezParserContext<KeyValueTree> px = new NezParserContext<>(s, MEMOSIZE, f, f2);
		e0(px);
		return px.tree;
	}

	public static <T> T parse(String inputs, NewFunc<T> newFunc, SetFunc<T> setFunc) {
		NezParserContext<T> px = new NezParserContext<>(inputs, MEMOSIZE, newFunc, setFunc);
		e0(px);
		return px.tree;
	}

