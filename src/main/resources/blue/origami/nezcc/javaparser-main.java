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

  static String readInputs(String a) throws IOException {
    File file = new File(a);
    if(file.exists()) {
      StringBuilder sb = new StringBuilder();
      BufferedReader br = new BufferedReader(new FileReader(file));
      String str = br.readLine();
      while(str != null){
        sb.append(str);
        sb.append("\n");
        str = br.readLine();
      }
      br.close();
      return sb.toString();
    }
    return a;
  }
  
  public final static void main(String[] args) throws IOException {
    for(String a: args) {
      String s = readInputs(a);
      long st = System.nanoTime();
      Object t = parse(a);
      long et = System.nanoTime();
      System.err.printf("%s %f[ms]: ", a, (et-st)/1000000.0);
      System.out.print(t);
      System.out.flush();
      System.err.printf("\n");
    }
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
    NezParserContext px = new NezParserContext(inputs, inputs.length-1, new TreeLog(null), newFunc, setFunc);
    initMemo(px);
    e0(px);
    return px.tree;
  }

