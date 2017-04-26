//trace

private static int indent = 0;

static <T> boolean B(String s, NezParserContext<T> px) {
	for(int i = 0; i < indent; i++) System.out.print(" ");
	System.out.printf("%s => pos=%d, %s\n", s, px.pos, px.tree);
	indent++;
	return true;
}

static <T> boolean E(String s, NezParserContext<T> px, boolean r) {
	indent--;
	for(int i = 0; i < indent; i++) System.out.print(" ");
	System.out.printf("%s <= %s pos=%d, %s\n", s, r, px.pos, px.tree);
	return r;
}
