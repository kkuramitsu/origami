import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Objects;
import java.util.ArrayList;
import java.io.*;
final class math {
  static byte[] B(String s) {
    return Base64.getDecoder().decode(s.getBytes());
  }
  static boolean[] B256(String s) {
    boolean[] b = new boolean[256];
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == 'T' || s.charAt(i) == '1') {
        b[i] = true;
      }
    }
    return b;
  }
  public static class TList<T> {
    String label;
    T tree;
    TList<T> next;
    TList(String label, T tree, TList<T> next) {
      this.label = label;
      this.tree = tree;
      this.next = next;
    }
  }
// *** const ***
private static final int MEMOSIZE = 4;
private static final int MEMOS = 257;
private static final boolean[] charset149 = B256("000000000100000000000000000000001");
private static final byte[] choice150 = B("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==");
private static final byte[] choice152 = B("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAgAAAAADAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==");
private static final boolean[] charset155 = B256("0000000000000000000000000000000000000000000000001111111111");
private static final byte[] choice156 = B("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAgICAgICAgICAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==");
private static final String[] SYMBOLs = {"","right","left","AddExpr","SubExpr","ModExpr","MulExpr","DivExpr","IntExpr","error"};
private static final int ParseError = 9;
private static final byte[] data157 = {};
private static final byte[][] VALUEs = {data157};
private static final int[] LENGTHs = {0};
// *** libs ***
static class TreeLog <T> {
  int op;
  int log;
  T tree;
  TreeLog<T> prevLog;
  TreeLog<T> nextLog;
  TreeLog(int op,int log,T tree,TreeLog<T> prevLog,TreeLog<T> nextLog) {
    this.op = op;
    this.log = log;
    this.tree = tree;
    this.prevLog = prevLog;
    this.nextLog = nextLog;
  }
}
public interface TreeFunc <T> {
  public T apply(String tag,byte[] inputs,int spos,int epos,int n);
}
public interface TreeSetFunc <T> {
  public T apply(T tree,int n,String label,T child);
}
static class State <T> {
  int ntag;
  int cnt;
  byte[] value;
  State<T> prevState;
  State(int ntag,int cnt,byte[] value,State<T> prevState) {
    this.ntag = ntag;
    this.cnt = cnt;
    this.value = value;
    this.prevState = prevState;
  }
}
static class MemoEntry <T> {
  long key;
  int result;
  int pos;
  T tree;
  State<T> state;
  MemoEntry(long key,int result,int pos,T tree,State<T> state) {
    this.key = key;
    this.result = result;
    this.pos = pos;
    this.tree = tree;
    this.state = state;
  }
}
static class NezParserContext <T> {
  byte[] inputs;
  int length;
  int pos;
  int headpos;
  T tree;
  TreeLog<T> treeLog;
  TreeFunc<T> newFunc;
  TreeSetFunc<T> setFunc;
  State<T> state;
  ArrayList<MemoEntry<T>> memos;
  NezParserContext(byte[] inputs,int length,int pos,int headpos,T tree,TreeLog<T> treeLog,TreeFunc<T> newFunc,TreeSetFunc<T> setFunc,State<T> state,ArrayList<MemoEntry<T>> memos) {
    this.inputs = inputs;
    this.length = length;
    this.pos = pos;
    this.headpos = headpos;
    this.tree = tree;
    this.treeLog = treeLog;
    this.newFunc = newFunc;
    this.setFunc = setFunc;
    this.state = state;
    this.memos = memos;
  }
}
private static final <T> ArrayList<MemoEntry<T>> newMemos(T tree,int length) {
  ArrayList<MemoEntry<T>> memos = new ArrayList<>();
  int cnt = 0;
  while(cnt < length) {
    memos.add(new MemoEntry<>(-1,0,0,tree,null));
    cnt = cnt + 1;
  }
  return memos;
}
private static final <T> boolean neof(NezParserContext<T> px) {
  return px.pos < px.length;
}
private static final <T> boolean move(NezParserContext<T> px,int shift) {
  px.pos = px.pos + shift;
  return true;
}
private static final <T> boolean back1(NezParserContext<T> px,int pos) {
  px.pos = pos;
  return true;
}
public interface ParserFunc <T> {
  public boolean apply(NezParserContext<T> px);
}
private static final <T> boolean many1(NezParserContext<T> px,ParserFunc<T> f) {
  int pos = px.pos;
  while(f.apply(px)) {
    pos = px.pos;
  }
  return back1(px,pos);
}
private static final <T> long longkey(long key,int memoPoint) {
  return key * 64 + memoPoint;
}
private static final <T> MemoEntry<T> getMemo(NezParserContext<T> px,long key) {
  return px.memos.get((int)(key % 257));
}
private static final <T> int consumeM2(NezParserContext<T> px,MemoEntry<T> m) {
  px.pos = m.pos;
  px.tree = m.tree;
  return m.result;
}
private static final <T> int lookupM2(NezParserContext<T> px,int memoPoint) {
  long key = longkey(px.pos,memoPoint);
  MemoEntry<T> m = getMemo(px,key);
  return (m.key == key) ? (consumeM2(px,m)) : (2);
}
private static final <T> boolean storeM(NezParserContext<T> px,int memoPoint,int pos,boolean matched) {
  long key = longkey(pos,memoPoint);
  MemoEntry<T> m = getMemo(px,key);
  m.key = key;
  m.result = (matched) ? (1) : (0);
  m.pos = (matched) ? (px.pos) : (pos);
  m.tree = px.tree;
  return matched;
}
private static final <T> boolean memo2(NezParserContext<T> px,int memoPoint,ParserFunc<T> f) {
  int pos = px.pos;
  switch(lookupM2(px,memoPoint)) {
    case 0 : return false;
    case 1 : return true;
    case 2 : return storeM(px,memoPoint,pos,f.apply(px));
  }
  return false;
}
private static final <T> TreeLog<T> useTreeLog(NezParserContext<T> px) {
  TreeLog<T> tcur = px.treeLog;
  if(tcur.nextLog == null) {
    tcur.nextLog = new TreeLog<>(0,0,px.tree,px.treeLog,null);
  }
  return tcur.nextLog;
}
private static final <T> boolean logT(NezParserContext<T> px,int op,int log,T tree) {
  TreeLog<T> tcur = useTreeLog(px);
  tcur.op = op;
  tcur.log = log;
  tcur.tree = tree;
  px.treeLog = tcur;
  return true;
}
private static final <T> boolean linkT(NezParserContext<T> px,int nlabel) {
  return logT(px,3,nlabel,px.tree);
}
private static final <T> boolean backLink(NezParserContext<T> px,TreeLog<T> treeLog,int nlabel,T tree) {
  px.treeLog = treeLog;
  linkT(px,nlabel);
  px.tree = tree;
  return true;
}
private static final <T> boolean link2(NezParserContext<T> px,int nlabel,ParserFunc<T> f) {
  TreeLog<T> treeLog = px.treeLog;
  T tree = px.tree;
  return f.apply(px) && backLink(px,treeLog,nlabel,tree);
}
private static final <T> String gettag(int ntag) {
  return SYMBOLs[ntag];
}
private static final <T> String getlabel(int nlabel) {
  return SYMBOLs[nlabel];
}
private static final <T> byte[] getvalue(int nvalue) {
  return VALUEs[nvalue];
}
private static final <T> int getlength(int nvalue) {
  return LENGTHs[nvalue];
}
private static final <T> boolean endT(NezParserContext<T> px,int shift,int ntag0) {
  int epos = px.pos + shift;
  TreeLog<T> tcur = px.treeLog;
  int ntag = ntag0;
  int nvalue = 0;
  int cnt = 0;
  while(tcur.op != 0) {
    if(tcur.op == 3) {
      cnt = cnt + 1;
    }
    else if(ntag == 0 && tcur.op == 1) {
      ntag = tcur.log;
    }
    else if(nvalue == 0 && tcur.op == 2) {
      nvalue = tcur.log;
    }
    tcur = tcur.prevLog;
  }
  px.tree = (nvalue == 0) ? (px.newFunc.apply(gettag(ntag),px.inputs,tcur.log,epos,cnt)) : (px.newFunc.apply(gettag(ntag),getvalue(nvalue),0,getlength(nvalue),cnt));
  tcur = px.treeLog;
  while(tcur.op != 0) {
    if(tcur.op == 3) {
      cnt = cnt - 1;
      px.tree = px.setFunc.apply(px.tree,cnt,getlabel(tcur.log),tcur.tree);
    }
    tcur = tcur.prevLog;
  }
  px.treeLog = tcur.prevLog;
  return true;
}
private static final <T> boolean beginT(NezParserContext<T> px,int shift) {
  return logT(px,0,px.pos + shift,px.tree);
}
private static final <T> boolean foldT(NezParserContext<T> px,int shift,int nlabel) {
  return beginT(px,shift) && linkT(px,nlabel);
}
private static final <T> boolean back3(NezParserContext<T> px,int pos,TreeLog<T> treeLog,T tree) {
  px.pos = pos;
  px.treeLog = treeLog;
  px.tree = tree;
  return true;
}
private static final <T> boolean many3(NezParserContext<T> px,ParserFunc<T> f) {
  int pos = px.pos;
  TreeLog<T> treeLog = px.treeLog;
  T tree = px.tree;
  while(f.apply(px)) {
    pos = px.pos;
    treeLog = px.treeLog;
    tree = px.tree;
  }
  return back3(px,pos,treeLog,tree);
}
private static final <T> boolean tagT(NezParserContext<T> px,int ntag) {
  return logT(px,1,ntag,px.tree);
}
private static final <T> int nextbyte(NezParserContext<T> px) {
  int c = px.inputs[px.pos] & 0xff;
  px.pos = px.pos + 1;
  return c;
}
private static final <T> boolean next1(NezParserContext<T> px,int c) {
  return nextbyte(px) == c;
}
private static final <T> int getbyte(NezParserContext<T> px) {
  return px.inputs[px.pos] & 0xff;
}
private static final <T> boolean many9(NezParserContext<T> px,ParserFunc<T> f) {
  int pos = px.pos;
  int cnt = 0;
  while(f.apply(px)) {
    pos = px.pos;
    cnt = cnt + 1;
  }
  return cnt > 0 && back1(px,pos);
}
// [\t ]*
private static final <T> boolean e8(NezParserContext<T> px) {
  return many1(px,(p0) -> charset149[nextbyte(p0)]);
}
// '%' [\t ]*
private static final <T> boolean t151(NezParserContext<T> px) {
  return next1(px,37) && e8(px);
}
// <switch '%'->math:"%" #ModExpr '*'->'*' [\t ]* #MulExpr '/'->'/' [\t ]* #DivExpr>
private static final <T> boolean e5(NezParserContext<T> px) {
  switch(choice152[getbyte(px)]) {
    case 0 : return false;
    case 1 : return t151(px) && tagT(px,5);
    case 2 : return next1(px,42) && e8(px) && tagT(px,6);
    case 3 : return next1(px,47) && e8(px) && tagT(px,7);
  }
  return false;
}
// [0-9]+
private static final <T> boolean e10(NezParserContext<T> px) {
  return many9(px,(p0) -> charset155[nextbyte(p0)]);
}
// ')' [\t ]*
private static final <T> boolean t154(NezParserContext<T> px) {
  return next1(px,41) && e8(px);
}
// '(' [\t ]*
private static final <T> boolean t153(NezParserContext<T> px) {
  return next1(px,40) && e8(px);
}
// <switch '('->math:"(" math:Expression math:")" [0-9]->{[0-9]+ #IntExpr } [\t ]*>
private static final <T> boolean e9(NezParserContext<T> px) {
  switch(choice156[getbyte(px)]) {
    case 0 : return false;
    case 1 : return t153(px) && math_Expression(px) && t154(px);
    case 2 : return beginT(px,0) && e10(px) && endT(px,0,8) && e8(px);
  }
  return false;
}
// <switch '('->math:"(" math:Expression math:")" [0-9]->{[0-9]+ #IntExpr } [\t ]*>
private static final <T> boolean math_Value(NezParserContext<T> px) {
  return memo2(px,3,math::e9);
}
// $right(math:Value)
private static final <T> boolean e6(NezParserContext<T> px) {
  return link2(px,1,math::math_Value);
}
// {$left <switch '%'->math:"%" #ModExpr '*'->'*' [\t ]* #MulExpr '/'->'/' [\t ]* #DivExpr> $right(math:Value) }*
private static final <T> boolean e7(NezParserContext<T> px) {
  return many3(px,(p0) -> foldT(p0,0,2) && e5(p0) && e6(p0) && endT(p0,0,0));
}
// math:Value {$left <switch '%'->math:"%" #ModExpr '*'->'*' [\t ]* #MulExpr '/'->'/' [\t ]* #DivExpr> $right(math:Value) }*
private static final <T> boolean math_Product(NezParserContext<T> px) {
  return memo2(px,2,(p1) -> math_Value(p1) && e7(p1));
}
// '+' [\t ]*
private static final <T> boolean t148(NezParserContext<T> px) {
  return next1(px,43) && e8(px);
}
// <switch '+'->math:"+" #AddExpr '-'->'-' [\t ]* #SubExpr>
private static final <T> boolean e2(NezParserContext<T> px) {
  switch(choice150[getbyte(px)]) {
    case 0 : return false;
    case 1 : return t148(px) && tagT(px,3);
    case 2 : return next1(px,45) && e8(px) && tagT(px,4);
  }
  return false;
}
// $right(math:Product)
private static final <T> boolean e3(NezParserContext<T> px) {
  return link2(px,1,math::math_Product);
}
// {$left <switch '+'->math:"+" #AddExpr '-'->'-' [\t ]* #SubExpr> $right(math:Product) }*
private static final <T> boolean e4(NezParserContext<T> px) {
  return many3(px,(p0) -> foldT(p0,0,2) && e2(p0) && e3(p0) && endT(p0,0,0));
}
// math:Product {$left <switch '+'->math:"+" #AddExpr '-'->'-' [\t ]* #SubExpr> $right(math:Product) }*
private static final <T> boolean math_Expression(NezParserContext<T> px) {
  return memo2(px,1,(p1) -> math_Product(p1) && e4(p1));
}
// .*
private static final <T> boolean e1(NezParserContext<T> px) {
  return many1(px,(p0) -> neof(p0) && move(p0,1));
}
// math:Expression .*
private static final <T> boolean e0(NezParserContext<T> px) {
  return math_Expression(px) && e1(px);
}
public static final <T> T parse(byte[] inputs,int length,TreeFunc<T> newFunc,TreeSetFunc<T> setFunc) {
  T tree = newFunc.apply(gettag(0),inputs,0,length,0);
  NezParserContext<T> px = new NezParserContext<>(inputs,length,0,0,tree,new TreeLog<>(0,0,tree,null,null),newFunc,setFunc,null,newMemos(tree,257));
  tree = (e0(px)) ? (px.tree) : (newFunc.apply(gettag(ParseError),inputs,px.headpos,length,0));
  return tree;
}
public static final <T> T parseText(String text,TreeFunc<T> newFunc,TreeSetFunc<T> setFunc) {
  byte[] inputs = text.getBytes(Charset.forName("UTF-8"));
  int length = inputs.length;
  return parse(inputs,length,newFunc,setFunc);
}
  /** Here is the main part. You may remove them if unnecessary. **/
  public static class SimpleTree {
    public String key;
    public Object value;

    SimpleTree(String key, Object value) {
      this.key = key;
      this.value = value;
    }

    SimpleTree set(int n, String label, SimpleTree child) {
    	((SimpleTree[])this.value)[n] = new SimpleTree(label, child);
    	return this;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      this.strOut(sb);
      return sb.toString();
    }

    private void strOut(StringBuilder sb) {
      sb.append("[#");
      sb.append(this.key);
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
  private static SimpleTree newAST(String tag, byte[] inputs, int pos, int epos, int size) {
    return new SimpleTree(tag, (size == 0) ? new String(inputs, pos, epos - pos) : new SimpleTree[size]);
    //return new SimpleTree(tag, new SimpleTree[size]);
    //return null;
  }
  private static SimpleTree subAST(SimpleTree parent, int n, String label, SimpleTree child) {
    SimpleTree[] childs = (SimpleTree[]) ((SimpleTree)parent).value;
    childs[n] = new SimpleTree(label, child);
    return parent;
  }
  static byte[] readInputs(String a) throws IOException {
    File file = new File(a);
    if(file.exists()) {
      byte[] buf = new byte[((int)file.length())+1];  // adding '\0' termination
      FileInputStream fin = new FileInputStream(file);
      fin.read(buf, 0, (int)file.length());
      return buf;
    }
    else {
      return (a + "\0").getBytes(Charset.forName("UTF-8"));
    }
  }
  
  public final static void main(String[] args) throws IOException {
    for(String a: args) {
      byte[] buf = readInputs(a);
      long st = System.nanoTime();
      SimpleTree t = parse(buf, buf.length-1, math::newAST, math::subAST);
      long et = System.nanoTime();
      System.err.printf("%s %f[ms]: ", a, (et-st)/1000000.0);
      System.out.print(t);
      System.out.flush();
      System.err.printf("\n");
    }
  }
}
