	// simple tree representation 

	public static class SimpleTree {
		public String key;
		public Object value;

		SimpleTree(String key, Object value) {
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
			if (this.value instanceof SimpleTree[]) {
				SimpleTree[] sub = (SimpleTree[]) this.value;
				for (SimpleTree child : sub) {
					sb.append(" ");
					if (child.key != null) {
						sb.append("$" + child.key + "=");
					}
					((SimpleTree) child.value).strOut(sb);
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
	
	public static Object parse(String s) throws IOException {
		TreeFunc f = (String tag, byte[] inputs, int pos, int epos, int size) -> {
			if (size == 0) {
				return new SimpleTree(tag, new String(inputs, pos, epos - pos));
			}
			return new SimpleTree(tag, new SimpleTree[size]);
		};
		TreeSetFunc f2 = (Object parent, int n, String label, Object child) -> {
			SimpleTree[] childs = (SimpleTree[]) ((SimpleTree)parent).value;
			childs[n] = new SimpleTree(label, child);
			return parent;
		};
		return parse(s, f, f2);
	}

	public static Object parse(String s, TreeFunc newFunc, TreeSetFunc setFunc) throws IOException {
		byte[] inputs = (s + "\0").getBytes(Charset.forName("UTF-8"));
		NezParserContext px = new NezParserContext(inputs, inputs.length-1, newFunc, setFunc);
		initMemo(px);
		e0(px);
		return px.tree;
	}

