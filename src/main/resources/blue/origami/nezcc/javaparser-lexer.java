/* Lexer */

static final <T> boolean pOption(NezParserContext<T> px, int uchar) {
	if (px.getbyte() == uchar) {
		px.move(1);
	}
	return true;
}

static final <T> boolean pOption(NezParserContext<T> px, boolean bools[]) {
	if (bools[px.getbyte()]) {
		px.move(1);
	}
	return true;
}

static final <T> boolean pOption(NezParserContext<T> px, byte[] text) {
	px.matchBytes(text);
	return true;
}

static final <T> boolean pMany(NezParserContext<T> px, int uchar) {
	while (px.getbyte() == uchar) {
		px.move(1);
	}
	return true;
}

static final <T> boolean pMany(NezParserContext<T> px, boolean bools[]) {
	while (bools[px.getbyte()]) {
		px.move(1);
	}
	return true;
}

static final <T> boolean pMany(NezParserContext<T> px, byte[] text) {
	while (px.matchBytes(text)) {
	}
	return true;
}

static final <T> boolean pAnd(NezParserContext<T> px, int uchar) {
	return (px.getbyte() == uchar);
}

static final <T> boolean pAnd(NezParserContext<T> px, boolean bools[]) {
	return bools[px.getbyte()];
}

static final <T> boolean pAnd(NezParserContext<T> px, byte[] text) {
	int pos = px.pos;
	boolean b = px.matchBytes(text);
	if (b) {
		px.pos = pos;
	}
	return b;
}

static final <T> boolean pNot(NezParserContext<T> px, int uchar) {
	return (px.getbyte() != uchar);
}

static final <T> boolean pNot(NezParserContext<T> px, boolean bools[]) {
	return !bools[px.getbyte()];
}

static final <T> boolean pNot(NezParserContext<T> px, byte[] text) {
	return !px.matchBytes(text);
}
