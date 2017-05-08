static boolean[] B(String s) {
  boolean[] b = new boolean[256];
  for (int i = 0; i < s.length(); i++) {
    if (s.charAt(i) == 'T' || s.charAt(i) == '1') {
      b[i] = true;
    }
  }
  return b;
}

static int[] I(String s) {
  int[] b = new int[256];
  for (int i = 0; i < s.length(); i++) {
    char c = s.charAt(i);
    if (c > '0' && c <= '9') {
      b[i] = c - '0';
    } else if (c >= 'A' && c <= 'Z') {
      b[i] = (c - 'A') + 10;
    }
  }
  return b;
}

static byte[] T(String s) {
  int len = s.length();
  for (int i = 0; i < s.length(); i++) {
    char c = s.charAt(i);
    if(c == '~') len -= 2;
  }  
  byte[] b = new byte [len];
  int p = 0;
  for (int i = 0; i < s.length(); i++) {
    char c = s.charAt(i);
    if(c != '~') {
      b[p] = (byte)c;
    }
    else {
      String hex2 = s.substring(i+1, i+3);
      b[p] = (byte)Integer.parseInt(hex2, 16);
      i+=2;
    }
    p++;
  }
  //System.out.println("DEBUG " + s + " => " + new String(b));
  return b;
}

private static int indent = 0;

static boolean B(String s, NezParserContext px) {
  for(int i = 0; i < indent; i++) System.out.print(" ");
  System.out.printf("%s => pos=%d, %s\n", s, px.pos, px.tree);
  indent++;
  return true;
}

static boolean E(String s, NezParserContext px, boolean r) {
  indent--;
  for(int i = 0; i < indent; i++) System.out.print(" ");
  System.out.printf("%s <= %s pos=%d, %s\n", s, r, px.pos, px.tree);
  return r;
}

private static final byte[] emptyValue = new byte[0];

static byte[] extract(NezParserContext px, int ppos) {
  if(px.pos == ppos) {
    return emptyValue;
  }
  byte[] b = new byte[px.pos - ppos];
  System.arraycopy(px.inputs, ppos, b, 0, b.length);
  return b;
}

static boolean matchBytes(NezParserContext px, byte[] t, int len) {
  if (px.pos + len <= px.length) {
    for (int i = 0; i < len; i++) {
      if (t[i] != px.inputs[px.pos + i]) {
        return false;
      }
    }
    px.pos += len;
    return true;
  }
  return false;
}


