static boolean[] B(String s) {
  boolean[] b = new boolean[256];
  for (int i = 0; i < s.length(); i++) {
    if (s.charAt(i) == 'T' || s.charAt(i) == '1') {
      b[i] = true;
    }
  }
  return b;
}

static short[] I(String s) {
  byte[] b = Base64.getDecoder().decode(s.getBytes());
  short[] b2 = new short[256];
  for (int i = 0; i < b.length; i++) {
    b2[i] = (short)(b[i] & 0xff);
  }
  return b2;
}

static byte[] B64(String s) {
  return Base64.getDecoder().decode(s.getBytes());
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

private final static boolean bitis(int[] bits, int n) {
	return (bits[n / 32] & (1 << (n % 32))) != 0;
}

private static final byte[] emptyValue = new byte[0];

private static byte[] extract(NezParserContext px, int ppos) {
  if(px.pos == ppos) {
    return emptyValue;
  }
  byte[] b = new byte[px.pos - ppos];
  System.arraycopy(px.inputs, ppos, b, 0, b.length);
  return b;
}

private static boolean matchBytes(NezParserContext px, byte[] t) {
  if (px.pos + t.length <= px.length) {
    for (int i = 0; i < t.length; i++) {
      if (t[i] != px.inputs[px.pos + i]) {
        return false;
      }
    }
    px.pos += t.length;
    return true;
  }
  return false;
}


